package com.sucheon.jobs.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class SingleInstanceRedisson {


    private Config config;


    public SingleInstanceRedisson(String host, Integer port, String password){
        config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setPassword(password);
    }


    public RedissonClient connect(){
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }
}
