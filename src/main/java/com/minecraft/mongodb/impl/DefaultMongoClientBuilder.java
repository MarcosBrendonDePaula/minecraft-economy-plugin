package com.minecraft.mongodb.impl;

import com.minecraft.mongodb.api.MongoClient;
import com.minecraft.mongodb.config.MongoConfig;

import java.util.concurrent.TimeUnit;

/**
 * Implementação padrão do builder para MongoClient
 */
public class DefaultMongoClientBuilder implements MongoClient.Builder {
    
    private String connectionString = "mongodb://localhost:27017";
    private String database = "minecraft";
    private boolean cacheEnabled = true;
    private long cacheExpiration = 60;
    private TimeUnit cacheExpirationUnit = TimeUnit.SECONDS;
    private int poolSize = 10;
    private int connectTimeout = 5000;
    private int socketTimeout = 5000;
    
    @Override
    public MongoClient.Builder connectionString(String connectionString) {
        this.connectionString = connectionString;
        return this;
    }
    
    @Override
    public MongoClient.Builder database(String database) {
        this.database = database;
        return this;
    }
    
    @Override
    public MongoClient.Builder cacheEnabled(boolean enabled) {
        this.cacheEnabled = enabled;
        return this;
    }
    
    @Override
    public MongoClient.Builder cacheExpiration(long duration, TimeUnit unit) {
        this.cacheExpiration = duration;
        this.cacheExpirationUnit = unit;
        return this;
    }
    
    @Override
    public MongoClient.Builder poolSize(int size) {
        this.poolSize = size;
        return this;
    }
    
    @Override
    public MongoClient.Builder connectTimeout(int timeout) {
        this.connectTimeout = timeout;
        return this;
    }
    
    @Override
    public MongoClient.Builder socketTimeout(int timeout) {
        this.socketTimeout = timeout;
        return this;
    }
    
    @Override
    public MongoClient build() {
        MongoConfig config = MongoConfig.builder()
                .connectionString(connectionString)
                .databaseName(database)
                .cacheEnabled(cacheEnabled)
                .cacheExpiration(cacheExpiration, cacheExpirationUnit)
                .poolSize(poolSize)
                .connectTimeout(connectTimeout)
                .socketTimeout(socketTimeout)
                .maxWaitTime(5000)
                .build();
        
        return new DefaultMongoClient(config);
    }
}
