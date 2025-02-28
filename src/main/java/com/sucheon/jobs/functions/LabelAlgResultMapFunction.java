package com.sucheon.jobs.functions;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sucheon.jobs.constant.CommonConstant;
import com.sucheon.jobs.event.PointData;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.MapFunction;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.sucheon.jobs.constant.CommonConstant.algLabel;
import static com.sucheon.jobs.constant.FieldConstants.ORIGIN_DATA;

/**
 * 算法侧打标
 */
@Slf4j
public class LabelAlgResultMapFunction implements MapFunction<String, String> {



    @Override
    public String map(String algResult)  {
        Map<String, Object> map = new HashMap<>();
        try {
            map = CommonConstant.objectMapper.readValue(algResult, new TypeReference<Map<String, Object>>() {
            });
            map.put(ORIGIN_DATA, algLabel);
            algResult = CommonConstant.objectMapper.writeValueAsString(map);
        } catch (IOException e){
            log.error(String.format("当前算法端数据解析错误,源数据为: %s", algResult));
        }

        return algResult;
    }
}
