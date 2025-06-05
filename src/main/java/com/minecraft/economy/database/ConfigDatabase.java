package com.minecraft.economy.database;

import com.minecraft.economy.core.EconomyPlugin;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Gerenciador de configurações armazenadas no MongoDB
 */
public class ConfigDatabase {

    private final EconomyPlugin plugin;
    private final ResilientMongoDBManager mongoManager;
    private final Map<String, Object> configCache = new HashMap<>();
    private final long cacheDuration = 60000; // 1 minuto
    private long lastCacheUpdate = 0;

    public ConfigDatabase(EconomyPlugin plugin) {
        this.plugin = plugin;
        this.mongoManager = plugin.getMongoDBManager();
        loadConfigFromDatabase();
    }

    /**
     * Carrega as configurações do banco de dados
     */
    private void loadConfigFromDatabase() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    MongoCollection<Document> configCollection = mongoManager.getConfigCollection();
                    
                    // Carrega todas as configurações para o cache
                    for (Document doc : configCollection.find()) {
                        String key = doc.getString("key");
                        Object value = doc.get("value");
                        configCache.put(key, value);
                    }
                    
                    lastCacheUpdate = System.currentTimeMillis();
                    plugin.getLogger().info("Configurações carregadas do banco de dados com sucesso!");
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Erro ao carregar configurações do banco de dados: " + e.getMessage(), e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Obtém uma configuração do banco de dados
     * @param key Chave da configuração
     * @param defaultValue Valor padrão caso a configuração não exista
     * @return CompletableFuture com o valor da configuração
     */
    public <T> CompletableFuture<T> getConfig(String key, T defaultValue) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        // Verifica se o cache está válido
        if (System.currentTimeMillis() - lastCacheUpdate < cacheDuration && configCache.containsKey(key)) {
            @SuppressWarnings("unchecked")
            T value = (T) configCache.get(key);
            future.complete(value);
            return future;
        }
        
        // Se o cache expirou ou a chave não existe, busca no banco de dados
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    MongoCollection<Document> configCollection = mongoManager.getConfigCollection();
                    Document doc = configCollection.find(Filters.eq("key", key)).first();
                    
                    if (doc != null) {
                        @SuppressWarnings("unchecked")
                        T value = (T) doc.get("value");
                        configCache.put(key, value);
                        future.complete(value);
                    } else {
                        future.complete(defaultValue);
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Erro ao obter configuração do banco de dados: " + e.getMessage(), e);
                    future.complete(defaultValue);
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return future;
    }

    /**
     * Define uma configuração no banco de dados
     * @param key Chave da configuração
     * @param value Valor da configuração
     * @return CompletableFuture com o resultado da operação
     */
    public CompletableFuture<Boolean> setConfig(String key, Object value) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        // Atualiza o cache imediatamente
        configCache.put(key, value);
        
        // Atualiza o banco de dados de forma assíncrona
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    MongoCollection<Document> configCollection = mongoManager.getConfigCollection();
                    
                    // Verifica se a configuração já existe
                    Document doc = configCollection.find(Filters.eq("key", key)).first();
                    
                    if (doc != null) {
                        // Atualiza a configuração existente
                        configCollection.updateOne(
                            Filters.eq("key", key),
                            new Document("$set", new Document("value", value))
                        );
                    } else {
                        // Cria uma nova configuração
                        configCollection.insertOne(
                            new Document()
                                .append("key", key)
                                .append("value", value)
                        );
                    }
                    
                    future.complete(true);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Erro ao definir configuração no banco de dados: " + e.getMessage(), e);
                    future.complete(false);
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return future;
    }

    /**
     * Remove uma configuração do banco de dados
     * @param key Chave da configuração
     * @return CompletableFuture com o resultado da operação
     */
    public CompletableFuture<Boolean> removeConfig(String key) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        // Remove do cache imediatamente
        configCache.remove(key);
        
        // Remove do banco de dados de forma assíncrona
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    MongoCollection<Document> configCollection = mongoManager.getConfigCollection();
                    configCollection.deleteOne(Filters.eq("key", key));
                    future.complete(true);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Erro ao remover configuração do banco de dados: " + e.getMessage(), e);
                    future.complete(false);
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return future;
    }

    /**
     * Recarrega as configurações do banco de dados
     */
    public void reloadConfig() {
        loadConfigFromDatabase();
    }
    
    /**
     * Obtém uma configuração do tipo String
     * @param key Chave da configuração
     * @param defaultValue Valor padrão caso a configuração não exista
     * @return Valor da configuração
     */
    public String getString(String key, String defaultValue) {
        Object value = configCache.getOrDefault(key, defaultValue);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }
    
    /**
     * Obtém uma configuração do tipo int
     * @param key Chave da configuração
     * @param defaultValue Valor padrão caso a configuração não exista
     * @return Valor da configuração
     */
    public int getInt(String key, int defaultValue) {
        Object value = configCache.getOrDefault(key, defaultValue);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    /**
     * Obtém uma configuração do tipo long
     * @param key Chave da configuração
     * @param defaultValue Valor padrão caso a configuração não exista
     * @return Valor da configuração
     */
    public long getLong(String key, long defaultValue) {
        Object value = configCache.getOrDefault(key, defaultValue);
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }
    
    /**
     * Obtém uma configuração do tipo double
     * @param key Chave da configuração
     * @param defaultValue Valor padrão caso a configuração não exista
     * @return Valor da configuração
     */
    public double getDouble(String key, double defaultValue) {
        Object value = configCache.getOrDefault(key, defaultValue);
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
    
    /**
     * Obtém uma configuração do tipo boolean
     * @param key Chave da configuração
     * @param defaultValue Valor padrão caso a configuração não exista
     * @return Valor da configuração
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = configCache.getOrDefault(key, defaultValue);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
}
