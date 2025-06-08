package com.minecraft.economy.infrastructure;

import com.minecraft.economy.api.infrastructure.CacheManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Implementação de CacheManager usando memória
 * @param <K> Tipo da chave
 * @param <V> Tipo do valor
 */
public class MemoryCacheManager<K, V> implements CacheManager<K, V> {
    
    private static class CacheEntry<V> {
        private final V value;
        private final long expirationTime;
        
        public CacheEntry(V value, long expirationTime) {
            this.value = value;
            this.expirationTime = expirationTime;
        }
        
        public V getValue() {
            return value;
        }
        
        public long getExpirationTime() {
            return expirationTime;
        }
        
        public boolean isExpired() {
            return expirationTime > 0 && System.currentTimeMillis() > expirationTime;
        }
    }
    
    private final Map<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor;
    private final long defaultDuration;
    private final TimeUnit defaultTimeUnit;
    
    /**
     * Construtor com duração padrão
     * @param defaultDuration Duração padrão
     * @param defaultTimeUnit Unidade de tempo padrão
     */
    public MemoryCacheManager(long defaultDuration, TimeUnit defaultTimeUnit) {
        this.defaultDuration = defaultDuration;
        this.defaultTimeUnit = defaultTimeUnit;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        
        // Agenda limpeza periódica de entradas expiradas
        this.cleanupExecutor.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.MINUTES);
    }
    
    /**
     * Construtor sem duração padrão (cache permanente)
     */
    public MemoryCacheManager() {
        this(0, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Limpa entradas expiradas
     */
    private void cleanup() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    @Override
    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        
        if (entry == null || entry.isExpired()) {
            if (entry != null && entry.isExpired()) {
                cache.remove(key);
            }
            return null;
        }
        
        return entry.getValue();
    }

    @Override
    public void put(K key, V value) {
        if (defaultDuration > 0) {
            put(key, value, defaultDuration, defaultTimeUnit);
        } else {
            cache.put(key, new CacheEntry<>(value, 0)); // Sem expiração
        }
    }

    @Override
    public void put(K key, V value, long duration, TimeUnit unit) {
        long expirationTime = duration > 0 
            ? System.currentTimeMillis() + unit.toMillis(duration)
            : 0; // 0 significa sem expiração
        
        cache.put(key, new CacheEntry<>(value, expirationTime));
    }

    @Override
    public V remove(K key) {
        CacheEntry<V> entry = cache.remove(key);
        return entry != null ? entry.getValue() : null;
    }

    @Override
    public boolean containsKey(K key) {
        CacheEntry<V> entry = cache.get(key);
        
        if (entry == null) {
            return false;
        }
        
        if (entry.isExpired()) {
            cache.remove(key);
            return false;
        }
        
        return true;
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public int size() {
        cleanup(); // Limpa entradas expiradas antes de retornar o tamanho
        return cache.size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> supplier) {
        V value = get(key);
        
        if (value == null) {
            value = supplier.apply(key);
            if (value != null) {
                put(key, value);
            }
        }
        
        return value;
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> supplier, long duration, TimeUnit unit) {
        V value = get(key);
        
        if (value == null) {
            value = supplier.apply(key);
            if (value != null) {
                put(key, value, duration, unit);
            }
        }
        
        return value;
    }
    
    /**
     * Finaliza o gerenciador de cache
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
    }
}
