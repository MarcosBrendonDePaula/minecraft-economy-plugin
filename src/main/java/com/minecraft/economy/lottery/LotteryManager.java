package com.minecraft.economy.lottery;

import com.minecraft.economy.core.EconomyPlugin;
import com.minecraft.economy.database.ConfigDatabase;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bson.Document;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Gerenciador do sistema de loteria
 */
public class LotteryManager {

    private final EconomyPlugin plugin;
    private final ConfigDatabase configDB;
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.00");
    
    // Cache de bilhetes comprados
    private final Map<UUID, List<LotteryTicket>> playerTickets = new HashMap<>();
    
    // Configurações da loteria
    private double ticketPrice;
    private double jackpot;
    private long nextDrawTime;
    private String lotteryType;
    private boolean lotteryActive;

    public LotteryManager(EconomyPlugin plugin) {
        this.plugin = plugin;
        this.configDB = plugin.getConfigDatabase();
        loadLotteryConfig();
    }
    
    /**
     * Carrega as configurações da loteria do banco de dados
     */
    public void loadLotteryConfig() {
        ticketPrice = configDB.getDouble("lottery.ticket_price", 100.0);
        jackpot = configDB.getDouble("lottery.jackpot", 1000.0);
        nextDrawTime = configDB.getLong("lottery.next_draw", System.currentTimeMillis() + 86400000); // Padrão: 24h
        lotteryType = configDB.getString("lottery.type", "daily");
        lotteryActive = configDB.getBoolean("lottery.active", true);
        
        // Carrega bilhetes existentes
        loadTickets();
        
        plugin.getLogger().info("Configurações da loteria carregadas com sucesso!");
        plugin.getLogger().info("Próximo sorteio: " + new Date(nextDrawTime));
        plugin.getLogger().info("Prêmio atual: " + PRICE_FORMAT.format(jackpot));
    }
    
    /**
     * Carrega os bilhetes existentes do banco de dados
     */
    private void loadTickets() {
        playerTickets.clear();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    plugin.getMongoDBManager().getDatabase().getCollection("lottery_tickets")
                        .find()
                        .forEach(doc -> {
                            UUID playerId = UUID.fromString(doc.getString("player_uuid"));
                            int ticketNumber = doc.getInteger("ticket_number");
                            long purchaseTime = doc.getLong("purchase_time");
                            String drawType = doc.getString("draw_type");
                            
                            LotteryTicket ticket = new LotteryTicket(playerId, ticketNumber, purchaseTime, drawType);
                            
                            playerTickets.computeIfAbsent(playerId, k -> new ArrayList<>()).add(ticket);
                        });
                    
                    plugin.getLogger().info("Carregados " + countTotalTickets() + " bilhetes de loteria.");
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Erro ao carregar bilhetes de loteria: " + e.getMessage(), e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }
    
    /**
     * Conta o total de bilhetes vendidos
     * @return Número total de bilhetes
     */
    public int countTotalTickets() {
        return playerTickets.values().stream().mapToInt(List::size).sum();
    }
    
    /**
     * Compra um bilhete de loteria para um jogador
     * @param player Jogador que está comprando
     * @return CompletableFuture com o resultado da compra (número do bilhete ou -1 se falhou)
     */
    public CompletableFuture<Integer> buyTicket(Player player) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        
        // Executa toda a lógica de compra em uma nova thread dedicada
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // Verifica se a loteria está ativa
                    if (!lotteryActive) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                player.sendMessage("§cA loteria está temporariamente desativada.");
                            }
                        }.runTask(plugin);
                        future.complete(-1);
                        return;
                    }
                    
                    // Verifica se o jogador tem dinheiro suficiente (no thread principal)
                    final CompletableFuture<Boolean> hasMoneyCF = new CompletableFuture<>();
                    
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            boolean hasMoney = plugin.getEconomyProvider().has(player, ticketPrice);
                            hasMoneyCF.complete(hasMoney);
                        }
                    }.runTask(plugin);
                    
                    // Aguarda o resultado de forma assíncrona
                    hasMoneyCF.thenAccept(hasMoney -> {
                        if (!hasMoney) {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.sendMessage("§cVocê não tem dinheiro suficiente para comprar um bilhete de loteria.");
                                    player.sendMessage("§cPreço do bilhete: §f" + PRICE_FORMAT.format(ticketPrice));
                                }
                            }.runTask(plugin);
                            future.complete(-1);
                            return;
                        }
                        
                        // Continua o processamento de forma assíncrona
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                try {
                                    // Verifica limite de bilhetes por jogador
                                    int maxTicketsPerPlayer = configDB.getInt("lottery.max_tickets_per_player", 5);
                                    List<LotteryTicket> tickets = playerTickets.getOrDefault(player.getUniqueId(), new ArrayList<>());
                                    
                                    if (tickets.size() >= maxTicketsPerPlayer) {
                                        new BukkitRunnable() {
                                            @Override
                                            public void run() {
                                                player.sendMessage("§cVocê já atingiu o limite de " + maxTicketsPerPlayer + " bilhetes para este sorteio.");
                                            }
                                        }.runTask(plugin);
                                        future.complete(-1);
                                        return;
                                    }
                                    
                                    // Gera um número aleatório para o bilhete
                                    Random random = new Random();
                                    final int ticketNumber = random.nextInt(999999) + 1; // Número entre 1 e 999999
                                    
                                    // Cobra o jogador (no thread principal)
                                    final CompletableFuture<Void> withdrawCF = new CompletableFuture<>();
                                    
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            plugin.getEconomyProvider().withdrawPlayer(player, ticketPrice);
                                            withdrawCF.complete(null);
                                        }
                                    }.runTask(plugin);
                                    
                                    // Continua o processamento após a cobrança
                                    withdrawCF.thenAccept(v -> {
                                        new BukkitRunnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    // Adiciona ao jackpot (80% do valor do bilhete)
                                                    double jackpotContribution = ticketPrice * 0.8;
                                                    jackpot += jackpotContribution;
                                                    configDB.setConfig("lottery.jackpot", jackpot);
                                                    
                                                    // Cria o bilhete
                                                    LotteryTicket ticket = new LotteryTicket(player.getUniqueId(), ticketNumber, System.currentTimeMillis(), lotteryType);
                                                    
                                                    // Salva no cache
                                                    playerTickets.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(ticket);
                                                    
                                                    // Salva no banco de dados
                                                    Document ticketDoc = new Document()
                                                            .append("player_uuid", player.getUniqueId().toString())
                                                            .append("player_name", player.getName())
                                                            .append("ticket_number", ticketNumber)
                                                            .append("purchase_time", System.currentTimeMillis())
                                                            .append("draw_type", lotteryType);
                                                    
                                                    plugin.getMongoDBManager().getDatabase().getCollection("lottery_tickets").insertOne(ticketDoc);
                                                    
                                                    // Notifica o jogador (no thread principal)
                                                    new BukkitRunnable() {
                                                        @Override
                                                        public void run() {
                                                            player.sendMessage("§aVocê comprou um bilhete de loteria com o número §f" + ticketNumber + "§a!");
                                                            player.sendMessage("§aPrêmio atual: §f" + PRICE_FORMAT.format(jackpot));
                                                            player.sendMessage("§aPróximo sorteio: §f" + new Date(nextDrawTime));
                                                        }
                                                    }.runTask(plugin);
                                                    
                                                    future.complete(ticketNumber);
                                                } catch (Exception e) {
                                                    plugin.getLogger().log(Level.SEVERE, "Erro ao salvar bilhete de loteria: " + e.getMessage(), e);
                                                    
                                                    new BukkitRunnable() {
                                                        @Override
                                                        public void run() {
                                                            player.sendMessage("§cOcorreu um erro ao comprar o bilhete. Tente novamente mais tarde.");
                                                        }
                                                    }.runTask(plugin);
                                                    
                                                    future.complete(-1);
                                                }
                                            }
                                        }.runTaskAsynchronously(plugin);
                                    });
                                } catch (Exception e) {
                                    plugin.getLogger().log(Level.SEVERE, "Erro ao processar compra de bilhete: " + e.getMessage(), e);
                                    
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            player.sendMessage("§cOcorreu um erro ao comprar o bilhete. Tente novamente mais tarde.");
                                        }
                                    }.runTask(plugin);
                                    
                                    future.complete(-1);
                                }
                            }
                        }.runTaskAsynchronously(plugin);
                    });
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Erro ao iniciar compra de bilhete: " + e.getMessage(), e);
                    
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.sendMessage("§cOcorreu um erro ao comprar o bilhete. Tente novamente mais tarde.");
                        }
                    }.runTask(plugin);
                    
                    future.complete(-1);
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return future;
    }
    
    /**
     * Realiza o sorteio da loteria
     * @return CompletableFuture com o resultado do sorteio (UUID do vencedor ou null se não houver)
     */
    public CompletableFuture<UUID> drawLottery() {
        CompletableFuture<UUID> future = new CompletableFuture<>();
        
        // Executa todo o sorteio em uma nova thread dedicada
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (countTotalTickets() == 0) {
                        // Não há bilhetes, adia o sorteio
                        scheduleNextDraw();
                        future.complete(null);
                        return;
                    }
                    
                    // Seleciona um bilhete aleatório
                    List<LotteryTicket> allTickets = new ArrayList<>();
                    playerTickets.values().forEach(allTickets::addAll);
                    
                    if (allTickets.isEmpty()) {
                        scheduleNextDraw();
                        future.complete(null);
                        return;
                    }
                    
                    Random random = new Random();
                    LotteryTicket winningTicket = allTickets.get(random.nextInt(allTickets.size()));
                    final UUID winnerId = winningTicket.getPlayerId();
                    
                    // Obtém o nome do jogador vencedor
                    final OfflinePlayer winnerPlayer = Bukkit.getOfflinePlayer(winnerId);
                    final String winnerName = winnerPlayer.getName();
                    final int ticketNumber = winningTicket.getTicketNumber();
                    final double currentJackpot = jackpot;
                    
                    // Paga o prêmio ao vencedor de forma assíncrona
                    plugin.getMongoDBManager().deposit(winnerId, jackpot, "Prêmio da loteria")
                        .thenAccept(success -> {
                            if (success) {
                                // Notifica o vencedor, se estiver online
                                if (winnerPlayer.isOnline()) {
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            Player onlineWinner = winnerPlayer.getPlayer();
                                            onlineWinner.sendMessage("§a§lPARABÉNS! §aVocê ganhou a loteria!");
                                            onlineWinner.sendMessage("§aPrêmio: §f" + PRICE_FORMAT.format(currentJackpot));
                                            onlineWinner.sendMessage("§aBilhete vencedor: §f" + ticketNumber);
                                        }
                                    }.runTask(plugin);
                                }
                                
                                // Anuncia o vencedor para todos
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        Bukkit.broadcastMessage("§a§l[LOTERIA] §f" + winnerName + " §aganhou §f" + 
                                                              PRICE_FORMAT.format(currentJackpot) + " §ana loteria!");
                                        Bukkit.broadcastMessage("§a§l[LOTERIA] §aBilhete vencedor: §f" + ticketNumber);
                                    }
                                }.runTask(plugin);
                                
                                // Registra no log
                                plugin.getLogger().info("Loteria: " + winnerName + " ganhou " + currentJackpot + 
                                                      " com o bilhete " + ticketNumber);
                                
                                // Limpa os bilhetes
                                clearTickets();
                                
                                // Reinicia o jackpot
                                double initialJackpot = configDB.getDouble("lottery.initial_jackpot", 1000.0);
                                jackpot = initialJackpot;
                                configDB.setConfig("lottery.jackpot", jackpot);
                                
                                // Agenda o próximo sorteio
                                scheduleNextDraw();
                                
                                future.complete(winnerId);
                            } else {
                                plugin.getLogger().severe("Erro ao pagar prêmio da loteria ao vencedor: " + winnerName);
                                future.complete(null);
                            }
                        });
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Erro ao realizar sorteio da loteria: " + e.getMessage(), e);
                    future.complete(null);
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return future;
    }
    
    /**
     * Limpa todos os bilhetes após um sorteio
     */
    private void clearTickets() {
        playerTickets.clear();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    plugin.getMongoDBManager().getDatabase().getCollection("lottery_tickets").deleteMany(new Document());
                    plugin.getLogger().info("Bilhetes de loteria limpos com sucesso.");
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Erro ao limpar bilhetes de loteria: " + e.getMessage(), e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }
    
    /**
     * Agenda o próximo sorteio
     */
    public void scheduleNextDraw() {
        long interval;
        
        switch (lotteryType.toLowerCase()) {
            case "daily":
                interval = 86400000; // 24 horas
                break;
            case "weekly":
                interval = 604800000; // 7 dias
                break;
            case "hourly":
                interval = 3600000; // 1 hora
                break;
            default:
                interval = 86400000; // Padrão: 24 horas
        }
        
        nextDrawTime = System.currentTimeMillis() + interval;
        configDB.setConfig("lottery.next_draw", nextDrawTime);
        
        plugin.getLogger().info("Próximo sorteio da loteria agendado para: " + new Date(nextDrawTime));
    }
    
    /**
     * Verifica se é hora de realizar o sorteio
     * @return true se for hora do sorteio
     */
    public boolean isTimeForDraw() {
        return System.currentTimeMillis() >= nextDrawTime;
    }
    
    /**
     * Obtém os bilhetes de um jogador
     * @param playerId UUID do jogador
     * @return Lista de bilhetes do jogador
     */
    public List<LotteryTicket> getPlayerTickets(UUID playerId) {
        return playerTickets.getOrDefault(playerId, new ArrayList<>());
    }
    
    /**
     * Obtém o preço atual do bilhete
     * @return Preço do bilhete
     */
    public double getTicketPrice() {
        return ticketPrice;
    }
    
    /**
     * Obtém o valor atual do jackpot
     * @return Valor do jackpot
     */
    public double getJackpot() {
        return jackpot;
    }
    
    /**
     * Obtém a data/hora do próximo sorteio
     * @return Data/hora do próximo sorteio
     */
    public long getNextDrawTime() {
        return nextDrawTime;
    }
    
    /**
     * Verifica se a loteria está ativa
     * @return true se a loteria estiver ativa
     */
    public boolean isLotteryActive() {
        return lotteryActive;
    }
    
    /**
     * Define se a loteria está ativa
     * @param active Estado da loteria
     */
    public void setLotteryActive(boolean active) {
        this.lotteryActive = active;
        configDB.setConfig("lottery.active", active);
    }
    
    /**
     * Define o tipo de loteria
     * @param type Tipo de loteria (daily, weekly, hourly)
     */
    public void setLotteryType(String type) {
        this.lotteryType = type;
        configDB.setConfig("lottery.type", type);
        scheduleNextDraw();
    }
    
    /**
     * Define o preço do bilhete
     * @param price Novo preço
     */
    public void setTicketPrice(double price) {
        this.ticketPrice = price;
        configDB.setConfig("lottery.ticket_price", price);
    }
}
