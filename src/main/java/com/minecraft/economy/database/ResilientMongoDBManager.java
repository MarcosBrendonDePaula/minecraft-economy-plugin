package com.minecraft.economy.database;

import com.minecraft.economy.core.EconomyPlugin;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
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
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Gerenciador resiliente de conexão e operações com MongoDB
 * Implementa mecanismos de fallback, reconexão automática e cache local
 */
public class ResilientMongoDBManager {

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
    
    // Cache local para operações críticas
    private final Map<UUID, Double> balanceCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private final long cacheDuration = 60000; // 60 segundos
    
    // Controle de estado da conexão
    private boolean isConnected = false;
    private int reconnectAttempts = 0;
    private final int maxReconnectAttempts = 5;
    private BukkitTask reconnectTask = null;
    private final Object connectionLock = new Object();
    
    /**
     * Obtém a instância do banco de dados MongoDB
     * Método de compatibilidade com código existente
     * @return Instância do banco de dados MongoDB
     */
    public MongoDatabase getDatabase() {
        ensureConnected();
        return database;
    }
    
    /**
     * Obtém a coleção de jogadores
     * Método de compatibilidade com código existente
     * @return Coleção de jogadores
     */
    public MongoCollection<Document> getPlayersCollection() {
        ensureConnected();
        return playersCollection;
    }
    
    /**
     * Obtém a coleção de transações
     * Método de compatibilidade com código existente
     * @return Coleção de transações
     */
    public MongoCollection<Document> getTransactionsCollection() {
        ensureConnected();
        return transactionsCollection;
    }
    
    /**
     * Obtém a coleção de mercado
     * Método de compatibilidade com código existente
     * @return Coleção de mercado
     */
    public MongoCollection<Document> getMarketCollection() {
        ensureConnected();
        return marketCollection;
    }
    
    /**
     * Obtém a coleção de configurações
     * Método de compatibilidade com código existente
     * @return Coleção de configurações
     */
    public MongoCollection<Document> getConfigCollection() {
        ensureConnected();
        return configCollection;
    }
    
    /**
     * Obtém a coleção de bilhetes de loteria
     * Método de compatibilidade com código existente
     * @return Coleção de bilhetes de loteria
     */
    public MongoCollection<Document> getLotteryCollection() {
        ensureConnected();
        return lotteryCollection;
    }

    /**
     * Construtor com configurações da config.yml
     * @param plugin Instância do plugin
     */
    public ResilientMongoDBManager(EconomyPlugin plugin) {
        this.plugin = plugin;
        
        // Usa apenas connection_string para evitar ambiguidade
        this.connectionString = plugin.getConfig().getString("mongodb.connection_string", "mongodb://localhost:27017");
        this.databaseName = plugin.getConfig().getString("mongodb.database", "minecraft_economy");
        
        plugin.getLogger().info("Inicializando gerenciador resiliente de MongoDB com conexão: " + 
                connectionString.replaceAll("mongodb://([^:]+):([^@]+)@", "mongodb://****:****@"));
    }

    /**
     * Inicializa a conexão com o MongoDB
     * @return true se a conexão foi estabelecida com sucesso
     */
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
                
                // Inicializa as coleções
                playersCollection = database.getCollection("players");
                transactionsCollection = database.getCollection("transactions");
                marketCollection = database.getCollection("market");
                configCollection = database.getCollection("config");
                lotteryCollection = database.getCollection("lottery_tickets");
                
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
                    plugin.getLogger().severe("Número máximo de tentativas de reconexão atingido. " +
                            "O plugin continuará funcionando com dados em cache quando possível.");
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
        
        reconnectTask = new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getLogger().info("Tentando reconectar ao MongoDB...");
                if (connect()) {
                    plugin.getLogger().info("Reconexão bem-sucedida!");
                    reconnectTask = null;
                }
            }
        }.runTaskLaterAsynchronously(plugin, delay * 20L); // 20 ticks = 1 segundo
    }

    /**
     * Fecha a conexão com o MongoDB
     */
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
    
    /**
     * Verifica se a conexão está ativa e tenta reconectar se necessário
     * @return true se a conexão está ativa ou foi restabelecida
     */
    private boolean ensureConnected() {
        if (isConnected) {
            return true;
        }
        
        return connect();
    }

    /**
     * Obtém o saldo de um jogador
     * @param playerId UUID do jogador
     * @return CompletableFuture com o saldo do jogador
     */
    public CompletableFuture<Double> getBalance(UUID playerId) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        
        // Verifica se há um valor em cache válido
        if (balanceCache.containsKey(playerId)) {
            long timestamp = cacheTimestamps.getOrDefault(playerId, 0L);
            if (System.currentTimeMillis() - timestamp < cacheDuration) {
                double cachedBalance = balanceCache.get(playerId);
                plugin.getLogger().fine("Usando saldo em cache para " + playerId + ": " + cachedBalance);
                return CompletableFuture.completedFuture(cachedBalance);
            }
        }
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (!ensureConnected()) {
                        // Se não conseguiu conectar, usa o cache mesmo que expirado
                        if (balanceCache.containsKey(playerId)) {
                            double cachedBalance = balanceCache.get(playerId);
                            plugin.getLogger().warning("Usando saldo em cache expirado para " + playerId + 
                                    " devido a falha de conexão: " + cachedBalance);
                            future.complete(cachedBalance);
                        } else {
                            // Se não há cache, usa o saldo inicial
                            double initialBalance = plugin.getConfigManager().getInitialBalance();
                            plugin.getLogger().warning("Usando saldo inicial para " + playerId + 
                                    " devido a falha de conexão: " + initialBalance);
                            future.complete(initialBalance);
                        }
                        return;
                    }
                    
                    Document playerDoc = playersCollection.find(Filters.eq("uuid", playerId.toString())).first();
                    
                    if (playerDoc != null) {
                        double balance = playerDoc.getDouble("balance");
                        
                        // Atualiza o cache
                        balanceCache.put(playerId, balance);
                        cacheTimestamps.put(playerId, System.currentTimeMillis());
                        
                        future.complete(balance);
                    } else {
                        // Jogador não encontrado, retorna saldo inicial
                        double initialBalance = plugin.getConfigManager().getInitialBalance();
                        
                        // Atualiza o cache
                        balanceCache.put(playerId, initialBalance);
                        cacheTimestamps.put(playerId, System.currentTimeMillis());
                        
                        future.complete(initialBalance);
                    }
                } catch (Exception e) {
                    String errorMsg = "Erro ao obter saldo do jogador " + playerId + ": " + e.getMessage();
                    plugin.getLogger().log(Level.SEVERE, errorMsg, e);
                    
                    // Em caso de erro, tenta usar o cache mesmo que expirado
                    if (balanceCache.containsKey(playerId)) {
                        double cachedBalance = balanceCache.get(playerId);
                        plugin.getLogger().warning("Usando saldo em cache para " + playerId + 
                                " devido a erro: " + cachedBalance);
                        future.complete(cachedBalance);
                    } else {
                        // Se não há cache, usa o saldo inicial
                        double initialBalance = plugin.getConfigManager().getInitialBalance();
                        plugin.getLogger().warning("Usando saldo inicial para " + playerId + 
                                " devido a erro: " + initialBalance);
                        future.complete(initialBalance);
                    }
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
        // Verifica se há um valor em cache válido
        if (balanceCache.containsKey(playerId)) {
            long timestamp = cacheTimestamps.getOrDefault(playerId, 0L);
            if (System.currentTimeMillis() - timestamp < cacheDuration) {
                boolean hasEnough = balanceCache.get(playerId) >= amount;
                return CompletableFuture.completedFuture(hasEnough);
            }
        }
        
        // Se não há cache válido, obtém o saldo do banco de dados
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
                    if (!ensureConnected()) {
                        plugin.getLogger().severe("Falha ao depositar " + amount + " para " + playerId + 
                                ": Sem conexão com o banco de dados");
                        future.complete(false);
                        return;
                    }
                    
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
                        
                        // Atualiza o cache
                        balanceCache.put(playerId, initialBalance + amount);
                        cacheTimestamps.put(playerId, System.currentTimeMillis());
                    } else {
                        // Jogador existe, atualiza o saldo
                        double currentBalance = playerDoc.getDouble("balance");
                        double newBalance = currentBalance + amount;
                        
                        playersCollection.updateOne(
                            Filters.eq("uuid", playerId.toString()),
                            Updates.combine(
                                Updates.set("balance", newBalance),
                                Updates.set("last_activity", System.currentTimeMillis())
                            )
                        );
                        
                        // Atualiza o cache
                        balanceCache.put(playerId, newBalance);
                        cacheTimestamps.put(playerId, System.currentTimeMillis());
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
                    String errorMsg = "Erro ao depositar " + amount + " para " + playerId + ": " + e.getMessage();
                    plugin.getLogger().log(Level.SEVERE, errorMsg, e);
                    future.complete(false);
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
                        if (!ensureConnected()) {
                            plugin.getLogger().severe("Falha ao retirar " + amount + " de " + playerId + 
                                    ": Sem conexão com o banco de dados");
                            future.complete(false);
                            return;
                        }
                        
                        // Atualiza o saldo do jogador
                        double newBalance = balance - amount;
                        playersCollection.updateOne(
                            Filters.eq("uuid", playerId.toString()),
                            Updates.combine(
                                Updates.set("balance", newBalance),
                                Updates.set("last_activity", System.currentTimeMillis())
                            )
                        );
                        
                        // Atualiza o cache
                        balanceCache.put(playerId, newBalance);
                        cacheTimestamps.put(playerId, System.currentTimeMillis());
                        
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
                        String errorMsg = "Erro ao retirar " + amount + " de " + playerId + ": " + e.getMessage();
                        plugin.getLogger().log(Level.SEVERE, errorMsg, e);
                        future.complete(false);
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
        // Se há um valor em cache, o jogador tem conta
        if (balanceCache.containsKey(playerId)) {
            return CompletableFuture.completedFuture(true);
        }
        
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (!ensureConnected()) {
                        // Se não conseguiu conectar, assume que o jogador tem conta
                        // para evitar problemas com jogadores existentes
                        plugin.getLogger().warning("Assumindo que o jogador " + playerId + 
                                " tem conta devido a falha de conexão");
                        future.complete(true);
                        return;
                    }
                    
                    Document playerDoc = playersCollection.find(Filters.eq("uuid", playerId.toString())).first();
                    future.complete(playerDoc != null);
                } catch (Exception e) {
                    String errorMsg = "Erro ao verificar conta do jogador " + playerId + ": " + e.getMessage();
                    plugin.getLogger().log(Level.SEVERE, errorMsg, e);
                    
                    // Em caso de erro, assume que o jogador tem conta
                    // para evitar problemas com jogadores existentes
                    future.complete(true);
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
                    if (!ensureConnected()) {
                        plugin.getLogger().severe("Falha ao criar conta para " + playerId + 
                                ": Sem conexão com o banco de dados");
                        future.complete(false);
                        return;
                    }
                    
                    // Verifica se o jogador já tem conta
                    Document playerDoc = playersCollection.find(Filters.eq("uuid", playerId.toString())).first();
                    
                    if (playerDoc != null) {
                        // Atualiza o cache
                        balanceCache.put(playerId, playerDoc.getDouble("balance"));
                        cacheTimestamps.put(playerId, System.currentTimeMillis());
                        
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
                    
                    // Atualiza o cache
                    balanceCache.put(playerId, initialBalance);
                    cacheTimestamps.put(playerId, System.currentTimeMillis());
                    
                    future.complete(true);
                } catch (Exception e) {
                    String errorMsg = "Erro ao criar conta para " + playerId + ": " + e.getMessage();
                    plugin.getLogger().log(Level.SEVERE, errorMsg, e);
                    future.complete(false);
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
                    if (!ensureConnected()) {
                        plugin.getLogger().severe("Falha ao obter top jogadores: Sem conexão com o banco de dados");
                        future.complete(new ArrayList<>());
                        return;
                    }
                    
                    List<Document> topPlayers = new ArrayList<>();
                    
                    playersCollection.find()
                        .sort(Sorts.descending("balance"))
                        .limit(limit)
                        .into(topPlayers);
                    
                    future.complete(topPlayers);
                } catch (Exception e) {
                    String errorMsg = "Erro ao obter top jogadores: " + e.getMessage();
                    plugin.getLogger().log(Level.SEVERE, errorMsg, e);
                    future.complete(new ArrayList<>());
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return future;
    }
    
    /**
     * Salva um bilhete de loteria
     * @param ticket Documento do bilhete
     * @return CompletableFuture com o resultado da operação
     */
    public CompletableFuture<Boolean> saveLotteryTicket(Document ticket) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (!ensureConnected()) {
                        plugin.getLogger().severe("Falha ao salvar bilhete de loteria: Sem conexão com o banco de dados");
                        future.complete(false);
                        return;
                    }
                    
                    lotteryCollection.insertOne(ticket);
                    future.complete(true);
                } catch (Exception e) {
                    String errorMsg = "Erro ao salvar bilhete de loteria: " + e.getMessage();
                    plugin.getLogger().log(Level.SEVERE, errorMsg, e);
                    future.complete(false);
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return future;
    }
    
    /**
     * Obtém todos os bilhetes de loteria
     * @return CompletableFuture com a lista de bilhetes
     */
    public CompletableFuture<List<Document>> getAllLotteryTickets() {
        CompletableFuture<List<Document>> future = new CompletableFuture<>();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (!ensureConnected()) {
                        plugin.getLogger().severe("Falha ao obter bilhetes de loteria: Sem conexão com o banco de dados");
                        future.complete(new ArrayList<>());
                        return;
                    }
                    
                    List<Document> tickets = new ArrayList<>();
                    lotteryCollection.find().into(tickets);
                    future.complete(tickets);
                } catch (Exception e) {
                    String errorMsg = "Erro ao obter bilhetes de loteria: " + e.getMessage();
                    plugin.getLogger().log(Level.SEVERE, errorMsg, e);
                    future.complete(new ArrayList<>());
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return future;
    }
    
    /**
     * Obtém os bilhetes de loteria de um jogador
     * @param playerId UUID do jogador
     * @return CompletableFuture com a lista de bilhetes
     */
    public CompletableFuture<List<Document>> getPlayerLotteryTickets(UUID playerId) {
        CompletableFuture<List<Document>> future = new CompletableFuture<>();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (!ensureConnected()) {
                        plugin.getLogger().severe("Falha ao obter bilhetes de loteria do jogador " + 
                                playerId + ": Sem conexão com o banco de dados");
                        future.complete(new ArrayList<>());
                        return;
                    }
                    
                    List<Document> tickets = new ArrayList<>();
                    lotteryCollection.find(Filters.eq("player_uuid", playerId.toString())).into(tickets);
                    future.complete(tickets);
                } catch (Exception e) {
                    String errorMsg = "Erro ao obter bilhetes de loteria do jogador " + playerId + ": " + e.getMessage();
                    plugin.getLogger().log(Level.SEVERE, errorMsg, e);
                    future.complete(new ArrayList<>());
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return future;
    }
    
    /**
     * Limpa todos os bilhetes de loteria
     * @return CompletableFuture com o resultado da operação
     */
    public CompletableFuture<Boolean> clearLotteryTickets() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (!ensureConnected()) {
                        plugin.getLogger().severe("Falha ao limpar bilhetes de loteria: Sem conexão com o banco de dados");
                        future.complete(false);
                        return;
                    }
                    
                    lotteryCollection.deleteMany(new Document());
                    future.complete(true);
                } catch (Exception e) {
                    String errorMsg = "Erro ao limpar bilhetes de loteria: " + e.getMessage();
                    plugin.getLogger().log(Level.SEVERE, errorMsg, e);
                    future.complete(false);
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return future;
    }
    
    /**
     * Salva uma configuração no banco de dados
     * @param key Chave da configuração
     * @param value Valor da configuração
     * @return CompletableFuture com o resultado da operação
     */
    public CompletableFuture<Boolean> saveConfig(String key, Object value) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (!ensureConnected()) {
                        plugin.getLogger().severe("Falha ao salvar configuração " + key + 
                                ": Sem conexão com o banco de dados");
                        future.complete(false);
                        return;
                    }
                    
                    configCollection.updateOne(
                        Filters.eq("key", key),
                        Updates.set("value", value),
                        new com.mongodb.client.model.UpdateOptions().upsert(true)
                    );
                    
                    future.complete(true);
                } catch (Exception e) {
                    String errorMsg = "Erro ao salvar configuração " + key + ": " + e.getMessage();
                    plugin.getLogger().log(Level.SEVERE, errorMsg, e);
                    future.complete(false);
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return future;
    }
    
    /**
     * Obtém uma configuração do banco de dados
     * @param key Chave da configuração
     * @param defaultValue Valor padrão caso a configuração não exista
     * @return CompletableFuture com o valor da configuração
     */
    public CompletableFuture<Object> getConfig(String key, Object defaultValue) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (!ensureConnected()) {
                        plugin.getLogger().warning("Falha ao obter configuração " + key + 
                                ": Sem conexão com o banco de dados. Usando valor padrão: " + defaultValue);
                        future.complete(defaultValue);
                        return;
                    }
                    
                    Document configDoc = configCollection.find(Filters.eq("key", key)).first();
                    
                    if (configDoc != null) {
                        future.complete(configDoc.get("value"));
                    } else {
                        future.complete(defaultValue);
                    }
                } catch (Exception e) {
                    String errorMsg = "Erro ao obter configuração " + key + ": " + e.getMessage();
                    plugin.getLogger().log(Level.SEVERE, errorMsg, e);
                    future.complete(defaultValue);
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return future;
    }
    
    /**
     * Verifica se a conexão está ativa
     * @return true se a conexão está ativa
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * Limpa o cache de um jogador específico
     * @param playerId UUID do jogador
     */
    public void clearCache(UUID playerId) {
        balanceCache.remove(playerId);
        cacheTimestamps.remove(playerId);
    }
    
    /**
     * Limpa todo o cache
     */
    public void clearAllCache() {
        balanceCache.clear();
        cacheTimestamps.clear();
    }
}
