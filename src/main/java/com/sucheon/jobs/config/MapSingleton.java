package com.sucheon.jobs.config;

import java.util.HashMap;
import java.util.Map;

public enum MapSingleton {

    INSTANCE;
    private HashMap mapInstance;
    MapSingleton(){
        mapInstance = new HashMap();
    }

    public HashMap getInstance(){
        return mapInstance;
    }
}
