package com.sucheon.jobs.state;

import com.sucheon.jobs.event.AlgInstanceInfo;
import com.sucheon.jobs.event.PointTree;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.util.List;
import java.util.Map;

public class StateDescContainer {


    public static MapStateDescriptor<String, String> ruleStateDesc
            = new MapStateDescriptor<String, String>("rule_broadcast_state",
            TypeInformation.of(String.class),
            TypeInformation.of(new TypeHint<String>() {
                @Override
                public TypeInformation<String> getTypeInfo() {
                    return super.getTypeInfo();
                }
            }));


    public static MapStateDescriptor<String, AlgInstanceInfo> algInstanceInfoMapStateDescriptor
            = new MapStateDescriptor<String, AlgInstanceInfo>("alg_instance_state",
            TypeInformation.of(String.class),
            TypeInformation.of(new TypeHint<AlgInstanceInfo>() {
                @Override
                public TypeInformation<AlgInstanceInfo> getTypeInfo() {
                    return super.getTypeInfo();
                }
            }));
}
