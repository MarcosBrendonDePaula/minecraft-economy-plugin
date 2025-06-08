package com.minecraft.economy.api.repository;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface para repositório de configurações
 * Define operações para acesso e manipulação de configurações
 */
public interface ConfigurationRepository {
    
    /**
     * Obtém uma configuração
     * @param key Chave da configuração
     * @param defaultValue Valor padrão caso a configuração não exista
     * @param <T> Tipo do valor
     * @return CompletableFuture com o valor da configuração
     */
    <T> CompletableFuture<T> getConfig(String key, T defaultValue);
    
    /**
     * Define uma configuração
     * @param key Chave da configuração
     * @param value Valor da configuração
     * @return CompletableFuture com o resultado da operação
     */
    CompletableFuture<Boolean> setConfig(String key, Object value);
    
    /**
     * Remove uma configuração
     * @param key Chave da configuração
     * @return CompletableFuture com o resultado da operação
     */
    CompletableFuture<Boolean> removeConfig(String key);
    
    /**
     * Verifica se uma configuração existe
     * @param key Chave da configuração
     * @return CompletableFuture com o resultado da verificação
     */
    CompletableFuture<Boolean> hasConfig(String key);
    
    /**
     * Obtém todas as configurações
     * @return CompletableFuture com um mapa de configurações
     */
    CompletableFuture<Map<String, Object>> getAllConfigs();
    
    /**
     * Carrega as configurações do armazenamento
     * @return CompletableFuture com o resultado da operação
     */
    CompletableFuture<Boolean> loadConfigs();
    
    /**
     * Salva as configurações no armazenamento
     * @return CompletableFuture com o resultado da operação
     */
    CompletableFuture<Boolean> saveConfigs();
    
    /**
     * Limpa o cache de configurações
     */
    void clearCache();
}
