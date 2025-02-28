package com.sucheon.jobs.utils;

import com.sucheon.jobs.listener.context.result.PointNotStoreInfo;
import com.sucheon.jobs.listener.context.result.PointTreeSpaceInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析订阅模版当中需要变更的类
 */
public class AnalysisUtils {

    /**
     * 解析配置中的不入库字段
     * @param pointTreeSpaceInfoList
     * @return
     */
    public static Map<Integer, List<String>> analysisNotStoreFields(List<PointTreeSpaceInfo> pointTreeSpaceInfoList){
        Map<Integer, List<String>> result = new HashMap<>();
        for (PointTreeSpaceInfo pointTreeSpaceInfo: pointTreeSpaceInfoList){
            List<PointNotStoreInfo> pointNotStoreInfoList = pointTreeSpaceInfo.getPointTree();
            for (PointNotStoreInfo pointNotStoreInfo: pointNotStoreInfoList){
                result.put(pointNotStoreInfo.getPointId(), pointNotStoreInfo.getNotStoreFields());
            }
        }
        return result;
    }

}
