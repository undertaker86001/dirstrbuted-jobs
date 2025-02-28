package com.sucheon.jobs.utils;

import com.sucheon.jobs.listener.MemoryLimitedLinkedBlockingQueue;
import com.sucheon.jobs.listener.conf.RedisSubEvent;
import net.bytebuddy.agent.ByteBuddyAgent;

import java.lang.instrument.Instrumentation;

public class QueueOperatorUtils {

    private MemoryLimitedLinkedBlockingQueue<RedisSubEvent> memoryLimitedLinkedBlockingQueue;

    public QueueOperatorUtils() {
        ByteBuddyAgent.install();
        Instrumentation inst = ByteBuddyAgent.getInstrumentation();
        memoryLimitedLinkedBlockingQueue = new MemoryLimitedLinkedBlockingQueue<RedisSubEvent>(inst);
        memoryLimitedLinkedBlockingQueue.setMemoryLimit(Integer.MAX_VALUE);
    }


    public void offer(RedisSubEvent redisSubEvent){
        memoryLimitedLinkedBlockingQueue.offer(redisSubEvent);
    }

}
