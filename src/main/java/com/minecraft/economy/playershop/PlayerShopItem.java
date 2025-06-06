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
        this.plugin = plugin;
        this.id = UUID.randomUUID();
        this.itemStack = itemStack.clone();
        this.price = price;
        this.available = true;
        this.createdAt = System.currentTimeMillis();
        this.dynamicPrice = dynamicPrice;
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
        
        // Carrega o item
        String itemId = doc.getString("item_id");
        this.itemStack = createItemStackFromId(itemId);
    }

    /**
     * Cria um ItemStack a partir de um ID
     * @param itemId ID do item
     * @return ItemStack criado
     */
    private ItemStack createItemStackFromId(String itemId) {
        return ModItemUtils.getModItem(itemId);
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
     * @return ItemStack para exibição
     */
    public ItemStack createDisplayItem() {
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
        
        if (!available) {
            lore.add("§cItem vendido");
        }
        
        meta.setLore(lore);
        displayItem.setItemMeta(meta);
        
        return displayItem;
    }

    /**
     * Cria um ItemStack para o inventário do jogador
     * @return ItemStack para o inventário
     */
    public ItemStack createItemStack() {
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
}
