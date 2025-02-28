package com.sucheon.jobs.source;

import com.sucheon.jobs.constant.CommonConstant;
import com.sucheon.jobs.listener.conf.RedisSubEvent;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

public class TestSource implements SourceFunction<String> {


    private RedisSubEvent redisSubEvent;

    public TestSource(RedisSubEvent redisSubEvent){
        this.redisSubEvent = redisSubEvent;
    }
    @Override
    public void run(SourceContext<String> sourceContext) throws Exception {

        sourceContext.collect(CommonConstant.objectMapper.writeValueAsString(redisSubEvent));
    }

    @Override
    public void cancel() {

    }
}
