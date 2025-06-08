package com.minecraft.economy.repository;

import com.minecraft.economy.api.infrastructure.AsyncExecutor;
import com.minecraft.economy.api.infrastructure.CacheManager;
import com.minecraft.economy.api.repository.ConfigurationRepository;
import com.minecraft.economy.infrastructure.MongoDBConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementação de ConfigurationRepository usando MongoDB
 */
public class MongoConfigurationRepository implements ConfigurationRepository {

    private final MongoDBConnection dbConnection;
    private final AsyncExecutor asyncExecutor;
    private final CacheManager<String, Object> configCache;
    private final Logger logger;
    
    private static final String COLLECTION_NAME = "config";
    
    /**
     * Construtor
     * @param dbConnection Conexão com o MongoDB
     * @param asyncExecutor Executor assíncrono
     * @param configCache Cache de configurações
     * @param logger Logger
     */
    public MongoConfigurationRepository(
            MongoDBConnection dbConnection,
            AsyncExecutor asyncExecutor,
            CacheManager<String, Object> configCache,
            Logger logger) {
        this.dbConnection = dbConnection;
        this.asyncExecutor = asyncExecutor;
        this.configCache = configCache;
        this.logger = logger;
        
        // Carrega as configurações iniciais
        loadConfigs();
    }
    
    /**
     * Obtém a coleção de configurações
     * @return Coleção de configurações
     */
    private MongoCollection<Document> getCollection() {
        return dbConnection.getDatabase().getCollection(COLLECTION_NAME);
    }

    @Override
    public <T> CompletableFuture<T> getConfig(String key, T defaultValue) {
        // Verifica se há um valor em cache
        Object cachedValue = configCache.get(key);
        if (cachedValue != null) {
            try {
                @SuppressWarnings("unchecked")
                T value = (T) cachedValue;
                return CompletableFuture.completedFuture(value);
            } catch (ClassCastException e) {
                logger.warning("Erro ao converter valor em cache para " + key + ": " + e.getMessage());
            }
        }
        
        return asyncExecutor.supplyAsync(() -> {
            try {
                if (!dbConnection.ensureConnected()) {
                    logger.warning("Falha ao obter configuração: Sem conexão com o banco de dados");
                    return defaultValue;
                }
                
                Document configDoc = getCollection().find(Filters.eq("key", key)).first();
                
                if (configDoc != null) {
                    Object value = configDoc.get("value");
                    
                    try {
                        @SuppressWarnings("unchecked")
                        T typedValue = (T) value;
                        
                        // Atualiza o cache
                        configCache.put(key, typedValue, 5, TimeUnit.MINUTES);
                        
                        return typedValue;
                    } catch (ClassCastException e) {
                        logger.warning("Erro ao converter valor para " + key + ": " + e.getMessage());
                        return defaultValue;
                    }
                } else {
                    return defaultValue;
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erro ao obter configuração: " + e.getMessage(), e);
                return defaultValue;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> setConfig(String key, Object value) {
        // Atualiza o cache imediatamente
        configCache.put(key, value, 5, TimeUnit.MINUTES);
        
        return asyncExecutor.supplyAsync(() -> {
            try {
                if (!dbConnection.ensureConnected()) {
                    logger.warning("Falha ao definir configuração: Sem conexão com o banco de dados");
                    return false;
                }
                
                Document configDoc = new Document()
                        .append("key", key)
                        .append("value", value);
                
                getCollection().replaceOne(
                    Filters.eq("key", key),
                    configDoc,
                    new ReplaceOptions().upsert(true)
                );
                
                return true;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erro ao definir configuração: " + e.getMessage(), e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> removeConfig(String key) {
        // Remove do cache imediatamente
        configCache.remove(key);
        
        return asyncExecutor.supplyAsync(() -> {
            try {
                if (!dbConnection.ensureConnected()) {
                    logger.warning("Falha ao remover configuração: Sem conexão com o banco de dados");
                    return false;
                }
                
                getCollection().deleteOne(Filters.eq("key", key));
                
                return true;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erro ao remover configuração: " + e.getMessage(), e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> hasConfig(String key) {
        // Verifica se há um valor em cache
        if (configCache.containsKey(key)) {
            return CompletableFuture.completedFuture(true);
        }
        
        return asyncExecutor.supplyAsync(() -> {
            try {
                if (!dbConnection.ensureConnected()) {
                    logger.warning("Falha ao verificar configuração: Sem conexão com o banco de dados");
                    return false;
                }
                
                Document configDoc = getCollection().find(Filters.eq("key", key)).first();
                return configDoc != null;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erro ao verificar configuração: " + e.getMessage(), e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Map<String, Object>> getAllConfigs() {
        return asyncExecutor.supplyAsync(() -> {
            try {
                if (!dbConnection.ensureConnected()) {
                    logger.warning("Falha ao obter todas as configurações: Sem conexão com o banco de dados");
                    return new HashMap<>();
                }
                
                Map<String, Object> configs = new HashMap<>();
                
                getCollection().find().forEach(doc -> {
                    String key = doc.getString("key");
                    Object value = doc.get("value");
                    
                    configs.put(key, value);
                    
                    // Atualiza o cache
                    configCache.put(key, value, 5, TimeUnit.MINUTES);
                });
                
                return configs;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erro ao obter todas as configurações: " + e.getMessage(), e);
                return new HashMap<>();
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> loadConfigs() {
        return asyncExecutor.supplyAsync(() -> {
            try {
                if (!dbConnection.ensureConnected()) {
                    logger.warning("Falha ao carregar configurações: Sem conexão com o banco de dados");
                    return false;
                }
                
                // Limpa o cache
                configCache.clear();
                
                // Carrega todas as configurações para o cache
                getCollection().find().forEach(doc -> {
                    String key = doc.getString("key");
                    Object value = doc.get("value");
                    
                    configCache.put(key, value, 5, TimeUnit.MINUTES);
                });
                
                logger.info("Configurações carregadas com sucesso!");
                return true;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erro ao carregar configurações: " + e.getMessage(), e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> saveConfigs() {
        // Esta implementação não precisa fazer nada especial para salvar,
        // pois as configurações são salvas individualmente quando setConfig é chamado
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public void clearCache() {
        configCache.clear();
    }
}
