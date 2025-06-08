package com.minecraft.economy.repository;

import com.minecraft.economy.api.infrastructure.AsyncExecutor;
import com.minecraft.economy.api.model.Transaction;
import com.minecraft.economy.api.repository.TransactionRepository;
import com.minecraft.economy.infrastructure.MongoDBConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementação de TransactionRepository usando MongoDB
 */
public class MongoTransactionRepository implements TransactionRepository {

    private final MongoDBConnection dbConnection;
    private final AsyncExecutor asyncExecutor;
    private final Logger logger;
    
    private static final String COLLECTION_NAME = "transactions";
    
    /**
     * Construtor
     * @param dbConnection Conexão com o MongoDB
     * @param asyncExecutor Executor assíncrono
     * @param logger Logger
     */
    public MongoTransactionRepository(
            MongoDBConnection dbConnection,
            AsyncExecutor asyncExecutor,
            Logger logger) {
        this.dbConnection = dbConnection;
        this.asyncExecutor = asyncExecutor;
        this.logger = logger;
    }
    
    /**
     * Obtém a coleção de transações
     * @return Coleção de transações
     */
    private MongoCollection<Document> getCollection() {
        return dbConnection.getDatabase().getCollection(COLLECTION_NAME);
    }

    @Override
    public CompletableFuture<Boolean> recordTransaction(Transaction transaction) {
        return asyncExecutor.supplyAsync(() -> {
            try {
                if (!dbConnection.ensureConnected()) {
                    logger.warning("Falha ao registrar transação: Sem conexão com o banco de dados");
                    return false;
                }
                
                Document transactionDoc = new Document()
                        .append("player_uuid", transaction.getPlayerId().toString())
                        .append("type", transaction.getType().name())
                        .append("amount", transaction.getAmount())
                        .append("reason", transaction.getReason())
                        .append("timestamp", transaction.getTimestamp());
                
                if (transaction.getTargetId() != null) {
                    transactionDoc.append("target_uuid", transaction.getTargetId().toString());
                }
                
                getCollection().insertOne(transactionDoc);
                
                return true;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erro ao registrar transação: " + e.getMessage(), e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<List<Transaction>> getPlayerTransactions(UUID playerId, int limit) {
        return asyncExecutor.supplyAsync(() -> {
            try {
                if (!dbConnection.ensureConnected()) {
                    logger.warning("Falha ao obter transações do jogador: Sem conexão com o banco de dados");
                    return new ArrayList<>();
                }
                
                List<Transaction> transactions = new ArrayList<>();
                
                getCollection().find(Filters.eq("player_uuid", playerId.toString()))
                    .sort(Sorts.descending("timestamp"))
                    .limit(limit)
                    .forEach(doc -> {
                        transactions.add(documentToTransaction(doc));
                    });
                
                return transactions;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erro ao obter transações do jogador: " + e.getMessage(), e);
                return new ArrayList<>();
            }
        });
    }

    @Override
    public CompletableFuture<List<Transaction>> getRecentTransactions(int limit) {
        return asyncExecutor.supplyAsync(() -> {
            try {
                if (!dbConnection.ensureConnected()) {
                    logger.warning("Falha ao obter transações recentes: Sem conexão com o banco de dados");
                    return new ArrayList<>();
                }
                
                List<Transaction> transactions = new ArrayList<>();
                
                getCollection().find()
                    .sort(Sorts.descending("timestamp"))
                    .limit(limit)
                    .forEach(doc -> {
                        transactions.add(documentToTransaction(doc));
                    });
                
                return transactions;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erro ao obter transações recentes: " + e.getMessage(), e);
                return new ArrayList<>();
            }
        });
    }
    
    /**
     * Converte um documento do MongoDB para uma transação
     * @param doc Documento do MongoDB
     * @return Transação
     */
    private Transaction documentToTransaction(Document doc) {
        UUID playerId = UUID.fromString(doc.getString("player_uuid"));
        UUID targetId = doc.containsKey("target_uuid") ? UUID.fromString(doc.getString("target_uuid")) : null;
        Transaction.Type type = Transaction.Type.valueOf(doc.getString("type"));
        double amount = doc.getDouble("amount");
        String reason = doc.getString("reason");
        long timestamp = doc.getLong("timestamp");
        
        return new Transaction(playerId, targetId, type, amount, reason, timestamp);
    }
}
