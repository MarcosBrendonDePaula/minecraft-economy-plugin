package com.minecraft.mongodb.impl;

import com.minecraft.mongodb.api.MongoClient;
import com.minecraft.mongodb.api.MongoCollection;
import com.minecraft.mongodb.api.MongoDatabase;
import com.minecraft.mongodb.config.MongoConfig;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClients;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementação padrão do MongoClient
 */
public class DefaultMongoClient implements MongoClient {
    
    private final MongoConfig config;
    private com.mongodb.client.MongoClient mongoClient;
    private DefaultMongoDatabase database;
    private final Map<String, MongoCollection> collections = new HashMap<>();
    private boolean connected = false;
    private final Logger logger = Logger.getLogger(DefaultMongoClient.class.getName());
    
    /**
     * Construtor
     * @param config Configuração do cliente
     */
    public DefaultMongoClient(MongoConfig config) {
        this.config = config;
    }
    
    @Override
    public MongoCollection getCollection(String collectionName) {
        if (!isConnected()) {
            connect();
        }
        
        return collections.computeIfAbsent(collectionName, name -> {
            com.mongodb.client.MongoCollection<Document> mongoCollection = database.getMongoDatabase().getCollection(name);
            return new DefaultMongoCollection(this, mongoCollection, name);
        });
    }
    
    @Override
    public MongoDatabase getDatabase() {
        if (!isConnected()) {
            connect();
        }
        
        return database;
    }
    
    @Override
    public boolean isConnected() {
        return connected && mongoClient != null;
    }
    
    @Override
    public boolean connect() {
        if (isConnected()) {
            return true;
        }
        
        try {
            logger.info("Conectando ao MongoDB: " + config.getConnectionString().replaceAll("mongodb://([^:]+):([^@]+)@", "mongodb://****:****@"));
            
            MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(config.getConnectionString()))
                .applyToConnectionPoolSettings(builder -> 
                    builder.maxSize(config.getPoolSize())
                          .maxWaitTime(config.getMaxWaitTime(), TimeUnit.MILLISECONDS))
                .applyToSocketSettings(builder -> 
                    builder.connectTimeout(config.getConnectTimeout(), TimeUnit.MILLISECONDS)
                          .readTimeout(config.getSocketTimeout(), TimeUnit.MILLISECONDS))
                .build();
            
            mongoClient = MongoClients.create(settings);
            
            // Testa a conexão com ping
            Document pingResult = mongoClient.getDatabase("admin").runCommand(new Document("ping", 1));
            if (pingResult.getDouble("ok") != 1.0) {
                throw new MongoException("Falha no ping ao servidor MongoDB");
            }
            
            com.mongodb.client.MongoDatabase mongoDatabase = mongoClient.getDatabase(config.getDatabaseName());
            database = new DefaultMongoDatabase(this, mongoDatabase);
            
            connected = true;
            logger.info("Conexão com MongoDB estabelecida com sucesso!");
            
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao conectar ao MongoDB: " + e.getMessage(), e);
            disconnect();
            return false;
        }
    }
    
    @Override
    public void disconnect() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
                logger.info("Conexão com MongoDB fechada com sucesso!");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Erro ao fechar conexão com MongoDB: " + e.getMessage(), e);
            } finally {
                mongoClient = null;
                database = null;
                collections.clear();
                connected = false;
            }
        }
    }
    
    @Override
    public <T> T withTransaction(TransactionOperation<T> operation) {
        if (!isConnected()) {
            connect();
        }
        
        try {
            return operation.execute(this);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao executar transação: " + e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public MongoConfig getConfig() {
        return config;
    }
    
    /**
     * Obtém o cliente MongoDB nativo
     * @return Cliente MongoDB nativo
     */
    com.mongodb.client.MongoClient getMongoClient() {
        if (!isConnected()) {
            connect();
        }
        
        return mongoClient;
    }
}
