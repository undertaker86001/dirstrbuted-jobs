package com.sucheon.jobs.listener;

import com.github.jesse.l2cache.Cache;
import com.github.jesse.l2cache.CacheConfig;
import com.sucheon.jobs.listener.conf.RedisSubEvent;
import com.sucheon.jobs.utils.MetadataUtils;
import com.sucheon.jobs.utils.RedisSubPubUtils;
import com.sucheon.jobs.utils.TransferBeanutils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义Redis消息监听器，用观察者模式监听
 */
public class ConfChangeListener implements MessageListener<String>, Serializable {


    /**
     * 存放多个配置观察者对象
     */
    private List<ConfObserver> confObserverList = new ArrayList<>();

    /**
     * redis订阅的频道
     */
    private String channel;

    /**
     * 推送的redis发布事件
     */
    private String message;

    /**
     * 保存redis频道和缓存实例的映射关系
     */
    private Map<String, String> channelAndCacheInstanceMapping;

    /**
     * 缓存配置
     */
    private CacheConfig cacheConfig;

    /**
     * 每个redis频道是否是第一次更新, 版本号作用
     */
    private Map<String, AtomicInteger> channelEffectCount;

    /**
     * 是否有频道更新消息
     */
    private Boolean channelUpdateSymbol;


    public ConfChangeListener(CacheConfig cacheConfig) {
        this.channelEffectCount = new HashMap<>();
        this.channelAndCacheInstanceMapping = MetadataUtils.assembleChannelAndCacheInstance();
        this.cacheConfig = cacheConfig;
        this.channelUpdateSymbol = false;
    }


    public void attach(ConfObserver observer){
        confObserverList.add(observer);
    }


    public void detach(ConfObserver observer){
        confObserverList.add(observer);
    }

    @Override
    public void onMessage(CharSequence charSequence, String message) {
        String channel = String.valueOf(charSequence);
        this.channel = channel;
        this.message = message;

        channelUpdateSymbol = true;
        //更新缓存当中的模版消息
        channelUpdateByVersion(channelUpdateSymbol, channel, message);
        //下发给观察者
        for (ConfObserver observer: confObserverList){
            observer.update(channel, message, channelUpdateSymbol);
        }




    }

    /**
     * 维护频道更新版本号，如果存在计数则更新缓存，下发更新消息
     * @param change 判断上游redis是否有消息更新
     * @param channel 更新的订阅频道
     * @param message 更新的消息模版
     */
    public void channelUpdateByVersion(boolean change,String channel, String message){

        if (!change){
            return;
        }

        //维护redis频道和发送下游http连接的映射关系
        String cacheInstance = channelAndCacheInstanceMapping.get(channel);
        //更新缓存的消息
        Cache cache = TransferBeanutils.accessCurrentCacheInstance(cacheInstance, cacheConfig);
        cache.put(cacheInstance, message);

        AtomicInteger currentCount = channelEffectCount.get(channel);
        if (currentCount == null){
            currentCount = new AtomicInteger(0);
        }
        currentCount.getAndIncrement();
        channelEffectCount.put(channel, currentCount);
    }
}
