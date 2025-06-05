package com.minecraft.economy.core;

import com.minecraft.economy.commands.*;
import com.minecraft.economy.database.ConfigDatabase;
import com.minecraft.economy.database.ResilientMongoDBManager;
import com.minecraft.economy.economy.VaultEconomyProvider;
import com.minecraft.economy.listeners.PlayerListener;
import com.minecraft.economy.lottery.LotteryManager;
import com.minecraft.economy.shop.ShopManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

/**
 * Classe principal do plugin de economia
 */
public class EconomyPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private ResilientMongoDBManager mongoDBManager;
    private VaultEconomyProvider economyProvider;
    private ShopManager shopManager;
    private LotteryManager lotteryManager;
    private ConfigDatabase configDatabase;

    @Override
    public void onEnable() {
        try {
            // Salva a configuração padrão
            saveDefaultConfig();
            
            // Inicializa o gerenciador de configurações
            configManager = new ConfigManager(this);
            
            // Inicializa o gerenciador resiliente de MongoDB
            mongoDBManager = new ResilientMongoDBManager(this);
            boolean connected = mongoDBManager.connect();
            
            if (!connected) {
                getLogger().warning("Não foi possível conectar ao MongoDB! O plugin continuará funcionando com dados em cache quando possível.");
                getLogger().warning("Verifique sua configuração de MongoDB e certifique-se de que o servidor está acessível.");
                getLogger().warning("O plugin tentará reconectar automaticamente em segundo plano.");
            } else {
                getLogger().info("Conexão com MongoDB estabelecida com sucesso!");
            }
            
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
            
            // Registra os comandos com verificação de nulidade
            registerCommand("money", new MoneyCommand(this));
            registerCommand("pay", new PayCommand(this));
            registerCommand("shop", new ShopCommand(this));
            registerCommand("tax", new TaxCommand(this));
            registerCommand("eco", new EcoAdminCommand(this));
            registerCommand("lottery", new LotteryCommand(this));
            
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
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Erro ao inicializar o plugin de economia: " + e.getMessage(), e);
            getLogger().warning("O plugin continuará funcionando com funcionalidades limitadas.");
            // Não desativa o plugin, permite que continue funcionando mesmo com erros
        }
    }

    /**
     * Registra um comando com verificação de nulidade
     * @param name Nome do comando
     * @param executor Executor do comando
     */
    private void registerCommand(String name, Object executor) {
        try {
            PluginCommand command = getCommand(name);
            if (command == null) {
                getLogger().warning("Comando '" + name + "' não encontrado no plugin.yml! Este comando não estará disponível.");
                return;
            }
            
            if (executor instanceof org.bukkit.command.CommandExecutor) {
                command.setExecutor((org.bukkit.command.CommandExecutor) executor);
            }
            
            if (executor instanceof org.bukkit.command.TabCompleter) {
                command.setTabCompleter((org.bukkit.command.TabCompleter) executor);
            }
            
            getLogger().fine("Comando '" + name + "' registrado com sucesso!");
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Erro ao registrar comando '" + name + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void onDisable() {
        // Fecha a conexão com o MongoDB
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
     * Obtém o gerenciador resiliente de MongoDB
     * @return Gerenciador resiliente de MongoDB
     */
    public ResilientMongoDBManager getMongoDBManager() {
        return mongoDBManager;
    }

    /**
     * Obtém o gerenciador de MongoDB para compatibilidade com código legado
     * @return Gerenciador de MongoDB
     * @deprecated Use getMongoDBManager() em vez disso
     */
    @Deprecated
    public ResilientMongoDBManager getAsyncMongoDBManager() {
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
