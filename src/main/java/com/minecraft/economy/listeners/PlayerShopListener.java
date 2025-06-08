package com.minecraft.economy.listeners;

import com.minecraft.economy.core.EconomyPlugin;
import com.minecraft.economy.playershop.PlayerShop;
import com.minecraft.economy.playershop.PlayerShopGUI;
import com.minecraft.economy.playershop.PlayerShopItem;
import com.minecraft.economy.playershop.PlayerShopManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Listener para eventos de inventário relacionados às lojas de jogadores
 */
public class PlayerShopListener implements Listener {

    private final EconomyPlugin plugin;
    private final PlayerShopManager shopManager;
    private final PlayerShopGUI shopGUI;
    private final PlayerChatListener chatListener;
    private final Map<UUID, ItemStack> pendingAddItems = new HashMap<>();
    private final Map<UUID, Double> pendingPrices = new HashMap<>();

    public PlayerShopListener(EconomyPlugin plugin) {
        this.plugin = plugin;
        this.shopManager = plugin.getPlayerShopManager();
        this.shopGUI = new PlayerShopGUI(plugin, shopManager);
        this.chatListener = new PlayerChatListener(plugin);
        
        // Registra o listener de chat
        plugin.getServer().getPluginManager().registerEvents(chatListener, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        String title = event.getView().getTitle();
        
        // Verifica se é um inventário de loja de jogador
        if (!shopGUI.isPlayerShopInventory(title)) {
            return;
        }
        
        // Cancela o evento para evitar roubo de itens
        event.setCancelled(true);
        
        // Processa o clique com base no tipo de inventário
        String inventoryType = shopGUI.getInventoryType(title);
        ItemStack clickedItem = event.getCurrentItem();
        
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        // Processa cliques no menu principal
        if (inventoryType.equals("main")) {
            handleMainMenuClick(player, clickedItem, event.getSlot());
        }
        // Processa cliques no menu de gerenciamento
        else if (inventoryType.equals("manage")) {
            handleManageMenuClick(player, clickedItem, event.getSlot());
        }
        // Processa cliques no menu de navegação
        else if (inventoryType.equals("browse")) {
            handleBrowseMenuClick(player, clickedItem, event.getSlot());
        }
        // Processa cliques no menu de loja
        else if (inventoryType.equals("shop")) {
            handleShopMenuClick(player, clickedItem, event.getSlot());
        }
        // Processa cliques no menu de adicionar item
        else if (inventoryType.equals("add_item")) {
            handleAddItemMenuClick(player, clickedItem, event.getSlot());
        }
        // Processa cliques no menu de confirmar compra
        else if (inventoryType.equals("confirm_buy")) {
            handleConfirmBuyMenuClick(player, clickedItem, event.getSlot());
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Inventory inventory = event.getInventory();
        String title = event.getView().getTitle();
        
        // Verifica se é um inventário de loja de jogador
        if (shopGUI.isPlayerShopInventory(title)) {
            // Cancela o evento para evitar roubo de itens
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        
        // Remove o jogador das listas de controle
        shopGUI.removePlayer(player);
    }
    
    /**
     * Processa cliques no menu principal
     */
    private void handleMainMenuClick(Player player, ItemStack clickedItem, int slot) {
        // Botão de criar loja (slot 4)
        if (slot == 4) {
            player.closeInventory();
            player.sendMessage("§aDigite o nome da sua nova loja no chat:");
            player.sendMessage("§7(ou digite 'cancelar' para cancelar)");
            
            // Define o estado de chat para criação de loja
            chatListener.setChatState(player, PlayerChatListener.ChatState.CREATING_SHOP);
            return;
        }
        
        // Botão de procurar lojas (slot 49)
        if (slot == 49) {
            shopGUI.openBrowseShopsMenu(player);
            return;
        }
        
        // Clique em uma loja do jogador
        if (slot >= 19 && slot <= 44) {
            List<PlayerShop> playerShops = shopManager.getPlayerShopsByOwner(player.getUniqueId());
            int index = slot - 19;
            if (index < playerShops.size()) {
                PlayerShop shop = playerShops.get(index);
                shopGUI.openShopManagementMenu(player, shop);
            }
        }
    }
    
    /**
     * Processa cliques no menu de gerenciamento
     */
    private void handleManageMenuClick(Player player, ItemStack clickedItem, int slot) {
        PlayerShop shop = shopGUI.getCurrentShop(player);
        if (shop == null) {
            return;
        }
        
        // Botão de voltar (slot 45)
        if (slot == 45) {
            shopGUI.openMainMenu(player);
            return;
        }
        
        // Botão de adicionar item (slot 20)
        if (slot == 20) {
            ItemStack itemInHand = player.getInventory().getItemInMainHand().clone();
            if (itemInHand == null || itemInHand.getType() == Material.AIR) {
                player.sendMessage("§cVocê precisa ter um item na mão para adicionar à loja.");
                return;
            }
            
            // Abre o menu de adicionar item
            shopGUI.openAddItemDialog(player, shop, itemInHand);
            return;
        }
        
        // Botão de sacar saldo (slot 22)
        if (slot == 22) {
            shop.withdrawBalance(player);
            shopGUI.openShopManagementMenu(player, shop);
            return;
        }
        
        // Botão de deletar loja (slot 24)
        if (slot == 24) {
            player.closeInventory();
            player.sendMessage("§cVocê tem certeza que deseja deletar esta loja? Digite 'confirmar' para confirmar:");
            player.sendMessage("§7(ou digite 'cancelar' para cancelar)");
            
            // Define o estado de chat para confirmação de exclusão
            chatListener.setChatState(player, PlayerChatListener.ChatState.CONFIRMING_DELETE_SHOP);
            chatListener.setPendingShop(player, shop);
            return;
        }
        
        // Botão de página anterior (slot 48)
        if (slot == 48) {
            int currentPage = shopGUI.getCurrentPage(player);
            if (currentPage > 0) {
                shopGUI.setCurrentPage(player, currentPage - 1);
                shopGUI.openShopManagementMenu(player, shop);
            }
            return;
        }
        
        // Botão de próxima página (slot 50)
        if (slot == 50) {
            int currentPage = shopGUI.getCurrentPage(player);
            List<PlayerShopItem> items = shop.getItems();
            int maxPage = (items.size() / 27);
            if (currentPage < maxPage) {
                shopGUI.setCurrentPage(player, currentPage + 1);
                shopGUI.openShopManagementMenu(player, shop);
            }
            return;
        }
        
        // Clique em um item da loja
        if (slot >= 27 && slot < 54) {
            List<PlayerShopItem> items = shop.getItems();
            int page = shopGUI.getCurrentPage(player);
            int startIndex = page * 27;
            int index = startIndex + (slot - 27);
            
            if (index < items.size()) {
                PlayerShopItem item = items.get(index);
                
                // Pergunta se quer remover o item
                player.closeInventory();
                player.sendMessage("§aO que deseja fazer com este item?");
                player.sendMessage("§7- Digite 'remover' para remover o item da loja");
                player.sendMessage("§7- Digite 'preço <valor>' para alterar o preço");
                player.sendMessage("§7- Digite 'dinâmico sim' para ativar preço dinâmico");
                player.sendMessage("§7- Digite 'dinâmico não' para desativar preço dinâmico");
                player.sendMessage("§7- Digite 'estoque adicionar <quantidade>' para adicionar itens ao estoque");
                player.sendMessage("§7- Digite 'estoque remover <quantidade>' para remover itens do estoque");
                player.sendMessage("§7- Digite 'cancelar' para cancelar");
                
                // Define o estado de chat para gerenciamento de item
                chatListener.setChatState(player, PlayerChatListener.ChatState.MANAGING_ITEM);
                chatListener.setPendingShop(player, shop);
                chatListener.setPendingItem(player, item);
            }
        }
    }
    
    /**
     * Processa cliques no menu de navegação
     */
    private void handleBrowseMenuClick(Player player, ItemStack clickedItem, int slot) {
        // Botão de voltar (slot 45)
        if (slot == 45) {
            shopGUI.openMainMenu(player);
            return;
        }
        
        // Botão de página anterior (slot 48)
        if (slot == 48) {
            int currentPage = shopGUI.getCurrentPage(player);
            if (currentPage > 0) {
                shopGUI.setCurrentPage(player, currentPage - 1);
                shopGUI.openBrowseShopsMenu(player);
            }
            return;
        }
        
        // Botão de próxima página (slot 50)
        if (slot == 50) {
            int currentPage = shopGUI.getCurrentPage(player);
            Map<String, PlayerShop> allShops = shopManager.getAllPlayerShops();
            int maxPage = (allShops.size() / 45);
            if (currentPage < maxPage) {
                shopGUI.setCurrentPage(player, currentPage + 1);
                shopGUI.openBrowseShopsMenu(player);
            }
            return;
        }
        
        // Clique em uma loja
        if (slot >= 0 && slot < 45) {
            Map<String, PlayerShop> allShops = shopManager.getAllPlayerShops();
            int page = shopGUI.getCurrentPage(player);
            int startIndex = page * 45;
            int index = startIndex + slot;
            
            if (index < allShops.size()) {
                PlayerShop shop = (PlayerShop) allShops.values().toArray()[index];
                shopGUI.openShopBuyMenu(player, shop);
            }
        }
    }
    
    /**
     * Processa cliques no menu de loja
     */
    private void handleShopMenuClick(Player player, ItemStack clickedItem, int slot) {
        PlayerShop shop = shopGUI.getCurrentShop(player);
        if (shop == null) {
            return;
        }
        
        // Botão de voltar (slot 45)
        if (slot == 45) {
            shopGUI.openBrowseShopsMenu(player);
            return;
        }
        
        // Botão de página anterior (slot 48)
        if (slot == 48) {
            int currentPage = shopGUI.getCurrentPage(player);
            if (currentPage > 0) {
                shopGUI.setCurrentPage(player, currentPage - 1);
                shopGUI.openShopBuyMenu(player, shop);
            }
            return;
        }
        
        // Botão de próxima página (slot 50)
        if (slot == 50) {
            int currentPage = shopGUI.getCurrentPage(player);
            List<PlayerShopItem> items = shop.getItems();
            int maxPage = (items.size() / 36);
            if (currentPage < maxPage) {
                shopGUI.setCurrentPage(player, currentPage + 1);
                shopGUI.openShopBuyMenu(player, shop);
            }
            return;
        }
        
        // Clique em um item da loja
        if (slot >= 9 && slot < 45) {
            List<PlayerShopItem> items = shop.getItems();
            int page = shopGUI.getCurrentPage(player);
            int startIndex = page * 36;
            
            // Calcula o índice real do item
            int row = slot / 9;
            int col = slot % 9;
            int index = startIndex + ((row - 1) * 8) + col;
            
            if (index < items.size()) {
                PlayerShopItem item = items.get(index);
                if (item.isAvailable()) {
                    shopGUI.openBuyConfirmationDialog(player, shop, item);
                }
            }
        }
    }
    
    /**
     * Processa cliques no menu de adicionar item
     */
    private void handleAddItemMenuClick(Player player, ItemStack clickedItem, int slot) {
        PlayerShop shop = shopGUI.getCurrentShop(player);
        if (shop == null) {
            return;
        }
        
        ItemStack itemToAdd = player.getInventory().getItemInMainHand().clone();
        if (itemToAdd == null || itemToAdd.getType() == Material.AIR) {
            player.sendMessage("§cVocê precisa ter um item na mão.");
            player.closeInventory();
            return;
        }
        
        // Preço baixo fixo (slot 10)
        if (slot == 10) {
            double price = 10.0;
            shopGUI.addItemToShop(player, shop, itemToAdd, price, false);
            return;
        }
        
        // Preço médio fixo (slot 11)
        if (slot == 11) {
            double price = 50.0;
            shopGUI.addItemToShop(player, shop, itemToAdd, price, false);
            return;
        }
        
        // Preço alto fixo (slot 12)
        if (slot == 12) {
            double price = 200.0;
            shopGUI.addItemToShop(player, shop, itemToAdd, price, false);
            return;
        }
        
        // Preço baixo dinâmico (slot 14)
        if (slot == 14) {
            double price = 10.0;
            shopGUI.addItemToShop(player, shop, itemToAdd, price, true);
            return;
        }
        
        // Preço médio dinâmico (slot 15)
        if (slot == 15) {
            double price = 50.0;
            shopGUI.addItemToShop(player, shop, itemToAdd, price, true);
            return;
        }
        
        // Preço alto dinâmico (slot 16)
        if (slot == 16) {
            double price = 200.0;
            shopGUI.addItemToShop(player, shop, itemToAdd, price, true);
            return;
        }
        
        // Preço personalizado fixo (slot 19)
        if (slot == 19) {
            player.closeInventory();
            player.sendMessage("§aDigite o preço do item no chat:");
            player.sendMessage("§7(ou digite 'cancelar' para cancelar)");
            
            // Define o estado de chat para definição de preço
            chatListener.setChatState(player, PlayerChatListener.ChatState.SETTING_ITEM_PRICE);
            chatListener.setPendingShop(player, shop);
            chatListener.setPendingAddItem(player, itemToAdd);
            chatListener.setPendingDynamicPrice(player, false);
            return;
        }
        
        // Preço personalizado dinâmico (slot 25)
        if (slot == 25) {
            player.closeInventory();
            player.sendMessage("§aDigite o preço do item no chat:");
            player.sendMessage("§7(ou digite 'cancelar' para cancelar)");
            
            // Define o estado de chat para definição de preço
            chatListener.setChatState(player, PlayerChatListener.ChatState.SETTING_ITEM_PRICE);
            chatListener.setPendingShop(player, shop);
            chatListener.setPendingAddItem(player, itemToAdd);
            chatListener.setPendingDynamicPrice(player, true);
            return;
        }
        
        // Botão de cancelar (slot 31)
        if (slot == 31) {
            shopGUI.openShopManagementMenu(player, shop);
            return;
        }
    }
    
    /**
     * Processa cliques no menu de confirmar compra
     */
    private void handleConfirmBuyMenuClick(Player player, ItemStack clickedItem, int slot) {
        PlayerShop shop = shopGUI.getCurrentShop(player);
        if (shop == null) {
            return;
        }
        
        // Botão de confirmar (slot 11)
        if (slot == 11) {
            ItemStack itemStack = player.getOpenInventory().getItem(13);
            if (itemStack != null) {
                for (PlayerShopItem item : shop.getItems()) {
                    if (item.isAvailable() && item.getItemStack().isSimilar(itemStack)) {
                        shop.buyItem(player, item.getId().toString());
                        break;
                    }
                }
            }
            shopGUI.openShopBuyMenu(player, shop);
            return;
        }
        
        // Botão de cancelar (slot 15)
        if (slot == 15) {
            shopGUI.openShopBuyMenu(player, shop);
            return;
        }
    }
}
