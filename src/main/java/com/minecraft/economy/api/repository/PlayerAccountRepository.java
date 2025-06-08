package com.minecraft.economy.api.repository;

import com.minecraft.economy.api.model.PlayerAccount;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface para repositório de contas de jogadores
 * Define operações para acesso e manipulação de contas
 */
public interface PlayerAccountRepository {
    
    /**
     * Obtém o saldo de um jogador
     * @param playerId UUID do jogador
     * @return CompletableFuture com o saldo do jogador
     */
    CompletableFuture<Double> getBalance(UUID playerId);
    
    /**
     * Atualiza o saldo de um jogador
     * @param playerId UUID do jogador
     * @param newBalance Novo saldo
     * @return CompletableFuture com o resultado da operação
     */
    CompletableFuture<Boolean> updateBalance(UUID playerId, double newBalance);
    
    /**
     * Verifica se um jogador tem conta
     * @param playerId UUID do jogador
     * @return CompletableFuture com o resultado da verificação
     */
    CompletableFuture<Boolean> accountExists(UUID playerId);
    
    /**
     * Cria uma conta para um jogador
     * @param playerId UUID do jogador
     * @param playerName Nome do jogador
     * @param initialBalance Saldo inicial
     * @return CompletableFuture com o resultado da operação
     */
    CompletableFuture<Boolean> createAccount(UUID playerId, String playerName, double initialBalance);
    
    /**
     * Obtém os jogadores com mais dinheiro
     * @param limit Limite de jogadores a retornar
     * @return CompletableFuture com a lista de jogadores e seus saldos
     */
    CompletableFuture<List<PlayerAccount>> getTopPlayers(int limit);
    
    /**
     * Atualiza a última atividade de um jogador
     * @param playerId UUID do jogador
     * @return CompletableFuture com o resultado da operação
     */
    CompletableFuture<Boolean> updateLastActivity(UUID playerId);
    
    /**
     * Obtém a conta de um jogador
     * @param playerId UUID do jogador
     * @return CompletableFuture com a conta do jogador
     */
    CompletableFuture<PlayerAccount> getPlayerAccount(UUID playerId);
}
