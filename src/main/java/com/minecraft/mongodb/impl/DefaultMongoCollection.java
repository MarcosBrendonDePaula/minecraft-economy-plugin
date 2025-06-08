package com.minecraft.mongodb.impl;

import com.minecraft.mongodb.api.MongoCollection;
import com.minecraft.mongodb.impl.mapper.DocumentMapper;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementação padrão do MongoCollection
 */
public class DefaultMongoCollection implements MongoCollection {
    
    private final DefaultMongoClient client;
    private final com.mongodb.client.MongoCollection<Document> mongoCollection;
    private final String name;
    private final Logger logger = Logger.getLogger(DefaultMongoCollection.class.getName());
    
    /**
     * Construtor
     * @param client Cliente MongoDB
     * @param mongoCollection Coleção MongoDB nativa
     * @param name Nome da coleção
     */
    public DefaultMongoCollection(DefaultMongoClient client, com.mongodb.client.MongoCollection<Document> mongoCollection, String name) {
        this.client = client;
        this.mongoCollection = mongoCollection;
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public MongoQuery find() {
        return new DefaultMongoQuery(this);
    }
    
    @Override
    public MongoUpdate update() {
        return new DefaultMongoUpdate(this);
    }
    
    @Override
    public MongoDelete delete() {
        return new DefaultMongoDelete(this);
    }
    
    @Override
    public boolean insert(Map<String, Object> document) {
        try {
            Document doc = new Document(document);
            InsertOneResult result = mongoCollection.insertOne(doc);
            return result.wasAcknowledged();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erro ao inserir documento: " + e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public CompletableFuture<Boolean> insertAsync(Map<String, Object> document) {
        return CompletableFuture.supplyAsync(() -> insert(document))
                .exceptionally(e -> {
                    logger.log(Level.WARNING, "Erro ao inserir documento de forma assíncrona: " + e.getMessage(), e);
                    return false;
                });
    }
    
    @Override
    public <T> boolean insert(T object) {
        try {
            Map<String, Object> docMap = DocumentMapper.toDocument(object);
            if (docMap == null) {
                return false;
            }
            
            Document doc = new Document(docMap);
            InsertOneResult result = mongoCollection.insertOne(doc);
            return result.wasAcknowledged();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erro ao inserir objeto: " + e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public <T> CompletableFuture<Boolean> insertAsync(T object) {
        return CompletableFuture.supplyAsync(() -> insert(object))
                .exceptionally(e -> {
                    logger.log(Level.WARNING, "Erro ao inserir objeto de forma assíncrona: " + e.getMessage(), e);
                    return false;
                });
    }
    
    @Override
    public int insertManyDocuments(List<Map<String, Object>> documents) {
        try {
            List<Document> docs = new ArrayList<>();
            for (Map<String, Object> document : documents) {
                docs.add(new Document(document));
            }
            InsertManyResult result = mongoCollection.insertMany(docs);
            return (int) result.getInsertedIds().size();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erro ao inserir vários documentos: " + e.getMessage(), e);
            return 0;
        }
    }
    
    @Override
    public CompletableFuture<Integer> insertManyDocumentsAsync(List<Map<String, Object>> documents) {
        return CompletableFuture.supplyAsync(() -> insertManyDocuments(documents))
                .exceptionally(e -> {
                    logger.log(Level.WARNING, "Erro ao inserir vários documentos de forma assíncrona: " + e.getMessage(), e);
                    return 0;
                });
    }
    
    @Override
    public <T> int insertManyObjects(List<T> objects) {
        try {
            List<Document> docs = new ArrayList<>();
            
            for (T object : objects) {
                Map<String, Object> docMap = DocumentMapper.toDocument(object);
                if (docMap != null) {
                    docs.add(new Document(docMap));
                }
            }
            
            if (docs.isEmpty()) {
                return 0;
            }
            
            InsertManyResult result = mongoCollection.insertMany(docs);
            return (int) result.getInsertedIds().size();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erro ao inserir vários objetos: " + e.getMessage(), e);
            return 0;
        }
    }
    
    @Override
    public <T> CompletableFuture<Integer> insertManyObjectsAsync(List<T> objects) {
        return CompletableFuture.supplyAsync(() -> insertManyObjects(objects))
                .exceptionally(e -> {
                    logger.log(Level.WARNING, "Erro ao inserir vários objetos de forma assíncrona: " + e.getMessage(), e);
                    return 0;
                });
    }
    
    @Override
    public long count() {
        try {
            return mongoCollection.countDocuments();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erro ao contar documentos: " + e.getMessage(), e);
            return 0;
        }
    }
    
    @Override
    public CompletableFuture<Long> countAsync() {
        return CompletableFuture.supplyAsync(this::count)
                .exceptionally(e -> {
                    logger.log(Level.WARNING, "Erro ao contar documentos de forma assíncrona: " + e.getMessage(), e);
                    return 0L;
                });
    }
    
    @Override
    public boolean isEmpty() {
        return count() == 0;
    }
    
    @Override
    public CompletableFuture<Boolean> isEmptyAsync() {
        return countAsync().thenApply(count -> count == 0);
    }
    
    @Override
    public long clear() {
        try {
            DeleteResult result = mongoCollection.deleteMany(new Document());
            return result.getDeletedCount();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erro ao limpar coleção: " + e.getMessage(), e);
            return 0;
        }
    }
    
    @Override
    public CompletableFuture<Long> clearAsync() {
        return CompletableFuture.supplyAsync(this::clear)
                .exceptionally(e -> {
                    logger.log(Level.WARNING, "Erro ao limpar coleção de forma assíncrona: " + e.getMessage(), e);
                    return 0L;
                });
    }
    
    /**
     * Obtém a coleção MongoDB nativa
     * @return Coleção MongoDB nativa
     */
    com.mongodb.client.MongoCollection<Document> getMongoCollection() {
        return mongoCollection;
    }
    
    /**
     * Implementação padrão do MongoQuery
     */
    private class DefaultMongoQuery implements MongoQuery {
        
        private final DefaultMongoCollection collection;
        private final List<Bson> filters = new ArrayList<>();
        private int limitValue = 0;
        private int skipValue = 0;
        private final List<Bson> sorts = new ArrayList<>();
        
        /**
         * Construtor
         * @param collection Coleção MongoDB
         */
        public DefaultMongoQuery(DefaultMongoCollection collection) {
            this.collection = collection;
        }
        
        @Override
        public Condition where(String field) {
            return new DefaultCondition(field);
        }
        
        @Override
        public MongoQuery limit(int limit) {
            this.limitValue = limit;
            return this;
        }
        
        @Override
        public MongoQuery skip(int skip) {
            this.skipValue = skip;
            return this;
        }
        
        @Override
        public MongoQuery sort(String field, boolean ascending) {
            sorts.add(ascending ? com.mongodb.client.model.Sorts.ascending(field) : com.mongodb.client.model.Sorts.descending(field));
            return this;
        }
        
        @Override
        public Optional<Map<String, Object>> first() {
            try {
                com.mongodb.client.FindIterable<Document> findIterable = applyQuery(collection.getMongoCollection().find());
                Document doc = findIterable.first();
                if (doc == null) {
                    return Optional.empty();
                }
                
                Map<String, Object> result = new HashMap<>();
                for (String key : doc.keySet()) {
                    result.put(key, doc.get(key));
                }
                
                return Optional.of(result);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Erro ao obter primeiro documento: " + e.getMessage(), e);
                return Optional.empty();
            }
        }
        
        @Override
        public CompletableFuture<Optional<Map<String, Object>>> firstAsync() {
            return CompletableFuture.supplyAsync(this::first)
                    .exceptionally(e -> {
                        logger.log(Level.WARNING, "Erro ao obter primeiro documento de forma assíncrona: " + e.getMessage(), e);
                        return Optional.empty();
                    });
        }
        
        @Override
        public <T> Optional<T> first(Class<T> clazz) {
            try {
                com.mongodb.client.FindIterable<Document> findIterable = applyQuery(collection.getMongoCollection().find());
                Document doc = findIterable.first();
                
                if (doc == null) {
                    return Optional.empty();
                }
                
                Map<String, Object> map = new HashMap<>();
                for (String key : doc.keySet()) {
                    map.put(key, doc.get(key));
                }
                
                T object = DocumentMapper.fromDocument(map, clazz);
                return Optional.ofNullable(object);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Erro ao obter primeiro objeto: " + e.getMessage(), e);
                return Optional.empty();
            }
        }
        
        @Override
        public <T> CompletableFuture<Optional<T>> firstAsync(Class<T> clazz) {
            return CompletableFuture.supplyAsync(() -> first(clazz))
                    .exceptionally(e -> {
                        logger.log(Level.WARNING, "Erro ao obter primeiro objeto de forma assíncrona: " + e.getMessage(), e);
                        return Optional.empty();
                    });
        }
        
        @Override
        public List<Map<String, Object>> toList() {
            try {
                com.mongodb.client.FindIterable<Document> findIterable = applyQuery(collection.getMongoCollection().find());
                List<Map<String, Object>> result = new ArrayList<>();
                
                for (Document doc : findIterable) {
                    Map<String, Object> map = new HashMap<>();
                    for (String key : doc.keySet()) {
                        map.put(key, doc.get(key));
                    }
                    result.add(map);
                }
                
                return result;
            } catch (Exception e) {
                logger.log(Level.WARNING, "Erro ao obter lista de documentos: " + e.getMessage(), e);
                return new ArrayList<>();
            }
        }
        
        @Override
        public CompletableFuture<List<Map<String, Object>>> toListAsync() {
            return CompletableFuture.supplyAsync(this::toList)
                    .exceptionally(e -> {
                        logger.log(Level.WARNING, "Erro ao obter lista de documentos de forma assíncrona: " + e.getMessage(), e);
                        return new ArrayList<>();
                    });
        }
        
        @Override
        public <T> List<T> toList(Class<T> clazz) {
            try {
                com.mongodb.client.FindIterable<Document> findIterable = applyQuery(collection.getMongoCollection().find());
                List<T> result = new ArrayList<>();
                
                for (Document doc : findIterable) {
                    Map<String, Object> map = new HashMap<>();
                    for (String key : doc.keySet()) {
                        map.put(key, doc.get(key));
                    }
                    
                    T object = DocumentMapper.fromDocument(map, clazz);
                    if (object != null) {
                        result.add(object);
                    }
                }
                
                return result;
            } catch (Exception e) {
                logger.log(Level.WARNING, "Erro ao obter lista de objetos: " + e.getMessage(), e);
                return new ArrayList<>();
            }
        }
        
        @Override
        public <T> CompletableFuture<List<T>> toListAsync(Class<T> clazz) {
            return CompletableFuture.supplyAsync(() -> toList(clazz))
                    .exceptionally(e -> {
                        logger.log(Level.WARNING, "Erro ao obter lista de objetos de forma assíncrona: " + e.getMessage(), e);
                        return new ArrayList<>();
                    });
        }
        
        @Override
        public long count() {
            try {
                Bson filter = filters.isEmpty() ? new Document() : com.mongodb.client.model.Filters.and(filters);
                return collection.getMongoCollection().countDocuments(filter);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Erro ao contar documentos: " + e.getMessage(), e);
                return 0;
            }
        }
        
        @Override
        public CompletableFuture<Long> countAsync() {
            return CompletableFuture.supplyAsync(this::count)
                    .exceptionally(e -> {
                        logger.log(Level.WARNING, "Erro ao contar documentos de forma assíncrona: " + e.getMessage(), e);
                        return 0L;
                    });
        }
        
        @Override
        public boolean exists() {
            return count() > 0;
        }
        
        @Override
        public CompletableFuture<Boolean> existsAsync() {
            return countAsync().thenApply(count -> count > 0);
        }
        
        /**
         * Aplica a consulta à coleção
         * @param findIterable Iterável de busca
         * @return Iterável de busca com a consulta aplicada
         */
        private com.mongodb.client.FindIterable<Document> applyQuery(com.mongodb.client.FindIterable<Document> findIterable) {
            if (!filters.isEmpty()) {
                findIterable = findIterable.filter(com.mongodb.client.model.Filters.and(filters));
            }
            
            if (limitValue > 0) {
                findIterable = findIterable.limit(limitValue);
            }
            
            if (skipValue > 0) {
                findIterable = findIterable.skip(skipValue);
            }
            
            if (!sorts.isEmpty()) {
                findIterable = findIterable.sort(com.mongodb.client.model.Sorts.orderBy(sorts));
            }
            
            return findIterable;
        }
        
        /**
         * Implementação padrão do Condition para consultas
         */
        private class DefaultCondition implements Condition {
            
            private final String field;
            
            /**
             * Construtor
             * @param field Campo a ser comparado
             */
            public DefaultCondition(String field) {
                this.field = field;
            }
            
            @Override
            public MongoQuery isEqualTo(Object value) {
                filters.add(Filters.eq(field, value));
                return DefaultMongoQuery.this;
            }
            
            @Override
            public MongoQuery notEquals(Object value) {
                filters.add(Filters.ne(field, value));
                return DefaultMongoQuery.this;
            }
            
            @Override
            public MongoQuery greaterThan(Object value) {
                filters.add(Filters.gt(field, value));
                return DefaultMongoQuery.this;
            }
            
            @Override
            public MongoQuery greaterThanOrEquals(Object value) {
                filters.add(Filters.gte(field, value));
                return DefaultMongoQuery.this;
            }
            
            @Override
            public MongoQuery lessThan(Object value) {
                filters.add(Filters.lt(field, value));
                return DefaultMongoQuery.this;
            }
            
            @Override
            public MongoQuery lessThanOrEquals(Object value) {
                filters.add(Filters.lte(field, value));
                return DefaultMongoQuery.this;
            }
            
            @Override
            public MongoQuery contains(String value) {
                filters.add(Filters.regex(field, ".*" + value + ".*"));
                return DefaultMongoQuery.this;
            }
            
            @Override
            public MongoQuery startsWith(String value) {
                filters.add(Filters.regex(field, "^" + value + ".*"));
                return DefaultMongoQuery.this;
            }
            
            @Override
            public MongoQuery endsWith(String value) {
                filters.add(Filters.regex(field, ".*" + value + "$"));
                return DefaultMongoQuery.this;
            }
            
            @Override
            public MongoQuery in(List<?> values) {
                filters.add(Filters.in(field, values));
                return DefaultMongoQuery.this;
            }
            
            @Override
            public MongoQuery notIn(List<?> values) {
                filters.add(Filters.nin(field, values));
                return DefaultMongoQuery.this;
            }
            
            @Override
            public MongoQuery exists() {
                filters.add(Filters.exists(field));
                return DefaultMongoQuery.this;
            }
            
            @Override
            public MongoQuery notExists() {
                filters.add(Filters.exists(field, false));
                return DefaultMongoQuery.this;
            }
        }
    }
    
    /**
     * Implementação padrão do MongoUpdate
     */
    private class DefaultMongoUpdate implements MongoUpdate {
        
        private final DefaultMongoCollection collection;
        private final List<Bson> filters = new ArrayList<>();
        private final List<Bson> updates = new ArrayList<>();
        
        /**
         * Construtor
         * @param collection Coleção MongoDB
         */
        public DefaultMongoUpdate(DefaultMongoCollection collection) {
            this.collection = collection;
        }
        
        @Override
        public Condition where(String field) {
            return new DefaultCondition(field);
        }
        
        @Override
        public MongoUpdate set(String field, Object value) {
            updates.add(Updates.set(field, value));
            return this;
        }
        
        @Override
        public MongoUpdate increment(String field, Number value) {
            updates.add(Updates.inc(field, value));
            return this;
        }
        
        @Override
        public MongoUpdate decrement(String field, Number value) {
            updates.add(Updates.inc(field, -value.doubleValue()));
            return this;
        }
        
        @Override
        public MongoUpdate multiply(String field, Number value) {
            updates.add(Updates.mul(field, value));
            return this;
        }
        
        @Override
        public MongoUpdate divide(String field, Number value) {
            updates.add(Updates.mul(field, 1.0 / value.doubleValue()));
            return this;
        }
        
        @Override
        public MongoUpdate push(String field, Object value) {
            updates.add(Updates.push(field, value));
            return this;
        }
        
        @Override
        public MongoUpdate pull(String field, Object value) {
            updates.add(Updates.pull(field, value));
            return this;
        }
        
        @Override
        public MongoUpdate unset(String field) {
            updates.add(Updates.unset(field));
            return this;
        }
        
        @Override
        public int execute() {
            try {
                Bson filter = filters.isEmpty() ? new Document() : com.mongodb.client.model.Filters.and(filters);
                Bson update = Updates.combine(updates);
                UpdateResult result = collection.getMongoCollection().updateMany(filter, update);
                return (int) result.getModifiedCount();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Erro ao executar atualização: " + e.getMessage(), e);
                return 0;
            }
        }
        
        @Override
        public CompletableFuture<Integer> executeAsync() {
            return CompletableFuture.supplyAsync(this::execute)
                    .exceptionally(e -> {
                        logger.log(Level.WARNING, "Erro ao executar atualização de forma assíncrona: " + e.getMessage(), e);
                        return 0;
                    });
        }
        
        /**
         * Implementação padrão do Condition para atualizações
         */
        private class DefaultCondition implements Condition {
            
            private final String field;
            
            /**
             * Construtor
             * @param field Campo a ser comparado
             */
            public DefaultCondition(String field) {
                this.field = field;
            }
            
            @Override
            public MongoUpdate isEqualTo(Object value) {
                filters.add(Filters.eq(field, value));
                return DefaultMongoUpdate.this;
            }
            
            @Override
            public MongoUpdate notEquals(Object value) {
                filters.add(Filters.ne(field, value));
                return DefaultMongoUpdate.this;
            }
            
            @Override
            public MongoUpdate greaterThan(Object value) {
                filters.add(Filters.gt(field, value));
                return DefaultMongoUpdate.this;
            }
            
            @Override
            public MongoUpdate greaterThanOrEquals(Object value) {
                filters.add(Filters.gte(field, value));
                return DefaultMongoUpdate.this;
            }
            
            @Override
            public MongoUpdate lessThan(Object value) {
                filters.add(Filters.lt(field, value));
                return DefaultMongoUpdate.this;
            }
            
            @Override
            public MongoUpdate lessThanOrEquals(Object value) {
                filters.add(Filters.lte(field, value));
                return DefaultMongoUpdate.this;
            }
            
            @Override
            public MongoUpdate in(List<?> values) {
                filters.add(Filters.in(field, values));
                return DefaultMongoUpdate.this;
            }
            
            @Override
            public MongoUpdate notIn(List<?> values) {
                filters.add(Filters.nin(field, values));
                return DefaultMongoUpdate.this;
            }
        }
    }
    
    /**
     * Implementação padrão do MongoDelete
     */
    private class DefaultMongoDelete implements MongoDelete {
        
        private final DefaultMongoCollection collection;
        private final List<Bson> filters = new ArrayList<>();
        
        /**
         * Construtor
         * @param collection Coleção MongoDB
         */
        public DefaultMongoDelete(DefaultMongoCollection collection) {
            this.collection = collection;
        }
        
        @Override
        public Condition where(String field) {
            return new DefaultCondition(field);
        }
        
        @Override
        public int execute() {
            try {
                Bson filter = filters.isEmpty() ? new Document() : com.mongodb.client.model.Filters.and(filters);
                DeleteResult result = collection.getMongoCollection().deleteMany(filter);
                return (int) result.getDeletedCount();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Erro ao executar exclusão: " + e.getMessage(), e);
                return 0;
            }
        }
        
        @Override
        public CompletableFuture<Integer> executeAsync() {
            return CompletableFuture.supplyAsync(this::execute)
                    .exceptionally(e -> {
                        logger.log(Level.WARNING, "Erro ao executar exclusão de forma assíncrona: " + e.getMessage(), e);
                        return 0;
                    });
        }
        
        /**
         * Implementação padrão do Condition para exclusões
         */
        private class DefaultCondition implements Condition {
            
            private final String field;
            
            /**
             * Construtor
             * @param field Campo a ser comparado
             */
            public DefaultCondition(String field) {
                this.field = field;
            }
            
            @Override
            public MongoDelete isEqualTo(Object value) {
                filters.add(Filters.eq(field, value));
                return DefaultMongoDelete.this;
            }
            
            @Override
            public MongoDelete notEquals(Object value) {
                filters.add(Filters.ne(field, value));
                return DefaultMongoDelete.this;
            }
            
            @Override
            public MongoDelete greaterThan(Object value) {
                filters.add(Filters.gt(field, value));
                return DefaultMongoDelete.this;
            }
            
            @Override
            public MongoDelete greaterThanOrEquals(Object value) {
                filters.add(Filters.gte(field, value));
                return DefaultMongoDelete.this;
            }
            
            @Override
            public MongoDelete lessThan(Object value) {
                filters.add(Filters.lt(field, value));
                return DefaultMongoDelete.this;
            }
            
            @Override
            public MongoDelete lessThanOrEquals(Object value) {
                filters.add(Filters.lte(field, value));
                return DefaultMongoDelete.this;
            }
            
            @Override
            public MongoDelete in(List<?> values) {
                filters.add(Filters.in(field, values));
                return DefaultMongoDelete.this;
            }
            
            @Override
            public MongoDelete notIn(List<?> values) {
                filters.add(Filters.nin(field, values));
                return DefaultMongoDelete.this;
            }
        }
    }
}
