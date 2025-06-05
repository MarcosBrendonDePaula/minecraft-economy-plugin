package com.minecraft.economy.commands;

import com.minecraft.economy.core.EconomyPlugin;
import com.minecraft.economy.lottery.LotteryTicket;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Comandos relacionados à loteria
 */
public class LotteryCommand implements CommandExecutor {

    private final EconomyPlugin plugin;
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.00");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public LotteryCommand(EconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Executa IMEDIATAMENTE em uma nova thread para evitar qualquer bloqueio do thread principal
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    processCommand(sender, args);
                } catch (Exception e) {
                    plugin.getLogger().severe("Erro ao processar comando de loteria: " + e.getMessage());
                    e.printStackTrace();
                    
                    // Volta para o thread principal para enviar mensagem de erro
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            sender.sendMessage("§cOcorreu um erro ao processar o comando. Tente novamente mais tarde.");
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return true;
    }
    
    /**
     * Processa o comando de loteria
     */
    private void processCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            showLotteryInfo(sender);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "buy":
                if (!(sender instanceof Player)) {
                    sendMessage(sender, "§cApenas jogadores podem comprar bilhetes de loteria.");
                    return;
                }
                buyTicket((Player) sender);
                break;
            case "tickets":
                if (!(sender instanceof Player)) {
                    sendMessage(sender, "§cApenas jogadores podem ver seus bilhetes.");
                    return;
                }
                showTickets((Player) sender);
                break;
            case "draw":
                if (!sender.hasPermission("economy.lottery.admin")) {
                    sendMessage(sender, "§cVocê não tem permissão para realizar sorteios.");
                    return;
                }
                drawLottery(sender);
                break;
            case "setprice":
                if (!sender.hasPermission("economy.lottery.admin")) {
                    sendMessage(sender, "§cVocê não tem permissão para alterar o preço dos bilhetes.");
                    return;
                }
                if (args.length < 2) {
                    sendMessage(sender, "§cUso correto: /lottery setprice <valor>");
                    return;
                }
                try {
                    double price = Double.parseDouble(args[1]);
                    setTicketPrice(sender, price);
                } catch (NumberFormatException e) {
                    sendMessage(sender, "§cValor inválido. Use um número válido.");
                }
                break;
            case "settype":
                if (!sender.hasPermission("economy.lottery.admin")) {
                    sendMessage(sender, "§cVocê não tem permissão para alterar o tipo da loteria.");
                    return;
                }
                if (args.length < 2) {
                    sendMessage(sender, "§cUso correto: /lottery settype <daily|weekly|hourly>");
                    return;
                }
                setLotteryType(sender, args[1]);
                break;
            case "enable":
                if (!sender.hasPermission("economy.lottery.admin")) {
                    sendMessage(sender, "§cVocê não tem permissão para ativar a loteria.");
                    return;
                }
                setLotteryActive(sender, true);
                break;
            case "disable":
                if (!sender.hasPermission("economy.lottery.admin")) {
                    sendMessage(sender, "§cVocê não tem permissão para desativar a loteria.");
                    return;
                }
                setLotteryActive(sender, false);
                break;
            case "help":
            default:
                showHelp(sender);
                break;
        }
    }

    /**
     * Mostra informações da loteria
     */
    private void showLotteryInfo(CommandSender sender) {
        // Obtém informações da loteria de forma assíncrona
        CompletableFuture<Double> jackpotFuture = CompletableFuture.supplyAsync(() -> plugin.getLotteryManager().getJackpot());
        CompletableFuture<Double> ticketPriceFuture = CompletableFuture.supplyAsync(() -> plugin.getLotteryManager().getTicketPrice());
        CompletableFuture<Long> nextDrawTimeFuture = CompletableFuture.supplyAsync(() -> plugin.getLotteryManager().getNextDrawTime());
        CompletableFuture<Boolean> activeFuture = CompletableFuture.supplyAsync(() -> plugin.getLotteryManager().isLotteryActive());
        CompletableFuture<Integer> totalTicketsFuture = CompletableFuture.supplyAsync(() -> plugin.getLotteryManager().countTotalTickets());
        
        // Combina todos os futuros para evitar bloqueios
        CompletableFuture.allOf(jackpotFuture, ticketPriceFuture, nextDrawTimeFuture, activeFuture, totalTicketsFuture)
            .thenAccept(v -> {
                double jackpot = jackpotFuture.join();
                double ticketPrice = ticketPriceFuture.join();
                long nextDrawTime = nextDrawTimeFuture.join();
                boolean active = activeFuture.join();
                int totalTickets = totalTicketsFuture.join();
                
                sendMessage(sender, "§a§l=== LOTERIA DO SERVIDOR ===");
                sendMessage(sender, "§aStatus: " + (active ? "§aAtiva" : "§cDesativada"));
                sendMessage(sender, "§aPrêmio atual: §f" + PRICE_FORMAT.format(jackpot));
                sendMessage(sender, "§aPreço do bilhete: §f" + PRICE_FORMAT.format(ticketPrice));
                sendMessage(sender, "§aPróximo sorteio: §f" + DATE_FORMAT.format(new Date(nextDrawTime)));
                sendMessage(sender, "§aBilhetes vendidos: §f" + totalTickets);
                sendMessage(sender, "§aUse §f/lottery buy §apara comprar um bilhete.");
                sendMessage(sender, "§aUse §f/lottery tickets §apara ver seus bilhetes.");
            });
    }

    /**
     * Compra um bilhete de loteria
     */
    private void buyTicket(Player player) {
        // Verifica se a loteria está ativa de forma assíncrona
        CompletableFuture.supplyAsync(() -> plugin.getLotteryManager().isLotteryActive())
            .thenAccept(active -> {
                if (!active) {
                    sendMessage(player, "§cA loteria está temporariamente desativada.");
                    return;
                }
                
                // Inicia o processo de compra de forma totalmente assíncrona
                plugin.getLotteryManager().buyTicket(player)
                    .exceptionally(ex -> {
                        plugin.getLogger().severe("Erro ao comprar bilhete: " + ex.getMessage());
                        sendMessage(player, "§cOcorreu um erro ao comprar o bilhete. Tente novamente mais tarde.");
                        return -1;
                    });
            });
    }

    /**
     * Mostra os bilhetes de um jogador
     */
    private void showTickets(Player player) {
        // Obtém os bilhetes de forma assíncrona
        CompletableFuture.supplyAsync(() -> plugin.getLotteryManager().getPlayerTickets(player.getUniqueId()))
            .thenAccept(tickets -> {
                if (tickets.isEmpty()) {
                    sendMessage(player, "§cVocê não possui bilhetes de loteria.");
                    return;
                }
                
                sendMessage(player, "§a§l=== SEUS BILHETES DE LOTERIA ===");
                for (int i = 0; i < tickets.size(); i++) {
                    LotteryTicket ticket = tickets.get(i);
                    sendMessage(player, "§aBilhete #" + (i + 1) + ": §f" + ticket.getTicketNumber());
                }
                
                // Obtém informações adicionais de forma assíncrona
                CompletableFuture<Double> jackpotFuture = CompletableFuture.supplyAsync(() -> plugin.getLotteryManager().getJackpot());
                CompletableFuture<Long> nextDrawTimeFuture = CompletableFuture.supplyAsync(() -> plugin.getLotteryManager().getNextDrawTime());
                
                CompletableFuture.allOf(jackpotFuture, nextDrawTimeFuture)
                    .thenAccept(v -> {
                        sendMessage(player, "§aPrêmio atual: §f" + PRICE_FORMAT.format(jackpotFuture.join()));
                        sendMessage(player, "§aPróximo sorteio: §f" + DATE_FORMAT.format(new Date(nextDrawTimeFuture.join())));
                    });
            });
    }

    /**
     * Realiza o sorteio da loteria
     */
    private void drawLottery(CommandSender sender) {
        sendMessage(sender, "§aIniciando sorteio da loteria...");
        
        plugin.getLotteryManager().drawLottery()
            .exceptionally(ex -> {
                plugin.getLogger().severe("Erro ao realizar sorteio: " + ex.getMessage());
                sendMessage(sender, "§cOcorreu um erro ao realizar o sorteio. Verifique o console para mais detalhes.");
                return null;
            })
            .thenAccept(winnerId -> {
                if (winnerId == null) {
                    sendMessage(sender, "§cNão foi possível realizar o sorteio. Verifique o console para mais detalhes.");
                    return;
                }
                
                // Mensagens de sucesso já são enviadas no método drawLottery
                sendMessage(sender, "§aSorteio realizado com sucesso!");
            });
    }

    /**
     * Define o preço do bilhete
     */
    private void setTicketPrice(CommandSender sender, double price) {
        if (price <= 0) {
            sendMessage(sender, "§cO preço do bilhete deve ser maior que zero.");
            return;
        }
        
        // Define o preço de forma assíncrona
        CompletableFuture.runAsync(() -> {
            plugin.getLotteryManager().setTicketPrice(price);
            sendMessage(sender, "§aPreço do bilhete definido para §f" + PRICE_FORMAT.format(price) + "§a.");
        });
    }

    /**
     * Define o tipo da loteria
     */
    private void setLotteryType(CommandSender sender, String type) {
        type = type.toLowerCase();
        
        if (!type.equals("daily") && !type.equals("weekly") && !type.equals("hourly")) {
            sendMessage(sender, "§cTipo de loteria inválido. Use daily, weekly ou hourly.");
            return;
        }
        
        // Define o tipo de forma assíncrona
        final String finalType = type;
        CompletableFuture.runAsync(() -> {
            plugin.getLotteryManager().setLotteryType(finalType);
            sendMessage(sender, "§aTipo de loteria definido para §f" + finalType + "§a.");
            
            // Traduz o tipo para português na mensagem
            String tipoTraduzido;
            switch (finalType) {
                case "daily":
                    tipoTraduzido = "diária";
                    break;
                case "weekly":
                    tipoTraduzido = "semanal";
                    break;
                case "hourly":
                    tipoTraduzido = "horária";
                    break;
                default:
                    tipoTraduzido = finalType;
            }
            
            sendMessage(sender, "§aPróximo sorteio agendado. Loteria agora é §f" + tipoTraduzido + "§a.");
        });
    }

    /**
     * Ativa ou desativa a loteria
     */
    private void setLotteryActive(CommandSender sender, boolean active) {
        // Ativa/desativa de forma assíncrona
        CompletableFuture.runAsync(() -> {
            plugin.getLotteryManager().setLotteryActive(active);
            
            if (active) {
                sendMessage(sender, "§aLoteria ativada com sucesso!");
            } else {
                sendMessage(sender, "§aLoteria desativada com sucesso!");
            }
        });
    }

    /**
     * Mostra a ajuda do comando
     */
    private void showHelp(CommandSender sender) {
        sendMessage(sender, "§a§l=== COMANDOS DA LOTERIA ===");
        sendMessage(sender, "§a/lottery §7- Mostra informações da loteria");
        sendMessage(sender, "§a/lottery buy §7- Compra um bilhete de loteria");
        sendMessage(sender, "§a/lottery tickets §7- Mostra seus bilhetes");
        
        if (sender.hasPermission("economy.lottery.admin")) {
            sendMessage(sender, "§a§l=== COMANDOS ADMINISTRATIVOS ===");
            sendMessage(sender, "§a/lottery draw §7- Realiza o sorteio");
            sendMessage(sender, "§a/lottery setprice <valor> §7- Define o preço do bilhete");
            sendMessage(sender, "§a/lottery settype <daily|weekly|hourly> §7- Define o tipo da loteria");
            sendMessage(sender, "§a/lottery enable §7- Ativa a loteria");
            sendMessage(sender, "§a/lottery disable §7- Desativa a loteria");
        }
    }
    
    /**
     * Envia uma mensagem para o sender de forma segura (no thread principal)
     */
    private void sendMessage(CommandSender sender, String message) {
        new BukkitRunnable() {
            @Override
            public void run() {
                sender.sendMessage(message);
            }
        }.runTask(plugin);
    }
}
