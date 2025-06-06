package com.minecraft.economy.playershop;

import com.minecraft.economy.core.EconomyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Interface gráfica para lojas de jogadores
 */
public class PlayerShopGUI {

    private final EconomyPlugin plugin;
    private final PlayerShopManager shopManager;
    private final Map<Player, Inventory> openInventories = new HashMap<>();
    private final Map<Player, PlayerShop> currentShop = new HashMap<>();
    private final Map<Player, Integer> currentPage = new HashMap<>();
    private final Map<UUID, ItemStack> pendingItems = new HashMap<>();
    private final Map<UUID, Boolean> pendingDynamicPrice = new HashMap<>();

    public PlayerShopGUI(EconomyPlugin plugin, PlayerShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
    }

    /**
     * Abre o menu principal de lojas de jogadores
     * @param player Jogador
     */
    public void openMainMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, "§8Lojas de Jogadores");
        
        // Adiciona botões de ação
        ItemStack createShopButton = createGuiItem(Material.EMERALD_BLOCK, "§aCriar Nova Loja", 
                                                 "§7Clique para criar uma nova loja");
        inventory.setItem(4, createShopButton);
        
        // Lista as lojas do jogador
        List<PlayerShop> playerShops = shopManager.getPlayerShopsByOwner(player.getUniqueId());
        if (playerShops.isEmpty()) {
            ItemStack noShopsItem = createGuiItem(Material.BARRIER, "§cVocê não tem lojas", 
                                                "§7Clique no botão acima para criar uma loja");
            inventory.setItem(22, noShopsItem);
        } else {
            int slot = 19;
            for (PlayerShop shop : playerShops) {
                ItemStack shopItem = createGuiItem(Material.CHEST, "§a" + shop.getShopName(), 
                                                 "§7Clique para gerenciar esta loja",
                                                 "§7Itens à venda: §f" + shop.getItems().size(),
                                                 "§7Saldo: §f" + plugin.getEconomyProvider().format(shop.getBalance()));
                inventory.setItem(slot, shopItem);
                slot++;
                if ((slot % 9) == 8) {
                    slot += 2;
                }
                if (slot >= 45) break;
            }
        }
        
        // Adiciona botão para ver todas as lojas
        ItemStack browseShopsButton = createGuiItem(Material.COMPASS, "§eProcurar Lojas", 
                                                  "§7Clique para ver todas as lojas disponíveis");
        inventory.setItem(49, browseShopsButton);
        
        // Abre o inventário
        player.openInventory(inventory);
        openInventories.put(player, inventory);
    }

    /**
     * Abre o menu de gerenciamento de uma loja
     * @param player Jogador
     * @param shop Loja
     */
    public void openShopManagementMenu(Player player, PlayerShop shop) {
        Inventory inventory = Bukkit.createInventory(null, 54, "§8Gerenciar Loja: " + shop.getShopName());
        
        // Informações da loja
        ItemStack infoItem = createGuiItem(Material.BOOK, "§aInformações da Loja", 
                                         "§7Nome: §f" + shop.getShopName(),
                                         "§7Dono: §f" + shop.getOwnerName(),
                                         "§7Saldo: §f" + plugin.getEconomyProvider().format(shop.getBalance()),
                                         "§7Itens à venda: §f" + shop.getItems().size());
        inventory.setItem(4, infoItem);
        
        // Botão para adicionar item
        ItemStack addItemButton = createGuiItem(Material.HOPPER, "§aAdicionar Item", 
                                              "§7Clique com um item na mão",
                                              "§7para adicioná-lo à loja");
        inventory.setItem(20, addItemButton);
        
        // Botão para sacar saldo
        ItemStack withdrawButton = createGuiItem(Material.GOLD_INGOT, "§aSacar Saldo", 
                                               "§7Clique para sacar o saldo da loja",
                                               "§7Saldo atual: §f" + plugin.getEconomyProvider().format(shop.getBalance()));
        inventory.setItem(22, withdrawButton);
        
        // Botão para deletar loja
        ItemStack deleteButton = createGuiItem(Material.BARRIER, "§cDeletar Loja", 
                                             "§7Clique para deletar esta loja",
                                             "§cAtenção: Esta ação não pode ser desfeita!");
        inventory.setItem(24, deleteButton);
        
        // Lista os itens da loja
        List<PlayerShopItem> items = shop.getItems();
        int page = currentPage.getOrDefault(player, 0);
        int startIndex = page * 27;
        int endIndex = Math.min(startIndex + 27, items.size());
        
        if (items.isEmpty()) {
            ItemStack noItemsItem = createGuiItem(Material.BARRIER, "§cNenhum item à venda", 
                                                "§7Adicione itens à sua loja");
            inventory.setItem(31, noItemsItem);
        } else {
            int slot = 27;
            for (int i = startIndex; i < endIndex; i++) {
                PlayerShopItem item = items.get(i);
                ItemStack displayItem = item.createDisplayItem();
                inventory.setItem(slot, displayItem);
                slot++;
                if (slot >= 54) break;
            }
        }
        
        // Botão de voltar
        ItemStack backButton = createGuiItem(Material.ARROW, "§cVoltar", 
                                           "§7Voltar ao menu principal");
        inventory.setItem(45, backButton);
        
        // Botões de navegação
        if (items.size() > 27) {
            // Botão de página anterior
            if (page > 0) {
                ItemStack prevButton = createGuiItem(Material.PAPER, "§ePágina Anterior", 
                                                   "§7Ir para a página " + page);
                inventory.setItem(48, prevButton);
            }
            
            // Botão de próxima página
            if (endIndex < items.size()) {
                ItemStack nextButton = createGuiItem(Material.PAPER, "§ePróxima Página", 
                                                   "§7Ir para a página " + (page + 2));
                inventory.setItem(50, nextButton);
            }
            
            // Informações da página
            ItemStack pageInfo = createGuiItem(Material.BOOK, "§ePágina " + (page + 1), 
                                             "§7Total de páginas: " + ((items.size() / 27) + 1));
            inventory.setItem(49, pageInfo);
        }
        
        // Abre o inventário
        player.openInventory(inventory);
        openInventories.put(player, inventory);
        currentShop.put(player, shop);
    }

    /**
     * Abre o menu de todas as lojas disponíveis
     * @param player Jogador
     */
    public void openBrowseShopsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, "§8Procurar Lojas");
        
        // Lista todas as lojas
        Map<String, PlayerShop> allShops = shopManager.getAllPlayerShops();
        List<PlayerShop> shopList = new ArrayList<>(allShops.values());
        
        int page = currentPage.getOrDefault(player, 0);
        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, shopList.size());
        
        if (shopList.isEmpty()) {
            ItemStack noShopsItem = createGuiItem(Material.BARRIER, "§cNenhuma loja disponível", 
                                                "§7Não há lojas para mostrar");
            inventory.setItem(22, noShopsItem);
        } else {
            int slot = 0;
            for (int i = startIndex; i < endIndex; i++) {
                PlayerShop shop = shopList.get(i);
                ItemStack shopItem = createGuiItem(Material.CHEST, "§a" + shop.getShopName(), 
                                                 "§7Dono: §f" + shop.getOwnerName(),
                                                 "§7Itens à venda: §f" + shop.getItems().size(),
                                                 "§7Clique para ver os itens");
                inventory.setItem(slot, shopItem);
                slot++;
                if (slot >= 45) break;
            }
        }
        
        // Botão de voltar
        ItemStack backButton = createGuiItem(Material.ARROW, "§cVoltar", 
                                           "§7Voltar ao menu principal");
        inventory.setItem(45, backButton);
        
        // Botões de navegação
        if (shopList.size() > 45) {
            // Botão de página anterior
            if (page > 0) {
                ItemStack prevButton = createGuiItem(Material.PAPER, "§ePágina Anterior", 
                                                   "§7Ir para a página " + page);
                inventory.setItem(48, prevButton);
            }
            
            // Botão de próxima página
            if (endIndex < shopList.size()) {
                ItemStack nextButton = createGuiItem(Material.PAPER, "§ePróxima Página", 
                                                   "§7Ir para a página " + (page + 2));
                inventory.setItem(50, nextButton);
            }
            
            // Informações da página
            ItemStack pageInfo = createGuiItem(Material.BOOK, "§ePágina " + (page + 1), 
                                             "§7Total de páginas: " + ((shopList.size() / 45) + 1));
            inventory.setItem(49, pageInfo);
        }
        
        // Abre o inventário
        player.openInventory(inventory);
        openInventories.put(player, inventory);
    }

    /**
     * Abre o menu de uma loja para compra
     * @param player Jogador
     * @param shop Loja
     */
    public void openShopBuyMenu(Player player, PlayerShop shop) {
        Inventory inventory = Bukkit.createInventory(null, 54, "§8Loja: " + shop.getShopName());
        
        // Informações da loja
        ItemStack infoItem = createGuiItem(Material.BOOK, "§aInformações da Loja", 
                                         "§7Nome: §f" + shop.getShopName(),
                                         "§7Dono: §f" + shop.getOwnerName(),
                                         "§7Itens à venda: §f" + shop.getItems().size());
        inventory.setItem(4, infoItem);
        
        // Lista os itens da loja
        List<PlayerShopItem> items = shop.getItems();
        int page = currentPage.getOrDefault(player, 0);
        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, items.size());
        
        if (items.isEmpty()) {
            ItemStack noItemsItem = createGuiItem(Material.BARRIER, "§cNenhum item à venda", 
                                                "§7Esta loja não tem itens à venda");
            inventory.setItem(22, noItemsItem);
        } else {
            int slot = 9;
            for (int i = startIndex; i < endIndex; i++) {
                PlayerShopItem item = items.get(i);
                if (item.isAvailable()) {
                    ItemStack displayItem = item.createDisplayItem();
                    inventory.setItem(slot, displayItem);
                    slot++;
                    if ((slot % 9) == 0) {
                        slot += 1;
                    }
                    if (slot >= 45) break;
                }
            }
        }
        
        // Botão de voltar
        ItemStack backButton = createGuiItem(Material.ARROW, "§cVoltar", 
                                           "§7Voltar à lista de lojas");
        inventory.setItem(45, backButton);
        
        // Botões de navegação
        if (items.size() > 36) {
            // Botão de página anterior
            if (page > 0) {
                ItemStack prevButton = createGuiItem(Material.PAPER, "§ePágina Anterior", 
                                                   "§7Ir para a página " + page);
                inventory.setItem(48, prevButton);
            }
            
            // Botão de próxima página
            if (endIndex < items.size()) {
                ItemStack nextButton = createGuiItem(Material.PAPER, "§ePróxima Página", 
                                                   "§7Ir para a página " + (page + 2));
                inventory.setItem(50, nextButton);
            }
            
            // Informações da página
            ItemStack pageInfo = createGuiItem(Material.BOOK, "§ePágina " + (page + 1), 
                                             "§7Total de páginas: " + ((items.size() / 36) + 1));
            inventory.setItem(49, pageInfo);
        }
        
        // Abre o inventário
        player.openInventory(inventory);
        openInventories.put(player, inventory);
        currentShop.put(player, shop);
    }

    /**
     * Abre o diálogo de confirmação para adicionar um item à loja
     * @param player Jogador
     * @param shop Loja
     * @param itemStack Item a ser adicionado
     */
    public void openAddItemDialog(Player player, PlayerShop shop, ItemStack itemStack) {
        Inventory inventory = Bukkit.createInventory(null, 36, "§8Adicionar Item à Loja");
        
        // Item a ser adicionado
        inventory.setItem(13, itemStack);
        
        // Botão de preço baixo com preço fixo
        double basePrice = 10.0;
        ItemStack lowPriceFixedButton = createGuiItem(Material.GOLD_NUGGET, "§aPreço Baixo (Fixo)", 
                                               "§7Clique para definir o preço como",
                                               "§f" + plugin.getEconomyProvider().format(basePrice),
                                               "§7Preço fixo: §aSim");
        inventory.setItem(10, lowPriceFixedButton);
        
        // Botão de preço médio com preço fixo
        ItemStack mediumPriceFixedButton = createGuiItem(Material.GOLD_INGOT, "§aPreço Médio (Fixo)", 
                                                  "§7Clique para definir o preço como",
                                                  "§f" + plugin.getEconomyProvider().format(basePrice * 5),
                                                  "§7Preço fixo: §aSim");
        inventory.setItem(11, mediumPriceFixedButton);
        
        // Botão de preço alto com preço fixo
        ItemStack highPriceFixedButton = createGuiItem(Material.GOLD_BLOCK, "§aPreço Alto (Fixo)", 
                                                "§7Clique para definir o preço como",
                                                "§f" + plugin.getEconomyProvider().format(basePrice * 20),
                                                "§7Preço fixo: §aSim");
        inventory.setItem(12, highPriceFixedButton);
        
        // Botão de preço baixo com preço dinâmico
        ItemStack lowPriceDynamicButton = createGuiItem(Material.EMERALD_BLOCK, "§aPreço Baixo (Dinâmico)", 
                                               "§7Clique para definir o preço como",
                                               "§f" + plugin.getEconomyProvider().format(basePrice),
                                               "§7Preço dinâmico: §aSim",
                                               "§7(Atualiza conforme oferta e demanda)");
        inventory.setItem(14, lowPriceDynamicButton);
        
        // Botão de preço médio com preço dinâmico
        ItemStack mediumPriceDynamicButton = createGuiItem(Material.EMERALD_BLOCK, "§aPreço Médio (Dinâmico)", 
                                                  "§7Clique para definir o preço como",
                                                  "§f" + plugin.getEconomyProvider().format(basePrice * 5),
                                                  "§7Preço dinâmico: §aSim",
                                                  "§7(Atualiza conforme oferta e demanda)");
        inventory.setItem(15, mediumPriceDynamicButton);
        
        // Botão de preço alto com preço dinâmico
        ItemStack highPriceDynamicButton = createGuiItem(Material.EMERALD_BLOCK, "§aPreço Alto (Dinâmico)", 
                                                "§7Clique para definir o preço como",
                                                "§f" + plugin.getEconomyProvider().format(basePrice * 20),
                                                "§7Preço dinâmico: §aSim",
                                                "§7(Atualiza conforme oferta e demanda)");
        inventory.setItem(16, highPriceDynamicButton);
        
        // Botão de preço personalizado com preço fixo
        ItemStack customPriceFixedButton = createGuiItem(Material.NAME_TAG, "§aPreço Personalizado (Fixo)", 
                                                  "§7Clique para definir um preço personalizado",
                                                  "§7Digite o preço no chat",
                                                  "§7Preço fixo: §aSim");
        inventory.setItem(19, customPriceFixedButton);
        
        // Botão de preço personalizado com preço dinâmico
        ItemStack customPriceDynamicButton = createGuiItem(Material.NAME_TAG, "§aPreço Personalizado (Dinâmico)", 
                                                  "§7Clique para definir um preço personalizado",
                                                  "§7Digite o preço no chat",
                                                  "§7Preço dinâmico: §aSim",
                                                  "§7(Atualiza conforme oferta e demanda)");
        inventory.setItem(25, customPriceDynamicButton);
        
        // Botão de cancelar
        ItemStack cancelButton = createGuiItem(Material.BARRIER, "§cCancelar", 
                                             "§7Cancelar adição de item");
        inventory.setItem(31, cancelButton);
        
        // Abre o inventário
        player.openInventory(inventory);
        openInventories.put(player, inventory);
        currentShop.put(player, shop);
        pendingItems.put(player.getUniqueId(), itemStack.clone());
    }

    /**
     * Abre o diálogo de confirmação para comprar um item
     * @param player Jogador
     * @param shop Loja
     * @param item Item a ser comprado
     */
    public void openBuyConfirmationDialog(Player player, PlayerShop shop, PlayerShopItem item) {
        Inventory inventory = Bukkit.createInventory(null, 27, "§8Confirmar Compra");
        
        // Item a ser comprado
        inventory.setItem(13, item.createDisplayItem());
        
        // Botão de confirmar
        ItemStack confirmButton = createGuiItem(Material.EMERALD_BLOCK, "§aConfirmar Compra", 
                                              "§7Clique para comprar este item por",
                                              "§f" + plugin.getEconomyProvider().format(item.getPrice()));
        inventory.setItem(11, confirmButton);
        
        // Botão de cancelar
        ItemStack cancelButton = createGuiItem(Material.BARRIER, "§cCancelar", 
                                             "§7Cancelar compra");
        inventory.setItem(15, cancelButton);
        
        // Abre o inventário
        player.openInventory(inventory);
        openInventories.put(player, inventory);
        currentShop.put(player, shop);
    }

    /**
     * Adiciona um item à loja com o preço e modo de precificação especificados
     * @param player Jogador
     * @param shop Loja
     * @param itemStack Item a ser adicionado
     * @param price Preço do item
     * @param dynamicPrice Se o preço deve ser atualizado conforme oferta e demanda
     */
    public void addItemToShop(Player player, PlayerShop shop, ItemStack itemStack, double price, boolean dynamicPrice) {
        // Verifica se o jogador é o dono da loja
        if (!shop.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage("§cVocê não é o dono desta loja.");
            return;
        }
        
        // Verifica se o preço é válido
        if (price <= 0) {
            player.sendMessage("§cO preço deve ser maior que zero.");
            return;
        }
        
        // Cria o item da loja
        PlayerShopItem shopItem = new PlayerShopItem(plugin, itemStack, price, dynamicPrice);
        
        // Adiciona o item à loja
        if (!shop.addItem(shopItem)) {
            player.sendMessage("§cA loja já atingiu o limite de itens.");
            return;
        }
        
        // Remove o item do inventário do jogador
        player.getInventory().removeItem(itemStack);
        
        // Salva a loja no banco de dados
        shop.saveAsync();
        
        // Mensagem de confirmação
        String priceType = dynamicPrice ? "dinâmico" : "fixo";
        player.sendMessage("§aItem adicionado à loja com sucesso com preço " + priceType + ".");
        
        // Abre o menu de gerenciamento
        openShopManagementMenu(player, shop);
    }

    /**
     * Cria um item para a interface gráfica
     * @param material Material do item
     * @param name Nome do item
     * @param lore Descrição do item
     * @return ItemStack configurado
     */
    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        
        List<String> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(line);
        }
        meta.setLore(loreList);
        
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Verifica se um inventário pertence a esta GUI
     * @param title Título do inventário
     * @return true se o inventário pertence a esta GUI
     */
    public boolean isPlayerShopInventory(String title) {
        return title.startsWith("§8Lojas de Jogadores") ||
               title.startsWith("§8Gerenciar Loja:") ||
               title.startsWith("§8Procurar Lojas") ||
               title.startsWith("§8Loja:") ||
               title.startsWith("§8Adicionar Item à Loja") ||
               title.startsWith("§8Confirmar Compra");
    }

    /**
     * Obtém o tipo de inventário
     * @param title Título do inventário
     * @return Tipo de inventário
     */
    public String getInventoryType(String title) {
        if (title.equals("§8Lojas de Jogadores")) {
            return "main";
        } else if (title.startsWith("§8Gerenciar Loja:")) {
            return "manage";
        } else if (title.equals("§8Procurar Lojas")) {
            return "browse";
        } else if (title.startsWith("§8Loja:")) {
            return "shop";
        } else if (title.equals("§8Adicionar Item à Loja")) {
            return "add_item";
        } else if (title.equals("§8Confirmar Compra")) {
            return "confirm_buy";
        }
        return "";
    }

    /**
     * Obtém a loja atual de um jogador
     * @param player Jogador
     * @return Loja atual
     */
    public PlayerShop getCurrentShop(Player player) {
        return currentShop.get(player);
    }

    /**
     * Define a página atual de um jogador
     * @param player Jogador
     * @param page Página
     */
    public void setCurrentPage(Player player, int page) {
        currentPage.put(player, page);
    }

    /**
     * Obtém a página atual de um jogador
     * @param player Jogador
     * @return Página atual
     */
    public int getCurrentPage(Player player) {
        return currentPage.getOrDefault(player, 0);
    }

    /**
     * Remove um jogador das listas de controle
     * @param player Jogador
     */
    public void removePlayer(Player player) {
        openInventories.remove(player);
        currentShop.remove(player);
        currentPage.remove(player);
        pendingItems.remove(player.getUniqueId());
        pendingDynamicPrice.remove(player.getUniqueId());
    }
    
    /**
     * Obtém o item pendente de um jogador
     * @param player Jogador
     * @return Item pendente
     */
    public ItemStack getPendingItem(Player player) {
        return pendingItems.get(player.getUniqueId());
    }
    
    /**
     * Define se o preço pendente é dinâmico
     * @param player Jogador
     * @param isDynamic Se o preço é dinâmico
     */
    public void setPendingDynamicPrice(Player player, boolean isDynamic) {
        pendingDynamicPrice.put(player.getUniqueId(), isDynamic);
    }
    
    /**
     * Verifica se o preço pendente é dinâmico
     * @param player Jogador
     * @return true se o preço for dinâmico
     */
    public boolean isPendingDynamicPrice(Player player) {
        return pendingDynamicPrice.getOrDefault(player.getUniqueId(), false);
    }
}
