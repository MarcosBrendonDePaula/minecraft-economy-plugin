package com.minecraft.mongodb.api;

import com.minecraft.mongodb.config.MongoConfig;

/**
 * Interface principal para interação com MongoDB
 */
public interface MongoClient {
    
    /**
     * Obtém uma referência para uma coleção MongoDB
     * @param collectionName Nome da coleção
     * @return Interface para operações na coleção
     */
    MongoCollection getCollection(String collectionName);
    
    /**
     * Obtém uma referência para o banco de dados MongoDB
     * @return Interface para operações no banco de dados
     */
    MongoDatabase getDatabase();
    
    /**
     * Verifica se o cliente está conectado
     * @return true se o cliente está conectado
     */
    boolean isConnected();
    
    /**
     * Conecta ao MongoDB
     * @return true se a conexão foi estabelecida com sucesso
     */
    boolean connect();
    
    /**
     * Desconecta do MongoDB
     */
    void disconnect();
    
    /**
     * Executa uma operação dentro de uma transação
     * @param operation Operação a ser executada
     * @param <T> Tipo do resultado
     * @return Resultado da operação
     */
    <T> T withTransaction(TransactionOperation<T> operation);
    
    /**
     * Obtém a configuração do cliente
     * @return Configuração do cliente
     */
    MongoConfig getConfig();
    
    /**
     * Cria um novo builder para configurar um cliente MongoDB
     * @return Builder para configuração
     */
    static Builder builder() {
        return new com.minecraft.mongodb.impl.DefaultMongoClientBuilder();
    }
    
    /**
     * Interface para operações em transações
     * @param <T> Tipo do resultado
     */
    interface TransactionOperation<T> {
        T execute(MongoClient client);
    }
    
    /**
     * Interface para construção de clientes MongoDB
     */
    interface Builder {
        /**
         * Define a string de conexão
         * @param connectionString String de conexão MongoDB
         * @return Builder para encadeamento
         */
        Builder connectionString(String connectionString);
        
        /**
         * Define o nome do banco de dados
         * @param database Nome do banco de dados
         * @return Builder para encadeamento
         */
        Builder database(String database);
        
        /**
         * Define se o cache está habilitado
         * @param enabled true para habilitar cache
         * @return Builder para encadeamento
         */
        Builder cacheEnabled(boolean enabled);
        
        /**
         * Define o tempo de expiração do cache
         * @param duration Duração
         * @param unit Unidade de tempo
         * @return Builder para encadeamento
         */
        Builder cacheExpiration(long duration, java.util.concurrent.TimeUnit unit);
        
        /**
         * Define o tamanho do pool de conexões
         * @param size Tamanho do pool
         * @return Builder para encadeamento
         */
        Builder poolSize(int size);
        
        /**
         * Define o timeout de conexão
         * @param timeout Timeout em milissegundos
         * @return Builder para encadeamento
         */
        Builder connectTimeout(int timeout);
        
        /**
         * Define o timeout de socket
         * @param timeout Timeout em milissegundos
         * @return Builder para encadeamento
         */
        Builder socketTimeout(int timeout);
        
        /**
         * Constrói o cliente MongoDB
         * @return Cliente MongoDB configurado
         */
        MongoClient build();
    }
}
