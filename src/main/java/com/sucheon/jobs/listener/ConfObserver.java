package com.sucheon.jobs.listener;

import com.sucheon.jobs.listener.conf.RedisSubEvent;

/**
 * 配置观察者，监听配置变更的消息
 */
public interface ConfObserver {

    /**
     * 监听从上游配置变更的到的消息(目前只针对redis类型)
     * @param channelUpdateSymbol 是否有频道更新数据
     */
    boolean update(String channel, String message, boolean channelUpdateSymbol);
}
