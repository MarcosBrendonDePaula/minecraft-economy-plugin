package com.minecraft.economy.core;

import com.minecraft.economy.commands.*;
import com.minecraft.economy.database.AsyncMongoDBManager;
import com.minecraft.economy.database.ConfigDatabase;
import com.minecraft.economy.database.ResilientMongoDBManager;
import com.minecraft.economy.economy.VaultEconomyProvider;
import com.minecraft.economy.listeners.PlayerListener;
import com.minecraft.economy.listeners.PlayerShopListener;
import com.minecraft.economy.listeners.ShopListener;
import com.minecraft.economy.lottery.LotteryManager;
import com.minecraft.economy.playershop.PlayerShopManager;
import com.minecraft.economy.shop.ShopManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Date;
import java.util.logging.Level;

/**
 * Plugin principal de economia
 */
public class EconomyPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private ResilientMongoDBManager mongoDBManager;
    private AsyncMongoDBManager asyncMongoDBManager;
    private VaultEconomyProvider economyProvider;
    private ShopManager shopManager;
    private PlayerShopManager playerShopManager;
    private LotteryManager lotteryManager;
    private ConfigDatabase configDatabase;

    @Override
    public void onEnable() {
        try {
            // Carrega a configuração
            saveDefaultConfig();
            configManager = new ConfigManager(this);
            
            // Inicializa o gerenciador de MongoDB
            String connectionString = getConfig().getString("mongodb.connection_string");
            String database = getConfig().getString("mongodb.database");
            
            getLogger().info("Inicializando gerenciador resiliente de MongoDB com conexão: " + connectionString);
            
            // Usa o construtor simplificado que existe na classe
            mongoDBManager = new ResilientMongoDBManager(this);
            asyncMongoDBManager = new AsyncMongoDBManager(this);
            
            // Inicializa o banco de dados de configuração
            configDatabase = new ConfigDatabase(this);
            
            // Registra o provedor de economia do Vault
            economyProvider = new VaultEconomyProvider(this);
            getServer().getServicesManager().register(Economy.class, economyProvider, this, ServicePriority.Normal);
            getLogger().info("Integração com Vault realizada com sucesso!");
            
            // Inicializa o gerenciador de loja
            shopManager = new ShopManager(this);
            
            // Inicializa o gerenciador de lojas de jogadores
            playerShopManager = new PlayerShopManager(this);
            
            // Inicializa o gerenciador de loteria
            lotteryManager = new LotteryManager(this);
            
            // Registra os listeners
            getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
            getServer().getPluginManager().registerEvents(new ShopListener(this), this);
            getServer().getPluginManager().registerEvents(new PlayerShopListener(this), this);
            
            // Registra os comandos de forma segura
            registerCommand("money", new MoneyCommand(this));
            registerCommand("pay", new PayCommand(this));
            registerCommand("eco", new EcoAdminCommand(this));
            registerCommand("shop", new ShopCommand(this));
            registerCommand("tax", new TaxCommand(this));
            registerCommand("lottery", new LotteryCommand(this));
            registerCommand("playershop", new PlayerShopCommand(this));
            
            // Agenda a atualização de preços do mercado
            int updateInterval = getConfig().getInt("shop.update_interval", 3600);
            getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                shopManager.updateMarketPrices();
            }, updateInterval * 20L, updateInterval * 20L);
            
            // Agenda o sorteio da loteria
            long drawInterval = getConfig().getLong("lottery.draw_interval", 86400);
            Date nextDraw = new Date(System.currentTimeMillis() + drawInterval * 1000);
            getLogger().info("Configurações da loteria carregadas com sucesso!");
            getLogger().info("Próximo sorteio: " + nextDraw);
            getLogger().info("Prêmio atual: 1.000,00");
            
            getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                lotteryManager.drawLottery();
            }, drawInterval * 20L, drawInterval * 20L);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Erro ao inicializar o plugin de economia", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // Fecha a conexão com o MongoDB
        if (mongoDBManager != null) {
            mongoDBManager.disconnect();
        }
        
        // Cancela todas as tarefas
        getServer().getScheduler().cancelTasks(this);
        
        getLogger().info("Plugin de economia desativado com sucesso!");
    }
    
    /**
     * Registra um comando de forma segura, com verificação de nulidade
     * @param name Nome do comando
     * @param executor Executor do comando
     */
    private void registerCommand(String name, CommandExecutor executor) {
        try {
            if (getCommand(name) != null) {
                getCommand(name).setExecutor(executor);
                
                // Se o executor também implementa TabCompleter, registra-o como completador de tab
                if (executor instanceof TabCompleter) {
                    getCommand(name).setTabCompleter((TabCompleter) executor);
                }
                
                getLogger().info("Comando /" + name + " registrado com sucesso!");
            } else {
                getLogger().warning("Não foi possível registrar o comando /" + name + " - verifique o plugin.yml");
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Erro ao registrar o comando /" + name, e);
        }
    }

    /**
     * Obtém o gerenciador de configuração
     * @return Gerenciador de configuração
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Obtém o gerenciador de MongoDB
     * @return Gerenciador de MongoDB
     */
    public ResilientMongoDBManager getMongoDBManager() {
        return mongoDBManager;
    }

    /**
     * Obtém o gerenciador assíncrono de MongoDB
     * @return Gerenciador assíncrono de MongoDB
     */
    public AsyncMongoDBManager getAsyncMongoDBManager() {
        return asyncMongoDBManager;
    }

    /**
     * Obtém o provedor de economia do Vault
     * @return Provedor de economia
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
     * Obtém o gerenciador de lojas de jogadores
     * @return Gerenciador de lojas de jogadores
     */
    public PlayerShopManager getPlayerShopManager() {
        return playerShopManager;
    }

    /**
     * Obtém o gerenciador de loteria
     * @return Gerenciador de loteria
     */
    public LotteryManager getLotteryManager() {
        return lotteryManager;
    }

    /**
     * Obtém o banco de dados de configuração
     * @return Banco de dados de configuração
     */
    public ConfigDatabase getConfigDatabase() {
        return configDatabase;
    }
}
