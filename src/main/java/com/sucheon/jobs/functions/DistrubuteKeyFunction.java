package com.sucheon.jobs.functions;

import cn.hutool.core.util.IdUtil;
import com.sucheon.jobs.event.DynamicKeyedBean;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.runtime.state.KeyGroupRangeAssignment;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Optional;

/**
 * 对于算法数据和iot数据进行区分
 * 如果合流之后的数据没有设置算法实例， 则意味着是从测点上报的数据， 当前没有算法实例在运行
 */
@Slf4j
public class DistrubuteKeyFunction implements KeySelector<DynamicKeyedBean, Object> {


    /**
     * keyby算子需要指定的并行度
     */
    private int parallelism;

    /**
     * 匀散到每个分区的平衡键
     */
    private Integer[] rebalanceKeys;

    public DistrubuteKeyFunction(int parallelism){
        this.parallelism = parallelism;
        rebalanceKeys = createRebalanceKeys(parallelism);
    }

    @Override
    public Object getKey(DynamicKeyedBean dynamicKeyedBean) throws Exception {

        //采用雪花id做唯一的逻辑id
        String sourceDataId = IdUtil.getSnowflakeNextIdStr();

        return rebalanceKeys[Integer.parseInt(sourceDataId) % parallelism];
    }

    /**
     * 根据并行度均匀的分发到各个subtask
     * @param parallelism
     * @return
     */
    public Integer[] createRebalanceKeys(int parallelism){
        HashMap<Integer, LinkedHashSet<Integer>> groupRanges = new HashMap<>();
        int maxParallelism = KeyGroupRangeAssignment.computeDefaultMaxParallelism(parallelism);
        int maxRandomKey = parallelism * 12;
        for (int randomKey = 0; randomKey < maxRandomKey; randomKey++) {
            int subtaskIndex = KeyGroupRangeAssignment.assignKeyToParallelOperator(randomKey, maxParallelism, parallelism);
            LinkedHashSet<Integer> randomKeys = groupRanges.computeIfAbsent(subtaskIndex, k -> new LinkedHashSet<>());
            randomKeys.add(randomKey);
        }
        Integer[] rebalanceKeys = new Integer[parallelism];
        for (int i = 0; i < parallelism; i++) {
            LinkedHashSet<Integer> ranges = groupRanges.get(i);
            if (ranges == null || ranges.isEmpty()) {

                log.error("Create rebalanceKeys fail.");
            } else {
                rebalanceKeys[i] = ranges.stream().findFirst().get();
            }
        }
        return rebalanceKeys;
    }
}
