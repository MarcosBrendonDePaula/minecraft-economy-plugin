package com.minecraft.economy.example;

import com.minecraft.economy.api.model.PlayerAccount;
import com.minecraft.mongodb.api.MongoClient;
import com.minecraft.mongodb.api.MongoCollection;
import com.minecraft.mongodb.api.annotation.Collection;
import com.minecraft.mongodb.api.annotation.Document;
import com.minecraft.mongodb.api.annotation.Field;
import com.minecraft.mongodb.api.annotation.Id;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Exemplo de uso da biblioteca MongoDB
 */
public class MongoDBExample {
    
    private final MongoClient mongoClient;
    private final Logger logger;
    
    /**
     * Construtor
     * @param logger Logger
     */
    public MongoDBExample(Logger logger) {
        this.logger = logger;
        
        // Inicializa o cliente MongoDB
        this.mongoClient = MongoClient.builder()
                .connectionString("mongodb://localhost:27017")
                .database("minecraft_economy")
                .cacheEnabled(true)
                .build();
        
        // Conecta ao MongoDB
        if (!mongoClient.connect()) {
            logger.severe("Falha ao conectar ao MongoDB!");
        }
    }
    
    /**
     * Exemplo de criação de conta de jogador
     * @param playerId ID do jogador
     * @param playerName Nome do jogador
     * @param initialBalance Saldo inicial
     * @return true se a conta foi criada com sucesso
     */
    public boolean createPlayerAccount(UUID playerId, String playerName, double initialBalance) {
        try {
            // Cria um objeto de conta de jogador
            MongoPlayerAccount account = new MongoPlayerAccount();
            account.setPlayerId(playerId);
            account.setPlayerName(playerName);
            account.setBalance(initialBalance);
            account.setLastActivity(System.currentTimeMillis());
            
            // Insere o objeto na coleção
            return mongoClient.getCollection("players").insert(account);
        } catch (Exception e) {
            logger.severe("Erro ao criar conta de jogador: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Exemplo de obtenção de saldo de jogador
     * @param playerId ID do jogador
     * @return Saldo do jogador
     */
    public double getPlayerBalance(UUID playerId) {
        try {
            // Busca a conta do jogador
            Optional<MongoPlayerAccount> account = mongoClient.getCollection("players")
                    .find()
                    .where("playerId").isEqualTo(playerId.toString())
                    .first(MongoPlayerAccount.class);
            
            // Retorna o saldo se a conta existir
            return account.map(MongoPlayerAccount::getBalance).orElse(0.0);
        } catch (Exception e) {
            logger.severe("Erro ao obter saldo de jogador: " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Exemplo de atualização de saldo de jogador
     * @param playerId ID do jogador
     * @param newBalance Novo saldo
     * @return true se o saldo foi atualizado com sucesso
     */
    public boolean updatePlayerBalance(UUID playerId, double newBalance) {
        try {
            // Atualiza o saldo do jogador
            int updated = mongoClient.getCollection("players")
                    .update()
                    .where("playerId").isEqualTo(playerId.toString())
                    .set("balance", newBalance)
                    .set("lastActivity", System.currentTimeMillis())
                    .execute();
            
            return updated > 0;
        } catch (Exception e) {
            logger.severe("Erro ao atualizar saldo de jogador: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Exemplo de obtenção dos jogadores mais ricos
     * @param limit Limite de jogadores
     * @return Lista de contas de jogadores
     */
    public List<PlayerAccount> getTopPlayers(int limit) {
        try {
            // Busca os jogadores mais ricos
            List<MongoPlayerAccount> accounts = mongoClient.getCollection("players")
                    .find()
                    .sort("balance", false) // Ordem decrescente
                    .limit(limit)
                    .toList(MongoPlayerAccount.class);
            
            // Converte para PlayerAccount
            return accounts.stream()
                    .map(account -> new PlayerAccount(
                            account.getPlayerId(),
                            account.getPlayerName(),
                            account.getBalance(),
                            account.getLastActivity()))
                    .toList();
        } catch (Exception e) {
            logger.severe("Erro ao obter top jogadores: " + e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Exemplo de exclusão de conta de jogador
     * @param playerId ID do jogador
     * @return true se a conta foi excluída com sucesso
     */
    public boolean deletePlayerAccount(UUID playerId) {
        try {
            // Exclui a conta do jogador
            int deleted = mongoClient.getCollection("players")
                    .delete()
                    .where("playerId").isEqualTo(playerId.toString())
                    .execute();
            
            return deleted > 0;
        } catch (Exception e) {
            logger.severe("Erro ao excluir conta de jogador: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Fecha a conexão com o MongoDB
     */
    public void close() {
        mongoClient.disconnect();
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
