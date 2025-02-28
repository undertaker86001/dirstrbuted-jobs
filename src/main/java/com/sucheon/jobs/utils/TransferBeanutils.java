package com.sucheon.jobs.utils;

import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.jesse.l2cache.Cache;
import com.github.jesse.l2cache.CacheConfig;
import com.sucheon.jobs.config.HttpConf;
import com.sucheon.jobs.config.ObjectSingleton;
import com.sucheon.jobs.constant.CommonConstant;
import com.sucheon.jobs.event.*;
import com.sucheon.jobs.listener.conf.RedisSubEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sucheon.jobs.constant.FieldConstants.*;
import static com.sucheon.jobs.utils.InternalTypeUtils.iaAllFieldsNull;

/**
 * 分发时转换规则匹配的bean
 */
public class TransferBeanutils {


    /**
     * 构建分发规则命中结果集(暂时支持边缘端)
     * @param iotTopicDistrbuteData
     * @param pointData
     * @return
     * @throws JsonProcessingException
     */
    public static RuleMatchResult buildRuleMatchResult(Logger logger, DistrbutePointData iotTopicDistrbuteData, Object pointData) throws JsonProcessingException {
        RuleMatchResult ruleMatchResult = new RuleMatchResult();
        ruleMatchResult.setTopic(iotTopicDistrbuteData.getTopic());
        String deviceTimeStamp = InternalTypeUtils.extractFieldValue(logger, pointData, DEVICE_TIMESTAMP);
        ruleMatchResult.setDeviceTimestamp(Long.parseLong(deviceTimeStamp));
        String pointId = InternalTypeUtils.extractFieldValue(logger, pointData, POINT_ID);
        ruleMatchResult.setPointId(pointId);
        String batchId = InternalTypeUtils.extractFieldValue(logger, pointData, BATCH_ID);
        ruleMatchResult.setBatchId(batchId);
        String deviceChannel = InternalTypeUtils.extractFieldValue(logger, pointData, DEVICE_CHANNEL);
        ruleMatchResult.setDeviceChannel(deviceChannel);
        ruleMatchResult.setIotJson(CommonConstant.objectMapper.writeValueAsString(iotTopicDistrbuteData));
        return ruleMatchResult;
    }

    /**
     * 构建边缘端待分发数据
     * @param pointData
     * @param topic
     * @return
     * @throws JsonProcessingException
     */
    public static DistrbutePointData buildRuleMatchResult(Logger logger, Object pointData, String topic) throws JsonProcessingException {
        RuleMatchResult iotTopicDistrbuteData = new RuleMatchResult();

        String iotJson = CommonConstant.objectMapper.writeValueAsString(pointData);
        iotTopicDistrbuteData.setEventBean(iotJson);
        String deviceTimeStamp = InternalTypeUtils.extractFieldValue(logger, pointData, DEVICE_TIMESTAMP);
        iotTopicDistrbuteData.setDeviceTimestamp(Long.parseLong(deviceTimeStamp));
        String pointId = InternalTypeUtils.extractFieldValue(logger, pointData, POINT_ID);
        iotTopicDistrbuteData.setPointId(pointId);
        String batchId = InternalTypeUtils.extractFieldValue(logger, pointData, BATCH_ID);
        iotTopicDistrbuteData.setBatchId(batchId);
        String deviceChannel = InternalTypeUtils.extractFieldValue(logger, pointData, DEVICE_CHANNEL);
        iotTopicDistrbuteData.setDeviceChannel(deviceChannel);
        DistrbutePointData distrbutePointData = new DistrbutePointData();
        BeanUtil.copyProperties(pointData, distrbutePointData);
        distrbutePointData.setTopic(topic);
        return distrbutePointData;
    }


    /**
     * 构建分发规则命中结果集(支持算法端)
     * @param distrbutePointData
     * @parampointData
     * @return
     * @throws JsonProcessingException
     */
    public static RuleMatchResult buildAlgRuleMatchResult(DistrbutePointData distrbutePointData, AlgData algResult) throws JsonProcessingException {

        RuleMatchResult ruleMatchResult = new RuleMatchResult();
        ruleMatchResult.setTopic(distrbutePointData.getTopic());
        ruleMatchResult.setDeviceTimestamp(algResult.getDeviceTimestamp());
        ruleMatchResult.setPointId(algResult.getPointId());
        ruleMatchResult.setBatchId(algResult.getBatchId());
        ruleMatchResult.setDeviceChannel(algResult.getDeviceChannel());
        ruleMatchResult.setIotJson(CommonConstant.objectMapper.writeValueAsString(algResult));
        if (StringUtils.isNotBlank(algResult.getGroupKey())){
            ruleMatchResult.setGroupKey(algResult.getGroupKey());
        }
        return ruleMatchResult;
    }

    /**
     * 构建算法侧待分发数据
     * @param algResult
     * @param topic
     * @return
     */
    public static DistrbutePointData buildAlgMatchResult(AlgData algResult, String topic) throws JsonProcessingException {
        RuleMatchResult algTopicDistrbuteData = new RuleMatchResult();
        String algJson = CommonConstant.objectMapper.writeValueAsString(algResult);
        algTopicDistrbuteData.setEventBean(algJson);
        algTopicDistrbuteData.setBatchId(algResult.getBatchId());
        algTopicDistrbuteData.setPointId(algResult.getPointId());
        algTopicDistrbuteData.setDeviceChannel(algResult.getDeviceChannel());
        algTopicDistrbuteData.setDeviceTimestamp(algResult.getDeviceTimestamp());
        DistrbutePointData distrbutePointData = new DistrbutePointData();
        BeanUtil.copyProperties(algResult, distrbutePointData);
        distrbutePointData.setTopic(topic);
        return distrbutePointData;
    }

    /**
     * 判断当前除了topic之外的实体属性是不是null值
     * @param data
     * @return
     */
    public static boolean sendIfMessageNotNull(RuleMatchResult data){
        String topic = data.getTopic();
        data.setTopic(null);

        if (iaAllFieldsNull(data)) {
            data.setTopic(topic);
            return false;
        }else {
            data.setTopic(topic);
            return true;
        }

    }

    /**
     * 为当前算法实例组装所有字段所归属的上游算法实例信息
     * @param pointTreeList
     * @return
     */
    public static List<AlgInstanceInfo> assembleAlgInstanceInfo(List<PointTree> pointTreeList){

        //记录当前实例中字段名所归属的上游算法实例
        Map<String, String> cache = new HashMap<>();
        Map<String, Map<String, String>> algInstanceAttachGroupField = new HashMap<>();

        for (PointTree pointTree: pointTreeList){

            List<AlgFieldList> algFieldLists = pointTree.getFields();

            //如果遍历到当前迭代实例，需要将之前记录的实例的字段信息记录到算法实例列表, 则需要从上下文中取得

            Map<String, String> algInstanceGroupCache = new HashMap<>();
            for (AlgFieldList algFieldList: algFieldLists) {
                cache.put(algFieldList.getKey(), pointTree.getCurrentAlgInstance());
                //todo 一个字段可能会对应多个算法组
                algInstanceGroupCache.put(algFieldList.getKey(), algFieldList.getGroup());
            }
            algInstanceAttachGroupField.put(pointTree.getAlgGroup(), algInstanceGroupCache);
        }

        //组装当前算法实例需要取值的字段信息
        List<AlgInstanceInfo> algInstanceInfoList = new ArrayList<>();
        for (PointTree pointTree: pointTreeList){
            AlgInstanceInfo algInstanceInfo = new AlgInstanceInfo();
            List<String> prevInstanceList = new ArrayList<>();
            List<AlgFieldList> algFieldList = pointTree.getFields();
            Map<String, String> algFieldGroup = algInstanceAttachGroupField.get(pointTree.getCurrentAlgInstance());

            algInstanceInfo.setInstanceId(pointTree.getCurrentAlgInstance());
            for (AlgFieldList field: algFieldList){
                String algInstance = cache.get(field.getKey());
                prevInstanceList.add(algInstance);

            }
            algInstanceInfo.setFieldBelongToInstance(cache);
            algInstanceInfo.setPrevInstanceList(prevInstanceList);
            algInstanceInfo.setFieldGroup(algFieldGroup);
            algInstanceInfoList.add(algInstanceInfo);
        }

        return algInstanceInfoList;
    }


    public static  <T> Object returnInstanceInfo(String message, T tClass) throws IOException {
        Class<? extends Object> currentClass = tClass.getClass();
        return CommonConstant.objectMapper.readValue(message, currentClass);
    }

    /**
     * 根据不同的redis订阅频道组装配置下发对象
     * @param cacheConfig 传入的cacheConfig配置
     * @param subEvent
     * @param originData
     * @param resultData
     * @param <T>
     * @param <R>
     * @return
     * @throws IOException
     */
    public static <T, R> String assemblePreSendContext(String httpUrl, String cacheInstance, CacheConfig cacheConfig, HttpConf httpConf, boolean subEvent, T originData, R resultData) throws IOException {

        if(!subEvent){
            return "";
        }

        //构建本地缓存+ 远程缓存
        Cache cache = accessThirdData(cacheInstance, httpUrl, cacheInstance,  cacheConfig, httpConf, true, new HashMap<>(), new HashMap<>());
        //如果http接口请求失败，走旧的缓存
        Object confResult = cache.get(cacheInstance);

        Object eventResult = CommonConstant.objectMapper.readValue(String.valueOf(confResult), resultData.getClass());

        //需要设置从哪个频道更新的在下游更新对应的数据
        InternalTypeUtils.addField(eventResult, REDIS_CACHE_INSTANCE, cacheInstance);

        String sendFinalResult = CommonConstant.objectMapper.writeValueAsString(eventResult);
        return sendFinalResult;
    }


    /**
     * 更新缓存中待推送的事件，判断是否应该调用
     *
     * @param change Redis是否变更订阅
     * @param channel 需要往下游告知的频道名
     * @param cacheInstance 当前缓存实例
     * @param cacheConfig 配置的缓存配置
     * @param httpConf 配置的http链接参数
     * @param change 是否配置发生变化
     * @param params http请求的餐素
     * @param header
     * @return 根据情况判断哪些字段不需要分发
     */
    public static Cache accessThirdData(String channel, String httpUrl, String cacheInstance, CacheConfig cacheConfig, HttpConf httpConf, boolean change,
                                        HashMap<String, String> params, HashMap<String, String> header){
        //构建本地缓存+ 远程缓存
        Cache cache = accessCurrentCacheInstance(cacheInstance, cacheConfig);
        String currentResult = "";
        if (change) {
            //请求http接口 支持重试
            String[] result = RetryUtils.executeWithRetry(
                    () -> HttpAsyncClientUtil.getAsyncHttpResult(httpUrl, params, header, 1),
                    httpConf.getRetryTimes(),
                    httpConf.getSleepTimeInMilliSecond(),
                    httpConf.getExponential());
            currentResult = result[0];
            cache.put(cacheInstance, currentResult);
        }

        return cache;
    }

    /**
     * 访问第三方缓存实例
     * @param cacheInstance 缓存实例
     * @param cacheConfig 缓存配置
     * @return
     */
    public static Cache accessCurrentCacheInstance(String cacheInstance, CacheConfig cacheConfig){
        Cache cache = CacheUtils.compositeCache(cacheInstance, cacheConfig);
        return cache;
    }


    /**
     * 为Object对象创建基于枚举类型的单例模式
     * @return
     */
    public static Object getObjectInstance(){
        return ObjectSingleton.INSTANCE.getInstance();
    }

    /**
     * 转换成内部结构的数据，根据字符串转换成测点数据
     * @param logger 上层哪个类传进来的日志门面
     * @param json 待转换的json
     * @return
     */
    public static Object transferNormalFormat(Logger logger, String json){
        Object pointData = getObjectInstance();
        try {
            pointData = CommonConstant.objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            String errorMessage = ExceptionUtil.getErrorMessage(e);
            logger.error("当前数据json解析失败，异常为: {}", errorMessage);
        }
        return pointData;
    }


    /**
     * 反射将上游的字段统一口径转换成下划线
     */


}
