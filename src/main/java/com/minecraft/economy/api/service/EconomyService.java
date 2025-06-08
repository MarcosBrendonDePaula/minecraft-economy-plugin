package com.minecraft.economy.api.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface para serviços de economia
 * Define operações básicas para gerenciamento de economia
 */
public interface EconomyService {
    
    /**
     * Obtém o saldo de um jogador
     * @param playerId UUID do jogador
     * @return CompletableFuture com o saldo do jogador
     */
    CompletableFuture<Double> getBalance(UUID playerId);
    
    /**
     * Verifica se um jogador tem saldo suficiente
     * @param playerId UUID do jogador
     * @param amount Valor a verificar
     * @return CompletableFuture com o resultado da verificação
     */
    CompletableFuture<Boolean> hasBalance(UUID playerId, double amount);
    
    /**
     * Deposita dinheiro na conta de um jogador
     * @param playerId UUID do jogador
     * @param amount Valor a depositar
     * @param reason Motivo da transação
     * @return CompletableFuture com o resultado da operação
     */
    CompletableFuture<Boolean> deposit(UUID playerId, double amount, String reason);
    
    /**
     * Retira dinheiro da conta de um jogador
     * @param playerId UUID do jogador
     * @param amount Valor a retirar
     * @param reason Motivo da transação
     * @return CompletableFuture com o resultado da operação
     */
    CompletableFuture<Boolean> withdraw(UUID playerId, double amount, String reason);
    
    /**
     * Transfere dinheiro entre jogadores
     * @param fromId UUID do jogador de origem
     * @param toId UUID do jogador de destino
     * @param amount Valor a transferir
     * @param reason Motivo da transferência
     * @return CompletableFuture com o resultado da operação
     */
    CompletableFuture<Boolean> transfer(UUID fromId, UUID toId, double amount, String reason);
    
    /**
     * Verifica se um jogador tem conta
     * @param playerId UUID do jogador
     * @return CompletableFuture com o resultado da verificação
     */
    CompletableFuture<Boolean> hasAccount(UUID playerId);
    
    /**
     * Cria uma conta para um jogador
     * @param playerId UUID do jogador
     * @param playerName Nome do jogador
     * @return CompletableFuture com o resultado da operação
     */
    CompletableFuture<Boolean> createAccount(UUID playerId, String playerName);
}
