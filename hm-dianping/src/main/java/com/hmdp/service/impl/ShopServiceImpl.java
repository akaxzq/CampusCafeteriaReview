package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;
    @Override
    public Result queryById(Long id) {
//        缓存穿透
//        Shop shop = queryWithPassThrough(id);
//        工具类实现
//        Shop shop=cacheClient.queryWithPassThrough(CACHE_SHOP_KEY,id,Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);
//        互斥锁解决缓存击穿
//        Shop shop = queryWithMutex(id);
//        工具类实现
        Shop shop=cacheClient.queryWithPassThrough(CACHE_SHOP_KEY,id,Shop.class,this::getById,20L,TimeUnit.SECONDS);

//        逻辑过期解决
//        Shop shop = queryWithLogicalExpire(id);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

//    线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public Shop queryWithLogicalExpire(Long id){
        String key = CACHE_SHOP_KEY + id;
        //1.从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            //3.不存在则返回
            return null;
        }
//        4.命中，把json反序列化为对象
        RedisData redisData=JSONUtil.toBean(shopJson,RedisData.class);
        JSONObject date=(JSONObject) redisData.getData();
        Shop shop=JSONUtil.toBean(date,Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
//        5.判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
//        5.1 未过期，直接返回店铺信息
            return shop;

        }

//        5.2 已过期，缓存重建
//        6.缓存重建
//        6.1 获取互斥锁
        String lockKey=LOCK_SHOP_KEY+id;
        boolean isLock=tryLock(lockKey);
//        6.2 判断是否获取成功
        if (isLock) {
//            6.3 开启独立线程
            CACHE_REBUILD_EXECUTOR.submit(()->{
//                重建缓存
                try {
                    this.saveShop2Redis(id,20L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
//                释放锁
                    unLock(lockKey);                }

            });
        }
//        6.4 返回过期的商铺信息
        return shop;
    }

//    互斥锁
    public Shop queryWithMutex(Long id){
        String key = CACHE_SHOP_KEY + id;
        //1.从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            //3.存在则返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);

            return shop;
        }
//        判断命中的是否为空值
        if(shopJson!=null){

            return null;
        }
//        4.缓存重建
//        4.1 获取互斥锁
        String lockKey="lock:shop:"+id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
//        4.2 判断是否获取成功
            if(!isLock){
    //        4.3 失败，休眠重试
                Thread.sleep(50);
                queryWithMutex(id);
            }

//        4.4 成功，根据id查询
            shop = getById(id);
//          模拟重建延时
            Thread.sleep(200);
            //5.不存在，返回错误
            if(shop==null){
    //            空值写入redis
                stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);

    //            return Result.fail("店铺不存在！");
                return null;
            }
            //6.存在，写入redis
            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            //7.释放互斥锁
            unLock(lockKey);
        }

//        8.返回
        return shop;
    }
    public Shop queryWithPassThrough(Long id){
        String key = CACHE_SHOP_KEY + id;
        //1.从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            //3.存在则返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
//            return Result.ok(shop);
            return shop;
        }
//        判断命中的是否为空值
        if(shopJson!=null){
//            返回错误信息
//            return Result.fail("店铺信息不存在");
            return null;
        }
        //4.不存在，根据id查询数据库
        Shop shop = getById(id);
        //5.不存在，返回错误
        if(shop==null){
//            空值写入redis
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);

//            return Result.fail("店铺不存在！");
            return null;
        }
        //6.存在，写入redis
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //7.返回
//        return Result.ok(shop);
        return shop;
    }

//  尝试获取锁
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
//        转基本类型返回
        return BooleanUtil.isTrue(flag);
    }
    //  释放锁
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }
//    逻辑过期
    public void saveShop2Redis(Long id,Long expireSeconds){
//        1.查询店铺数据
        Shop shop=getById(id);
//        2.封装逻辑过期时间
        RedisData redisData=new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
//        3.写入Redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }

    @Override
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id==null){
            return Result.fail("店铺id不能为空");
        }
//        1.更新数据库
        updateById(shop);
//        2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY+ id);
        return Result.ok();
    }
}
