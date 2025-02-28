package com.sucheon.jobs.functions;

import com.alibaba.fastjson.JSON;
import com.sucheon.jobs.event.PointData;
import com.sucheon.jobs.event.PointTree;
import com.sucheon.jobs.utils.ExceptionUtil;
import com.sucheon.jobs.utils.ReflectUtils;
import com.sucheon.jobs.utils.TransferBeanutils;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.MapFunction;

@Slf4j
public class Json2EventBeanMapFunction implements MapFunction<String, Object> {


    @Override
    public Object map(String value) {

        Object pointData = null;
        try {
            pointData = TransferBeanutils.transferNormalFormat(log, value);

        } catch (Exception ex) {
            String errorMessage = ExceptionUtil.getErrorMessage(ex);
            log.error("数据解析失败，当前时间戳为: {}, 具体原因为: {}", System.currentTimeMillis(), errorMessage);
        }
        return pointData;
    }
}
