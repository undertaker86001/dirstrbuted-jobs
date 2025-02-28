package com.sucheon.jobs.utils;

import com.github.jesse.l2cache.Cache;
import com.github.jesse.l2cache.CacheConfig;
import com.github.jesse.l2cache.CacheSyncPolicy;
import com.github.jesse.l2cache.sync.CacheMessageListener;
import com.github.jesse.l2cache.sync.RedisCacheSyncPolicy;
import com.github.jesse.l2cache.builder.CompositeCacheBuilder;
import com.github.jesse.l2cache.cache.expire.DefaultCacheExpiredListener;

import org.redisson.Redisson;
import com.sucheon.jobs.utils.RetryUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class CacheUtils {

    /**
     * 维护需要缓存映射,避免函数调用的时候重复申请cache实例
     */
    private static Map<String, Cache> virtualCacheMapping = new HashMap<>();


    /**
     * 根据redis字典构建缓存实例
     * @param cacheName
     * @return
     */
    public static Cache compositeCache(String cacheInstance, CacheConfig cacheConfig){

        if (Objects.isNull(virtualCacheMapping.get(cacheInstance))) {

            CacheSyncPolicy cacheSyncPolicy = new RedisCacheSyncPolicy()
                    .setCacheConfig(cacheConfig)
                    .setCacheMessageListener(new CacheMessageListener(cacheConfig.getInstanceId()))
                    .setActualClient(Redisson.create(cacheConfig.getRedis().getRedissonConfig()));

            //重试如果发生异常 redis是否可以连的上
            RetryUtils.executeWithRetry(() -> {cacheSyncPolicy.connnect(); return "invoked";}, 3, 2000, false);


            Cache cache = new CompositeCacheBuilder()
                    .setCacheConfig(cacheConfig)
                    .setExpiredListener(new DefaultCacheExpiredListener())
                    .setCacheSyncPolicy(cacheSyncPolicy)
                    .build(cacheInstance);
            virtualCacheMapping.put(cacheInstance, cache);
            return cache;
        }else {
            Cache cache = virtualCacheMapping.get(cacheInstance);

            return cache;
        }
    }



}
