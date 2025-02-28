package com.sucheon.jobs.utils;

import com.sucheon.jobs.constant.RedisConstant;
import com.sucheon.jobs.listener.ConfChangeListener;
import com.sucheon.jobs.listener.RedisConfObserver;
import com.sucheon.jobs.listener.conf.RedisSubEvent;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RedisSubPubUtils {


    /**
     * 根据不同的redis频道绑定对应的观察者，找到对应的监听器
     * @param redissonClient
     * @param confChangeListener
     * @return
     */
    public static boolean attachTopicAndResultSubEvent(String channelName, RedissonClient redissonClient, ConfChangeListener confChangeListener, RedisConfObserver redisConfObserver){
        //消息发布主题
        RTopic topic = redissonClient.getTopic(channelName);

        //关联redis订阅事件，先注册监听器得到异步计算出结果后，再开始下一个异步计算
        CompletableFuture<Void> cfStage1 = CompletableFuture.completedFuture(topic)
                .thenAcceptAsync( rTopic ->{
                    rTopic.addListener(String.class, confChangeListener);
                } );

        CompletableFuture<Boolean> cfStage2 =
                cfStage1.thenApply(rtopic -> redisConfObserver.getRealtimeRedisUpdateStatus());

        //获取最终结果，阻塞当前线程
        Boolean subEvent = cfStage2.join();
        if (subEvent ==null || !subEvent) {
            return false;
        }
        return subEvent;
    }

    /**
     * 监听所有频道，得到更新的通知
     * @param redissonClient
     * @param confChangeListener
     * @return
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public static Map<String, Boolean> pollRedisEvent(RedissonClient redissonClient, ConfChangeListener confChangeListener, RedisConfObserver redisConfObserver) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        Map<String, Boolean> updateEventCollection = new HashMap<>();

        List<String> channelList = assembleChannelList();
        for (String channelName : channelList){
            Boolean changeSymbol = attachTopicAndResultSubEvent(channelName, redissonClient, confChangeListener, redisConfObserver);
            updateEventCollection.put(channelName, changeSymbol);
        }
        return updateEventCollection;
    }

    public static List<String> assembleChannelList() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        RedisConstant redisConstant = new RedisConstant();
        List<String> channelList = redisConstant.allChannelNames();
        return channelList;
    }
}
