package com.xytl.project.caipiaoapi.utils.json;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

/**
 * redis的中间类
 */
@Component
@Configurable
public class RedisUtil {


    public static Jedis jedis=null;

    @Bean
    public Jedis initJedis(@Value("${spring.redis.host}")String host, @Value("${spring.redis.port}")int port,@Value("${spring.redis.password}")String passwd){
        jedis = new Jedis(host, port);
        if(StringUtils.isNotBlank(passwd)){
            jedis.auth(passwd);
        }
        return jedis;
    }


    /**
     * 连接到Redis服务器
     */
    public static void connect() {
        jedis.connect();
    }


    /**
     * 断开与Redis服务器的连接
     */
    public static void disconnect() {
        jedis.disconnect();
    }

    /**
     * 设置键值对
     *
     * @param key 键
     * @param value 值
     */
    public static void set(String key, String value) {
        jedis.set(key, value);
    }

    /**
     * 获取键对应的值
     *
     * @param key 键
     * @return 值
     */
    public static String get(String key) {
        return jedis.get(key);
    }

    /**
     * 删除键
     *
     * @param key 键
     */
    public static void del(String key) {
        jedis.del(key);
    }

    /**
     * 更新键对应的值
     *
     * @param key 键
     * @param value 新值
     */
    public static void update(String key, String value) {
        jedis.set(key, value);
    }

    /**
     * 检查键是否存在
     *
     * @param key 键
     * @return 存在返回true，否则返回false
     */
    public static boolean exists(String key) {
        return jedis.exists(key);
    }

    /**
     * 获取Redis服务器的PING响应
     *
     * @return PONG表示连接正常
     */
    public static String ping() {
        return jedis.ping();
    }

    /**
     * 关闭Redis连接
     */
    public static void close() {
        jedis.close();
    }


    public static void main(String[] args) {
        RedisUtil.connect();
        RedisUtil.ping();

        RedisUtil.set("key1", "value1");
        System.out.println("Get key1: " + RedisUtil.get("key1"));
        RedisUtil.update("key1", "new_value1");
        System.out.println("Updated key1: " + RedisUtil.get("key1"));
        RedisUtil.del("key1");
        System.out.println("After delete, key1: " + RedisUtil.get("key1"));
        RedisUtil.close();
    }

}
