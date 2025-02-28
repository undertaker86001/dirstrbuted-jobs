package com.sucheon.jobs;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.jesse.l2cache.CacheConfig;
import com.sucheon.jobs.config.*;
import com.sucheon.jobs.constant.CommonConstant;
import com.sucheon.jobs.event.*;
import com.sucheon.jobs.functions.*;
import com.sucheon.jobs.partitioner.CustomRangePartitioner;
import com.sucheon.jobs.sink.FlinkKafkaMutliSink;
import com.sucheon.jobs.state.StateDescContainer;
import com.sucheon.jobs.utils.ExceptionUtil;
import com.sucheon.jobs.utils.InternalTypeUtils;
import com.sucheon.jobs.utils.TransferBeanutils;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.serialization.TypeInformationSerializationSchema;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.*;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.flink.streaming.connectors.kafka.internals.KafkaDeserializationSchemaWrapper;
import org.apache.flink.streaming.connectors.kafka.internals.KafkaSerializationSchemaWrapper;
import org.apache.flink.streaming.util.serialization.KeyedSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.checkerframework.checker.units.qual.C;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * 对两种数据进行处理 1.算法组回传的数据 2.边缘端上送的数据
 * 两种数据格式并不一样
 * 通用数据格式
 * {
 * "point_id":"",//测点id
 * "device_channel":"",//uuid
 * "device_timestamp"：122345667,//时间戳 5.x数据字段需变动
 * "batch_id":"",//批次号
 * //其他字段 扁平
 * }
 */
@Slf4j
@SpringBootApplication(exclude = {GsonAutoConfiguration.class, JacksonAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class})
public class DistrbuteJobMain {


    public static void main(String[] args) throws Exception {

        /**
         *
         * 数据格式: algorithm_id, result_data, devices列表中存在哪几个点位
         * 多个点位需要做合并算法结果的操作
         * 1464 -> elec_noise -> state_alarlm
         * 1767 -> elec_nois -> state_alarm
         *  instance_id 1
         *
         *  1767 -> elec_nois -stats_alarlm
         *  1686 -> elec_nosi -> stats_alarlm
         *
         * 目前单个点位输出，包含单个算法实例的结果，以及对应多点位的输出
         * 目前一个作业里跑多个点位的情况包括 同种工作流的点位放在一个作业执行
         * 一个算法实例包括多个点位, 如果多个点位并行在跑
         *         self.code = code
         *         self.device_channel_number = device_channel_number
         *         self.alg_instance_id = alg_instance_id
         *         self.instance_work = instance_work
         *         self.input_second_data = input_second_data
         *         self.result_time = int(feature_time)
         *         self.health_effective = False
         *         # 算法计算根据特征值计算出来的结果
         *         self.health_data = {
         *             "code": {
         *                 "code_number": 111,
         *                 "instance_work": [],
         *             },
         *             "dataTime": self.result_time,
         *             # "status": status_code.close,
         *             "status": 0,
         *             # 健康度需要后端新增float的支持，以及不限制数据的大小
         *             # 健康度
         *             "health": {
         *             },
         *             "event": [
         *             ]
         *         }
         *         # 算法根据原始数据计算出来的特征值数据
         *         self.feature_data = {
         *             "code": 1212,
         *             "instance_work": [{
         *                 "instance_id": "1",
         *                 "feature_data": {}
         *             },
         *             {
         *                 "instance_id": "2",
         *                 "feature_data": {}
         *             }
         *             ]
         *         }
         *
         */
        System.setProperty("spring.devtools.restart.enabled", "false");
        ConfigurableApplicationContext applicationContext = SpringApplication.run(DistrbuteJobMain.class, args);

        //get properties pojo
        UserProperties ddpsProperties = applicationContext.getBean(UserProperties.class);
        CacheUtils cacheUtils = applicationContext.getBean(CacheUtils.class);

        CacheConfig cacheConfig = cacheUtils.getCacheConfig();

        HttpConf httpConf = HttpConf.builder()
                .retryTimes(Integer.valueOf(ddpsProperties.getRetryTimes()))
                .exponential(Boolean.valueOf(ddpsProperties.getExponential()))
                .sleepTimeInMilliSecond(Integer.valueOf(ddpsProperties.getSleepTimeInMilliSecond())).build();

        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", ddpsProperties.getBootStrapServers());
        properties.setProperty("auto.offset.reset", ddpsProperties.getAutoOffsetReset());
        properties.setProperty("fetch.max.bytes", ddpsProperties.getFetchMaxBytes());
        properties.setProperty("auto.create.topics", ddpsProperties.getAutoCreateTopics());

        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironment();
        env.setParallelism(1);

        ParameterTool parameterTool = ParameterTool.fromArgs(args);

        Long algStartTimestamp = Long.valueOf(Optional.ofNullable(parameterTool.get("algStartTimestamp")).orElse("0"));
        Long iotStartTimestamp = Long.valueOf(Optional.ofNullable(parameterTool.get("iotStartTimestamp")).orElse("0"));

        KafkaSourceBuilder kafkaSourceBuilder = new KafkaSourceBuilder();
        DataStream<String> algResult = env.fromSource(kafkaSourceBuilder.newBuild(ddpsProperties.getAlgSourceTopic(), ddpsProperties.getAlgSourceGroup(), properties, algStartTimestamp), WatermarkStrategy.noWatermarks(), "iot-data");
        algResult =  algResult.map(new LabelAlgResultMapFunction());
        DataStream<String> iotResult = env.fromSource(kafkaSourceBuilder.newBuild(ddpsProperties.getIotSourceTopic(), ddpsProperties.getIotSourceGroup(), properties, iotStartTimestamp), WatermarkStrategy.noWatermarks(), "alg-data");
        iotResult = iotResult.map(new LabelIotResultMapFunction());
        algResult = algResult.union(iotResult);
        //json解析
        DataStream<String> pointTreeDataStream = algResult;


        //添加事件事件分配 去掉补的数据
        WatermarkStrategy<String> watermarkStrategy = WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofMillis(0))
                .withTimestampAssigner(new SerializableTimestampAssigner<String>() {
                    @Override
                    public long extractTimestamp(String pointDataStr, long timestamp) {
                        Object deviceTimestampObject = null;

                        try {
                            Object value = TransferBeanutils.transferNormalFormat(log, pointDataStr);
                            deviceTimestampObject = InternalTypeUtils.getValueByFieldName(value, "device_timestamp");
                        } catch (Exception e) {
                            String errorMessage = ExceptionUtil.getErrorMessage(e);
                            log.error("当前打水印阶段数据序列化失败, 当前时间戳: {}, 原始数据为: {}, 当前错误原因为: {}", timestamp, pointDataStr, errorMessage);
                        }
                        String deviceTimestampStr = String.valueOf(Optional.ofNullable(deviceTimestampObject).orElse(System.currentTimeMillis()));
                        return Long.parseLong(deviceTimestampStr);
                    }
                });

        SingleOutputStreamOperator<String> pointTreeWithWatermark = pointTreeDataStream.assignTimestampsAndWatermarks(watermarkStrategy);

        NetworkConf networkConf = NetworkConf.builder().address(ddpsProperties.getAddress()).port(ddpsProperties.getPort()).build();

        DataStream<String> ruleBinlogs = env.addSource(new ConfPushDownSource(cacheConfig, httpConf, networkConf));

        //解析用户下发的规则配置
        BroadcastStream<String> ruleBroadcast = ruleBinlogs.broadcast(StateDescContainer.ruleStateDesc);


        //将数据流connect规则广播流
        BroadcastConnectedStream<String, String> connect1 = pointTreeWithWatermark.connect(ruleBroadcast);


        //根据不同的code1,code2,code3....进行动态keyby的分发
        SingleOutputStreamOperator<DynamicKeyedBean> withDynamicKey = connect1.process(new DynamicKeyByByReplicationFunction(cacheConfig));

        String keyByParallelism = ddpsProperties.getParallelism();

        //按照自定义负载策略进行分发
        KeyedStream<DynamicKeyedBean, Object> keyedStream = withDynamicKey.keyBy(new DistrubuteKeyFunction(Integer.parseInt(Optional.of(keyByParallelism).orElse("1"))));

        //规则计算
        BroadcastConnectedStream<DynamicKeyedBean, String> connect2 = keyedStream.connect(ruleBroadcast);

        //根据不同的算法组做相关的分发
        DistrbuteDependonDownStreamConf conf = DistrbuteDependonDownStreamConf.builder()
                .isAlarmOpen(ddpsProperties.getIsAlarmOpen())
                .isAlgOpen(ddpsProperties.getIsAlgOpen())
                .isStoreOpen(ddpsProperties.getIsStoreOpen()).build();
        DataStream<RuleMatchResult> ruleMatchResultDataStream =  connect2.process(new RuleMatchKeyedProcessFunction(conf));


        final TypeInformation<String> resultType =
                TypeInformation.of(new TypeHint<String>() {});





        KeyedSerializationSchema<RuleMatchResult> routeDistrute = new KeyedSerializationSchema<RuleMatchResult>() {
            @Override
            public byte[] serializeKey(RuleMatchResult data) {
               return InternalTypeUtils.transferData(log, data);
            }

            @Override
            public byte[] serializeValue(RuleMatchResult data) {
                return InternalTypeUtils.transferData(log, data);
            }

            @Override
            public String getTargetTopic(RuleMatchResult algResult) {
                if (algResult == null) {
                    return "default-topic";
                }

               return algResult.getTopic();
            }
        };

        FlinkKafkaMutliSink distrbuteSink = new FlinkKafkaMutliSink("default-topic", routeDistrute, properties, new CustomRangePartitioner());

        //匹配的算法结果输出到kafka
        ruleMatchResultDataStream.addSink(distrbuteSink);

        env.execute();

    }
}
