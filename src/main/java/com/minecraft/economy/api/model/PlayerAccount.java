package com.minecraft.economy.api.model;

import java.util.UUID;

/**
 * Modelo para conta de jogador
 */
public class PlayerAccount {
    private final UUID playerId;
    private final String playerName;
    private double balance;
    private long lastActivity;
    
    /**
     * Construtor para conta de jogador
     * @param playerId UUID do jogador
     * @param playerName Nome do jogador
     * @param balance Saldo inicial
     */
    public PlayerAccount(UUID playerId, String playerName, double balance) {
        this(playerId, playerName, balance, System.currentTimeMillis());
    }
    
    /**
     * Construtor para conta de jogador com última atividade
     * @param playerId UUID do jogador
     * @param playerName Nome do jogador
     * @param balance Saldo inicial
     * @param lastActivity Timestamp da última atividade
     */
    public PlayerAccount(UUID playerId, String playerName, double balance, long lastActivity) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.balance = balance;
        this.lastActivity = lastActivity;
    }
    
    /**
     * Obtém o UUID do jogador
     * @return UUID do jogador
     */
    public UUID getPlayerId() {
        return playerId;
    }
    
    /**
     * Obtém o nome do jogador
     * @return Nome do jogador
     */
    public String getPlayerName() {
        return playerName;
    }
    
    /**
     * Obtém o saldo do jogador
     * @return Saldo do jogador
     */
    public double getBalance() {
        return balance;
    }
    
    /**
     * Define o saldo do jogador
     * @param balance Novo saldo
     */
    public void setBalance(double balance) {
        this.balance = balance;
    }
    
    /**
     * Adiciona um valor ao saldo do jogador
     * @param amount Valor a adicionar
     * @return Novo saldo
     */
    public double deposit(double amount) {
        this.balance += amount;
        updateActivity();
        return this.balance;
    }
    
    /**
     * Retira um valor do saldo do jogador
     * @param amount Valor a retirar
     * @return Novo saldo
     * @throws IllegalArgumentException Se o saldo for insuficiente
     */
    public double withdraw(double amount) {
        if (this.balance < amount) {
            throw new IllegalArgumentException("Saldo insuficiente");
        }
        this.balance -= amount;
        updateActivity();
        return this.balance;
    }
    
    /**
     * Verifica se o jogador tem saldo suficiente
     * @param amount Valor a verificar
     * @return true se o jogador tem saldo suficiente, false caso contrário
     */
    public boolean hasBalance(double amount) {
        return this.balance >= amount;
    }
    
    /**
     * Obtém o timestamp da última atividade
     * @return Timestamp da última atividade
     */
    public long getLastActivity() {
        return lastActivity;
    }
    
    /**
     * Atualiza o timestamp da última atividade para o momento atual
     */
    public void updateActivity() {
        this.lastActivity = System.currentTimeMillis();
    }
    
    /**
     * Define o timestamp da última atividade
     * @param lastActivity Novo timestamp
     */
    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }
    
    @Override
    public String toString() {
        return "PlayerAccount{" +
                "playerId=" + playerId +
                ", playerName='" + playerName + '\'' +
                ", balance=" + balance +
                ", lastActivity=" + lastActivity +
                '}';
    }
}
