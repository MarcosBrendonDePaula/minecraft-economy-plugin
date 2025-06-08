package com.minecraft.economy.adapter;

import com.minecraft.economy.api.service.ConfigurationService;
import com.minecraft.economy.core.ConfigManager;
import com.minecraft.economy.core.EconomyPlugin;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Adaptador para compatibilidade com ConfigManager
 */
public class ConfigManagerAdapter extends ConfigManager {
    
    private final ConfigurationService configService;
    
    /**
     * Construtor
     * @param plugin Plugin
     * @param configService Serviço de configuração
     */
    public ConfigManagerAdapter(EconomyPlugin plugin, ConfigurationService configService) {
        super(plugin);
        this.configService = configService;
    }
    
    @Override
    public double getInitialBalance() {
        return configService.getInitialBalance();
    }
    
    @Override
    public String getCurrencyName() {
        return configService.getCurrencyName();
    }
    
    @Override
    public String getCurrencyNamePlural() {
        return configService.getCurrencyNamePlural();
    }
    
    @Override
    public double getTransactionTaxRate() {
        return configService.getTransactionTaxRate();
    }
    
    @Override
    public double getWealthTaxRate() {
        return configService.getDouble("economy.wealth_tax_rate", 0.02);
    }
    
    @Override
    public double getWealthTaxThreshold() {
        return configService.getDouble("economy.wealth_tax_threshold", 10000.0);
    }
    
    @Override
    public double getTaxRedistributionRate() {
        return configService.getDouble("economy.tax_redistribution_rate", 0.5);
    }
    
    @Override
    public int getInactivityPeriod() {
        return configService.getInt("economy.inactivity_period", 7);
    }
    
    @Override
    public double getInactivityDecayRate() {
        return configService.getDouble("economy.inactivity_decay_rate", 0.01);
    }
    
    @Override
    public double getLotteryTicketPrice() {
        return configService.getDouble("lottery.ticket_price", 100.0);
    }
    
    @Override
    public long getLotteryDrawInterval() {
        return configService.getLong("lottery.draw_interval", 86400000);
    }
    
    @Override
    public int getMaxTicketsPerPlayer() {
        return configService.getInt("lottery.max_tickets_per_player", 5);
    }
    
    @Override
    public boolean isLotteryEnabled() {
        return configService.getBoolean("lottery.enabled", true);
    }
    
    @Override
    public void reloadConfig() {
        configService.reloadConfig();
    }
}
