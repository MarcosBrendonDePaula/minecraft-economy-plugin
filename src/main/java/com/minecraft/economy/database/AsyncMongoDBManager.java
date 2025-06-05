package com.minecraft.economy.database;

import com.minecraft.economy.core.EconomyPlugin;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Gerenciador assíncrono de conexão e operações com MongoDB
 */
public class AsyncMongoDBManager {

    private final EconomyPlugin plugin;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private final String connectionString;
    private final String databaseName;
    
    // Coleções principais
    private MongoCollection<Document> playersCollection;
    private MongoCollection<Document> transactionsCollection;
    private MongoCollection<Document> marketCollection;
    private MongoCollection<Document> configCollection;
    private MongoCollection<Document> lotteryCollection;

    /**
     * Construtor com configurações padrão
     * @param plugin Instância do plugin
     */
    public AsyncMongoDBManager(EconomyPlugin plugin) {
        this.plugin = plugin;
        this.connectionString = plugin.getConfig().getString("mongodb.connection_string", "mongodb://localhost:27017");
        this.databaseName = plugin.getConfig().getString("mongodb.database", "minecraft_economy");
    }

    /**
     * Inicializa a conexão com o MongoDB
     */
    public void connect() {
        try {
            // Configura as opções de conexão
            MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .applyToConnectionPoolSettings(builder -> 
                    builder.maxSize(20)
                          .maxWaitTime(5000, TimeUnit.MILLISECONDS))
                .applyToSocketSettings(builder -> 
                    builder.connectTimeout(5000, TimeUnit.MILLISECONDS)
                          .readTimeout(5000, TimeUnit.MILLISECONDS))
                .build();
            
            // Cria o cliente MongoDB
            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase(databaseName);
            
            // Inicializa as coleções
            playersCollection = database.getCollection("players");
            transactionsCollection = database.getCollection("transactions");
            marketCollection = database.getCollection("market");
            configCollection = database.getCollection("config");
            lotteryCollection = database.getCollection("lottery_tickets");
            
            plugin.getLogger().info("Conexão com MongoDB estabelecida com sucesso!");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao conectar ao MongoDB: " + e.getMessage(), e);
        }
    }

    /**
     * Fecha a conexão com o MongoDB
     */
    public void disconnect() {
        if (mongoClient != null) {
            mongoClient.close();
            plugin.getLogger().info("Conexão com MongoDB fechada com sucesso!");
        }
    }

    /**
     * Obtém o saldo de um jogador
     * @param playerId UUID do jogador
     * @return CompletableFuture com o saldo do jogador
     */
    public CompletableFuture<Double> getBalance(UUID playerId) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Document playerDoc = playersCollection.find(Filters.eq("uuid", playerId.toString())).first();
                    
                    if (playerDoc != null) {
                        future.complete(playerDoc.getDouble("balance"));
                    } else {
                        // Jogador não encontrado, retorna saldo inicial
                        double initialBalance = plugin.getConfigManager().getInitialBalance();
                        future.complete(initialBalance);
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Erro ao obter saldo do jogador: " + e.getMessage(), e);
                    future.completeExceptionally(e);
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return future;
    }

    /**
     * Verifica se um jogador tem saldo suficiente
     * @param playerId UUID do jogador
     * @param amount Valor a verificar
     * @return CompletableFuture com o resultado da verificação
     */
    public CompletableFuture<Boolean> hasBalance(UUID playerId, double amount) {
        return getBalance(playerId).thenApply(balance -> balance >= amount);
    }

    /**
     * Deposita dinheiro na conta de um jogador
     * @param playerId UUID do jogador
     * @param amount Valor a depositar
     * @param reason Motivo da transação
     * @return CompletableFuture com o resultado da operação
     */
    public CompletableFuture<Boolean> deposit(UUID playerId, double amount, String reason) {
        if (amount <= 0) {
            return CompletableFuture.completedFuture(false);
        }
        
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // Verifica se o jogador existe
                    Document playerDoc = playersCollection.find(Filters.eq("uuid", playerId.toString())).first();
                    
                    if (playerDoc == null) {
                        // Jogador não existe, cria um novo documento
                        double initialBalance = plugin.getConfigManager().getInitialBalance();
                        playerDoc = new Document()
                                .append("uuid", playerId.toString())
                                .append("balance", initialBalance + amount)
                                .append("last_activity", System.currentTimeMillis());
                        
                        playersCollection.insertOne(playerDoc);
                    } else {
                        // Jogador existe, atualiza o saldo
                        playersCollection.updateOne(
                            Filters.eq("uuid", playerId.toString()),
                            Updates.combine(
                                Updates.inc("balance", amount),
                                Updates.set("last_activity", System.currentTimeMillis())
                            )
                        );
                    }
                    
                    // Registra a transação
                    Document transactionDoc = new Document()
                            .append("player_uuid", playerId.toString())
                            .append("type", "deposit")
                            .append("amount", amount)
                            .append("reason", reason)
                            .append("timestamp", System.currentTimeMillis());
                    
                    transactionsCollection.insertOne(transactionDoc);
                    
                    future.complete(true);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Erro ao depositar: " + e.getMessage(), e);
                    future.completeExceptionally(e);
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return future;
    }

    /**
     * Retira dinheiro da conta de um jogador
     * @param playerId UUID do jogador
     * @param amount Valor a retirar
     * @param reason Motivo da transação
     * @return CompletableFuture com o resultado da operação
     */
    public CompletableFuture<Boolean> withdraw(UUID playerId, double amount, String reason) {
        if (amount <= 0) {
            return CompletableFuture.completedFuture(false);
        }
        
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        // Primeiro verifica se o jogador tem saldo suficiente
        getBalance(playerId).thenAccept(balance -> {
            if (balance < amount) {
                future.complete(false);
                return;
            }
            
            // Se tem saldo suficiente, realiza a retirada
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        // Atualiza o saldo do jogador
                        playersCollection.updateOne(
                            Filters.eq("uuid", playerId.toString()),
                            Updates.combine(
                                Updates.inc("balance", -amount),
                                Updates.set("last_activity", System.currentTimeMillis())
                            )
                        );
                        
                        // Registra a transação
                        Document transactionDoc = new Document()
                                .append("player_uuid", playerId.toString())
                                .append("type", "withdraw")
                                .append("amount", amount)
                                .append("reason", reason)
                                .append("timestamp", System.currentTimeMillis());
                        
                        transactionsCollection.insertOne(transactionDoc);
                        
                        future.complete(true);
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "Erro ao retirar: " + e.getMessage(), e);
                        future.completeExceptionally(e);
                    }
                }
            }.runTaskAsynchronously(plugin);
        });
        
        return future;
    }

    /**
     * Transfere dinheiro entre jogadores
     * @param fromId UUID do jogador de origem
     * @param toId UUID do jogador de destino
     * @param amount Valor a transferir
     * @return CompletableFuture com o resultado da operação
     */
    public CompletableFuture<Boolean> transferMoney(UUID fromId, UUID toId, double amount) {
        return transferMoney(fromId, toId, amount, "Transferência de dinheiro");
    }

    /**
     * Transfere dinheiro entre jogadores
     * @param fromId UUID do jogador de origem
     * @param toId UUID do jogador de destino
     * @param amount Valor a transferir
     * @param reason Motivo da transferência
     * @return CompletableFuture com o resultado da operação
     */
    public CompletableFuture<Boolean> transferMoney(UUID fromId, UUID toId, double amount, String reason) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        // Verifica se o jogador tem saldo suficiente
        getBalance(fromId).thenAccept(balance -> {
            if (balance < amount) {
                future.complete(false);
                return;
            }
            
            // Retira o dinheiro do jogador de origem
            withdraw(fromId, amount, "Transferência para " + toId + ": " + reason)
                .thenAccept(success -> {
                    if (!success) {
                        future.complete(false);
                        return;
                    }
                    
                    // Deposita o dinheiro no jogador de destino
                    deposit(toId, amount, "Transferência de " + fromId + ": " + reason)
                        .thenAccept(depositSuccess -> {
                            if (!depositSuccess) {
                                // Se falhar o depósito, devolve o dinheiro ao jogador de origem
                                deposit(fromId, amount, "Devolução de transferência falha para " + toId);
                                future.complete(false);
                                return;
                            }
                            
                            future.complete(true);
                        });
                });
        });
        
        return future;
    }

    /**
     * Verifica se um jogador tem conta
     * @param playerId UUID do jogador
     * @return CompletableFuture com o resultado da verificação
     */
    public CompletableFuture<Boolean> hasAccount(UUID playerId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Document playerDoc = playersCollection.find(Filters.eq("uuid", playerId.toString())).first();
                    future.complete(playerDoc != null);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Erro ao verificar conta do jogador: " + e.getMessage(), e);
                    future.completeExceptionally(e);
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return future;
    }

    /**
     * Cria uma conta para um jogador
     * @param playerId UUID do jogador
     * @param playerName Nome do jogador
     * @param initialBalance Saldo inicial
     * @return CompletableFuture com o resultado da operação
     */
    public CompletableFuture<Boolean> createAccount(UUID playerId, String playerName, double initialBalance) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // Verifica se o jogador já tem conta
                    Document playerDoc = playersCollection.find(Filters.eq("uuid", playerId.toString())).first();
                    
                    if (playerDoc != null) {
                        future.complete(true); // Conta já existe
                        return;
                    }
                    
                    // Cria uma nova conta
                    playerDoc = new Document()
                            .append("uuid", playerId.toString())
                            .append("name", playerName)
                            .append("balance", initialBalance)
                            .append("last_activity", System.currentTimeMillis());
                    
                    playersCollection.insertOne(playerDoc);
                    
                    future.complete(true);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Erro ao criar conta do jogador: " + e.getMessage(), e);
                    future.completeExceptionally(e);
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return future;
    }

    /**
     * Obtém os jogadores com mais dinheiro
     * @param limit Limite de jogadores a retornar
     * @return CompletableFuture com a lista de jogadores
     */
    public CompletableFuture<List<Document>> getTopPlayers(int limit) {
        CompletableFuture<List<Document>> future = new CompletableFuture<>();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    List<Document> topPlayers = new ArrayList<>();
                    
                    playersCollection.find()
                        .sort(Sorts.descending("balance"))
                        .limit(limit)
                        .into(topPlayers);
                    
                    future.complete(topPlayers);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Erro ao obter top jogadores: " + e.getMessage(), e);
                    future.completeExceptionally(e);
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return future;
    }

    /**
     * Obtém o banco de dados MongoDB
     * @return Instância do banco de dados
     */
    public MongoDatabase getDatabase() {
        return database;
    }
    
    /**
     * Obtém a coleção de jogadores
     * @return Coleção de jogadores
     */
    public MongoCollection<Document> getPlayersCollection() {
        return playersCollection;
    }
    
    /**
     * Obtém a coleção de transações
     * @return Coleção de transações
     */
    public MongoCollection<Document> getTransactionsCollection() {
        return transactionsCollection;
    }
    
    /**
     * Obtém a coleção de mercado
     * @return Coleção de mercado
     */
    public MongoCollection<Document> getMarketCollection() {
        return marketCollection;
    }
    
    /**
     * Obtém a coleção de configurações
     * @return Coleção de configurações
     */
    public MongoCollection<Document> getConfigCollection() {
        return configCollection;
    }
    
    /**
     * Obtém a coleção de bilhetes de loteria
     * @return Coleção de bilhetes de loteria
     */
    public MongoCollection<Document> getLotteryCollection() {
        return lotteryCollection;
    }
}
