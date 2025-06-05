package com.minecraft.economy.core;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Gerenciador de configurações do plugin
 */
public class ConfigManager {

    private final EconomyPlugin plugin;
    private FileConfiguration config;

    /**
     * Construtor do gerenciador de configurações
     * @param plugin Instância do plugin
     */
    public ConfigManager(EconomyPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        loadDefaultConfig();
    }

    /**
     * Carrega as configurações padrão
     */
    private void loadDefaultConfig() {
        // Configurações de economia
        config.addDefault("economy.initial_balance", 100.0);
        config.addDefault("economy.currency_name", "Moeda");
        config.addDefault("economy.currency_name_plural", "Moedas");
        
        // Configurações de impostos
        config.addDefault("economy.transaction_tax_rate", 0.05);
        config.addDefault("economy.wealth_tax_rate", 0.02);
        config.addDefault("economy.wealth_tax_threshold", 10000.0);
        config.addDefault("economy.tax_redistribution_rate", 0.5);
        
        // Configurações de inatividade
        config.addDefault("economy.inactivity_period", 7);
        config.addDefault("economy.inactivity_decay_rate", 0.01);
        
        // Configurações de MongoDB
        config.addDefault("mongodb.connection_string", "mongodb://localhost:27017");
        config.addDefault("mongodb.database", "minecraft_economy");
        config.addDefault("mongodb.pool_size", 10);
        config.addDefault("mongodb.connect_timeout", 5000);
        config.addDefault("mongodb.socket_timeout", 5000);
        config.addDefault("mongodb.max_wait_time", 5000);
        
        // Configurações de loteria
        config.addDefault("lottery.ticket_price", 100.0);
        config.addDefault("lottery.draw_interval", 86400000); // 24 horas em milissegundos
        config.addDefault("lottery.max_tickets_per_player", 5);
        config.addDefault("lottery.enabled", true);
        
        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    /**
     * Recarrega as configurações
     */
    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    /**
     * Obtém o saldo inicial
     * @return Saldo inicial
     */
    public double getInitialBalance() {
        return config.getDouble("economy.initial_balance", 100.0);
    }

    /**
     * Obtém o nome da moeda (singular)
     * @return Nome da moeda
     */
    public String getCurrencyName() {
        return config.getString("economy.currency_name", "Moeda");
    }

    /**
     * Obtém o nome da moeda (plural)
     * @return Nome da moeda no plural
     */
    public String getCurrencyNamePlural() {
        return config.getString("economy.currency_name_plural", "Moedas");
    }

    /**
     * Obtém a taxa de imposto sobre transações
     * @return Taxa de imposto sobre transações
     */
    public double getTransactionTaxRate() {
        return config.getDouble("economy.transaction_tax_rate", 0.05);
    }

    /**
     * Obtém a taxa de imposto sobre riqueza
     * @return Taxa de imposto sobre riqueza
     */
    public double getWealthTaxRate() {
        return config.getDouble("economy.wealth_tax_rate", 0.02);
    }

    /**
     * Obtém o limite para aplicação de imposto sobre riqueza
     * @return Limite para aplicação de imposto sobre riqueza
     */
    public double getWealthTaxThreshold() {
        return config.getDouble("economy.wealth_tax_threshold", 10000.0);
    }

    /**
     * Obtém a taxa de redistribuição de impostos
     * @return Taxa de redistribuição de impostos
     */
    public double getTaxRedistributionRate() {
        return config.getDouble("economy.tax_redistribution_rate", 0.5);
    }

    /**
     * Obtém o período de inatividade em dias
     * @return Período de inatividade em dias
     */
    public int getInactivityPeriod() {
        return config.getInt("economy.inactivity_period", 7);
    }

    /**
     * Obtém a taxa de decaimento por inatividade
     * @return Taxa de decaimento por inatividade
     */
    public double getInactivityDecayRate() {
        return config.getDouble("economy.inactivity_decay_rate", 0.01);
    }

    /**
     * Obtém o preço do bilhete de loteria
     * @return Preço do bilhete de loteria
     */
    public double getLotteryTicketPrice() {
        return config.getDouble("lottery.ticket_price", 100.0);
    }

    /**
     * Obtém o intervalo de sorteio da loteria em milissegundos
     * @return Intervalo de sorteio da loteria em milissegundos
     */
    public long getLotteryDrawInterval() {
        return config.getLong("lottery.draw_interval", 86400000);
    }

    /**
     * Obtém o número máximo de bilhetes por jogador
     * @return Número máximo de bilhetes por jogador
     */
    public int getMaxTicketsPerPlayer() {
        return config.getInt("lottery.max_tickets_per_player", 5);
    }

    /**
     * Verifica se a loteria está habilitada
     * @return true se a loteria está habilitada, false caso contrário
     */
    public boolean isLotteryEnabled() {
        return config.getBoolean("lottery.enabled", true);
    }
}
