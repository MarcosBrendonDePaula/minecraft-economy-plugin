package com.minecraft.economy.example;

import com.minecraft.economy.api.infrastructure.AsyncExecutor;
import com.minecraft.economy.api.infrastructure.CacheManager;
import com.minecraft.economy.api.model.PlayerAccount;
import com.minecraft.economy.api.repository.PlayerAccountRepository;
import com.minecraft.mongodb.api.MongoClient;
import com.minecraft.mongodb.api.MongoCollection;
import com.minecraft.mongodb.api.annotation.Collection;
import com.minecraft.mongodb.api.annotation.Document;
import com.minecraft.mongodb.api.annotation.Field;
import com.minecraft.mongodb.api.annotation.Id;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Implementação refatorada de PlayerAccountRepository usando a nova biblioteca MongoDB
 */
public class RefactoredMongoPlayerAccountRepository implements PlayerAccountRepository {

    private final MongoClient mongoClient;
    private final AsyncExecutor asyncExecutor;
    private final CacheManager<UUID, Double> balanceCache;
    private final CacheManager<UUID, Object> accountCache;
    private final Logger logger;
    
    /**
     * Construtor
     * @param mongoClient Cliente MongoDB
     * @param asyncExecutor Executor assíncrono
     * @param balanceCache Cache de saldos
     * @param accountCache Cache de contas
     * @param logger Logger
     */
    public RefactoredMongoPlayerAccountRepository(
            MongoClient mongoClient,
            AsyncExecutor asyncExecutor,
            CacheManager<UUID, Double> balanceCache,
            CacheManager<UUID, Object> accountCache,
            Logger logger) {
        this.mongoClient = mongoClient;
        this.asyncExecutor = asyncExecutor;
        this.balanceCache = balanceCache;
        this.accountCache = accountCache;
        this.logger = logger;
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
                // Busca a conta do jogador
                Optional<MongoPlayerAccount> account = getCollection()
                        .find()
                        .where("uuid").isEqualTo(playerId.toString())
                        .first(MongoPlayerAccount.class);
                
                // Retorna o saldo se a conta existir
                double balance = account.map(MongoPlayerAccount::getBalance).orElse(0.0);
                
                // Atualiza o cache
                balanceCache.put(playerId, balance, 60, TimeUnit.SECONDS);
                
                return balance;
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
                // Atualiza o saldo do jogador
                int updated = getCollection()
                        .update()
                        .where("uuid").isEqualTo(playerId.toString())
                        .set("balance", newBalance)
                        .set("last_activity", System.currentTimeMillis())
                        .execute();
                
                if (updated > 0) {
                    // Atualiza o cache
                    balanceCache.put(playerId, newBalance, 60, TimeUnit.SECONDS);
                    
                    // Invalida o cache de conta
                    accountCache.remove(playerId);
                    
                    return true;
                }
                
                return false;
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
                // Verifica se a conta existe
                return getCollection()
                        .find()
                        .where("uuid").isEqualTo(playerId.toString())
                        .exists();
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
                // Verifica se o jogador já tem conta
                boolean exists = getCollection()
                        .find()
                        .where("uuid").isEqualTo(playerId.toString())
                        .exists();
                
                if (exists) {
                    // Busca o saldo atual
                    Optional<MongoPlayerAccount> account = getCollection()
                            .find()
                            .where("uuid").isEqualTo(playerId.toString())
                            .first(MongoPlayerAccount.class);
                    
                    // Atualiza o cache
                    account.ifPresent(a -> balanceCache.put(playerId, a.getBalance(), 60, TimeUnit.SECONDS));
                    
                    return true; // Conta já existe
                }
                
                // Cria uma nova conta
                MongoPlayerAccount account = new MongoPlayerAccount();
                account.setPlayerId(playerId);
                account.setPlayerName(playerName);
                account.setBalance(initialBalance);
                account.setLastActivity(System.currentTimeMillis());
                
                // Insere a conta
                boolean success = getCollection().insert(account);
                
                if (success) {
                    // Atualiza o cache
                    balanceCache.put(playerId, initialBalance, 60, TimeUnit.SECONDS);
                    
                    // Cria e armazena no cache de conta
                    PlayerAccount playerAccount = new PlayerAccount(playerId, playerName, initialBalance);
                    accountCache.put(playerId, playerAccount, 60, TimeUnit.SECONDS);
                }
                
                return success;
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
                // Busca os jogadores mais ricos
                List<MongoPlayerAccount> accounts = getCollection()
                        .find()
                        .sort("balance", false) // Ordem decrescente
                        .limit(limit)
                        .toList(MongoPlayerAccount.class);
                
                // Converte para PlayerAccount
                List<PlayerAccount> result = accounts.stream()
                        .map(account -> {
                            UUID playerId = account.getPlayerId();
                            String playerName = account.getPlayerName();
                            double balance = account.getBalance();
                            long lastActivity = account.getLastActivity();
                            
                            // Atualiza o cache
                            balanceCache.put(playerId, balance, 60, TimeUnit.SECONDS);
                            
                            PlayerAccount playerAccount = new PlayerAccount(playerId, playerName, balance, lastActivity);
                            accountCache.put(playerId, playerAccount, 60, TimeUnit.SECONDS);
                            
                            return playerAccount;
                        })
                        .collect(Collectors.toList());
                
                return result;
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
                // Atualiza a última atividade do jogador
                int updated = getCollection()
                        .update()
                        .where("uuid").isEqualTo(playerId.toString())
                        .set("last_activity", System.currentTimeMillis())
                        .execute();
                
                if (updated > 0) {
                    // Invalida o cache de conta
                    accountCache.remove(playerId);
                    
                    return true;
                }
                
                return false;
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
                // Busca a conta do jogador
                Optional<MongoPlayerAccount> account = getCollection()
                        .find()
                        .where("uuid").isEqualTo(playerId.toString())
                        .first(MongoPlayerAccount.class);
                
                if (account.isPresent()) {
                    MongoPlayerAccount mongoAccount = account.get();
                    String playerName = mongoAccount.getPlayerName();
                    double balance = mongoAccount.getBalance();
                    long lastActivity = mongoAccount.getLastActivity();
                    
                    PlayerAccount playerAccount = new PlayerAccount(playerId, playerName, balance, lastActivity);
                    
                    // Atualiza os caches
                    balanceCache.put(playerId, balance, 60, TimeUnit.SECONDS);
                    accountCache.put(playerId, playerAccount, 60, TimeUnit.SECONDS);
                    
                    return playerAccount;
                } else {
                    return null;
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erro ao obter conta: " + e.getMessage(), e);
                return null;
            }
        });
    }
    
    /**
     * Obtém a coleção de jogadores
     * @return Coleção de jogadores
     */
    private MongoCollection getCollection() {
        return mongoClient.getCollection("players");
    }
    
    /**
     * Classe de modelo para conta de jogador no MongoDB
     */
    @Document
    @Collection("players")
    public static class MongoPlayerAccount {
        
        @Id
        private String id;
        
        @Field("uuid")
        private String playerId;
        
        @Field("name")
        private String playerName;
        
        @Field
        private double balance;
        
        @Field("last_activity")
        private long lastActivity;
        
        /**
         * Construtor padrão
         */
        public MongoPlayerAccount() {
        }
        
        /**
         * Obtém o ID do documento
         * @return ID do documento
         */
        public String getId() {
            return id;
        }
        
        /**
         * Define o ID do documento
         * @param id ID do documento
         */
        public void setId(String id) {
            this.id = id;
        }
        
        /**
         * Obtém o ID do jogador
         * @return ID do jogador
         */
        public UUID getPlayerId() {
            return UUID.fromString(playerId);
        }
        
        /**
         * Define o ID do jogador
         * @param playerId ID do jogador
         */
        public void setPlayerId(UUID playerId) {
            this.playerId = playerId.toString();
        }
        
        /**
         * Obtém o nome do jogador
         * @return Nome do jogador
         */
        public String getPlayerName() {
            return playerName;
        }
        
        /**
         * Define o nome do jogador
         * @param playerName Nome do jogador
         */
        public void setPlayerName(String playerName) {
            this.playerName = playerName;
        }
        
        /**
         * Obtém o saldo
         * @return Saldo
         */
        public double getBalance() {
            return balance;
        }
        
        /**
         * Define o saldo
         * @param balance Saldo
         */
        public void setBalance(double balance) {
            this.balance = balance;
        }
        
        /**
         * Obtém a última atividade
         * @return Última atividade
         */
        public long getLastActivity() {
            return lastActivity;
        }
        
        /**
         * Define a última atividade
         * @param lastActivity Última atividade
         */
        public void setLastActivity(long lastActivity) {
            this.lastActivity = lastActivity;
        }
    }
}
