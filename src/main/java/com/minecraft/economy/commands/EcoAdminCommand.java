package com.minecraft.economy.commands;

import com.minecraft.economy.core.EconomyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.UUID;

/**
 * Comando administrativo para gerenciar a economia
 */
public class EcoAdminCommand implements CommandExecutor {

    private final EconomyPlugin plugin;
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.00");

    public EcoAdminCommand(EconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("economy.admin")) {
            sender.sendMessage("§cVocê não tem permissão para usar este comando.");
            return true;
        }

        if (args.length < 2) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String playerName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);

        switch (subCommand) {
            case "give":
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /eco give <jogador> <quantia>");
                    return true;
                }
                giveMoney(sender, target, args[2]);
                break;

            case "take":
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /eco take <jogador> <quantia>");
                    return true;
                }
                takeMoney(sender, target, args[2]);
                break;

            case "set":
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /eco set <jogador> <quantia>");
                    return true;
                }
                setMoney(sender, target, args[2]);
                break;

            case "reset":
                resetAccount(sender, target);
                break;

            default:
                showHelp(sender);
                break;
        }

        return true;
    }

    /**
     * Mostra a ajuda do comando
     * @param sender Remetente do comando
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage("§6=== Comandos de Economia ===");
        sender.sendMessage("§f/eco give <jogador> <quantia> §7- Dá dinheiro a um jogador");
        sender.sendMessage("§f/eco take <jogador> <quantia> §7- Remove dinheiro de um jogador");
        sender.sendMessage("§f/eco set <jogador> <quantia> §7- Define o saldo de um jogador");
        sender.sendMessage("§f/eco reset <jogador> §7- Reseta a conta de um jogador");
        sender.sendMessage("§6===========================");
    }

    /**
     * Dá dinheiro a um jogador
     * @param sender Remetente do comando
     * @param target Jogador alvo
     * @param amountStr Quantia a ser dada
     */
    private void giveMoney(CommandSender sender, OfflinePlayer target, String amountStr) {
        // Verifica se a quantia é válida
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                sender.sendMessage("§cA quantia deve ser maior que zero.");
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cQuantia inválida. Use um número válido.");
            return;
        }

        // Verifica se o jogador existe
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            // Cria a conta se não existir
            if (!plugin.getEconomyProvider().createPlayerAccount(target)) {
                sender.sendMessage("§cErro ao criar conta para o jogador.");
                return;
            }
        }

        // Adiciona o dinheiro
        UUID uuid = target.getUniqueId();
        plugin.getMongoDBManager().deposit(uuid, amount, "Comando administrativo").thenAccept(success -> {
            if (success) {
                sender.sendMessage("§aAdicionado §f" + plugin.getEconomyProvider().format(amount) + 
                                  " §aà conta de §f" + target.getName() + "§a.");
                
                // Notifica o jogador, se estiver online
                if (target.isOnline()) {
                    Player targetPlayer = target.getPlayer();
                    targetPlayer.sendMessage("§aVocê recebeu §f" + plugin.getEconomyProvider().format(amount) + 
                                           " §ade um administrador.");
                }
            } else {
                sender.sendMessage("§cErro ao adicionar dinheiro. Tente novamente.");
            }
        });
    }

    /**
     * Remove dinheiro de um jogador
     * @param sender Remetente do comando
     * @param target Jogador alvo
     * @param amountStr Quantia a ser removida
     */
    private void takeMoney(CommandSender sender, OfflinePlayer target, String amountStr) {
        // Verifica se a quantia é válida
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                sender.sendMessage("§cA quantia deve ser maior que zero.");
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cQuantia inválida. Use um número válido.");
            return;
        }

        // Verifica se o jogador existe
        if (!plugin.getEconomyProvider().hasAccount(target)) {
            sender.sendMessage("§cJogador não tem uma conta.");
            return;
        }

        // Verifica se o jogador tem saldo suficiente
        if (!plugin.getEconomyProvider().has(target, amount)) {
            sender.sendMessage("§cO jogador não tem dinheiro suficiente.");
            return;
        }

        // Remove o dinheiro
        UUID uuid = target.getUniqueId();
        plugin.getMongoDBManager().withdraw(uuid, amount, "Comando administrativo").thenAccept(success -> {
            if (success) {
                sender.sendMessage("§aRemovido §f" + plugin.getEconomyProvider().format(amount) + 
                                  " §ada conta de §f" + target.getName() + "§a.");
                
                // Notifica o jogador, se estiver online
                if (target.isOnline()) {
                    Player targetPlayer = target.getPlayer();
                    targetPlayer.sendMessage("§cUm administrador removeu §f" + plugin.getEconomyProvider().format(amount) + 
                                           " §cda sua conta.");
                }
            } else {
                sender.sendMessage("§cErro ao remover dinheiro. Tente novamente.");
            }
        });
    }

    /**
     * Define o saldo de um jogador
     * @param sender Remetente do comando
     * @param target Jogador alvo
     * @param amountStr Novo saldo
     */
    private void setMoney(CommandSender sender, OfflinePlayer target, String amountStr) {
        // Verifica se a quantia é válida
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount < 0) {
                sender.sendMessage("§cA quantia não pode ser negativa.");
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cQuantia inválida. Use um número válido.");
            return;
        }

        // Verifica se o jogador existe
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            // Cria a conta se não existir
            if (!plugin.getEconomyProvider().createPlayerAccount(target)) {
                sender.sendMessage("§cErro ao criar conta para o jogador.");
                return;
            }
        }

        // Define o saldo
        UUID uuid = target.getUniqueId();
        
        // Primeiro obtém o saldo atual
        plugin.getMongoDBManager().getBalance(uuid).thenAccept(currentBalance -> {
            if (currentBalance > amount) {
                // Se o novo saldo for menor, retira a diferença
                double difference = currentBalance - amount;
                plugin.getMongoDBManager().withdraw(uuid, difference, "Comando administrativo").join();
            } else if (currentBalance < amount) {
                // Se o novo saldo for maior, adiciona a diferença
                double difference = amount - currentBalance;
                plugin.getMongoDBManager().deposit(uuid, difference, "Comando administrativo").join();
            }
            
            sender.sendMessage("§aSaldo de §f" + target.getName() + " §adefinido para §f" + 
                              plugin.getEconomyProvider().format(amount) + "§a.");
            
            // Notifica o jogador, se estiver online
            if (target.isOnline()) {
                Player targetPlayer = target.getPlayer();
                targetPlayer.sendMessage("§aSeu saldo foi definido para §f" + 
                                       plugin.getEconomyProvider().format(amount) + 
                                       " §apor um administrador.");
            }
        });
    }

    /**
     * Reseta a conta de um jogador
     * @param sender Remetente do comando
     * @param target Jogador alvo
     */
    private void resetAccount(CommandSender sender, OfflinePlayer target) {
        // Verifica se o jogador existe
        if (!plugin.getEconomyProvider().hasAccount(target)) {
            sender.sendMessage("§cJogador não tem uma conta.");
            return;
        }

        // Reseta a conta
        UUID uuid = target.getUniqueId();
        double initialBalance = plugin.getConfig().getDouble("economy.starting_balance", 1000.0);
        
        // Primeiro obtém o saldo atual
        plugin.getMongoDBManager().getBalance(uuid).thenAccept(currentBalance -> {
            if (currentBalance > initialBalance) {
                // Se o saldo atual for maior, retira a diferença
                double difference = currentBalance - initialBalance;
                plugin.getMongoDBManager().withdraw(uuid, difference, "Reset de conta").join();
            } else if (currentBalance < initialBalance) {
                // Se o saldo atual for menor, adiciona a diferença
                double difference = initialBalance - currentBalance;
                plugin.getMongoDBManager().deposit(uuid, difference, "Reset de conta").join();
            }
            
            sender.sendMessage("§aConta de §f" + target.getName() + " §aresetada para §f" + 
                              plugin.getEconomyProvider().format(initialBalance) + "§a.");
            
            // Notifica o jogador, se estiver online
            if (target.isOnline()) {
                Player targetPlayer = target.getPlayer();
                targetPlayer.sendMessage("§aSua conta foi resetada para §f" + 
                                       plugin.getEconomyProvider().format(initialBalance) + 
                                       " §apor um administrador.");
            }
        });
    }
}
