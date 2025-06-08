package com.minecraft.economy.service;

import com.minecraft.economy.api.repository.ConfigurationRepository;
import com.minecraft.economy.api.service.ConfigurationService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementação padrão de ConfigurationService
 */
public class DefaultConfigurationService implements ConfigurationService {

    private final ConfigurationRepository configRepository;
    private final Logger logger;
    private final long timeout;
    private final TimeUnit timeoutUnit;
    
    /**
     * Construtor
     * @param configRepository Repositório de configurações
     * @param logger Logger
     */
    public DefaultConfigurationService(ConfigurationRepository configRepository, Logger logger) {
        this(configRepository, logger, 500, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Construtor com timeout
     * @param configRepository Repositório de configurações
     * @param logger Logger
     * @param timeout Timeout para operações síncronas
     * @param timeoutUnit Unidade de tempo para timeout
     */
    public DefaultConfigurationService(
            ConfigurationRepository configRepository,
            Logger logger,
            long timeout,
            TimeUnit timeoutUnit) {
        this.configRepository = configRepository;
        this.logger = logger;
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
    }

    @Override
    public String getString(String key, String defaultValue) {
        try {
            return configRepository.getConfig(key, defaultValue).get(timeout, timeoutUnit);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.log(Level.WARNING, "Erro ao obter configuração String: " + e.getMessage(), e);
            return defaultValue;
        }
    }

    @Override
    public int getInt(String key, int defaultValue) {
        try {
            Object value = configRepository.getConfig(key, defaultValue).get(timeout, timeoutUnit);
            
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof Number) {
                return ((Number) value).intValue();
            } else if (value instanceof String) {
                try {
                    return Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    logger.warning("Erro ao converter String para int: " + value);
                }
            }
            
            return defaultValue;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.log(Level.WARNING, "Erro ao obter configuração int: " + e.getMessage(), e);
            return defaultValue;
        }
    }

    @Override
    public long getLong(String key, long defaultValue) {
        try {
            Object value = configRepository.getConfig(key, defaultValue).get(timeout, timeoutUnit);
            
            if (value instanceof Long) {
                return (Long) value;
            } else if (value instanceof Number) {
                return ((Number) value).longValue();
            } else if (value instanceof String) {
                try {
                    return Long.parseLong((String) value);
                } catch (NumberFormatException e) {
                    logger.warning("Erro ao converter String para long: " + value);
                }
            }
            
            return defaultValue;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.log(Level.WARNING, "Erro ao obter configuração long: " + e.getMessage(), e);
            return defaultValue;
        }
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        try {
            Object value = configRepository.getConfig(key, defaultValue).get(timeout, timeoutUnit);
            
            if (value instanceof Double) {
                return (Double) value;
            } else if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String) {
                try {
                    return Double.parseDouble((String) value);
                } catch (NumberFormatException e) {
                    logger.warning("Erro ao converter String para double: " + value);
                }
            }
            
            return defaultValue;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.log(Level.WARNING, "Erro ao obter configuração double: " + e.getMessage(), e);
            return defaultValue;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            Object value = configRepository.getConfig(key, defaultValue).get(timeout, timeoutUnit);
            
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else if (value instanceof String) {
                String strValue = (String) value;
                return "true".equalsIgnoreCase(strValue) || "yes".equalsIgnoreCase(strValue) || "1".equals(strValue);
            } else if (value instanceof Number) {
                return ((Number) value).intValue() != 0;
            }
            
            return defaultValue;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.log(Level.WARNING, "Erro ao obter configuração boolean: " + e.getMessage(), e);
            return defaultValue;
        }
    }

    @Override
    public CompletableFuture<Boolean> setConfig(String key, Object value) {
        return configRepository.setConfig(key, value);
    }

    @Override
    public <T> CompletableFuture<T> getConfigAsync(String key, T defaultValue) {
        return configRepository.getConfig(key, defaultValue);
    }

    @Override
    public CompletableFuture<Boolean> removeConfig(String key) {
        return configRepository.removeConfig(key);
    }

    @Override
    public void reloadConfig() {
        try {
            configRepository.loadConfigs().get(timeout, timeoutUnit);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.log(Level.WARNING, "Erro ao recarregar configurações: " + e.getMessage(), e);
        }
    }

    @Override
    public double getInitialBalance() {
        return getDouble("economy.initial_balance", 100.0);
    }

    @Override
    public String getCurrencyName() {
        return getString("economy.currency_name", "Moeda");
    }

    @Override
    public String getCurrencyNamePlural() {
        return getString("economy.currency_name_plural", "Moedas");
    }

    @Override
    public double getTransactionTaxRate() {
        return getDouble("economy.transaction_tax_rate", 0.05);
    }
}
