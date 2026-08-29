package com.xytl.project.caipiaoapi.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RedisService {

    private static final ThreadLocal<Jedis> SESSION = new ThreadLocal<>();

    @Autowired
    JedisPool jedisPool;

    /**
     * 同一业务流程内复用一条 Redis 连接，避免每次 get/set 都借还连接。
     */
    public void beginSession() {
        if (SESSION.get() == null) {
            SESSION.set(jedisPool.getResource());
        }
    }

    public void endSession() {
        Jedis jedis = SESSION.get();
        if (jedis != null) {
            try {
                jedis.close();
            } finally {
                SESSION.remove();
            }
        }
    }

    private Jedis borrowJedis() {
        Jedis session = SESSION.get();
        if (session != null) {
            return session;
        }
        return jedisPool.getResource();
    }

    private void returnJedis(Jedis jedis) {
        if (SESSION.get() == null && jedis != null) {
            jedis.close();
        }
    }

    public void set(String key, String value) {
        Jedis jedis = borrowJedis();
        try {
            jedis.set(key, value);
        } finally {
            returnJedis(jedis);
        }
    }

    /**
     * 仅当 key 不存在时写入，并设置过期秒数
     */
    public boolean setIfAbsent(String key, String value, int expireSeconds) {
        Jedis jedis = borrowJedis();
        try {
            String result = jedis.set(key, value, "NX", "EX", expireSeconds);
            return "OK".equals(result);
        } finally {
            returnJedis(jedis);
        }
    }

    /**
     * 批量写入多个 key，单次网络往返
     */
    public void mset(Map<String, String> keyValues) {
        if (keyValues == null || keyValues.isEmpty()) {
            return;
        }
        Jedis jedis = borrowJedis();
        try {
            String[] keysValues = new String[keyValues.size() * 2];
            int index = 0;
            for (Map.Entry<String, String> entry : keyValues.entrySet()) {
                keysValues[index++] = entry.getKey();
                keysValues[index++] = entry.getValue();
            }
            jedis.mset(keysValues);
        } finally {
            returnJedis(jedis);
        }
    }

    public String get(String key) {
        Jedis jedis = borrowJedis();
        try {
            return jedis.get(key);
        } finally {
            returnJedis(jedis);
        }
    }

    public List<String> mget(String... keys) {
        if (keys == null || keys.length == 0) {
            return new ArrayList<>();
        }
        Jedis jedis = borrowJedis();
        try {
            return jedis.mget(keys);
        } finally {
            returnJedis(jedis);
        }
    }

    public void hmset(String key, HashMap<String, String> parammap) {
        if (key == null || parammap == null || parammap.isEmpty()) {
            return;
        }
        Jedis jedis = borrowJedis();
        try {
            for (Map.Entry<String, String> entry : parammap.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                jedis.hset(key, entry.getKey(), entry.getValue());
            }
        } finally {
            returnJedis(jedis);
        }
    }

    public void hdel(String key) {
        Jedis jedis = borrowJedis();
        try {
            jedis.hdel(key);
        } finally {
            returnJedis(jedis);
        }
    }

    public Map<String, String> hget(String key) {
        Jedis jedis = borrowJedis();
        try {
            return jedis.hgetAll(key);
        } finally {
            returnJedis(jedis);
        }
    }

    public void del(String key) {
        Jedis jedis = borrowJedis();
        try {
            jedis.del(key);
        } finally {
            returnJedis(jedis);
        }
    }

    public void getother(String keyname) {
        Jedis jedis = borrowJedis();
        try {
            Set<String> keys = jedis.keys(keyname + "*");
            for (String key : keys) {
                jedis.del(key);
            }
        } finally {
            returnJedis(jedis);
        }
    }

    public void delbyname(String keyname) {
        Jedis jedis = borrowJedis();
        try {
            Set<String> keys = jedis.keys(keyname);
            for (String key : keys) {
                jedis.del(key);
            }
        } finally {
            returnJedis(jedis);
        }
    }
}
