package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @create 2023-06-05
 */
public class LoginInterceptor implements HandlerInterceptor{
    //不能自动注入，因为这个类不是Spring创建的，而是手动的
    private StringRedisTemplate stringRedisTemplate;

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    public LoginInterceptor() {

    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception{
//        1.判断是否需要拦截（threadlocal中是否有用户）
        if(UserHolder.getUser()==null){
            response.setStatus(401);
            return false;
        }
        return true;
    }
//    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
////        1.获取请求头中的token（前段代码把token放入）
//        String token=request.getHeader("authorization");
//        if(StrUtil.isBlank(token)){
//            response.setStatus(401);
//            return false;
//        }
////        2.基于token获取redis中的用户
//        String key = RedisConstants.LOGIN_USER_KEY + token;
//        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
////        3.判断用户是否存在
//        if(userMap.isEmpty()){
////            上面entries方法判断为null则返回空map，所以只需判断是否为空
//
////            4.不存在，拦截
//            response.setStatus(401);
//            return false;
//        }
////        5.将查询Hash数据转化UserDTO对象
//        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
////        6.存在，保存用户信息到threadlocal
//        UserHolder.saveUser(userDTO);
////        7.刷新token有效期
//        stringRedisTemplate.expire(key,RedisConstants.LOGIN_USER_TTL,TimeUnit.MINUTES);
//        return true;
//    }
}

