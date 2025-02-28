package com.sucheon.jobs.event;

import java.io.Serializable;
import java.util.Map;

/**
 * 从上游算法实例算完算法结果
 */
public class AlgResult implements Serializable {

    /**
     * 存两级算法缓存.
     * 1.第一层存算法结果的分组里的特征值，
     * 比如（meanHf: 1.42222, mean: 21535353535354）
     * 2。第二层存该算法结果需要进行多少组字段分组
     * 比如
     * group1: { "meanHf": 1.42222, "mean": 21535353535354  },
     * group2: { "meanHf": 1.42222, "mean": 21535353535354 }
     */
    private Map<String, String> groupKeyList;

}
