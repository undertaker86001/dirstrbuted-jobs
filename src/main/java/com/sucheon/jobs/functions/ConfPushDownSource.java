package com.sucheon.jobs.functions;

import com.github.jesse.l2cache.CacheConfig;
import com.sucheon.jobs.config.HttpConf;
import com.sucheon.jobs.config.NetworkConf;
import com.sucheon.jobs.constant.RedisConstant;
import com.sucheon.jobs.listener.ConfChangeListener;
import com.sucheon.jobs.listener.RedisConfObserver;
import com.sucheon.jobs.listener.context.IotNotStoreEventResult;
import com.sucheon.jobs.listener.context.event.PointNotStoreEvent;

import com.sucheon.jobs.utils.ExceptionUtil;
import com.sucheon.jobs.utils.MetadataUtils;
import com.sucheon.jobs.utils.TransferBeanutils;
import com.sucheon.jobs.utils.RedisSubPubUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 配置下推数据源（只有第一次启动程序循环遍历http接口的时候，会记录到本地缓存，远程缓存也会存一份）
 */
@Slf4j
public class ConfPushDownSource extends RichSourceFunction<String> {

    /**
     * redisson客户端
     */
    private RedissonClient redissonClient;

    /**
     * 配置所有的缓存配置
     */
    private CacheConfig cacheConfig;

    /**
     * 获取订阅redis的所有频道和调用的http连接
     */
    private Map<String, String> channelAndHttpUrl;

    /**
     * 获取订阅redis的所有频道和对应的缓存实例，避免手动替换
     */
    private Map<String, String> channelAndCacheInstance;

    /**
     * http的缓存配置
     */
    private HttpConf httpConf;

    /**
     * 回调第三方接口的网络配置
     */
    private String thirdAddress;

    private ConfChangeListener confChangeListener;

    private RedisConfObserver redisConfObserver;

    @SneakyThrows
    public ConfPushDownSource(CacheConfig cacheConfig, HttpConf httpConf, NetworkConf networkConf){
        this.cacheConfig = cacheConfig;
        this.channelAndHttpUrl = MetadataUtils.assembleChannelAndHttpUrl();
        this.channelAndCacheInstance = MetadataUtils.assembleChannelAndCacheInstance();
        this.thirdAddress = MetadataUtils.assembleHttpUrl(networkConf.getAddress(), networkConf.getPort());
        this.httpConf = httpConf;

        //绑定redis的监听器
        this.confChangeListener = new ConfChangeListener(cacheConfig);
        this.redisConfObserver = new RedisConfObserver();
        confChangeListener.attach(redisConfObserver);
    }

    @Override
    public void open(Configuration parameters) throws Exception {

        CacheConfig cacheConfig = this.cacheConfig;
        redissonClient = Redisson.create(cacheConfig.getRedis().getRedissonConfig());

    }



    @Override
    public void run(SourceContext<String> context)  {

        try {

            // 当有新配置写入的时候，替换缓存里的配置
            Map<String, Boolean> subEventMap = RedisSubPubUtils.pollRedisEvent(redissonClient, confChangeListener, redisConfObserver);
            for (Map.Entry<String, Boolean> event : subEventMap.entrySet()) {
                String channel = event.getKey();
                Boolean changeSymbol = event.getValue();
                String currentHttpUri = channelAndHttpUrl.get(channel) == null ? "" : channelAndHttpUrl.get(channel);
                String currentCacheInstance = channelAndCacheInstance.get(channel) == null ? "" : channelAndCacheInstance.get(channel);
                String currentHttpAddress = thirdAddress + currentHttpUri;

                if (channel.equals(RedisConstant.IOT_NOT_STORE_FIELDS)) {
                    //算法实例与数据源的绑定关系变更
                    TransferBeanutils.assemblePreSendContext(currentHttpAddress, currentCacheInstance, cacheConfig, httpConf, changeSymbol, new PointNotStoreEvent(), new IotNotStoreEventResult());
                    redisConfObserver.setChannelChangeSymbol(false);
                } else if (channel.equals(RedisConstant.IOT_POINT_TREE)) {
                    //实现点位树变更
                    TransferBeanutils.assemblePreSendContext(currentHttpAddress, currentCacheInstance, cacheConfig, httpConf, changeSymbol, new PointNotStoreEvent(), new IotNotStoreEventResult());
                    redisConfObserver.setChannelChangeSymbol(false);
                } else if (channel.equals(RedisConstant.ALG_INSTANCE_RULE)) {
                    //todo 算法实例和数据源关系变更

                }
            }
        }catch (Exception ex){
            String errorMessage = ExceptionUtil.getErrorMessage(ex);
            log.error("当前请求http连接出现异常, 原因:{}", errorMessage);
        }

    }

    @Override
    public void cancel() {
        redissonClient.shutdown(0L, 2L, TimeUnit.SECONDS);
    }

}
