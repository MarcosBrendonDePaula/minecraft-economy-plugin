package com.minecraft.economy.service;

import com.minecraft.economy.api.service.ConfigurationService;
import com.minecraft.economy.api.service.EconomyService;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adaptador para a API Vault
 */
public class VaultEconomyAdapter implements Economy {

    private final EconomyService economyService;
    private final ConfigurationService configService;
    private final Logger logger;
    private final long timeout;
    private final TimeUnit timeoutUnit;
    
    /**
     * Construtor
     * @param economyService Serviço de economia
     * @param configService Serviço de configuração
     * @param logger Logger
     */
    public VaultEconomyAdapter(
            EconomyService economyService,
            ConfigurationService configService,
            Logger logger) {
        this(economyService, configService, logger, 500, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Construtor com timeout
     * @param economyService Serviço de economia
     * @param configService Serviço de configuração
     * @param logger Logger
     * @param timeout Timeout para operações síncronas
     * @param timeoutUnit Unidade de tempo para timeout
     */
    public VaultEconomyAdapter(
            EconomyService economyService,
            ConfigurationService configService,
            Logger logger,
            long timeout,
            TimeUnit timeoutUnit) {
        this.economyService = economyService;
        this.configService = configService;
        this.logger = logger;
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "EconomyPlugin";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double amount) {
        return String.format("%.2f", amount);
    }

    @Override
    public String currencyNamePlural() {
        return configService.getCurrencyNamePlural();
    }

    @Override
    public String currencyNameSingular() {
        return configService.getCurrencyName();
    }

    @Override
    public boolean hasAccount(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            return false;
        }
        return hasAccount(player.getUniqueId());
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return hasAccount(player.getUniqueId());
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }
    
    /**
     * Verifica se um jogador tem conta
     * @param playerId UUID do jogador
     * @return true se o jogador tem conta, false caso contrário
     */
    private boolean hasAccount(UUID playerId) {
        try {
            return economyService.hasAccount(playerId).get(timeout, timeoutUnit);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.log(Level.WARNING, "Erro ao verificar conta: " + e.getMessage(), e);
            return true; // Assume que tem conta para evitar problemas
        }
    }

    @Override
    public double getBalance(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            return 0.0;
        }
        return getBalance(player.getUniqueId());
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return getBalance(player.getUniqueId());
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }
    
    /**
     * Obtém o saldo de um jogador
     * @param playerId UUID do jogador
     * @return Saldo do jogador
     */
    private double getBalance(UUID playerId) {
        try {
            return economyService.getBalance(playerId).get(timeout, timeoutUnit);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.log(Level.WARNING, "Erro ao obter saldo: " + e.getMessage(), e);
            return configService.getInitialBalance();
        }
    }

    @Override
    public boolean has(String playerName, double amount) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            return false;
        }
        return has(player.getUniqueId(), amount);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return has(player.getUniqueId(), amount);
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }
    
    /**
     * Verifica se um jogador tem saldo suficiente
     * @param playerId UUID do jogador
     * @param amount Valor a verificar
     * @return true se o jogador tem saldo suficiente, false caso contrário
     */
    private boolean has(UUID playerId, double amount) {
        try {
            return economyService.hasBalance(playerId, amount).get(timeout, timeoutUnit);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.log(Level.WARNING, "Erro ao verificar saldo: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Jogador não encontrado");
        }
        return withdrawPlayer(player.getUniqueId(), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        return withdrawPlayer(player.getUniqueId(), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }
    
    /**
     * Retira dinheiro da conta de um jogador
     * @param playerId UUID do jogador
     * @param amount Valor a retirar
     * @return Resposta da operação
     */
    private EconomyResponse withdrawPlayer(UUID playerId, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(playerId), EconomyResponse.ResponseType.FAILURE, "Não é possível retirar um valor negativo");
        }
        
        if (!has(playerId, amount)) {
            return new EconomyResponse(0, getBalance(playerId), EconomyResponse.ResponseType.FAILURE, "Saldo insuficiente");
        }
        
        try {
            boolean success = economyService.withdraw(playerId, amount, "Vault API").get(timeout, timeoutUnit);
            
            if (success) {
                double newBalance = getBalance(playerId);
                return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, null);
            } else {
                return new EconomyResponse(0, getBalance(playerId), EconomyResponse.ResponseType.FAILURE, "Erro ao retirar dinheiro");
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.log(Level.WARNING, "Erro ao retirar dinheiro: " + e.getMessage(), e);
            return new EconomyResponse(0, getBalance(playerId), EconomyResponse.ResponseType.FAILURE, "Erro ao retirar dinheiro: " + e.getMessage());
        }
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Jogador não encontrado");
        }
        return depositPlayer(player.getUniqueId(), amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        return depositPlayer(player.getUniqueId(), amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }
    
    /**
     * Deposita dinheiro na conta de um jogador
     * @param playerId UUID do jogador
     * @param amount Valor a depositar
     * @return Resposta da operação
     */
    private EconomyResponse depositPlayer(UUID playerId, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(playerId), EconomyResponse.ResponseType.FAILURE, "Não é possível depositar um valor negativo");
        }
        
        try {
            boolean success = economyService.deposit(playerId, amount, "Vault API").get(timeout, timeoutUnit);
            
            if (success) {
                double newBalance = getBalance(playerId);
                return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, null);
            } else {
                return new EconomyResponse(0, getBalance(playerId), EconomyResponse.ResponseType.FAILURE, "Erro ao depositar dinheiro");
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.log(Level.WARNING, "Erro ao depositar dinheiro: " + e.getMessage(), e);
            return new EconomyResponse(0, getBalance(playerId), EconomyResponse.ResponseType.FAILURE, "Erro ao depositar dinheiro: " + e.getMessage());
        }
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos não são suportados");
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos não são suportados");
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos não são suportados");
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos não são suportados");
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos não são suportados");
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos não são suportados");
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos não são suportados");
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos não são suportados");
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos não são suportados");
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos não são suportados");
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos não são suportados");
    }

    @Override
    public List<String> getBanks() {
        return new ArrayList<>();
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            return false;
        }
        return createPlayerAccount(player.getUniqueId(), playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return createPlayerAccount(player.getUniqueId(), player.getName());
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }
    
    /**
     * Cria uma conta para um jogador
     * @param playerId UUID do jogador
     * @param playerName Nome do jogador
     * @return true se a conta foi criada com sucesso, false caso contrário
     */
    private boolean createPlayerAccount(UUID playerId, String playerName) {
        if (hasAccount(playerId)) {
            return true;
        }
        
        try {
            return economyService.createAccount(playerId, playerName).get(timeout, timeoutUnit);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.log(Level.WARNING, "Erro ao criar conta: " + e.getMessage(), e);
            return false;
        }
    }
}
