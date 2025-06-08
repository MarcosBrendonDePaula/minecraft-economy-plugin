package com.minecraft.economy.api.infrastructure;

import java.util.concurrent.TimeUnit;

/**
 * Interface para gerenciamento de cache
 * @param <K> Tipo da chave
 * @param <V> Tipo do valor
 */
public interface CacheManager<K, V> {
    
    /**
     * Obtém um valor do cache
     * @param key Chave do valor
     * @return Valor armazenado ou null se não existir
     */
    V get(K key);
    
    /**
     * Armazena um valor no cache
     * @param key Chave do valor
     * @param value Valor a armazenar
     */
    void put(K key, V value);
    
    /**
     * Armazena um valor no cache com tempo de expiração
     * @param key Chave do valor
     * @param value Valor a armazenar
     * @param duration Duração até expirar
     * @param unit Unidade de tempo
     */
    void put(K key, V value, long duration, TimeUnit unit);
    
    /**
     * Remove um valor do cache
     * @param key Chave do valor
     * @return Valor removido ou null se não existir
     */
    V remove(K key);
    
    /**
     * Verifica se uma chave existe no cache
     * @param key Chave a verificar
     * @return true se a chave existe e não expirou
     */
    boolean containsKey(K key);
    
    /**
     * Limpa todo o cache
     */
    void clear();
    
    /**
     * Obtém o tamanho atual do cache
     * @return Número de entradas no cache
     */
    int size();
    
    /**
     * Verifica se o cache está vazio
     * @return true se o cache está vazio
     */
    boolean isEmpty();
    
    /**
     * Obtém ou calcula um valor se não existir
     * @param key Chave do valor
     * @param supplier Função para calcular o valor se não existir
     * @return Valor armazenado ou calculado
     */
    V computeIfAbsent(K key, java.util.function.Function<? super K, ? extends V> supplier);
    
    /**
     * Obtém ou calcula um valor se não existir, com tempo de expiração
     * @param key Chave do valor
     * @param supplier Função para calcular o valor se não existir
     * @param duration Duração até expirar
     * @param unit Unidade de tempo
     * @return Valor armazenado ou calculado
     */
    V computeIfAbsent(K key, java.util.function.Function<? super K, ? extends V> supplier, 
                     long duration, TimeUnit unit);
}
