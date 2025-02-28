package com.sucheon.jobs.event;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ResultData implements Serializable {

    /**
     * 当前计算完的算法实例instanceId
     */
    private String algInstanceId;

    /**
     * 当前计算完算法结果的时间戳
     * 1.秒级数据和定时评估 (取最后一秒的数据)
     * 2.联动点位取进入滑动窗口第一秒的数据
     */
    private Long deviceTimestamp;

    /**
     * 从边缘端上报上来的每一笔数据的批次编号
     */
    private String batchId;

    /**
     * 上一个算法实例的算法结果
     */
    private AlgResult algResult;


}
