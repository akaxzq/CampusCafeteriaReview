package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @create 2023-12-04
 */
@Component
public class RedisIdWorker {
    private static final long BEGIN_TIMESTAMP=1640995200L;
    private static final int COUNT_BITS=32;   //位运算移位

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public long nextId(String keyPrefix){
//        1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowsecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowsecond-BEGIN_TIMESTAMP;
//        2.生成序列号
//        一个业务同一个key不合适，可能超过上限。解决方法：拼接个日期字符串
//        2.1获取当前日期
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
//        2.2自增长
        Long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);
//        3.拼接并返回
//        拼接不能用字符串直接拼，因为返回值是long，所以用数字拼接，借助位运算

        return timestamp<<COUNT_BITS|count;  //时间戳左移32位后，和count或运算（拼接效果）
    }

}
