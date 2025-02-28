package com.sucheon.jobs.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Configuration
@Data
public class UserProperties implements Serializable {

    private static final long serialVersionUID = 997809857078533653L;

    @Value("${ddps.kafka.bootstrap-servers}")
    private String bootStrapServers;

    @Value("${ddps.kafka.auto-offset-reset}")
    private String autoOffsetReset;

    @Value("${ddps.kafka.fetch-max-bytes}")
    private String fetchMaxBytes;

    @Value("${ddps.kafka.auto-create-topics}")
    private String autoCreateTopics;

    @Value("${ddps.kafka.iot-source-topic}")
    private String iotSourceTopic;

    @Value("${ddps.kafka.iot-group-id}")
    private String iotSourceGroup;

    @Value("${ddps.kafka.alg-source-topic}")
    private String algSourceTopic;

    @Value("${ddps.kafka.alg-group-id}")
    private String algSourceGroup;

    /**
     * keyby分发时候的并行度
     */
    @Value("${ddps.flink.key-by.parallelism:1}")
    private String parallelism;


    /**
     * 算法链路分发开关
     */
    @Value("${ddps.distribute.is-alg-open:false}")
    private Boolean isAlgOpen;


    /**
     * 告警链路分发开关
     */
    @Value("${ddps.distribute.is-alarm-open:false}")
    private Boolean isAlarmOpen;

    /**
     * ck入库分发开关
     */
    @Value("${ddps.distribute.is-store-open:false}")
    private Boolean isStoreOpen;


    /**
     * 重试时间
     */
    @Value("${ddps.http.retry-times:3}")
    private String retryTimes;

    /**
     * 运行失败后休眠对应时间再重试
     */
    @Value("${ddps.http.sleep-time-in-milli-second:20000}")
    private String sleepTimeInMilliSecond;

    /**
     * 休眠时间是否指数递增
     */
    @Value("${ddps.http.exponential:false}")
    private String exponential;

    /**
     * 第三方HTTP请求地址
     */
    @Value("${ddps.http.address:127.0.0.1}")
    private String address;

    /**
     * 第三方HTTP请求端口
     */
    @Value("${ddps.http.port:8090}")
    private String port;

}
