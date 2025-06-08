package com.minecraft.economy.repository;

import com.minecraft.economy.api.infrastructure.AsyncExecutor;
import com.minecraft.economy.api.infrastructure.CacheManager;
import com.minecraft.economy.api.model.PlayerAccount;
import com.minecraft.economy.api.repository.PlayerAccountRepository;
import com.minecraft.economy.infrastructure.MongoDBConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementação de PlayerAccountRepository usando MongoDB
 */
public class MongoPlayerAccountRepository implements PlayerAccountRepository {

    private final MongoDBConnection dbConnection;
    private final AsyncExecutor asyncExecutor;
    private final CacheManager<UUID, Double> balanceCache;
    private final CacheManager<UUID, Object> accountCache;
    private final Logger logger;
    
    private static final String COLLECTION_NAME = "players";
    
    /**
     * Construtor
     * @param dbConnection Conexão com o MongoDB
     * @param asyncExecutor Executor assíncrono
     * @param balanceCache Cache de saldos
     * @param accountCache Cache de contas
     * @param logger Logger
     */
    @SuppressWarnings("unchecked")
    public MongoPlayerAccountRepository(
            MongoDBConnection dbConnection,
            AsyncExecutor asyncExecutor,
            CacheManager<UUID, Double> balanceCache,
            CacheManager<UUID, Object> accountCache,
            Logger logger) {
        this.dbConnection = dbConnection;
        this.asyncExecutor = asyncExecutor;
        this.balanceCache = balanceCache;
        this.accountCache = accountCache;
        this.logger = logger;
    }
    
    /**
     * Obtém a coleção de jogadores
     * @return Coleção de jogadores
     */
    private MongoCollection<Document> getCollection() {
        return dbConnection.getDatabase().getCollection(COLLECTION_NAME);
    }

    @Override
    public CompletableFuture<Double> getBalance(UUID playerId) {
        // Verifica se há um valor em cache
        Double cachedBalance = balanceCache.get(playerId);
        if (cachedBalance != null) {
            return CompletableFuture.completedFuture(cachedBalance);
        }
        
        return asyncExecutor.supplyAsync(() -> {
            try {
                if (!dbConnection.ensureConnected()) {
                    logger.warning("Falha ao obter saldo: Sem conexão com o banco de dados");
                    return 0.0;
                }
                
                Document playerDoc = getCollection().find(Filters.eq("uuid", playerId.toString())).first();
                
                if (playerDoc != null) {
                    double balance = playerDoc.getDouble("balance");
                    
                    // Atualiza o cache
                    balanceCache.put(playerId, balance, 60, TimeUnit.SECONDS);
                    
                    return balance;
                } else {
                    return 0.0;
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erro ao obter saldo: " + e.getMessage(), e);
                return 0.0;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> updateBalance(UUID playerId, double newBalance) {
        return asyncExecutor.supplyAsync(() -> {
            try {
                if (!dbConnection.ensureConnected()) {
                    logger.warning("Falha ao atualizar saldo: Sem conexão com o banco de dados");
                    return false;
                }
                
                getCollection().updateOne(
                    Filters.eq("uuid", playerId.toString()),
                    Updates.combine(
                        Updates.set("balance", newBalance),
                        Updates.set("last_activity", System.currentTimeMillis())
                    )
                );
                
                // Atualiza o cache
                balanceCache.put(playerId, newBalance, 60, TimeUnit.SECONDS);
                
                // Invalida o cache de conta
                accountCache.remove(playerId);
                
                return true;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erro ao atualizar saldo: " + e.getMessage(), e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> accountExists(UUID playerId) {
        // Verifica se há um valor em cache
        if (balanceCache.containsKey(playerId) || accountCache.containsKey(playerId)) {
            return CompletableFuture.completedFuture(true);
        }
        
        return asyncExecutor.supplyAsync(() -> {
            try {
                if (!dbConnection.ensureConnected()) {
                    logger.warning("Falha ao verificar conta: Sem conexão com o banco de dados");
                    return false;
                }
                
                Document playerDoc = getCollection().find(Filters.eq("uuid", playerId.toString())).first();
                return playerDoc != null;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erro ao verificar conta: " + e.getMessage(), e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> createAccount(UUID playerId, String playerName, double initialBalance) {
        return asyncExecutor.supplyAsync(() -> {
            try {
                if (!dbConnection.ensureConnected()) {
                    logger.warning("Falha ao criar conta: Sem conexão com o banco de dados");
                    return false;
                }
                
                // Verifica se o jogador já tem conta
                Document playerDoc = getCollection().find(Filters.eq("uuid", playerId.toString())).first();
                
                if (playerDoc != null) {
                    // Atualiza o cache
                    balanceCache.put(playerId, playerDoc.getDouble("balance"), 60, TimeUnit.SECONDS);
                    
                    return true; // Conta já existe
                }
                
                // Cria uma nova conta
                playerDoc = new Document()
                        .append("uuid", playerId.toString())
                        .append("name", playerName)
                        .append("balance", initialBalance)
                        .append("last_activity", System.currentTimeMillis());
                
                getCollection().insertOne(playerDoc);
                
                // Atualiza o cache
                balanceCache.put(playerId, initialBalance, 60, TimeUnit.SECONDS);
                
                // Cria e armazena no cache de conta
                PlayerAccount account = new PlayerAccount(playerId, playerName, initialBalance);
                accountCache.put(playerId, account, 60, TimeUnit.SECONDS);
                
                return true;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erro ao criar conta: " + e.getMessage(), e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<List<PlayerAccount>> getTopPlayers(int limit) {
        return asyncExecutor.supplyAsync(() -> {
            try {
                if (!dbConnection.ensureConnected()) {
                    logger.warning("Falha ao obter top jogadores: Sem conexão com o banco de dados");
                    return new ArrayList<>();
                }
                
                List<PlayerAccount> topPlayers = new ArrayList<>();
                
                getCollection().find()
                    .sort(Sorts.descending("balance"))
                    .limit(limit)
                    .forEach(doc -> {
                        UUID playerId = UUID.fromString(doc.getString("uuid"));
                        String playerName = doc.getString("name");
                        double balance = doc.getDouble("balance");
                        long lastActivity = doc.getLong("last_activity");
                        
                        PlayerAccount account = new PlayerAccount(playerId, playerName, balance, lastActivity);
                        topPlayers.add(account);
                        
                        // Atualiza o cache
                        balanceCache.put(playerId, balance, 60, TimeUnit.SECONDS);
                        accountCache.put(playerId, account, 60, TimeUnit.SECONDS);
                    });
                
                return topPlayers;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erro ao obter top jogadores: " + e.getMessage(), e);
                return new ArrayList<>();
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> updateLastActivity(UUID playerId) {
        return asyncExecutor.supplyAsync(() -> {
            try {
                if (!dbConnection.ensureConnected()) {
                    logger.warning("Falha ao atualizar última atividade: Sem conexão com o banco de dados");
                    return false;
                }
                
                getCollection().updateOne(
                    Filters.eq("uuid", playerId.toString()),
                    Updates.set("last_activity", System.currentTimeMillis())
                );
                
                // Invalida o cache de conta
                accountCache.remove(playerId);
                
                return true;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erro ao atualizar última atividade: " + e.getMessage(), e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<PlayerAccount> getPlayerAccount(UUID playerId) {
        // Verifica se há um valor em cache
        Object cachedObj = accountCache.get(playerId);
        PlayerAccount cachedAccount = cachedObj instanceof PlayerAccount ? (PlayerAccount) cachedObj : null;
        if (cachedAccount != null) {
            return CompletableFuture.completedFuture(cachedAccount);
        }
        
        return asyncExecutor.supplyAsync(() -> {
            try {
                if (!dbConnection.ensureConnected()) {
                    logger.warning("Falha ao obter conta: Sem conexão com o banco de dados");
                    return null;
                }
                
                Document playerDoc = getCollection().find(Filters.eq("uuid", playerId.toString())).first();
                
                if (playerDoc != null) {
                    String playerName = playerDoc.getString("name");
                    double balance = playerDoc.getDouble("balance");
                    long lastActivity = playerDoc.getLong("last_activity");
                    
                    PlayerAccount account = new PlayerAccount(playerId, playerName, balance, lastActivity);
                    
                    // Atualiza os caches
                    balanceCache.put(playerId, balance, 60, TimeUnit.SECONDS);
                    accountCache.put(playerId, account, 60, TimeUnit.SECONDS);
                    
                    return account;
                } else {
                    return null;
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erro ao obter conta: " + e.getMessage(), e);
                return null;
            }
        });
    }
}
