package com.minecraft.economy.core;

import com.minecraft.economy.adapter.ConfigManagerAdapter;
import com.minecraft.economy.api.infrastructure.AsyncExecutor;
import com.minecraft.economy.api.infrastructure.CacheManager;
import com.minecraft.economy.api.repository.ConfigurationRepository;
import com.minecraft.economy.api.repository.PlayerAccountRepository;
import com.minecraft.economy.api.repository.TransactionRepository;
import com.minecraft.economy.api.service.ConfigurationService;
import com.minecraft.economy.api.service.EconomyService;
import com.minecraft.economy.commands.*;
import com.minecraft.economy.database.AsyncMongoDBManager;
import com.minecraft.economy.database.ConfigDatabase;
import com.minecraft.economy.database.ResilientMongoDBManager;
import com.minecraft.economy.economy.VaultEconomyProvider;
import com.minecraft.economy.infrastructure.BukkitAsyncExecutor;
import com.minecraft.economy.infrastructure.MemoryCacheManager;
import com.minecraft.economy.infrastructure.MongoDBConnection;
import com.minecraft.economy.lottery.LotteryManager;
import com.minecraft.economy.playershop.PlayerShopManager;
import com.minecraft.economy.repository.MongoConfigurationRepository;
import com.minecraft.economy.repository.MongoPlayerAccountRepository;
import com.minecraft.economy.repository.MongoTransactionRepository;
import com.minecraft.economy.service.DefaultConfigurationService;
import com.minecraft.economy.service.DefaultEconomyService;
import com.minecraft.economy.service.VaultEconomyAdapter;
import com.minecraft.economy.shop.ShopManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.ServicePriority;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Plugin principal de economia com arquitetura modular
 */
public class ModularEconomyPlugin extends EconomyPlugin {

    // Infraestrutura
    private MongoDBConnection dbConnection;
    private AsyncExecutor asyncExecutor;
    private CacheManager<UUID, Double> balanceCache;
    private CacheManager<UUID, Object> accountCache;
    private CacheManager<String, Object> configCache;
    
    // Repositórios
    private PlayerAccountRepository playerAccountRepository;
    private TransactionRepository transactionRepository;
    private ConfigurationRepository configurationRepository;
    
    // Serviços
    private ConfigurationService configService;
    private EconomyService economyService;
    private VaultEconomyAdapter vaultAdapter;
    
    // Adaptadores para compatibilidade
    private ConfigManagerAdapter configManagerAdapter;
    private ResilientMongoDBManager mongoDBManager;
    private AsyncMongoDBManager asyncMongoDBManager;
    private VaultEconomyProvider economyProvider;
    private ConfigDatabase configDatabase;
    
    // Gerenciadores
    private ShopManager shopManager;
    private PlayerShopManager playerShopManager;
    private LotteryManager lotteryManager;

    @Override
    public void onEnable() {
        try {
            // Inicializa a infraestrutura
            initializeInfrastructure();
            
            // Inicializa os repositórios
            initializeRepositories();
            
            // Inicializa os serviços
            initializeServices();
            
            // Registra o provedor de economia do Vault
            registerVaultProvider();
            
            // Inicializa os gerenciadores
            initializeManagers();
            
            // Registra os listeners
            registerListeners();
            
            // Registra os comandos
            registerCommands();
            
            // Agenda tarefas periódicas
            schedulePeriodicTasks();
            
            getLogger().info("Plugin de economia modular inicializado com sucesso!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Erro ao inicializar o plugin de economia", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // Fecha a conexão com o MongoDB
        if (dbConnection != null) {
            dbConnection.disconnect();
        }
        
        // Cancela todas as tarefas
        if (asyncExecutor != null) {
            asyncExecutor.cancelAllTasks();
        }
        
        getLogger().info("Plugin de economia desativado com sucesso!");
    }
    
    /**
     * Inicializa a infraestrutura
     */
    private void initializeInfrastructure() {
        // Carrega a configuração
        saveDefaultConfig();
        
        // Inicializa o executor assíncrono
        asyncExecutor = new BukkitAsyncExecutor(this);
        
        // Inicializa os caches
        balanceCache = new MemoryCacheManager<>(60, TimeUnit.SECONDS);
        accountCache = new MemoryCacheManager<>(60, TimeUnit.SECONDS);
        configCache = new MemoryCacheManager<>(5, TimeUnit.MINUTES);
        
        // Inicializa a conexão com o MongoDB
        dbConnection = new MongoDBConnection(this);
        dbConnection.connect();
        
        // Inicializa os adaptadores para compatibilidade
        mongoDBManager = new ResilientMongoDBManager(this);
        asyncMongoDBManager = new AsyncMongoDBManager(this);
        configDatabase = new ConfigDatabase(this);
    }
    
    /**
     * Inicializa os repositórios
     */
    private void initializeRepositories() {
        // Inicializa o repositório de contas de jogadores
        playerAccountRepository = new MongoPlayerAccountRepository(
                dbConnection,
                asyncExecutor,
                balanceCache,
                accountCache,
                getLogger()
        );
        
        // Inicializa o repositório de transações
        transactionRepository = new MongoTransactionRepository(
                dbConnection,
                asyncExecutor,
                getLogger()
        );
        
        // Inicializa o repositório de configurações
        configurationRepository = new MongoConfigurationRepository(
                dbConnection,
                asyncExecutor,
                configCache,
                getLogger()
        );
    }
    
    /**
     * Inicializa os serviços
     */
    private void initializeServices() {
        // Inicializa o serviço de configuração
        configService = new DefaultConfigurationService(
                configurationRepository,
                getLogger()
        );
        
        // Inicializa o serviço de economia
        economyService = new DefaultEconomyService(
                playerAccountRepository,
                transactionRepository,
                configService,
                asyncExecutor,
                getLogger()
        );
        
        // Inicializa o adaptador para o Vault
        vaultAdapter = new VaultEconomyAdapter(
                economyService,
                configService,
                getLogger()
        );
        
        // Inicializa os adaptadores para compatibilidade
        configManagerAdapter = new ConfigManagerAdapter(this, configService);
        economyProvider = new VaultEconomyProvider(this);
    }
    
    /**
     * Registra o provedor de economia do Vault
     */
    private void registerVaultProvider() {
        getServer().getServicesManager().register(
                Economy.class,
                vaultAdapter,
                this,
                ServicePriority.Normal
        );
        getLogger().info("Integração com Vault realizada com sucesso!");
    }
    
    /**
     * Inicializa os gerenciadores
     */
    private void initializeManagers() {
        // Inicializa o gerenciador de loja
        shopManager = new ShopManager(this);
        
        // Inicializa o gerenciador de lojas de jogadores
        playerShopManager = new PlayerShopManager(this);
        
        // Inicializa o gerenciador de loteria
        lotteryManager = new LotteryManager(this);
    }
    
    /**
     * Registra os listeners
     */
    private void registerListeners() {
        // TODO: Atualizar os listeners para usar a nova arquitetura
        getServer().getPluginManager().registerEvents(new com.minecraft.economy.listeners.PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new com.minecraft.economy.listeners.ShopListener(this), this);
        getServer().getPluginManager().registerEvents(new com.minecraft.economy.listeners.PlayerShopListener(this), this);
    }
    
    /**
     * Registra os comandos
     */
    private void registerCommands() {
        // TODO: Atualizar os comandos para usar a nova arquitetura
        registerCommand("money", new MoneyCommand(this));
        registerCommand("pay", new PayCommand(this));
        registerCommand("eco", new EcoAdminCommand(this));
        registerCommand("shop", new ShopCommand(this));
        registerCommand("tax", new TaxCommand(this));
        registerCommand("lottery", new LotteryCommand(this));
        registerCommand("playershop", new PlayerShopCommand(this));
    }
    
    /**
     * Agenda tarefas periódicas
     */
    private void schedulePeriodicTasks() {
        // Agenda a atualização de preços do mercado
        int updateInterval = getConfig().getInt("shop.update_interval", 3600);
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            shopManager.updateMarketPrices();
        }, updateInterval * 20L, updateInterval * 20L);
        
        // Agenda o sorteio da loteria
        long drawInterval = getConfig().getLong("lottery.draw_interval", 86400);
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            lotteryManager.drawLottery();
        }, drawInterval * 20L, drawInterval * 20L);
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
    
    // Getters para os componentes
    
    public AsyncExecutor getAsyncExecutor() {
        return asyncExecutor;
    }
    
    public ConfigurationService getConfigService() {
        return configService;
    }
    
    public EconomyService getEconomyService() {
        return economyService;
    }
    
    public PlayerAccountRepository getPlayerAccountRepository() {
        return playerAccountRepository;
    }
    
    public TransactionRepository getTransactionRepository() {
        return transactionRepository;
    }
    
    public ConfigurationRepository getConfigurationRepository() {
        return configurationRepository;
    }
    
    public ShopManager getShopManager() {
        return shopManager;
    }
    
    public PlayerShopManager getPlayerShopManager() {
        return playerShopManager;
    }
    
    public LotteryManager getLotteryManager() {
        return lotteryManager;
    }
    
    // Métodos de compatibilidade com código existente
    
    @Override
    public ConfigManager getConfigManager() {
        return configManagerAdapter;
    }
    
    @Override
    public ResilientMongoDBManager getMongoDBManager() {
        return mongoDBManager;
    }
    
    @Override
    public AsyncMongoDBManager getAsyncMongoDBManager() {
        return asyncMongoDBManager;
    }
    
    @Override
    public VaultEconomyProvider getEconomyProvider() {
        return economyProvider;
    }
    
    @Override
    public ConfigDatabase getConfigDatabase() {
        return configDatabase;
    }
}
