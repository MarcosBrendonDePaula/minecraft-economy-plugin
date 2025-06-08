package com.minecraft.economy.playershop;

import com.minecraft.economy.core.EconomyPlugin;
import com.minecraft.economy.utils.ModItemUtils;
import org.bson.Document;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Representa um item à venda em uma loja de jogador
 */
public class PlayerShopItem {

    private final EconomyPlugin plugin;
    private final UUID id;
    private final ItemStack itemStack;
    private double price;
    private boolean available;
    private final long createdAt;
    private boolean dynamicPrice; // Indica se o preço é dinâmico (baseado em oferta e demanda)
    private int quantity; // Quantidade de itens em estoque

    /**
     * Construtor para criar um novo item
     * @param plugin Instância do plugin
     * @param itemStack Item a ser vendido
     * @param price Preço do item
     */
    public PlayerShopItem(EconomyPlugin plugin, ItemStack itemStack, double price) {
        this(plugin, itemStack, price, false);
    }
    
    /**
     * Construtor para criar um novo item com opção de preço dinâmico
     * @param plugin Instância do plugin
     * @param itemStack Item a ser vendido
     * @param price Preço do item
     * @param dynamicPrice Se o preço deve ser atualizado conforme oferta e demanda
     */
    public PlayerShopItem(EconomyPlugin plugin, ItemStack itemStack, double price, boolean dynamicPrice) {
        this(plugin, itemStack, price, dynamicPrice, 1);
    }
    
    /**
     * Construtor para criar um novo item com opção de preço dinâmico e quantidade
     * @param plugin Instância do plugin
     * @param itemStack Item a ser vendido
     * @param price Preço do item
     * @param dynamicPrice Se o preço deve ser atualizado conforme oferta e demanda
     * @param quantity Quantidade inicial do item
     */
    public PlayerShopItem(EconomyPlugin plugin, ItemStack itemStack, double price, boolean dynamicPrice, int quantity) {
        this.plugin = plugin;
        this.id = UUID.randomUUID();
        this.itemStack = itemStack.clone();
        this.price = price;
        this.available = true;
        this.createdAt = System.currentTimeMillis();
        this.dynamicPrice = dynamicPrice;
        this.quantity = quantity;
    }

    /**
     * Construtor para carregar um item do banco de dados
     * @param plugin Instância do plugin
     * @param doc Documento do MongoDB
     */
    public PlayerShopItem(EconomyPlugin plugin, Document doc) {
        this.plugin = plugin;
        this.id = UUID.fromString(doc.getString("id"));
        this.price = doc.getDouble("price");
        this.available = doc.getBoolean("available", true);
        this.createdAt = doc.getLong("created_at");
        this.dynamicPrice = doc.getBoolean("dynamic_price", false);
        this.quantity = doc.getInteger("quantity", 1); // Carrega a quantidade, padrão 1
        
        // Carrega o item
        String itemId = doc.getString("item_id");
        this.itemStack = createItemStackFromId(itemId);
    }

    /**
     * Cria um ItemStack a partir de um ID
     * @param itemId ID do item
     * @return ItemStack criado ou null se o item não for encontrado
     */
    private ItemStack createItemStackFromId(String itemId) {
        ItemStack result = ModItemUtils.getModItem(itemId);
        
        // Se o item não for encontrado, registra um aviso e retorna null
        if (result == null) {
            plugin.getLogger().warning("Item não encontrado com ID: " + itemId + ". O item será removido da loja.");
        }
        
        return result;
    }

    /**
     * Converte o item para um documento do MongoDB
     * @return Documento do MongoDB
     */
    public Document toDocument() {
        Document doc = new Document();
        doc.append("id", id.toString());
        doc.append("item_id", ModItemUtils.getItemId(itemStack));
        doc.append("price", price);
        doc.append("available", available);
        doc.append("created_at", createdAt);
        doc.append("dynamic_price", dynamicPrice);
        doc.append("quantity", quantity); // Salva a quantidade
        return doc;
    }

    /**
     * Obtém o nome de exibição do item
     * @return Nome de exibição do item
     */
    public String getDisplayName() {
        return ModItemUtils.getItemName(itemStack);
    }

    /**
     * Cria um ItemStack para exibição na interface
     * @return ItemStack para exibição ou null se o item não existir
     */
    public ItemStack createDisplayItem() {
        // Se o itemStack for nulo, retorna null para indicar que o item deve ser removido
        if (itemStack == null) {
            return null;
        }
        
        // Procede normalmente se o itemStack não for nulo
        ItemStack displayItem = itemStack.clone();
        ItemMeta meta = displayItem.getItemMeta();
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Preço: §f" + plugin.getEconomyProvider().format(price));
        
        if (dynamicPrice) {
            lore.add("§7Preço dinâmico: §aSim");
            lore.add("§7(Atualiza conforme oferta e demanda)");
        } else {
            lore.add("§7Preço fixo: §aSim");
        }
        
        // Adiciona informações de estoque
        lore.add("§7Estoque: §f" + quantity);
        
        if (!available) {
            lore.add("§cItem vendido");
        }
        
        meta.setLore(lore);
        displayItem.setItemMeta(meta);
        
        return displayItem;
    }

    /**
     * Cria um ItemStack para o inventário do jogador
     * @return ItemStack para o inventário ou null se o item não existir
     */
    public ItemStack createItemStack() {
        // Se o itemStack for nulo, retorna null para indicar que o item deve ser removido
        if (itemStack == null) {
            return null;
        }
        
        return itemStack.clone();
    }

    /**
     * Obtém o ID do item
     * @return ID do item
     */
    public UUID getId() {
        return id;
    }

    /**
     * Obtém o ItemStack do item
     * @return ItemStack do item
     */
    public ItemStack getItemStack() {
        return itemStack;
    }

    /**
     * Obtém o preço do item
     * @return Preço do item
     */
    public double getPrice() {
        return price;
    }

    /**
     * Define o preço do item
     * @param price Novo preço
     */
    public void setPrice(double price) {
        this.price = price;
    }

    /**
     * Verifica se o item está disponível
     * @return true se o item estiver disponível
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Define se o item está disponível
     * @param available Disponibilidade do item
     */
    public void setAvailable(boolean available) {
        this.available = available;
    }

    /**
     * Obtém a data de criação do item
     * @return Data de criação
     */
    public long getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Verifica se o preço do item é dinâmico
     * @return true se o preço for dinâmico
     */
    public boolean isDynamicPrice() {
        return dynamicPrice;
    }
    
    /**
     * Define se o preço do item é dinâmico
     * @param dynamicPrice Se o preço deve ser atualizado conforme oferta e demanda
     */
    public void setDynamicPrice(boolean dynamicPrice) {
        this.dynamicPrice = dynamicPrice;
    }
    
    /**
     * Atualiza o preço do item com base na oferta e demanda
     * @param marketFactor Fator de mercado (> 1 para aumentar, < 1 para diminuir)
     */
    public void updateDynamicPrice(double marketFactor) {
        if (dynamicPrice) {
            // Limita a variação para evitar preços extremos
            double maxFactor = 2.0; // Máximo dobro do preço
            double minFactor = 0.5; // Mínimo metade do preço
            
            if (marketFactor > maxFactor) marketFactor = maxFactor;
            if (marketFactor < minFactor) marketFactor = minFactor;
            
            price = price * marketFactor;
            
            // Garante um preço mínimo
            double minPrice = 1.0;
            if (price < minPrice) price = minPrice;
        }
    }
    
    /**
     * Obtém a quantidade de itens em estoque
     * @return Quantidade de itens
     */
    public int getQuantity() {
        return quantity;
    }
    
    /**
     * Define a quantidade de itens em estoque
     * @param quantity Nova quantidade
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    /**
     * Adiciona itens ao estoque
     * @param amount Quantidade a adicionar
     * @return Quantidade atual após a adição
     */
    public int addStock(int amount) {
        if (amount <= 0) {
            return quantity;
        }
        
        quantity += amount;
        return quantity;
    }
    
    /**
     * Remove itens do estoque
     * @param amount Quantidade a remover
     * @return Quantidade atual após a remoção ou -1 se não houver estoque suficiente
     */
    public int removeStock(int amount) {
        if (amount <= 0) {
            return quantity;
        }
        
        if (quantity < amount) {
            return -1; // Estoque insuficiente
        }
        
        quantity -= amount;
        
        // Se o estoque chegar a zero, o item não está mais disponível
        if (quantity == 0) {
            available = false;
        }
        
        return quantity;
    }
    
    /**
     * Verifica se há estoque suficiente
     * @param amount Quantidade a verificar
     * @return true se houver estoque suficiente
     */
    public boolean hasStock(int amount) {
        return quantity >= amount;
    }
}
