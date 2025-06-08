package com.minecraft.economy.service;

import com.minecraft.economy.api.infrastructure.AsyncExecutor;
import com.minecraft.economy.api.model.Transaction;
import com.minecraft.economy.api.repository.PlayerAccountRepository;
import com.minecraft.economy.api.repository.TransactionRepository;
import com.minecraft.economy.api.service.ConfigurationService;
import com.minecraft.economy.api.service.EconomyService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementação padrão de EconomyService
 */
public class DefaultEconomyService implements EconomyService {

    private final PlayerAccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ConfigurationService configService;
    private final AsyncExecutor asyncExecutor;
    private final Logger logger;
    
    /**
     * Construtor
     * @param accountRepository Repositório de contas
     * @param transactionRepository Repositório de transações
     * @param configService Serviço de configuração
     * @param asyncExecutor Executor assíncrono
     * @param logger Logger
     */
    public DefaultEconomyService(
            PlayerAccountRepository accountRepository,
            TransactionRepository transactionRepository,
            ConfigurationService configService,
            AsyncExecutor asyncExecutor,
            Logger logger) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.configService = configService;
        this.asyncExecutor = asyncExecutor;
        this.logger = logger;
    }

    @Override
    public CompletableFuture<Double> getBalance(UUID playerId) {
        return accountRepository.getBalance(playerId);
    }

    @Override
    public CompletableFuture<Boolean> hasBalance(UUID playerId, double amount) {
        return accountRepository.getBalance(playerId)
                .thenApply(balance -> balance >= amount);
    }

    @Override
    public CompletableFuture<Boolean> deposit(UUID playerId, double amount, String reason) {
        if (amount <= 0) {
            logger.warning("Tentativa de depósito com valor não positivo: " + amount);
            return CompletableFuture.completedFuture(false);
        }
        
        return accountRepository.getBalance(playerId)
                .thenCompose(currentBalance -> {
                    double newBalance = currentBalance + amount;
                    
                    // Atualiza o saldo
                    return accountRepository.updateBalance(playerId, newBalance)
                            .thenCompose(success -> {
                                if (!success) {
                                    logger.warning("Falha ao atualizar saldo para depósito: " + playerId);
                                    return CompletableFuture.completedFuture(false);
                                }
                                
                                // Registra a transação
                                Transaction transaction = new Transaction(
                                        playerId,
                                        Transaction.Type.DEPOSIT,
                                        amount,
                                        reason
                                );
                                
                                return transactionRepository.recordTransaction(transaction);
                            });
                });
    }

    @Override
    public CompletableFuture<Boolean> withdraw(UUID playerId, double amount, String reason) {
        if (amount <= 0) {
            logger.warning("Tentativa de saque com valor não positivo: " + amount);
            return CompletableFuture.completedFuture(false);
        }
        
        return accountRepository.getBalance(playerId)
                .thenCompose(currentBalance -> {
                    if (currentBalance < amount) {
                        logger.fine("Saldo insuficiente para saque: " + playerId + ", saldo: " + currentBalance + ", valor: " + amount);
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    double newBalance = currentBalance - amount;
                    
                    // Atualiza o saldo
                    return accountRepository.updateBalance(playerId, newBalance)
                            .thenCompose(success -> {
                                if (!success) {
                                    logger.warning("Falha ao atualizar saldo para saque: " + playerId);
                                    return CompletableFuture.completedFuture(false);
                                }
                                
                                // Registra a transação
                                Transaction transaction = new Transaction(
                                        playerId,
                                        Transaction.Type.WITHDRAW,
                                        amount,
                                        reason
                                );
                                
                                return transactionRepository.recordTransaction(transaction);
                            });
                });
    }

    @Override
    public CompletableFuture<Boolean> transfer(UUID fromId, UUID toId, double amount, String reason) {
        if (fromId.equals(toId)) {
            logger.warning("Tentativa de transferência para si mesmo: " + fromId);
            return CompletableFuture.completedFuture(false);
        }
        
        if (amount <= 0) {
            logger.warning("Tentativa de transferência com valor não positivo: " + amount);
            return CompletableFuture.completedFuture(false);
        }
        
        // Calcula a taxa de transação
        double taxRate = configService.getTransactionTaxRate();
        double taxAmount = amount * taxRate;
        double totalAmount = amount + taxAmount;
        
        return accountRepository.getBalance(fromId)
                .thenCompose(fromBalance -> {
                    if (fromBalance < totalAmount) {
                        logger.fine("Saldo insuficiente para transferência: " + fromId + ", saldo: " + fromBalance + ", valor total: " + totalAmount);
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    // Retira o dinheiro do remetente
                    return withdraw(fromId, totalAmount, "Transferência para " + toId + ": " + reason)
                            .thenCompose(withdrawSuccess -> {
                                if (!withdrawSuccess) {
                                    logger.warning("Falha ao retirar dinheiro para transferência: " + fromId);
                                    return CompletableFuture.completedFuture(false);
                                }
                                
                                // Deposita o dinheiro no destinatário
                                return deposit(toId, amount, "Transferência de " + fromId + ": " + reason)
                                        .thenCompose(depositSuccess -> {
                                            if (!depositSuccess) {
                                                logger.warning("Falha ao depositar dinheiro para transferência: " + toId);
                                                
                                                // Devolve o dinheiro ao remetente
                                                return deposit(fromId, totalAmount, "Devolução de transferência falha para " + toId)
                                                        .thenApply(refundSuccess -> {
                                                            if (!refundSuccess) {
                                                                logger.severe("Falha ao devolver dinheiro após transferência falha: " + fromId);
                                                            }
                                                            return false;
                                                        });
                                            }
                                            
                                            // Registra a transação de transferência
                                            Transaction transaction = new Transaction(
                                                    fromId,
                                                    toId,
                                                    Transaction.Type.TRANSFER,
                                                    amount,
                                                    reason
                                            );
                                            
                                            return transactionRepository.recordTransaction(transaction)
                                                    .thenCompose(transactionSuccess -> {
                                                        if (!transactionSuccess) {
                                                            logger.warning("Falha ao registrar transação de transferência");
                                                        }
                                                        
                                                        // Registra a taxa
                                                        if (taxAmount > 0) {
                                                            Transaction taxTransaction = new Transaction(
                                                                    fromId,
                                                                    Transaction.Type.TAX,
                                                                    taxAmount,
                                                                    "Taxa de transferência"
                                                            );
                                                            
                                                            return transactionRepository.recordTransaction(taxTransaction)
                                                                    .thenApply(taxSuccess -> {
                                                                        if (!taxSuccess) {
                                                                            logger.warning("Falha ao registrar transação de taxa");
                                                                        }
                                                                        
                                                                        // Atualiza o contador de taxas coletadas
                                                                        updateTaxCollected(taxAmount);
                                                                        
                                                                        return true;
                                                                    });
                                                        }
                                                        
                                                        return CompletableFuture.completedFuture(true);
                                                    });
                                        });
                            });
                });
    }
    
    /**
     * Atualiza o contador de taxas coletadas
     * @param taxAmount Valor da taxa
     */
    private void updateTaxCollected(double taxAmount) {
        asyncExecutor.runAsync(() -> {
            try {
                configService.getConfigAsync("tax_collected", 0.0)
                        .thenAccept(currentTaxes -> {
                            double newTotal = currentTaxes + taxAmount;
                            configService.setConfig("tax_collected", newTotal);
                        });
            } catch (Exception e) {
                logger.log(Level.WARNING, "Erro ao atualizar taxas coletadas: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> hasAccount(UUID playerId) {
        return accountRepository.accountExists(playerId);
    }

    @Override
    public CompletableFuture<Boolean> createAccount(UUID playerId, String playerName) {
        double initialBalance = configService.getInitialBalance();
        return accountRepository.createAccount(playerId, playerName, initialBalance);
    }
}
