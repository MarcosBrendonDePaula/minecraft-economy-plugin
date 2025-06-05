package com.minecraft.economy.core;

import com.minecraft.economy.commands.*;
import com.minecraft.economy.database.AsyncMongoDBManager;
import com.minecraft.economy.database.ConfigDatabase;
import com.minecraft.economy.database.MongoDBManager;
import com.minecraft.economy.economy.VaultEconomyProvider;
import com.minecraft.economy.listeners.PlayerListener;
import com.minecraft.economy.lottery.LotteryManager;
import com.minecraft.economy.shop.ShopManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Classe principal do plugin de economia
 */
public class EconomyPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private AsyncMongoDBManager asyncMongoDBManager;
    private MongoDBManager mongoDBManager;
    private VaultEconomyProvider economyProvider;
    private ShopManager shopManager;
    private LotteryManager lotteryManager;
    private ConfigDatabase configDatabase;

    @Override
    public void onEnable() {
        // Salva a configuração padrão
        saveDefaultConfig();
        
        // Inicializa o gerenciador de configurações
        configManager = new ConfigManager(this);
        
        // Inicializa o gerenciador de MongoDB assíncrono
        asyncMongoDBManager = new AsyncMongoDBManager(this);
        asyncMongoDBManager.connect();
        
        // Inicializa o gerenciador de MongoDB (compatibilidade)
        mongoDBManager = new MongoDBManager(this);
        mongoDBManager.connect();
        
        // Inicializa o banco de dados de configurações
        configDatabase = new ConfigDatabase(this);
        
        // Registra o provedor de economia do Vault
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            economyProvider = new VaultEconomyProvider(this);
            getServer().getServicesManager().register(
                Economy.class,
                economyProvider,
                this,
                ServicePriority.Normal
            );
            getLogger().info("Integração com Vault realizada com sucesso!");
        } else {
            getLogger().warning("Vault não encontrado! Funcionalidades de economia podem não funcionar corretamente.");
        }
        
        // Inicializa o gerenciador de loja
        shopManager = new ShopManager(this);
        
        // Inicializa o gerenciador de loteria
        lotteryManager = new LotteryManager(this);
        
        // Registra os comandos
        getCommand("money").setExecutor(new MoneyCommand(this));
        getCommand("pay").setExecutor(new PayCommand(this));
        getCommand("shop").setExecutor(new ShopCommand(this));
        getCommand("tax").setExecutor(new TaxCommand(this));
        getCommand("ecoadmin").setExecutor(new EcoAdminCommand(this));
        getCommand("lottery").setExecutor(new LotteryCommand(this));
        
        // Registra os listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        // Agenda a atualização de preços do mercado
        new BukkitRunnable() {
            @Override
            public void run() {
                shopManager.updateMarketPrices();
            }
        }.runTaskTimerAsynchronously(this, 20 * 60, 20 * 60 * 30); // A cada 30 minutos
        
        getLogger().info("Plugin de economia inicializado com sucesso!");
    }

    @Override
    public void onDisable() {
        // Fecha a conexão com o MongoDB
        if (asyncMongoDBManager != null) {
            asyncMongoDBManager.disconnect();
        }
        
        if (mongoDBManager != null) {
            mongoDBManager.disconnect();
        }
        
        getLogger().info("Plugin de economia desativado com sucesso!");
    }

    /**
     * Obtém o gerenciador de configurações
     * @return Gerenciador de configurações
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Obtém o gerenciador de MongoDB assíncrono
     * @return Gerenciador de MongoDB assíncrono
     */
    public AsyncMongoDBManager getAsyncMongoDBManager() {
        return asyncMongoDBManager;
    }

    /**
     * Obtém o gerenciador de MongoDB (compatibilidade)
     * @return Gerenciador de MongoDB
     */
    public MongoDBManager getMongoDBManager() {
        return mongoDBManager;
    }

    /**
     * Obtém o provedor de economia do Vault
     * @return Provedor de economia do Vault
     */
    public VaultEconomyProvider getEconomyProvider() {
        return economyProvider;
    }

    /**
     * Obtém o gerenciador de loja
     * @return Gerenciador de loja
     */
    public ShopManager getShopManager() {
        return shopManager;
    }

    /**
     * Obtém o gerenciador de loteria
     * @return Gerenciador de loteria
     */
    public LotteryManager getLotteryManager() {
        return lotteryManager;
    }
    
    /**
     * Obtém o banco de dados de configurações
     * @return Banco de dados de configurações
     */
    public ConfigDatabase getConfigDatabase() {
        return configDatabase;
    }
}
