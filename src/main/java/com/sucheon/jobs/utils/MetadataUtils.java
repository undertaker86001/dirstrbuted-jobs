package com.sucheon.jobs.utils;

import com.sucheon.jobs.constant.CacheConstant;
import com.sucheon.jobs.constant.HttpConstant;
import com.sucheon.jobs.constant.RedisConstant;

import java.util.HashMap;
import java.util.Map;

public class MetadataUtils {

    /**
     * 组装redis订阅和调用对应http接口更新事件
     * @return
     */
    public static Map<String, String> assembleChannelAndHttpUrl (){


        //todo 由于一个channel可能会发送两种不同的事件，所以需要抽象成二级缓存
        return new HashMap<String, String>() {
            { put(RedisConstant.IOT_NOT_STORE_FIELDS, HttpConstant.iotUpdateHttpUrl);}

            { put(RedisConstant.IOT_POINT_TREE, HttpConstant.iotUpdateHttpUrl);}

            { put(RedisConstant.ALG_INSTANCE_RULE, HttpConstant.algInstanceAndDataSourceUpdateUrl); }

        };
    }

    /**
     * 组装redis频道和对应缓存实例的映射关系
     * @return
     */
    public static Map<String, String> assembleChannelAndCacheInstance(){
        return new HashMap<String, String>() {
            { put(RedisConstant.IOT_NOT_STORE_FIELDS, CacheConstant.POINT_NOT_STORE_CACHE_INSTANCE); }

            { put(RedisConstant.IOT_POINT_TREE, CacheConstant.POINT_NOT_STORE_CACHE_INSTANCE); }

            { put(RedisConstant.ALARM_RULE , CacheConstant.ALARM_DATA_CACHE_INSTANCE); }

            { put(RedisConstant.ALG_INSTANCE_RULE, CacheConstant.ALG_DATA_SOURCE_CACHE_INSTANCE); }
        };
    }

    /**
     * 内外网转换ip地址部署
     * @param uri
     * @return
     */
    public static String assembleHttpUrl(String uri, String port){
        return "http://" + uri + ":" + port;
    }



}
