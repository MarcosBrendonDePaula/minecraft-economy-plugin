package com.minecraft.economy.playershop;

import com.minecraft.economy.core.EconomyPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Representa uma loja de jogador
 */
public class PlayerShop {

    private final ObjectId id;
    private final UUID ownerUUID;
    private String ownerName;
    private String shopName;
    private Location location;
    private final List<PlayerShopItem> items;
    private double balance;
    private final Date creationDate;
    private boolean isOpen;
    private final EconomyPlugin plugin;

    /**
     * Construtor para criar uma nova loja
     * @param plugin Instância do plugin
     * @param ownerUUID UUID do dono da loja
     * @param ownerName Nome do dono da loja
     * @param shopName Nome da loja
     * @param location Localização da loja (opcional)
     */
    public PlayerShop(EconomyPlugin plugin, UUID ownerUUID, String ownerName, String shopName, Location location) {
        this.plugin = plugin;
        this.id = new ObjectId();
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.shopName = shopName;
        this.location = location;
        this.items = new ArrayList<>();
        this.balance = 0.0;
        this.creationDate = new Date();
        this.isOpen = true;
    }

    /**
     * Construtor para carregar uma loja existente do banco de dados
     * @param plugin Instância do plugin
     * @param document Documento do MongoDB
     */
    public PlayerShop(EconomyPlugin plugin, Document document) {
        this.plugin = plugin;
        this.id = document.getObjectId("_id");
        this.ownerUUID = UUID.fromString(document.getString("owner_uuid"));
        this.ownerName = document.getString("owner_name");
        this.shopName = document.getString("shop_name");
        
        // Carrega a localização se existir
        if (document.containsKey("location")) {
            Document locDoc = document.get("location", Document.class);
            String worldName = locDoc.getString("world");
            double x = locDoc.getDouble("x");
            double y = locDoc.getDouble("y");
            double z = locDoc.getDouble("z");
            float yaw = locDoc.getDouble("yaw").floatValue();
            float pitch = locDoc.getDouble("pitch").floatValue();
            
            this.location = new Location(plugin.getServer().getWorld(worldName), x, y, z, yaw, pitch);
        } else {
            this.location = null;
        }
        
        this.balance = document.getDouble("balance");
        this.creationDate = document.getDate("creation_date");
        this.isOpen = document.getBoolean("is_open", true);
        
        // Carrega os itens
        this.items = new ArrayList<>();
        List<Document> itemDocs = document.getList("items", Document.class);
        if (itemDocs != null) {
            for (Document itemDoc : itemDocs) {
                PlayerShopItem item = new PlayerShopItem(plugin, itemDoc);
                this.items.add(item);
            }
        }
    }

    /**
     * Converte a loja para um documento do MongoDB
     * @return Documento do MongoDB
     */
    public Document toDocument() {
        Document doc = new Document();
        doc.append("_id", id);
        doc.append("owner_uuid", ownerUUID.toString());
        doc.append("owner_name", ownerName);
        doc.append("shop_name", shopName);
        
        // Salva a localização se existir
        if (location != null) {
            Document locDoc = new Document();
            locDoc.append("world", location.getWorld().getName());
            locDoc.append("x", location.getX());
            locDoc.append("y", location.getY());
            locDoc.append("z", location.getZ());
            locDoc.append("yaw", (double) location.getYaw());
            locDoc.append("pitch", (double) location.getPitch());
            
            doc.append("location", locDoc);
        }
        
        doc.append("balance", balance);
        doc.append("creation_date", creationDate);
        doc.append("is_open", isOpen);
        
        // Salva os itens
        List<Document> itemDocs = new ArrayList<>();
        for (PlayerShopItem item : items) {
            itemDocs.add(item.toDocument());
        }
        doc.append("items", itemDocs);
        
        return doc;
    }

    /**
     * Adiciona um item à loja
     * @param item Item a ser adicionado
     * @return true se o item foi adicionado com sucesso
     */
    public boolean addItem(PlayerShopItem item) {
        // Verifica se já atingiu o limite de itens
        int maxItems = plugin.getConfig().getInt("playershop.max_items_per_shop", 54);
        if (items.size() >= maxItems) {
            return false;
        }
        
        items.add(item);
        return true;
    }

    /**
     * Remove um item da loja
     * @param itemId ID do item a ser removido
     * @return O item removido, ou null se não foi encontrado
     */
    public PlayerShopItem removeItem(String itemId) {
        for (int i = 0; i < items.size(); i++) {
            PlayerShopItem item = items.get(i);
            if (item.getId().toString().equals(itemId)) {
                items.remove(i);
                return item;
            }
        }
        return null;
    }

    /**
     * Obtém um item da loja pelo ID
     * @param itemId ID do item
     * @return O item, ou null se não foi encontrado
     */
    public PlayerShopItem getItem(String itemId) {
        for (PlayerShopItem item : items) {
            if (item.getId().toString().equals(itemId)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Processa a compra de um item
     * @param buyer Jogador que está comprando
     * @param itemId ID do item
     * @return true se a compra foi bem-sucedida
     */
    public boolean buyItem(Player buyer, String itemId) {
        PlayerShopItem item = getItem(itemId);
        if (item == null || !item.isAvailable()) {
            return false;
        }
        
        // Verifica se o jogador tem dinheiro suficiente
        double price = item.getPrice();
        UUID buyerUUID = buyer.getUniqueId();
        
        return plugin.getMongoDBManager().hasBalance(buyerUUID, price).thenCompose(hasMoney -> {
            if (!hasMoney) {
                buyer.sendMessage("§cVocê não tem dinheiro suficiente para comprar este item.");
                return java.util.concurrent.CompletableFuture.completedFuture(false);
            }
            
            // Cobra o jogador
            return plugin.getMongoDBManager().withdraw(buyerUUID, price, "Compra de item na loja " + shopName)
                .thenCompose(success -> {
                    if (!success) {
                        buyer.sendMessage("§cOcorreu um erro ao processar o pagamento.");
                        return java.util.concurrent.CompletableFuture.completedFuture(false);
                    }
                    
                    // Adiciona o dinheiro ao saldo da loja
                    balance += price;
                    
                    // Marca o item como vendido
                    item.setAvailable(false);
                    
                    // Dá o item ao jogador
                    buyer.getInventory().addItem(item.createItemStack());
                    buyer.sendMessage("§aVocê comprou §f" + item.getDisplayName() + " §apor §f" + 
                                     plugin.getEconomyProvider().format(price) + "§a.");
                    
                    // Notifica o dono da loja se estiver online
                    Player owner = plugin.getServer().getPlayer(ownerUUID);
                    if (owner != null && owner.isOnline()) {
                        owner.sendMessage("§a" + buyer.getName() + " comprou §f" + item.getDisplayName() + 
                                         " §ada sua loja por §f" + plugin.getEconomyProvider().format(price) + "§a.");
                    }
                    
                    // Salva a loja no banco de dados
                    saveAsync();
                    
                    return java.util.concurrent.CompletableFuture.completedFuture(true);
                });
        }).join();
    }

    /**
     * Saca o dinheiro acumulado na loja
     * @param player Jogador que está sacando (deve ser o dono)
     * @return true se o saque foi bem-sucedido
     */
    public boolean withdrawBalance(Player player) {
        if (!player.getUniqueId().equals(ownerUUID)) {
            return false;
        }
        
        if (balance <= 0) {
            player.sendMessage("§cSua loja não tem saldo para sacar.");
            return false;
        }
        
        double amount = balance;
        balance = 0;
        
        plugin.getMongoDBManager().deposit(ownerUUID, amount, "Saque da loja " + shopName)
            .thenAccept(success -> {
                if (success) {
                    player.sendMessage("§aVocê sacou §f" + plugin.getEconomyProvider().format(amount) + 
                                      " §ada sua loja.");
                    
                    // Salva a loja no banco de dados
                    saveAsync();
                } else {
                    player.sendMessage("§cOcorreu um erro ao processar o saque.");
                    balance = amount; // Restaura o saldo
                }
            });
        
        return true;
    }

    /**
     * Salva a loja no banco de dados de forma assíncrona
     */
    public void saveAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Document doc = toDocument();
                plugin.getMongoDBManager().getDatabase().getCollection("player_shops")
                    .replaceOne(new Document("_id", id), doc, new com.mongodb.client.model.ReplaceOptions().upsert(true));
            } catch (Exception e) {
                plugin.getLogger().severe("Erro ao salvar loja de jogador: " + e.getMessage());
            }
        });
    }

    /**
     * Deleta a loja do banco de dados
     */
    public void delete() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getMongoDBManager().getDatabase().getCollection("player_shops")
                    .deleteOne(new Document("_id", id));
            } catch (Exception e) {
                plugin.getLogger().severe("Erro ao deletar loja de jogador: " + e.getMessage());
            }
        });
    }

    // Getters e Setters

    public ObjectId getId() {
        return id;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public List<PlayerShopItem> getItems() {
        return new ArrayList<>(items);
    }

    public double getBalance() {
        return balance;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        isOpen = open;
    }
}
