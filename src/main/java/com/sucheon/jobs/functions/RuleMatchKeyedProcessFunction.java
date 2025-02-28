package com.sucheon.jobs.functions;

import com.sucheon.jobs.config.DistrbuteDependonDownStreamConf;
import com.sucheon.jobs.config.MapSingleton;
import com.sucheon.jobs.constant.CommonConstant;
import com.sucheon.jobs.event.*;
import com.sucheon.jobs.state.StateDescContainer;
import com.sucheon.jobs.utils.InternalTypeUtils;
import com.sucheon.jobs.utils.ReflectUtils;
import com.sucheon.jobs.utils.TransferBeanutils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.sucheon.jobs.constant.FieldConstants.NODE_CODE;
import static com.sucheon.jobs.constant.FieldConstants.ORIGIN_DATA;
import static com.sucheon.jobs.utils.TransferBeanutils.*;

/**
 * 规则匹配是否复合算法组的点位树json配置
 */
@Slf4j
public class RuleMatchKeyedProcessFunction extends KeyedBroadcastProcessFunction<String, DynamicKeyedBean, String, RuleMatchResult> {

    /**
     * 从算法控制台拿到的点位树配置
     */
    private ReadOnlyBroadcastState<String, String> broadcastState;

    /**
     * 记录当前实例是否会随着算法侧的配置进行变更
     */
    private MapStateDescriptor<String, AlgInstanceInfo> algInstanceInfoMapStateDescriptor;

    /**
     * 用户配置 仅用到分发开关
     */
    private DistrbuteDependonDownStreamConf userProperties;

    public RuleMatchKeyedProcessFunction(DistrbuteDependonDownStreamConf userProperties){
        this.userProperties = userProperties;


    }


    @Override
    public void processElement(DynamicKeyedBean dynamicKeyedBean, KeyedBroadcastProcessFunction<String, DynamicKeyedBean, String, RuleMatchResult>.ReadOnlyContext readOnlyContext, Collector<RuleMatchResult> collector) throws Exception {
        //todo 从广播的配置当中整理批次号， 时间戳等字段
        broadcastState = readOnlyContext.getBroadcastState(StateDescContainer.ruleStateDesc);


        List<PointTree> pointTrees = new ArrayList<>();
//        if (broadcastState!= null) {
//            if (dynamicKeyedBean != null || StringUtils.isNotBlank(dynamicKeyedBean.getInstanceId())){
//                pointTrees = broadcastState.get(dynamicKeyedBean.getInstanceId());
//            }
//        }



        AlgData algResult = new AlgData();

        //将输出字段按照实例进行分组整理
        List<AlgInstanceInfo> algInstanceInfoList = assembleAlgInstanceInfo(pointTrees);


        if (dynamicKeyedBean == null) {
            return;
        }

        //将当前执行的算法实例进行匹配(仅当算法控制台配置之后才进行匹配)
        AlgInstanceInfo currentInstance = new AlgInstanceInfo();
        if (StringUtils.isNotBlank(dynamicKeyedBean.getInstanceId()) && algInstanceInfoList.size() > 0){
            String currentInstanceId = dynamicKeyedBean.getInstanceId();

            for (AlgInstanceInfo instanceInfo: algInstanceInfoList) {
                if (instanceInfo.getInstanceId().equals(currentInstanceId)) {
                    currentInstance = instanceInfo;
                    break;
                }
            }

            //将获取的当前算法实例信息转换成重新投递到算法侧的数据格式
            String fieldGroup = CommonConstant.objectMapper.writeValueAsString(currentInstance.getFieldGroup());
            algResult.setGroupKey(fieldGroup);


            InstanceWork instanceWork = new InstanceWork();
            instanceWork.setFieldMap(currentInstance.getFieldBelongToInstance());
            instanceWork.setInstanceId(currentInstanceId);
            algResult.setInstanceWork(instanceWork);
        }





        String pointData = dynamicKeyedBean.getPointData();
        if (pointData == null) {
            return;
        }

        // 清空来源和节点编号
        Object originData = TransferBeanutils.transferNormalFormat(log, pointData);
        Map changeProperties = MapSingleton.INSTANCE.getInstance();
        InternalTypeUtils.addField(originData, ORIGIN_DATA, null);
        changeProperties.put(ORIGIN_DATA, null);
        changeProperties.put(NODE_CODE, null);
        Object changeData = ReflectUtils.getObject(originData, changeProperties);
        String changeDataStr = InternalTypeUtils.getValueStr(log, changeData);

        // 边缘端从默认配置取数据， 算法侧从广播变量取数据
        if (dynamicKeyedBean.getOrigin().equals(CommonConstant.iotLabel)) {

            //分发到clickhouse
            DistrbutePointData iotTopicDistrbuteData = buildRuleMatchResult(log, changeDataStr, CommonConstant.iotSinkTopic);
            RuleMatchResult ckRuleMatchResult = buildRuleMatchResult(log, iotTopicDistrbuteData, changeData);
            if (sendIfMessageNotNull(ckRuleMatchResult) && userProperties.getIsStoreOpen()) {
                collector.collect(ckRuleMatchResult);
            }

            //分发到告警
            DistrbutePointData alarmTopicData = buildRuleMatchResult(log, pointData, CommonConstant.alarmTopic);
            RuleMatchResult alarmRuleMatchResult = buildRuleMatchResult(log, alarmTopicData, changeData);
            if (sendIfMessageNotNull(alarmRuleMatchResult) && userProperties.getIsAlarmOpen()) {
                collector.collect(alarmRuleMatchResult);
            }
        } else if (dynamicKeyedBean.getOrigin().equals(CommonConstant.algLabel)){
            //todo 如果是联动点位可能会有多个code 暂时不做处理
            String code = dynamicKeyedBean.getCodeList();

            //todo 算法侧的topic名称需要从算法配置服务获取
            DistrbutePointData algDistrbuteData = buildAlgMatchResult(algResult, CommonConstant.algComputeTopic);
            RuleMatchResult ruleMatchResult = buildAlgRuleMatchResult(algDistrbuteData, algResult);

            //分发到算法框架
            if (sendIfMessageNotNull(ruleMatchResult) && userProperties.getIsAlgOpen()){
                collector.collect(ruleMatchResult);
            }

            //分发到告警
            DistrbutePointData alarmTopicData = buildAlgMatchResult(algResult, CommonConstant.alarmTopic);
            RuleMatchResult alarmRuleMatchResult = buildAlgRuleMatchResult(alarmTopicData, algResult);
            if (sendIfMessageNotNull(alarmRuleMatchResult) && userProperties.getIsAlarmOpen()) {
                collector.collect(alarmRuleMatchResult);
            }

            //分发到ck
            DistrbutePointData ckTopicData = buildAlgMatchResult(algResult, CommonConstant.algSinkTopic);
            RuleMatchResult ckRuleMatchResult = buildAlgRuleMatchResult(ckTopicData, algResult);
            if (sendIfMessageNotNull(ckRuleMatchResult) && userProperties.getIsStoreOpen()) {
                collector.collect(ckRuleMatchResult);
            }

        }


    }


    @Override
    public void processBroadcastElement(String channel, KeyedBroadcastProcessFunction<String, DynamicKeyedBean, String, RuleMatchResult>.Context context, Collector<RuleMatchResult> collector) throws Exception {
        //todo 算法侧处理广播数据

        broadcastState = context.getBroadcastState(StateDescContainer.ruleStateDesc);


    }



}
