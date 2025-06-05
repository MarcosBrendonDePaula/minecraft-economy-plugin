package com.minecraft.economy.shop;

import com.minecraft.economy.core.EconomyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface gráfica da loja
 */
public class ShopGUI {

    private final EconomyPlugin plugin;
    private final ShopManager shopManager;
    private final Map<Player, Inventory> openInventories = new HashMap<>();
    private final Map<Player, ShopCategory> currentCategory = new HashMap<>();
    private final Map<Player, Integer> currentPage = new HashMap<>();
    private final Map<Player, String> searchQuery = new HashMap<>();

    public ShopGUI(EconomyPlugin plugin, ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
    }

    /**
     * Abre o menu principal da loja
     * @param player Jogador
     */
    public void openMainMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, "§8Loja - Menu Principal");
        
        // Adiciona as categorias
        int slot = 10;
        for (ShopCategory category : shopManager.getCategories().values()) {
            ItemStack icon = new ItemStack(category.getIcon());
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName("§a" + category.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Clique para ver os itens desta categoria");
            meta.setLore(lore);
            
            icon.setItemMeta(meta);
            inventory.setItem(slot, icon);
            
            slot++;
            if ((slot % 9) == 8) {
                slot += 2;
            }
        }
        
        // Adiciona botões de navegação
        addNavigationButtons(inventory, player);
        
        // Abre o inventário
        player.openInventory(inventory);
        openInventories.put(player, inventory);
        currentPage.put(player, 0);
    }

    /**
     * Abre o menu de uma categoria
     * @param player Jogador
     * @param categoryId ID da categoria
     */
    public void openCategoryMenu(Player player, String categoryId) {
        ShopCategory category = shopManager.getCategory(categoryId);
        if (category == null) {
            player.sendMessage("§cCategoria não encontrada.");
            return;
        }
        
        Inventory inventory = Bukkit.createInventory(null, 54, "§8Loja - " + category.getName());
        
        // Adiciona os itens da categoria
        List<ShopItem> items = new ArrayList<>(category.getItems().values());
        int page = currentPage.getOrDefault(player, 0);
        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, items.size());
        
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            ShopItem item = items.get(i);
            ItemStack icon = item.createItemStack(1);
            inventory.setItem(slot, icon);
            
            slot++;
            if ((slot % 9) == 0 && slot >= 45) {
                break;
            }
        }
        
        // Adiciona botões de navegação
        addNavigationButtons(inventory, player);
        
        // Abre o inventário
        player.openInventory(inventory);
        openInventories.put(player, inventory);
        currentCategory.put(player, category);
    }

    /**
     * Adiciona botões de navegação ao inventário
     * @param inventory Inventário
     * @param player Jogador
     */
    private void addNavigationButtons(Inventory inventory, Player player) {
        // Botão de voltar
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§cVoltar");
        backButton.setItemMeta(backMeta);
        inventory.setItem(45, backButton);
        
        // Botão de página anterior
        ItemStack prevButton = new ItemStack(Material.PAPER);
        ItemMeta prevMeta = prevButton.getItemMeta();
        prevMeta.setDisplayName("§ePágina Anterior");
        prevButton.setItemMeta(prevMeta);
        inventory.setItem(48, prevButton);
        
        // Botão de página seguinte
        ItemStack nextButton = new ItemStack(Material.PAPER);
        ItemMeta nextMeta = nextButton.getItemMeta();
        nextMeta.setDisplayName("§ePróxima Página");
        nextButton.setItemMeta(nextMeta);
        inventory.setItem(50, nextButton);
        
        // Botão de pesquisa
        ItemStack searchButton = new ItemStack(Material.COMPASS);
        ItemMeta searchMeta = searchButton.getItemMeta();
        searchMeta.setDisplayName("§ePesquisar");
        searchButton.setItemMeta(searchMeta);
        inventory.setItem(49, searchButton);
        
        // Informações da página
        int page = currentPage.getOrDefault(player, 0) + 1;
        ItemStack pageInfo = new ItemStack(Material.BOOK);
        ItemMeta pageInfoMeta = pageInfo.getItemMeta();
        pageInfoMeta.setDisplayName("§ePágina " + page);
        pageInfo.setItemMeta(pageInfoMeta);
        inventory.setItem(53, pageInfo);
    }

    /**
     * Abre o menu de detalhes de um item
     * @param player Jogador
     * @param shopItem Item da loja
     */
    public void openItemDetails(Player player, ShopItem shopItem) {
        Inventory inventory = Bukkit.createInventory(null, 27, "§8Detalhes do Item");
        
        // Item
        ItemStack item = shopItem.createItemStack(1);
        inventory.setItem(13, item);
        
        // Informações do item
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§aInformações");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Nome: §f" + shopItem.getDisplayName());
        lore.add("§7Preço: §f" + String.format("%.2f", shopItem.getCurrentPrice()) + " " + plugin.getConfigManager().getCurrencyNamePlural());
        lore.add("§7Estoque: §f" + shopItem.getStock());
        
        if (!shopItem.getDescription().isEmpty()) {
            lore.add("§7Descrição: §f" + shopItem.getDescription());
        }
        
        infoMeta.setLore(lore);
        info.setItemMeta(infoMeta);
        inventory.setItem(11, info);
        
        // Botão de compra
        ItemStack buyButton = new ItemStack(Material.EMERALD);
        ItemMeta buyMeta = buyButton.getItemMeta();
        buyMeta.setDisplayName("§aComprar");
        
        List<String> buyLore = new ArrayList<>();
        buyLore.add("§7Clique para comprar este item");
        buyLore.add("§7Preço: §f" + String.format("%.2f", shopItem.getCurrentPrice()) + " " + plugin.getConfigManager().getCurrencyNamePlural());
        buyMeta.setLore(buyLore);
        
        buyButton.setItemMeta(buyMeta);
        inventory.setItem(15, buyButton);
        
        // Botão de voltar
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§cVoltar");
        backButton.setItemMeta(backMeta);
        inventory.setItem(18, backButton);
        
        // Abre o inventário
        player.openInventory(inventory);
        openInventories.put(player, inventory);
    }

    /**
     * Abre o menu de compra de um item
     * @param player Jogador
     * @param shopItem Item da loja
     */
    public void openBuyMenu(Player player, ShopItem shopItem) {
        Inventory inventory = Bukkit.createInventory(null, 27, "§8Comprar Item");
        
        // Item
        ItemStack item = shopItem.createItemStack(1);
        inventory.setItem(13, item);
        
        // Botão de compra x1
        ItemStack buy1 = new ItemStack(Material.EMERALD, 1);
        ItemMeta buy1Meta = buy1.getItemMeta();
        buy1Meta.setDisplayName("§aComprar 1x");
        
        List<String> buy1Lore = new ArrayList<>();
        buy1Lore.add("§7Preço: §f" + String.format("%.2f", shopItem.getCurrentPrice()) + " " + plugin.getConfigManager().getCurrencyNamePlural());
        buy1Meta.setLore(buy1Lore);
        
        buy1.setItemMeta(buy1Meta);
        inventory.setItem(10, buy1);
        
        // Botão de compra x8
        ItemStack buy8 = new ItemStack(Material.EMERALD, 8);
        ItemMeta buy8Meta = buy8.getItemMeta();
        buy8Meta.setDisplayName("§aComprar 8x");
        
        List<String> buy8Lore = new ArrayList<>();
        buy8Lore.add("§7Preço: §f" + String.format("%.2f", shopItem.getCurrentPrice() * 8) + " " + plugin.getConfigManager().getCurrencyNamePlural());
        buy8Meta.setLore(buy8Lore);
        
        buy8.setItemMeta(buy8Meta);
        inventory.setItem(11, buy8);
        
        // Botão de compra x16
        ItemStack buy16 = new ItemStack(Material.EMERALD, 16);
        ItemMeta buy16Meta = buy16.getItemMeta();
        buy16Meta.setDisplayName("§aComprar 16x");
        
        List<String> buy16Lore = new ArrayList<>();
        buy16Lore.add("§7Preço: §f" + String.format("%.2f", shopItem.getCurrentPrice() * 16) + " " + plugin.getConfigManager().getCurrencyNamePlural());
        buy16Meta.setLore(buy16Lore);
        
        buy16.setItemMeta(buy16Meta);
        inventory.setItem(12, buy16);
        
        // Botão de compra x32
        ItemStack buy32 = new ItemStack(Material.EMERALD, 32);
        ItemMeta buy32Meta = buy32.getItemMeta();
        buy32Meta.setDisplayName("§aComprar 32x");
        
        List<String> buy32Lore = new ArrayList<>();
        buy32Lore.add("§7Preço: §f" + String.format("%.2f", shopItem.getCurrentPrice() * 32) + " " + plugin.getConfigManager().getCurrencyNamePlural());
        buy32Meta.setLore(buy32Lore);
        
        buy32.setItemMeta(buy32Meta);
        inventory.setItem(14, buy32);
        
        // Botão de compra x64
        ItemStack buy64 = new ItemStack(Material.EMERALD, 64);
        ItemMeta buy64Meta = buy64.getItemMeta();
        buy64Meta.setDisplayName("§aComprar 64x");
        
        List<String> buy64Lore = new ArrayList<>();
        buy64Lore.add("§7Preço: §f" + String.format("%.2f", shopItem.getCurrentPrice() * 64) + " " + plugin.getConfigManager().getCurrencyNamePlural());
        buy64Meta.setLore(buy64Lore);
        
        buy64.setItemMeta(buy64Meta);
        inventory.setItem(15, buy64);
        
        // Botão de voltar
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§cVoltar");
        backButton.setItemMeta(backMeta);
        inventory.setItem(18, backButton);
        
        // Abre o inventário
        player.openInventory(inventory);
        openInventories.put(player, inventory);
    }

    /**
     * Abre o menu de venda de um item
     * @param player Jogador
     */
    public void openSellMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, "§8Vender Itens");
        
        // Adiciona os itens disponíveis para venda
        Map<String, ShopItem> items = shopManager.getShopItems();
        int page = currentPage.getOrDefault(player, 0);
        int startIndex = page * 45;
        
        List<ShopItem> itemList = new ArrayList<>(items.values());
        int endIndex = Math.min(startIndex + 45, itemList.size());
        
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            ShopItem item = itemList.get(i);
            
            // Preço de venda é 70% do preço de compra
            double sellPrice = item.getCurrentPrice() * 0.7;
            
            ItemStack icon = new ItemStack(item.getMaterial());
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName("§a" + item.getDisplayName());
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Preço de venda: §f" + String.format("%.2f", sellPrice) + " " + plugin.getConfigManager().getCurrencyNamePlural());
            lore.add("§7Clique para vender este item");
            meta.setLore(lore);
            
            icon.setItemMeta(meta);
            inventory.setItem(slot, icon);
            
            slot++;
            if ((slot % 9) == 0 && slot >= 45) {
                break;
            }
        }
        
        // Adiciona botões de navegação
        addNavigationButtons(inventory, player);
        
        // Abre o inventário
        player.openInventory(inventory);
        openInventories.put(player, inventory);
    }

    /**
     * Processa a compra de um item
     * @param player Jogador
     * @param shopItem Item da loja
     * @param amount Quantidade
     */
    public void buyItem(Player player, ShopItem shopItem, int amount) {
        // Verifica se o jogador tem dinheiro suficiente
        double totalPrice = shopItem.getCurrentPrice() * amount;
        
        plugin.getAsyncMongoDBManager().hasBalance(player.getUniqueId(), totalPrice)
            .thenAccept(hasMoney -> {
                if (!hasMoney) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.sendMessage("§cVocê não tem dinheiro suficiente para comprar este item.");
                            player.closeInventory();
                        }
                    }.runTask(plugin);
                    return;
                }
                
                // Cobra o jogador
                plugin.getAsyncMongoDBManager().withdraw(player.getUniqueId(), totalPrice, "Compra de " + amount + "x " + shopItem.getDisplayName())
                    .thenAccept(success -> {
                        if (success) {
                            // Dá o item ao jogador
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    ItemStack itemStack = shopItem.createItemStack(amount);
                                    player.getInventory().addItem(itemStack);
                                    player.sendMessage("§aVocê comprou §f" + amount + "x " + shopItem.getDisplayName() + " §apor §f" + totalPrice + " " + plugin.getConfigManager().getCurrencyNamePlural() + "§a.");
                                    player.closeInventory();
                                }
                            }.runTask(plugin);
                        } else {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.sendMessage("§cOcorreu um erro ao processar a compra. Tente novamente mais tarde.");
                                    player.closeInventory();
                                }
                            }.runTask(plugin);
                        }
                    });
            });
    }

    /**
     * Processa a venda de um item
     * @param player Jogador
     * @param shopItem Item da loja
     */
    public void sellItem(Player player, ShopItem shopItem) {
        // Verifica se o jogador tem o item
        ItemStack itemStack = shopItem.createItemStack(1);
        
        if (!player.getInventory().containsAtLeast(itemStack, 1)) {
            player.sendMessage("§cVocê não tem este item para vender.");
            player.closeInventory();
            return;
        }
        
        // Preço de venda é 70% do preço de compra
        double sellPrice = shopItem.getCurrentPrice() * 0.7;
        
        // Remove o item do inventário
        player.getInventory().removeItem(itemStack);
        
        // Deposita o dinheiro na conta do jogador
        plugin.getAsyncMongoDBManager().deposit(player.getUniqueId(), sellPrice, "Venda de " + shopItem.getDisplayName())
            .thenAccept(success -> {
                if (success) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.sendMessage("§aVocê vendeu §f" + shopItem.getDisplayName() + " §apor §f" + sellPrice + " " + plugin.getConfigManager().getCurrencyNamePlural() + "§a.");
                        }
                    }.runTask(plugin);
                } else {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.sendMessage("§cOcorreu um erro ao processar a venda. Tente novamente mais tarde.");
                            // Devolve o item ao jogador
                            player.getInventory().addItem(itemStack);
                        }
                    }.runTask(plugin);
                }
            });
    }

    /**
     * Verifica se um inventário pertence à loja
     * @param inventory Inventário
     * @return true se o inventário pertence à loja, false caso contrário
     */
    public boolean isShopInventory(Inventory inventory) {
        return openInventories.containsValue(inventory);
    }

    /**
     * Fecha todos os inventários abertos
     */
    public void closeAllInventories() {
        for (Player player : openInventories.keySet()) {
            player.closeInventory();
        }
        openInventories.clear();
        currentCategory.clear();
        currentPage.clear();
        searchQuery.clear();
    }
}
