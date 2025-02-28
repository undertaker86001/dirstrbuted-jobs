package com.sucheon.jobs;

import com.github.jesse.l2cache.CacheConfig;
import com.github.jesse.l2cache.consts.CacheType;
import com.sucheon.jobs.constant.RedisConstant;
import com.sucheon.jobs.listener.conf.RedisSubEvent;
import com.sucheon.jobs.source.TestSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

/**
 * Redis mock写入点位配置
 */
@Slf4j
public class RedisMockWritePointTreeConfTest {

    private static final String TEST_KEY = "redisson";

    private static final long TEST_VALUE = 100L;

    //redis客户端
    private RedissonClient redisClient;

    @Before
    public void setup(){
        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setCacheType(CacheType.REDIS.name())
                .setAllowNullValues(true)
                .getRedis()
                .setExpireTime(30000000)
                .setLock(true)
                .setRedissonYamlConfig("redisson.yaml");
        redisClient = Redisson.create(cacheConfig.getRedis().getRedissonConfig());

    }

    @Test
    public void testRedisKeyChange(){
        RAtomicLong atomicLong = redisClient.getAtomicLong(TEST_KEY);
        atomicLong.set(TEST_VALUE);
        log.info("redisson单机单实例: {}", redisClient.getAtomicLong(TEST_KEY).get());
        Assert.assertEquals(TEST_VALUE, redisClient.getAtomicLong(TEST_KEY).get());
    }

    @Test
    public void testRedisPointTreeChangeVerify(){

        String changeContext = "{\n" +
                "\n" +
                "   \"config\":\"PointTree\", \n" +
                "\n" +
                "   \"event\":\"TreeChange\", \n" +
                "\n" +
                "   \"data\":[2342423422,2342423422 ]\n" +
                "\n" +
                "}";

        RTopic topic = redisClient.getTopic(RedisConstant.IOT_NOT_STORE_FIELDS);
        topic.publish(changeContext);


    }


    @Test
    public void testRedisPointTreeChange(){

        RTopic topic = redisClient.getTopic(RedisConstant.IOT_NOT_STORE_FIELDS);
        topic.publish("{\n" +
                "\"code\":\"B0120022\",\n" +
                "\"data\":[\n" +
                "\n" +
                " {\n" +
                " \"pointId\":12311,\n" +
                " \"deviceChannel\":\"sc01234243-01\",\n" +
                " \"notStoreTemplateId\":85662222177\n" +
                " },\n" +
                "{\n" +
                " \"pointId\":12311,\n" +
                " \"deviceChanle\":\"sc01234243-01\",\n" +
                " \"notStoreTemplateId\":85662222177\n" +
                "}]}");
    }

    @Test
    public void testNotFailedSerialble() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironment();
        env.setParallelism(1);
        DataStream<String> dataStream = env.addSource(new TestSource(new RedisSubEvent()));
        dataStream.map(new MapFunction<String, Object>() {
            @Override
            public Object map(String s) throws Exception {
                return s;
            }
        });
        env.execute();
    }
}
