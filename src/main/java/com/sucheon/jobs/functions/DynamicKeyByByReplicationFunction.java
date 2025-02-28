package com.sucheon.jobs.functions;


import com.github.jesse.l2cache.Cache;
import com.github.jesse.l2cache.CacheConfig;
import com.sucheon.jobs.config.MapSingleton;
import com.sucheon.jobs.constant.CommonConstant;
import com.sucheon.jobs.constant.RedisConstant;
import com.sucheon.jobs.event.*;
import com.sucheon.jobs.listener.context.IotNotStoreEventResult;
import com.sucheon.jobs.state.StateDescContainer;
import com.sucheon.jobs.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;

import java.util.*;

import static com.sucheon.jobs.constant.FieldConstants.*;

/**
 * 兼容联动点位（code1,code2,code3...）的多个keyby分发
 */
@Slf4j
public class DynamicKeyByByReplicationFunction extends BroadcastProcessFunction<String, String, DynamicKeyedBean> {


    /**
     * 通过点位树的配置(算法实例)得知需要分发到哪些topic当中
     */
    BroadcastState<String, String> broadcastState;

    /**
     * 全局需要广播的数据树配置
     */
    private List<PointTree> pointTreeList = new ArrayList<>();


    /**
     * 组合缓存配置
     */
    private CacheConfig cacheConfig;

    /**
     * 全局维护缓存操作工具类，维护同一个应用
     */
    private CacheUtils cacheUtils;

    /**
     * 维护所有redis订阅的频道
     */
    private List<String> redisAllChannel;


    /**
     * 维护redis频道和对应缓存实例的映射关系
     */
    private Map<String,String> channelAndCacheInstance;


    public DynamicKeyByByReplicationFunction(CacheConfig cacheConfig){
        this.cacheConfig = cacheConfig;
        this.channelAndCacheInstance = MetadataUtils.assembleChannelAndCacheInstance();
    }

    @Override
    public void open(Configuration parameters) throws Exception {

        RedisConstant redisConstant = new RedisConstant();
        redisConstant.allChannelNames();
        super.open(parameters);
    }

    /**
     * 算法控制台 存在算法配置，数据流存在点位配置
     * 输出到kafka的需要以算法组为准
     * @param pointDataStr
     * @param ctx
     * @param collector
     * @throws Exception
     */

    @Override
    public void processElement(String pointDataStr, BroadcastProcessFunction<String, String, DynamicKeyedBean>.ReadOnlyContext ctx, Collector<DynamicKeyedBean> collector) throws Exception {

        Object pointData = TransferBeanutils.transferNormalFormat(log, pointDataStr);


        //todo 从广播变量取值

        //配置需要变更的字段
        Map changeProperties = MapSingleton.INSTANCE.getInstance();

        //如果没有数据发送，则不往下传递, 打印日志
        if (InternalTypeUtils.iaAllFieldsNull(pointData)){
            log.error("当前数据为空，请检查上游采集端是否出现问题!, 时间戳: {}", System.currentTimeMillis());
            return;
        }

        ReadOnlyBroadcastState<String, String> broadcastState = ctx.getBroadcastState(StateDescContainer.ruleStateDesc);


        //判断当前变更的配置是属于边缘端还是算法端，减少循环次数
        String needUpdateChannel = "";
        for (Map.Entry<String, String> item: channelAndCacheInstance.entrySet()){
            String channel = item.getValue();
            String value = broadcastState.get(channel);
            if (StringUtils.isBlank(value)){
                continue;
            }else {
                needUpdateChannel = value;
                break;
            }
        }

        Map<String, String> channelAndUpdateContext = new HashMap<>();
        if (!redisAllChannel.contains(needUpdateChannel)){

            Cache cache = cacheUtils.compositeCache(needUpdateChannel, cacheConfig);
            Object value = cache.get(needUpdateChannel);
            channelAndUpdateContext.put(needUpdateChannel, String.valueOf(value));
        }

        String pointIdStr = InternalTypeUtils.extractFieldValue(log, pointData, POINT_ID);

        if (StringUtils.isBlank(pointIdStr)){
            log.error("当前测点数据为空, 请排查!");
        }

        String origin = InternalTypeUtils.extractFieldValue(log, pointData, ORIGIN_DATA);
        if (StringUtils.isNotBlank(origin) && origin.equals(CommonConstant.algLabel)) {

            List<PointTree> pointTreeList = new ArrayList<>();

            //根据下发的规则重新解析需要下发的规则

            //保存节点点位Id映射到测点id到对应特征值二级映射关系
            HashMap<String, Map<String, String>> codePointAndAlgFieldMap = new HashMap<>();

            //如果当前算法结果算出算法实例了， 则从算法控制台的点位配置进行匹配
            PointTree currentPointTree = PointTree.getInstance();

            for (PointTree pointTree: pointTreeList) {
                String algInstanceId = InternalTypeUtils.extractFieldValue(log, pointData, ALG_INSTANCE_ID);

                if (pointTree.getCurrentAlgInstance().equals(algInstanceId)) {
                    currentPointTree = pointTree;
                    // 如果数据树当中存在当前算法实例的测点数据
                    InternalTypeUtils.addField(pointData, NODE_CODE, pointTree.getCode());
                    pointData = ReflectUtils.getObject(pointData, changeProperties);
                    break;
                }
            }

            DynamicKeyedBean dynamicKeyedBean = DynamicKeyedBean.getInstance();
            changeProperties = MapSingleton.INSTANCE.getInstance();
            changeProperties.put(ORIGIN_DATA, origin);
            pointData = ReflectUtils.getObject(pointData, changeProperties);

            //如果算法控制台没有匹配到这个配置,只传递算法标识
            if (InternalTypeUtils.iaAllFieldsNull(currentPointTree)){
                return;
            }

            //匹配需要将哪些code下的字段收拢，分发到哪个算法组
            List<AlgFieldList> algResultList =  currentPointTree.getFields();

            Map<String, String> pointAndAlgFieldMap = new HashMap<>();

            //收集算法组和字段列表 根据算法组将字段分组
            Map<String, StringBuilder> algGroupMap = new HashMap<>();
            for (int j=0;j<algResultList.size();j++) {
                AlgFieldList algFieldList = algResultList.get(j);

                pointAndAlgFieldMap.put(algFieldList.getPointId(), algFieldList.getKey());

                if (StringUtils.isBlank(algGroupMap.get(algFieldList.getGroup()))){
                    algGroupMap.put(algFieldList.getGroup(),new StringBuilder().append(algFieldList.getKey()));
                } else {
                    StringBuilder fieldList = algGroupMap.get(algFieldList.getGroup());
                    fieldList.append(algFieldList.getKey());
                    algGroupMap.put(algFieldList.getGroup(), fieldList);
                }

            }

            //如果上一个算法实例没有出结果写错误日志
            String result = InternalTypeUtils.extractFieldValue(log, pointData, RESULT_DATA);
            ResultData resultData = CommonConstant.objectMapper.readValue(result, ResultData.class);

            if (resultData == null || InternalTypeUtils.iaAllFieldsNull(resultData)){
                log.error(String.format("当前分发的算法实例没有结果数据, 分发的当前数据为: %s", CommonConstant.objectMapper.writeValueAsString(pointData)));
            }else {

                String code = InternalTypeUtils.extractFieldValue(log, pointData, NODE_CODE);
                codePointAndAlgFieldMap.put(code, pointAndAlgFieldMap);
            }


            AlgConf algConf = new AlgConf();

            List<InstanceWork> instanceWorkList = new ArrayList<>();
            //将算法控制台广播的规则和数据主流相关联 方便下游按照code进行分流
            for (Map.Entry<String, Map<String,String>> algItem: codePointAndAlgFieldMap.entrySet() ) {

                //将本次算法结果绑定点位
                algConf.setCode(algItem.getKey());
                //点位和算法字段之间的映射关系
                Map<String, String> algFieldList = algItem.getValue();
                List<String> currentPointList = new ArrayList<>();
                List<String> algFieldNamesList = new ArrayList<>();
                for (Map.Entry<String, String> item: algFieldList.entrySet()) {
                    currentPointList.add(item.getKey());
                    algFieldNamesList.add(item.getValue());
                }

                //比较当前点位数据存在的特征值，和点位树配置的特征值相匹配
                Map<String, String> diffFeatureFieldResult = ReflectUtils.pointCompare(algFieldNamesList, pointData);

                algConf.setNodeIdList(new ArrayList<>(currentPointList));
                InstanceWork instanceWork = new InstanceWork();
                instanceWork.setFieldMap(diffFeatureFieldResult);
                instanceWorkList.add(instanceWork);



            }
            //设置当前节点编号code发往下游的算法实例列表
            algConf.setInstanceWorkList(instanceWorkList);
            algConf.setAlgFieldGroupMapping(algGroupMap);

            //发送给算法下游的算法实例格式
            dynamicKeyedBean.setAlgConf(algConf);
            dynamicKeyedBean.setAlgGroup(currentPointTree.getAlgGroup());
            dynamicKeyedBean.setCodeList(algConf.getCode());
            dynamicKeyedBean.setOrigin(origin);
            collector.collect(dynamicKeyedBean);


        }else if (StringUtils.isNotBlank(origin) && origin.equals(CommonConstant.iotLabel)){
            DynamicKeyedBean dynamicKeyedBean = DynamicKeyedBean.getInstance();
            dynamicKeyedBean.setPointData(pointDataStr);
            dynamicKeyedBean.setOrigin(origin);


            for (Map.Entry<String, String> entry: channelAndUpdateContext.entrySet()){

                //设置不入库字段 ,根据更新的缓存模版进行屏蔽
                if (entry.getKey().equals(RedisConstant.IOT_NOT_STORE_FIELDS)){
                    String updateConfContext = entry.getValue();
                    IotNotStoreEventResult eventResult = CommonConstant.objectMapper.readValue(updateConfContext, IotNotStoreEventResult.class);
                    Map<Integer, List<String>> notStoreFields = AnalysisUtils.analysisNotStoreFields(eventResult.getData()==null?new ArrayList<>():eventResult.getData());
                    List<String> notUpdateFields = notStoreFields.get(Integer.parseInt(pointIdStr));

                    Map<String, Object> pointNotStoreData = new HashMap<>();
                    for (String fields: notUpdateFields){
                        pointNotStoreData.put(fields,null);
                    }

                    pointData = ReflectUtils.dynamicAddProperty(pointData, Object.class, pointNotStoreData);
                    String pointDataNewStr = InternalTypeUtils.getValueStr(log, pointData);
                    dynamicKeyedBean.setPointData(pointDataNewStr);
                } else if (entry.getKey().equals(RedisConstant.IOT_POINT_TREE)){
                    //todo 设置点位树变更相关字段
                }

                collector.collect(dynamicKeyedBean);
            }


        }
    }

    @Override
    public void processBroadcastElement(String s, BroadcastProcessFunction<String, String, DynamicKeyedBean>.Context context, Collector<DynamicKeyedBean> collector) throws Exception {

    }


}
