package com.minecraft.economy.api.service;

import java.util.concurrent.CompletableFuture;

/**
 * Interface para serviço de configuração
 * Define operações para acesso e manipulação de configurações
 */
public interface ConfigurationService {
    
    /**
     * Obtém uma configuração do tipo String
     * @param key Chave da configuração
     * @param defaultValue Valor padrão caso a configuração não exista
     * @return Valor da configuração
     */
    String getString(String key, String defaultValue);
    
    /**
     * Obtém uma configuração do tipo int
     * @param key Chave da configuração
     * @param defaultValue Valor padrão caso a configuração não exista
     * @return Valor da configuração
     */
    int getInt(String key, int defaultValue);
    
    /**
     * Obtém uma configuração do tipo long
     * @param key Chave da configuração
     * @param defaultValue Valor padrão caso a configuração não exista
     * @return Valor da configuração
     */
    long getLong(String key, long defaultValue);
    
    /**
     * Obtém uma configuração do tipo double
     * @param key Chave da configuração
     * @param defaultValue Valor padrão caso a configuração não exista
     * @return Valor da configuração
     */
    double getDouble(String key, double defaultValue);
    
    /**
     * Obtém uma configuração do tipo boolean
     * @param key Chave da configuração
     * @param defaultValue Valor padrão caso a configuração não exista
     * @return Valor da configuração
     */
    boolean getBoolean(String key, boolean defaultValue);
    
    /**
     * Define uma configuração
     * @param key Chave da configuração
     * @param value Valor da configuração
     * @return CompletableFuture com o resultado da operação
     */
    CompletableFuture<Boolean> setConfig(String key, Object value);
    
    /**
     * Obtém uma configuração de forma assíncrona
     * @param key Chave da configuração
     * @param defaultValue Valor padrão caso a configuração não exista
     * @param <T> Tipo do valor
     * @return CompletableFuture com o valor da configuração
     */
    <T> CompletableFuture<T> getConfigAsync(String key, T defaultValue);
    
    /**
     * Remove uma configuração
     * @param key Chave da configuração
     * @return CompletableFuture com o resultado da operação
     */
    CompletableFuture<Boolean> removeConfig(String key);
    
    /**
     * Recarrega as configurações
     */
    void reloadConfig();
    
    /**
     * Obtém o saldo inicial para novos jogadores
     * @return Saldo inicial
     */
    double getInitialBalance();
    
    /**
     * Obtém o nome da moeda (singular)
     * @return Nome da moeda
     */
    String getCurrencyName();
    
    /**
     * Obtém o nome da moeda (plural)
     * @return Nome da moeda no plural
     */
    String getCurrencyNamePlural();
    
    /**
     * Obtém a taxa de imposto sobre transações
     * @return Taxa de imposto sobre transações
     */
    double getTransactionTaxRate();
}
