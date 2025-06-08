package com.minecraft.mongodb.impl;

import com.minecraft.mongodb.api.MongoCollection;
import com.minecraft.mongodb.api.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementação padrão do MongoDatabase
 */
public class DefaultMongoDatabase implements MongoDatabase {
    
    private final DefaultMongoClient client;
    private final com.mongodb.client.MongoDatabase mongoDatabase;
    private final Map<String, MongoCollection> collections = new HashMap<>();
    private final Logger logger = Logger.getLogger(DefaultMongoDatabase.class.getName());
    
    /**
     * Construtor
     * @param client Cliente MongoDB
     * @param mongoDatabase Banco de dados MongoDB nativo
     */
    public DefaultMongoDatabase(DefaultMongoClient client, com.mongodb.client.MongoDatabase mongoDatabase) {
        this.client = client;
        this.mongoDatabase = mongoDatabase;
    }
    
    @Override
    public String getName() {
        return mongoDatabase.getName();
    }
    
    @Override
    public MongoCollection getCollection(String collectionName) {
        return collections.computeIfAbsent(collectionName, name -> {
            com.mongodb.client.MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(name);
            return new DefaultMongoCollection(client, mongoCollection, name);
        });
    }
    
    @Override
    public boolean collectionExists(String collectionName) {
        try {
            for (String name : listCollections()) {
                if (name.equals(collectionName)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erro ao verificar existência da coleção: " + e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public CompletableFuture<Boolean> collectionExistsAsync(String collectionName) {
        return CompletableFuture.supplyAsync(this::listCollections)
                .thenApply(collections -> collections.contains(collectionName))
                .exceptionally(e -> {
                    logger.log(Level.WARNING, "Erro ao verificar existência da coleção de forma assíncrona: " + e.getMessage(), e);
                    return false;
                });
    }
    
    @Override
    public boolean createCollection(String collectionName) {
        try {
            mongoDatabase.createCollection(collectionName);
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erro ao criar coleção: " + e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public CompletableFuture<Boolean> createCollectionAsync(String collectionName) {
        return CompletableFuture.supplyAsync(() -> createCollection(collectionName))
                .exceptionally(e -> {
                    logger.log(Level.WARNING, "Erro ao criar coleção de forma assíncrona: " + e.getMessage(), e);
                    return false;
                });
    }
    
    @Override
    public boolean dropCollection(String collectionName) {
        try {
            mongoDatabase.getCollection(collectionName).drop();
            collections.remove(collectionName);
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erro ao excluir coleção: " + e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public CompletableFuture<Boolean> dropCollectionAsync(String collectionName) {
        return CompletableFuture.supplyAsync(() -> dropCollection(collectionName))
                .exceptionally(e -> {
                    logger.log(Level.WARNING, "Erro ao excluir coleção de forma assíncrona: " + e.getMessage(), e);
                    return false;
                });
    }
    
    @Override
    public List<String> listCollections() {
        List<String> result = new ArrayList<>();
        try {
            mongoDatabase.listCollectionNames().into(result);
            return result;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erro ao listar coleções: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public CompletableFuture<List<String>> listCollectionsAsync() {
        return CompletableFuture.supplyAsync(this::listCollections)
                .exceptionally(e -> {
                    logger.log(Level.WARNING, "Erro ao listar coleções de forma assíncrona: " + e.getMessage(), e);
                    return new ArrayList<>();
                });
    }
    
    @Override
    public Object runCommand(Object command) {
        try {
            if (command instanceof Document) {
                return mongoDatabase.runCommand((Document) command);
            } else if (command instanceof String) {
                return mongoDatabase.runCommand(Document.parse((String) command));
            } else {
                throw new IllegalArgumentException("Comando deve ser um Document ou uma String JSON");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erro ao executar comando: " + e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public CompletableFuture<Object> runCommandAsync(Object command) {
        return CompletableFuture.supplyAsync(() -> runCommand(command))
                .exceptionally(e -> {
                    logger.log(Level.WARNING, "Erro ao executar comando de forma assíncrona: " + e.getMessage(), e);
                    return null;
                });
    }
    
    /**
     * Obtém o banco de dados MongoDB nativo
     * @return Banco de dados MongoDB nativo
     */
    com.mongodb.client.MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }
}
