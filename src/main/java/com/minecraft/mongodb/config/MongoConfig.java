package com.minecraft.mongodb.config;

import java.util.concurrent.TimeUnit;

/**
 * Configuração para conexão com MongoDB
 */
public class MongoConfig {
    
    private final String connectionString;
    private final String databaseName;
    private final int connectTimeout;
    private final int socketTimeout;
    private final int maxWaitTime;
    private final int poolSize;
    private final boolean cacheEnabled;
    private final long cacheExpiration;
    private final TimeUnit cacheExpirationUnit;
    
    /**
     * Construtor
     * @param builder Builder com as configurações
     */
    private MongoConfig(Builder builder) {
        this.connectionString = builder.connectionString;
        this.databaseName = builder.databaseName;
        this.connectTimeout = builder.connectTimeout;
        this.socketTimeout = builder.socketTimeout;
        this.maxWaitTime = builder.maxWaitTime;
        this.poolSize = builder.poolSize;
        this.cacheEnabled = builder.cacheEnabled;
        this.cacheExpiration = builder.cacheExpiration;
        this.cacheExpirationUnit = builder.cacheExpirationUnit;
    }
    
    /**
     * Obtém a string de conexão
     * @return String de conexão
     */
    public String getConnectionString() {
        return connectionString;
    }
    
    /**
     * Obtém o nome do banco de dados
     * @return Nome do banco de dados
     */
    public String getDatabaseName() {
        return databaseName;
    }
    
    /**
     * Obtém o timeout de conexão
     * @return Timeout de conexão em milissegundos
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }
    
    /**
     * Obtém o timeout de socket
     * @return Timeout de socket em milissegundos
     */
    public int getSocketTimeout() {
        return socketTimeout;
    }
    
    /**
     * Obtém o tempo máximo de espera
     * @return Tempo máximo de espera em milissegundos
     */
    public int getMaxWaitTime() {
        return maxWaitTime;
    }
    
    /**
     * Obtém o tamanho do pool de conexões
     * @return Tamanho do pool de conexões
     */
    public int getPoolSize() {
        return poolSize;
    }
    
    /**
     * Verifica se o cache está habilitado
     * @return true se o cache está habilitado
     */
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }
    
    /**
     * Obtém o tempo de expiração do cache
     * @return Tempo de expiração do cache
     */
    public long getCacheExpiration() {
        return cacheExpiration;
    }
    
    /**
     * Obtém a unidade de tempo para expiração do cache
     * @return Unidade de tempo
     */
    public TimeUnit getCacheExpirationUnit() {
        return cacheExpirationUnit;
    }
    
    /**
     * Cria um novo builder para configuração
     * @return Builder para configuração
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder para configuração
     */
    public static class Builder {
        private String connectionString = "mongodb://localhost:27017";
        private String databaseName = "minecraft";
        private int connectTimeout = 5000;
        private int socketTimeout = 5000;
        private int maxWaitTime = 5000;
        private int poolSize = 10;
        private boolean cacheEnabled = true;
        private long cacheExpiration = 60;
        private TimeUnit cacheExpirationUnit = TimeUnit.SECONDS;
        
        /**
         * Define a string de conexão
         * @param connectionString String de conexão
         * @return Builder para encadeamento
         */
        public Builder connectionString(String connectionString) {
            this.connectionString = connectionString;
            return this;
        }
        
        /**
         * Define o nome do banco de dados
         * @param databaseName Nome do banco de dados
         * @return Builder para encadeamento
         */
        public Builder databaseName(String databaseName) {
            this.databaseName = databaseName;
            return this;
        }
        
        /**
         * Define o timeout de conexão
         * @param connectTimeout Timeout de conexão em milissegundos
         * @return Builder para encadeamento
         */
        public Builder connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }
        
        /**
         * Define o timeout de socket
         * @param socketTimeout Timeout de socket em milissegundos
         * @return Builder para encadeamento
         */
        public Builder socketTimeout(int socketTimeout) {
            this.socketTimeout = socketTimeout;
            return this;
        }
        
        /**
         * Define o tempo máximo de espera
         * @param maxWaitTime Tempo máximo de espera em milissegundos
         * @return Builder para encadeamento
         */
        public Builder maxWaitTime(int maxWaitTime) {
            this.maxWaitTime = maxWaitTime;
            return this;
        }
        
        /**
         * Define o tamanho do pool de conexões
         * @param poolSize Tamanho do pool de conexões
         * @return Builder para encadeamento
         */
        public Builder poolSize(int poolSize) {
            this.poolSize = poolSize;
            return this;
        }
        
        /**
         * Define se o cache está habilitado
         * @param cacheEnabled true para habilitar cache
         * @return Builder para encadeamento
         */
        public Builder cacheEnabled(boolean cacheEnabled) {
            this.cacheEnabled = cacheEnabled;
            return this;
        }
        
        /**
         * Define o tempo de expiração do cache
         * @param cacheExpiration Tempo de expiração
         * @param unit Unidade de tempo
         * @return Builder para encadeamento
         */
        public Builder cacheExpiration(long cacheExpiration, TimeUnit unit) {
            this.cacheExpiration = cacheExpiration;
            this.cacheExpirationUnit = unit;
            return this;
        }
        
        /**
         * Constrói a configuração
         * @return Configuração
         */
        public MongoConfig build() {
            return new MongoConfig(this);
        }
    }
}
