package com.minecraft.economy.infrastructure;

import com.minecraft.economy.api.infrastructure.DatabaseConnection;
import com.minecraft.economy.core.EconomyPlugin;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Implementação de conexão com MongoDB
 */
public class MongoDBConnection implements DatabaseConnection {

    private final EconomyPlugin plugin;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private final String connectionString;
    private final String databaseName;
    
    // Controle de estado da conexão
    private boolean isConnected = false;
    private int reconnectAttempts = 0;
    private final int maxReconnectAttempts = 5;
    private BukkitTask reconnectTask = null;
    private final Object connectionLock = new Object();
    
    /**
     * Construtor com configurações da config.yml
     * @param plugin Instância do plugin
     */
    public MongoDBConnection(EconomyPlugin plugin) {
        this.plugin = plugin;
        
        // Usa apenas connection_string para evitar ambiguidade
        this.connectionString = plugin.getConfig().getString("mongodb.connection_string", "mongodb://localhost:27017");
        this.databaseName = plugin.getConfig().getString("mongodb.database", "minecraft_economy");
        
        plugin.getLogger().info("Inicializando conexão MongoDB com: " + 
                connectionString.replaceAll("mongodb://([^:]+):([^@]+)@", "mongodb://****:****@"));
    }

    @Override
    public boolean connect() {
        synchronized (connectionLock) {
            if (isConnected) {
                return true;
            }
            
            try {
                plugin.getLogger().info("Tentando conectar ao MongoDB...");
                
                // Configura as opções de conexão
                int connectTimeout = plugin.getConfig().getInt("mongodb.connect_timeout", 5000);
                int socketTimeout = plugin.getConfig().getInt("mongodb.socket_timeout", 5000);
                int maxWaitTime = plugin.getConfig().getInt("mongodb.max_wait_time", 5000);
                int poolSize = plugin.getConfig().getInt("mongodb.pool_size", 10);
                
                MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(connectionString))
                    .applyToConnectionPoolSettings(builder -> 
                        builder.maxSize(poolSize)
                              .maxWaitTime(maxWaitTime, TimeUnit.MILLISECONDS))
                    .applyToSocketSettings(builder -> 
                        builder.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                              .readTimeout(socketTimeout, TimeUnit.MILLISECONDS))
                    .build();
                
                // Cria o cliente MongoDB
                mongoClient = MongoClients.create(settings);
                
                // Testa a conexão com ping
                Document pingResult = mongoClient.getDatabase("admin").runCommand(new Document("ping", 1));
                if (pingResult.getDouble("ok") != 1.0) {
                    throw new MongoException("Falha no ping ao servidor MongoDB");
                }
                
                database = mongoClient.getDatabase(databaseName);
                
                isConnected = true;
                reconnectAttempts = 0;
                
                plugin.getLogger().info("Conexão com MongoDB estabelecida com sucesso!");
                return true;
            } catch (Exception e) {
                reconnectAttempts++;
                String errorMsg = String.format("Erro ao conectar ao MongoDB (tentativa %d/%d): %s", 
                        reconnectAttempts, maxReconnectAttempts, e.getMessage());
                plugin.getLogger().log(Level.SEVERE, errorMsg, e);
                
                // Agenda reconexão automática se ainda não atingiu o limite de tentativas
                if (reconnectAttempts < maxReconnectAttempts && reconnectTask == null) {
                    scheduleReconnect();
                } else if (reconnectAttempts >= maxReconnectAttempts) {
                    plugin.getLogger().severe("Número máximo de tentativas de reconexão atingido.");
                }
                
                return false;
            }
        }
    }
    
    /**
     * Agenda uma tentativa de reconexão
     */
    private void scheduleReconnect() {
        if (reconnectTask != null && !reconnectTask.isCancelled()) {
            return;
        }
        
        long delay = Math.min(30, reconnectAttempts * 5); // Backoff exponencial limitado a 30 segundos
        plugin.getLogger().info("Agendando reconexão em " + delay + " segundos...");
        
        reconnectTask = plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            plugin.getLogger().info("Tentando reconectar ao MongoDB...");
            if (connect()) {
                plugin.getLogger().info("Reconexão bem-sucedida!");
                reconnectTask = null;
            }
        }, delay * 20L); // 20 ticks = 1 segundo
    }

    @Override
    public void disconnect() {
        synchronized (connectionLock) {
            if (mongoClient != null) {
                try {
                    mongoClient.close();
                    plugin.getLogger().info("Conexão com MongoDB fechada com sucesso!");
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Erro ao fechar conexão com MongoDB: " + e.getMessage(), e);
                } finally {
                    mongoClient = null;
                    isConnected = false;
                }
            }
            
            if (reconnectTask != null && !reconnectTask.isCancelled()) {
                reconnectTask.cancel();
                reconnectTask = null;
            }
        }
    }
    
    @Override
    public boolean ensureConnected() {
        if (isConnected) {
            return true;
        }
        
        return connect();
    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public String getDatabaseName() {
        return databaseName;
    }

    @Override
    public String getConnectionString() {
        return connectionString;
    }
    
    /**
     * Obtém o cliente MongoDB
     * @return Cliente MongoDB
     */
    public MongoClient getMongoClient() {
        ensureConnected();
        return mongoClient;
    }
    
    /**
     * Obtém o banco de dados MongoDB
     * @return Banco de dados MongoDB
     */
    public MongoDatabase getDatabase() {
        ensureConnected();
        return database;
    }
}
