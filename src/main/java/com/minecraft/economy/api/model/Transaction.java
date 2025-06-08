package com.minecraft.economy.api.model;

import java.util.UUID;

/**
 * Modelo para transação
 */
public class Transaction {
    
    /**
     * Tipos de transação
     */
    public enum Type {
        DEPOSIT, WITHDRAW, TRANSFER, TAX, SHOP, LOTTERY, ADMIN
    }
    
    private final UUID playerId;
    private final UUID targetId; // Opcional, usado em transferências
    private final Type type;
    private final double amount;
    private final String reason;
    private final long timestamp;
    
    /**
     * Construtor para transação simples
     * @param playerId UUID do jogador
     * @param type Tipo da transação
     * @param amount Valor da transação
     * @param reason Motivo da transação
     */
    public Transaction(UUID playerId, Type type, double amount, String reason) {
        this(playerId, null, type, amount, reason, System.currentTimeMillis());
    }
    
    /**
     * Construtor para transação com jogador alvo (ex: transferência)
     * @param playerId UUID do jogador
     * @param targetId UUID do jogador alvo
     * @param type Tipo da transação
     * @param amount Valor da transação
     * @param reason Motivo da transação
     */
    public Transaction(UUID playerId, UUID targetId, Type type, double amount, String reason) {
        this(playerId, targetId, type, amount, reason, System.currentTimeMillis());
    }
    
    /**
     * Construtor completo para transação
     * @param playerId UUID do jogador
     * @param targetId UUID do jogador alvo
     * @param type Tipo da transação
     * @param amount Valor da transação
     * @param reason Motivo da transação
     * @param timestamp Timestamp da transação
     */
    public Transaction(UUID playerId, UUID targetId, Type type, double amount, String reason, long timestamp) {
        this.playerId = playerId;
        this.targetId = targetId;
        this.type = type;
        this.amount = amount;
        this.reason = reason;
        this.timestamp = timestamp;
    }
    
    /**
     * Obtém o UUID do jogador
     * @return UUID do jogador
     */
    public UUID getPlayerId() {
        return playerId;
    }
    
    /**
     * Obtém o UUID do jogador alvo
     * @return UUID do jogador alvo
     */
    public UUID getTargetId() {
        return targetId;
    }
    
    /**
     * Obtém o tipo da transação
     * @return Tipo da transação
     */
    public Type getType() {
        return type;
    }
    
    /**
     * Obtém o valor da transação
     * @return Valor da transação
     */
    public double getAmount() {
        return amount;
    }
    
    /**
     * Obtém o motivo da transação
     * @return Motivo da transação
     */
    public String getReason() {
        return reason;
    }
    
    /**
     * Obtém o timestamp da transação
     * @return Timestamp da transação
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return "Transaction{" +
                "playerId=" + playerId +
                ", targetId=" + targetId +
                ", type=" + type +
                ", amount=" + amount +
                ", reason='" + reason + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
