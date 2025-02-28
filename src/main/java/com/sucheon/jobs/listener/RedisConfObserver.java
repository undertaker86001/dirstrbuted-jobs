package com.sucheon.jobs.listener;

import com.sucheon.jobs.listener.conf.RedisSubEvent;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

import static com.sucheon.jobs.constant.CommonConstant.queueOperatorUtils;

/**
 * redis配置变更监听器
 */
@Setter
@Getter
public class RedisConfObserver implements ConfObserver, Serializable {

    /**
     * 上游是否有消息变更到下游
     */
    private boolean channelChangeSymbol;

    public RedisConfObserver(){
        this.channelChangeSymbol = true;
    }


    /**
     * 获取实时推送状态
     * @return
     */
    public boolean getRealtimeRedisUpdateStatus(){
        return channelChangeSymbol;
    }

    @Override
    public boolean update(String channel, String message, boolean channelUpdateSymbol) {
        channelChangeSymbol = channelUpdateSymbol;
        RedisSubEvent redisSubEvent = new RedisSubEvent();
        redisSubEvent.setChannel(channel);
        redisSubEvent.setMessage(message);
        //维护redis的事件队列, 为任务启动后，当redis发生变更时，拉取每个channel的消息做准备
        //todo 当本地缓存拉取不到最新的事件时，去事件队列当中去拉
        queueOperatorUtils.offer(redisSubEvent);
        return channelChangeSymbol;
    }
}
