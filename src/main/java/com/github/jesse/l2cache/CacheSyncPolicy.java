package com.github.jesse.l2cache;

import com.github.jesse.l2cache.spi.SPI;
import com.github.jesse.l2cache.sync.CacheMessage;
import com.github.jesse.l2cache.sync.MessageListener;

import java.io.Serializable;

@SPI
public interface CacheSyncPolicy extends Serializable {
    CacheConfig getCacheConfig();

    CacheSyncPolicy setCacheConfig(CacheConfig var1);

    MessageListener getCacheMessageListener();

    CacheSyncPolicy setCacheMessageListener(MessageListener var1);

    Object getActualClient();

    CacheSyncPolicy setActualClient(Object var1);

    void connnect();

    void publish(CacheMessage var1);

    void disconnect();
}
