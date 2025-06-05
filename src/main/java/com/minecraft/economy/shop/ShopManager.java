package com.minecraft.economy.shop;

import com.minecraft.economy.core.EconomyPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Gerenciador da loja do servidor
 */
public class ShopManager {

    private final EconomyPlugin plugin;
    private final Map<String, ShopItem> shopItems = new HashMap<>();
    private final Map<String, ShopCategory> categories = new HashMap<>();

    public ShopManager(EconomyPlugin plugin) {
        this.plugin = plugin;
        loadShopItems();
    }

    /**
     * Carrega os itens da loja a partir da configuração
     */
    private void loadShopItems() {
        // Implementação básica para resolver erro de compilação
        // Será expandida posteriormente
        new BukkitRunnable() {
            @Override
            public void run() {
                // Carrega categorias e itens de forma assíncrona
                plugin.getLogger().info("Carregando itens da loja...");
                
                // Adiciona algumas categorias padrão
                categories.put("blocks", new ShopCategory("blocks", "Blocos", Material.STONE));
                categories.put("tools", new ShopCategory("tools", "Ferramentas", Material.IRON_PICKAXE));
                categories.put("food", new ShopCategory("food", "Alimentos", Material.BREAD));
                categories.put("misc", new ShopCategory("misc", "Diversos", Material.CHEST));
                
                // Adiciona alguns itens padrão
                addShopItem("stone", "Pedra", Material.STONE, 10.0, "blocks");
                addShopItem("dirt", "Terra", Material.DIRT, 5.0, "blocks");
                addShopItem("iron_pickaxe", "Picareta de Ferro", Material.IRON_PICKAXE, 100.0, "tools");
                addShopItem("bread", "Pão", Material.BREAD, 15.0, "food");
                
                plugin.getLogger().info("Itens da loja carregados com sucesso!");
            }
        }.runTaskAsynchronously(plugin);
    }
    
    /**
     * Adiciona um item à loja
     */
    private void addShopItem(String id, String name, Material material, double basePrice, String categoryId) {
        ShopCategory category = categories.get(categoryId);
        if (category == null) {
            category = categories.get("misc"); // Categoria padrão
        }
        
        ShopItem item = new ShopItem(id, name, material, basePrice, category);
        shopItems.put(id, item);
    }

    /**
     * Atualiza os preços do mercado com base na oferta e demanda
     */
    public void updateMarketPrices() {
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getLogger().info("Atualizando preços do mercado...");
                
                // Implementação básica para resolver erro de compilação
                // Será expandida posteriormente
                for (ShopItem item : shopItems.values()) {
                    // Simula flutuação de preço baseada em oferta e demanda
                    double priceFactor = 0.9 + Math.random() * 0.2; // Fator entre 0.9 e 1.1
                    double newPrice = item.getBasePrice() * priceFactor;
                    
                    // Limita o preço aos valores mínimo e máximo
                    double minPrice = item.getBasePrice() * 0.5;
                    double maxPrice = item.getBasePrice() * 2.0;
                    
                    if (newPrice < minPrice) newPrice = minPrice;
                    if (newPrice > maxPrice) newPrice = maxPrice;
                    
                    item.setCurrentPrice(newPrice);
                }
                
                plugin.getLogger().info("Preços do mercado atualizados com sucesso!");
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Abre a interface da loja para um jogador
     */
    public void openShop(Player player) {
        // Implementação básica para resolver erro de compilação
        // Será expandida posteriormente
        new BukkitRunnable() {
            @Override
            public void run() {
                player.sendMessage("§aAbrindo loja...");
                // A implementação real usaria inventários GUI
            }
        }.runTask(plugin);
    }

    /**
     * Compra um item da loja
     */
    public CompletableFuture<Boolean> buyItem(Player player, String itemId, int amount) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        ShopItem item = shopItems.get(itemId);
        if (item == null) {
            future.complete(false);
            return future;
        }
        
        double totalPrice = item.getCurrentPrice() * amount;
        
        // Verifica se o jogador tem dinheiro suficiente
        plugin.getAsyncMongoDBManager().hasBalance(player.getUniqueId(), totalPrice)
            .thenAccept(hasMoney -> {
                if (!hasMoney) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.sendMessage("§cVocê não tem dinheiro suficiente para comprar este item.");
                        }
                    }.runTask(plugin);
                    future.complete(false);
                    return;
                }
                
                // Cobra o jogador
                plugin.getAsyncMongoDBManager().withdraw(player.getUniqueId(), totalPrice, "Compra de " + amount + "x " + item.getName())
                    .thenAccept(success -> {
                        if (success) {
                            // Dá o item ao jogador
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    ItemStack itemStack = new ItemStack(item.getMaterial(), amount);
                                    player.getInventory().addItem(itemStack);
                                    player.sendMessage("§aVocê comprou §f" + amount + "x " + item.getName() + " §apor §f" + totalPrice + " " + plugin.getConfigManager().getCurrencyNamePlural() + "§a.");
                                }
                            }.runTask(plugin);
                            future.complete(true);
                        } else {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.sendMessage("§cOcorreu um erro ao processar a compra. Tente novamente mais tarde.");
                                }
                            }.runTask(plugin);
                            future.complete(false);
                        }
                    });
            });
        
        return future;
    }

    /**
     * Vende um item para a loja
     */
    public CompletableFuture<Boolean> sellItem(Player player, String itemId, int amount) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        ShopItem item = shopItems.get(itemId);
        if (item == null) {
            future.complete(false);
            return future;
        }
        
        // Preço de venda é 70% do preço de compra
        double sellPrice = item.getCurrentPrice() * 0.7 * amount;
        
        // Verifica se o jogador tem o item
        new BukkitRunnable() {
            @Override
            public void run() {
                ItemStack itemStack = new ItemStack(item.getMaterial(), amount);
                if (!player.getInventory().containsAtLeast(itemStack, amount)) {
                    player.sendMessage("§cVocê não tem itens suficientes para vender.");
                    future.complete(false);
                    return;
                }
                
                // Remove o item do inventário
                player.getInventory().removeItem(itemStack);
                
                // Deposita o dinheiro na conta do jogador
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        plugin.getAsyncMongoDBManager().deposit(player.getUniqueId(), sellPrice, "Venda de " + amount + "x " + item.getName())
                            .thenAccept(success -> {
                                if (success) {
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            player.sendMessage("§aVocê vendeu §f" + amount + "x " + item.getName() + " §apor §f" + sellPrice + " " + plugin.getConfigManager().getCurrencyNamePlural() + "§a.");
                                        }
                                    }.runTask(plugin);
                                    future.complete(true);
                                } else {
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            player.sendMessage("§cOcorreu um erro ao processar a venda. Tente novamente mais tarde.");
                                            // Devolve o item ao jogador
                                            player.getInventory().addItem(itemStack);
                                        }
                                    }.runTask(plugin);
                                    future.complete(false);
                                }
                            });
                    }
                }.runTaskAsynchronously(plugin);
            }
        }.runTask(plugin);
        
        return future;
    }
    
    /**
     * Obtém um item da loja pelo ID
     */
    public ShopItem getItem(String id) {
        return shopItems.get(id);
    }
    
    /**
     * Obtém todos os itens da loja
     */
    public Map<String, ShopItem> getAllItems() {
        return shopItems;
    }
    
    /**
     * Obtém todos os itens da loja
     * Método alternativo para compatibilidade
     */
    public Map<String, ShopItem> getShopItems() {
        return shopItems;
    }
    
    /**
     * Obtém todas as categorias da loja
     */
    public Map<String, ShopCategory> getCategories() {
        return categories;
    }
    
    /**
     * Obtém uma categoria pelo ID
     */
    public ShopCategory getCategory(String id) {
        return categories.get(id);
    }
}
