# Biblioteca MongoDB para Bukkit/Spigot

Uma biblioteca Java simples e poderosa para interagir com MongoDB em plugins Bukkit/Spigot.

## Índice

- [Instalação](#instalação)
- [Uso Básico](#uso-básico)
- [Configuração](#configuração)
- [Operações CRUD](#operações-crud)
  - [Inserção](#inserção)
  - [Consulta](#consulta)
  - [Atualização](#atualização)
  - [Exclusão](#exclusão)
- [Mapeamento Objeto-Documento](#mapeamento-objeto-documento)
- [Operações Assíncronas](#operações-assíncronas)
- [Cache](#cache)
- [Exemplos Completos](#exemplos-completos)
- [Referência da API](#referência-da-api)
- [Solução de Problemas](#solução-de-problemas)

## Instalação

### Maven

```xml
<dependency>
    <groupId>com.minecraft</groupId>
    <artifactId>bukkit-mongodb</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.minecraft:bukkit-mongodb:1.0.0'
```

### Manual

1. Baixe o arquivo JAR da biblioteca
2. Adicione-o ao classpath do seu projeto

## Uso Básico

### Inicialização

```java
// Inicializa o cliente MongoDB
MongoClient mongoClient = MongoClient.builder()
        .connectionString("mongodb://localhost:27017")
        .database("meu_banco")
        .cacheEnabled(true)
        .build();

// Conecta ao MongoDB
if (!mongoClient.connect()) {
    getLogger().severe("Falha ao conectar ao MongoDB!");
}
```

### Operações Simples

```java
// Obtém uma coleção
MongoCollection collection = mongoClient.getCollection("minha_colecao");

// Insere um documento
Map<String, Object> document = new HashMap<>();
document.put("nome", "Steve");
document.put("nivel", 10);
collection.insert(document);

// Consulta documentos
List<Map<String, Object>> resultados = collection.find()
        .where("nivel").greaterThan(5)
        .toList();

// Fecha a conexão quando não for mais necessária
mongoClient.disconnect();
```

## Configuração

A biblioteca oferece várias opções de configuração através do builder:

```java
MongoClient mongoClient = MongoClient.builder()
        .connectionString("mongodb://usuario:senha@localhost:27017/meu_banco")
        .database("meu_banco")
        .cacheEnabled(true)
        .cacheExpiration(60, TimeUnit.SECONDS)
        .poolSize(10)
        .connectTimeout(5000)
        .socketTimeout(5000)
        .build();
```

| Opção | Descrição | Padrão |
|-------|-----------|--------|
| `connectionString` | String de conexão MongoDB | `mongodb://localhost:27017` |
| `database` | Nome do banco de dados | `minecraft` |
| `cacheEnabled` | Habilita o cache | `true` |
| `cacheExpiration` | Tempo de expiração do cache | `60` segundos |
| `poolSize` | Tamanho do pool de conexões | `10` |
| `connectTimeout` | Timeout de conexão em ms | `5000` |
| `socketTimeout` | Timeout de socket em ms | `5000` |

## Operações CRUD

### Inserção

#### Inserir um documento

```java
// Usando Map
Map<String, Object> document = new HashMap<>();
document.put("nome", "Steve");
document.put("nivel", 10);
collection.insert(document);

// Usando objeto
Player player = new Player("Steve", 10);
collection.insert(player);
```

#### Inserir vários documentos

```java
// Usando Maps
List<Map<String, Object>> documents = new ArrayList<>();
documents.add(document1);
documents.add(document2);
collection.insertManyDocuments(documents);

// Usando objetos
List<Player> players = new ArrayList<>();
players.add(player1);
players.add(player2);
collection.insertManyObjects(players);
```

### Consulta

#### Consulta simples

```java
// Buscar um documento
Optional<Map<String, Object>> result = collection.find()
        .where("nome").isEqualTo("Steve")
        .first();

// Buscar um objeto
Optional<Player> player = collection.find()
        .where("nome").isEqualTo("Steve")
        .first(Player.class);
```

#### Consultas complexas

```java
// Múltiplas condições
List<Player> players = collection.find()
        .where("nivel").greaterThan(5)
        .where("nome").contains("eve")
        .where("ativo").isEqualTo(true)
        .toList(Player.class);

// Ordenação e paginação
List<Player> topPlayers = collection.find()
        .sort("nivel", false) // Ordem decrescente
        .limit(10)
        .skip(20) // Pular os primeiros 20
        .toList(Player.class);
```

#### Operadores de consulta

| Operador | Descrição |
|----------|-----------|
| `isEqualTo(value)` | Igual a |
| `notEquals(value)` | Diferente de |
| `greaterThan(value)` | Maior que |
| `greaterThanOrEquals(value)` | Maior ou igual a |
| `lessThan(value)` | Menor que |
| `lessThanOrEquals(value)` | Menor ou igual a |
| `contains(value)` | Contém (para strings) |
| `startsWith(value)` | Começa com (para strings) |
| `endsWith(value)` | Termina com (para strings) |
| `in(values)` | Está na lista |
| `notIn(values)` | Não está na lista |
| `exists()` | Campo existe |
| `notExists()` | Campo não existe |

### Atualização

#### Atualização simples

```java
// Atualizar um campo
collection.update()
        .where("nome").isEqualTo("Steve")
        .set("nivel", 20)
        .execute();

// Atualizar múltiplos campos
collection.update()
        .where("nivel").lessThan(5)
        .set("nivel", 5)
        .set("mensagem", "Nível mínimo atingido")
        .execute();
```

#### Operações de atualização

```java
// Incrementar um valor
collection.update()
        .where("nome").isEqualTo("Steve")
        .increment("nivel", 1)
        .execute();

// Decrementar um valor
collection.update()
        .where("nome").isEqualTo("Steve")
        .decrement("moedas", 10)
        .execute();

// Multiplicar um valor
collection.update()
        .where("nome").isEqualTo("Steve")
        .multiply("pontos", 2)
        .execute();

// Dividir um valor
collection.update()
        .where("nome").isEqualTo("Steve")
        .divide("pontos", 2)
        .execute();

// Adicionar a um array
collection.update()
        .where("nome").isEqualTo("Steve")
        .push("itens", "espada")
        .execute();

// Remover de um array
collection.update()
        .where("nome").isEqualTo("Steve")
        .pull("itens", "espada")
        .execute();

// Remover um campo
collection.update()
        .where("nome").isEqualTo("Steve")
        .unset("temporario")
        .execute();
```

### Exclusão

```java
// Excluir um documento
collection.delete()
        .where("nome").isEqualTo("Steve")
        .execute();

// Excluir vários documentos
collection.delete()
        .where("nivel").lessThan(5)
        .execute();

// Limpar a coleção
collection.clear();
```

## Mapeamento Objeto-Documento

A biblioteca suporta mapeamento automático entre objetos Java e documentos MongoDB usando anotações.

### Definindo uma classe de modelo

```java
@Document
@Collection("players")
public class Player {
    
    @Id
    private String id;
    
    @Field("username")
    private String name;
    
    @Field
    private int level;
    
    @Field("is_active")
    private boolean active;
    
    // Construtor padrão (obrigatório)
    public Player() {
    }
    
    // Construtor com parâmetros
    public Player(String name, int level) {
        this.name = name;
        this.level = level;
        this.active = true;
    }
    
    // Getters e setters
    // ...
}
```

### Anotações

| Anotação | Descrição |
|----------|-----------|
| `@Document` | Marca a classe como um documento MongoDB |
| `@Collection("nome")` | Define o nome da coleção (opcional) |
| `@Id` | Marca o campo como ID do documento |
| `@Field("nome")` | Define o nome do campo no documento (opcional) |

### Usando objetos mapeados

```java
// Inserir
Player player = new Player("Steve", 10);
collection.insert(player);

// Consultar
Optional<Player> result = collection.find()
        .where("username").isEqualTo("Steve")
        .first(Player.class);

// Listar
List<Player> players = collection.find()
        .where("level").greaterThan(5)
        .toList(Player.class);
```

## Operações Assíncronas

Todas as operações têm versões assíncronas que retornam `CompletableFuture`.

```java
// Inserir de forma assíncrona
collection.insertAsync(player)
        .thenAccept(success -> {
            if (success) {
                getLogger().info("Jogador inserido com sucesso!");
            }
        });

// Consultar de forma assíncrona
collection.find()
        .where("username").isEqualTo("Steve")
        .firstAsync(Player.class)
        .thenAccept(optionalPlayer -> {
            optionalPlayer.ifPresent(player -> {
                getLogger().info("Jogador encontrado: " + player.getName());
            });
        });

// Atualizar de forma assíncrona
collection.update()
        .where("username").isEqualTo("Steve")
        .set("level", 20)
        .executeAsync()
        .thenAccept(count -> {
            getLogger().info(count + " documentos atualizados");
        });

// Excluir de forma assíncrona
collection.delete()
        .where("username").isEqualTo("Steve")
        .executeAsync()
        .thenAccept(count -> {
            getLogger().info(count + " documentos excluídos");
        });
```

## Cache

A biblioteca inclui um sistema de cache integrado para melhorar o desempenho.

### Configuração do cache

```java
MongoClient mongoClient = MongoClient.builder()
        .connectionString("mongodb://localhost:27017")
        .database("meu_banco")
        .cacheEnabled(true) // Habilita o cache
        .cacheExpiration(60, TimeUnit.SECONDS) // Define o tempo de expiração
        .build();
```

O cache é usado automaticamente para operações de leitura. Os resultados são armazenados em cache e reutilizados para consultas subsequentes com os mesmos parâmetros.

## Exemplos Completos

### Exemplo 1: Sistema de jogadores

```java
public class PlayerManager {
    
    private final MongoClient mongoClient;
    private final MongoCollection collection;
    
    public PlayerManager(Plugin plugin) {
        // Inicializa o cliente MongoDB
        this.mongoClient = MongoClient.builder()
                .connectionString("mongodb://localhost:27017")
                .database("meu_servidor")
                .build();
        
        // Conecta ao MongoDB
        if (!mongoClient.connect()) {
            plugin.getLogger().severe("Falha ao conectar ao MongoDB!");
        }
        
        // Obtém a coleção de jogadores
        this.collection = mongoClient.getCollection("players");
    }
    
    public void savePlayer(Player player) {
        // Cria um objeto de jogador
        GamePlayer gamePlayer = new GamePlayer();
        gamePlayer.setUuid(player.getUniqueId().toString());
        gamePlayer.setName(player.getName());
        gamePlayer.setLevel(1);
        gamePlayer.setCoins(0);
        gamePlayer.setLastLogin(System.currentTimeMillis());
        
        // Insere ou atualiza o jogador
        collection.find()
                .where("uuid").isEqualTo(player.getUniqueId().toString())
                .firstAsync(GamePlayer.class)
                .thenAccept(optionalGamePlayer -> {
                    if (optionalGamePlayer.isPresent()) {
                        // Atualiza o jogador existente
                        GamePlayer existingPlayer = optionalGamePlayer.get();
                        existingPlayer.setName(player.getName());
                        existingPlayer.setLastLogin(System.currentTimeMillis());
                        
                        collection.update()
                                .where("uuid").isEqualTo(player.getUniqueId().toString())
                                .set("name", player.getName())
                                .set("lastLogin", System.currentTimeMillis())
                                .executeAsync();
                    } else {
                        // Insere um novo jogador
                        collection.insertAsync(gamePlayer);
                    }
                });
    }
    
    public CompletableFuture<Optional<GamePlayer>> getPlayer(UUID uuid) {
        return collection.find()
                .where("uuid").isEqualTo(uuid.toString())
                .firstAsync(GamePlayer.class);
    }
    
    public CompletableFuture<Boolean> addCoins(UUID uuid, int amount) {
        return collection.update()
                .where("uuid").isEqualTo(uuid.toString())
                .increment("coins", amount)
                .executeAsync()
                .thenApply(count -> count > 0);
    }
    
    public CompletableFuture<List<GamePlayer>> getTopPlayers(int limit) {
        return collection.find()
                .sort("coins", false) // Ordem decrescente
                .limit(limit)
                .toListAsync(GamePlayer.class);
    }
    
    public void close() {
        mongoClient.disconnect();
    }
    
    @Document
    @Collection("players")
    public static class GamePlayer {
        @Id
        private String id;
        
        @Field
        private String uuid;
        
        @Field
        private String name;
        
        @Field
        private int level;
        
        @Field
        private int coins;
        
        @Field("lastLogin")
        private long lastLogin;
        
        // Getters e setters
        // ...
    }
}
```

### Exemplo 2: Sistema de economia

```java
public class EconomyManager {
    
    private final MongoClient mongoClient;
    private final MongoCollection collection;
    private final Logger logger;
    
    public EconomyManager(Plugin plugin) {
        this.logger = plugin.getLogger();
        
        // Inicializa o cliente MongoDB
        this.mongoClient = MongoClient.builder()
                .connectionString("mongodb://localhost:27017")
                .database("meu_servidor")
                .cacheEnabled(true)
                .build();
        
        // Conecta ao MongoDB
        if (!mongoClient.connect()) {
            logger.severe("Falha ao conectar ao MongoDB!");
        }
        
        // Obtém a coleção de contas
        this.collection = mongoClient.getCollection("accounts");
    }
    
    public CompletableFuture<Boolean> createAccount(UUID playerId, String playerName) {
        return collection.find()
                .where("playerId").isEqualTo(playerId.toString())
                .existsAsync()
                .thenCompose(exists -> {
                    if (exists) {
                        return CompletableFuture.completedFuture(true);
                    }
                    
                    Account account = new Account();
                    account.setPlayerId(playerId.toString());
                    account.setPlayerName(playerName);
                    account.setBalance(100.0); // Saldo inicial
                    account.setCreatedAt(System.currentTimeMillis());
                    
                    return collection.insertAsync(account);
                });
    }
    
    public CompletableFuture<Double> getBalance(UUID playerId) {
        return collection.find()
                .where("playerId").isEqualTo(playerId.toString())
                .firstAsync(Account.class)
                .thenApply(optionalAccount -> 
                    optionalAccount.map(Account::getBalance).orElse(0.0)
                );
    }
    
    public CompletableFuture<Boolean> deposit(UUID playerId, double amount) {
        if (amount <= 0) {
            return CompletableFuture.completedFuture(false);
        }
        
        return collection.update()
                .where("playerId").isEqualTo(playerId.toString())
                .increment("balance", amount)
                .executeAsync()
                .thenApply(count -> count > 0);
    }
    
    public CompletableFuture<Boolean> withdraw(UUID playerId, double amount) {
        if (amount <= 0) {
            return CompletableFuture.completedFuture(false);
        }
        
        return getBalance(playerId)
                .thenCompose(balance -> {
                    if (balance < amount) {
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    return collection.update()
                            .where("playerId").isEqualTo(playerId.toString())
                            .decrement("balance", amount)
                            .executeAsync()
                            .thenApply(count -> count > 0);
                });
    }
    
    public CompletableFuture<Boolean> transfer(UUID fromId, UUID toId, double amount) {
        if (amount <= 0) {
            return CompletableFuture.completedFuture(false);
        }
        
        return getBalance(fromId)
                .thenCompose(balance -> {
                    if (balance < amount) {
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    return mongoClient.withTransaction(client -> {
                        boolean success = true;
                        
                        // Debita da conta de origem
                        success &= client.getCollection("accounts")
                                .update()
                                .where("playerId").isEqualTo(fromId.toString())
                                .decrement("balance", amount)
                                .execute() > 0;
                        
                        // Credita na conta de destino
                        success &= client.getCollection("accounts")
                                .update()
                                .where("playerId").isEqualTo(toId.toString())
                                .increment("balance", amount)
                                .execute() > 0;
                        
                        return success;
                    });
                });
    }
    
    public void close() {
        mongoClient.disconnect();
    }
    
    @Document
    @Collection("accounts")
    public static class Account {
        @Id
        private String id;
        
        @Field
        private String playerId;
        
        @Field
        private String playerName;
        
        @Field
        private double balance;
        
        @Field("createdAt")
        private long createdAt;
        
        // Getters e setters
        // ...
    }
}
```

## Referência da API

### MongoClient

```java
// Criar um cliente
MongoClient client = MongoClient.builder()
        .connectionString("mongodb://localhost:27017")
        .database("meu_banco")
        .build();

// Conectar
boolean connected = client.connect();

// Obter uma coleção
MongoCollection collection = client.getCollection("minha_colecao");

// Obter o banco de dados
MongoDatabase database = client.getDatabase();

// Verificar se está conectado
boolean isConnected = client.isConnected();

// Executar uma transação
boolean success = client.withTransaction(transactionClient -> {
    // Operações na transação
    return true; // ou false para rollback
});

// Desconectar
client.disconnect();
```

### MongoCollection

```java
// Obter o nome da coleção
String name = collection.getName();

// Criar uma consulta
MongoQuery query = collection.find();

// Criar uma atualização
MongoUpdate update = collection.update();

// Criar uma exclusão
MongoDelete delete = collection.delete();

// Inserir um documento
boolean success = collection.insert(document);

// Inserir um objeto
boolean success = collection.insert(object);

// Inserir vários documentos
int count = collection.insertManyDocuments(documents);

// Inserir vários objetos
int count = collection.insertManyObjects(objects);

// Contar documentos
long count = collection.count();

// Verificar se a coleção está vazia
boolean isEmpty = collection.isEmpty();

// Limpar a coleção
long removed = collection.clear();
```

### MongoQuery

```java
// Adicionar uma condição
query.where("campo").isEqualTo(valor);

// Limitar resultados
query.limit(10);

// Pular resultados
query.skip(20);

// Ordenar resultados
query.sort("campo", true); // Ascendente
query.sort("campo", false); // Descendente

// Obter o primeiro resultado como mapa
Optional<Map<String, Object>> result = query.first();

// Obter o primeiro resultado como objeto
Optional<MeuObjeto> result = query.first(MeuObjeto.class);

// Obter todos os resultados como mapas
List<Map<String, Object>> results = query.toList();

// Obter todos os resultados como objetos
List<MeuObjeto> results = query.toList(MeuObjeto.class);

// Contar resultados
long count = query.count();

// Verificar se existe algum resultado
boolean exists = query.exists();
```

### MongoUpdate

```java
// Adicionar uma condição
update.where("campo").isEqualTo(valor);

// Definir um valor
update.set("campo", valor);

// Incrementar um valor
update.increment("campo", valor);

// Decrementar um valor
update.decrement("campo", valor);

// Multiplicar um valor
update.multiply("campo", valor);

// Dividir um valor
update.divide("campo", valor);

// Adicionar a um array
update.push("campo", valor);

// Remover de um array
update.pull("campo", valor);

// Remover um campo
update.unset("campo");

// Executar a atualização
int count = update.execute();
```

### MongoDelete

```java
// Adicionar uma condição
delete.where("campo").isEqualTo(valor);

// Executar a exclusão
int count = delete.execute();
```

### MongoDatabase

```java
// Obter o nome do banco de dados
String name = database.getName();

// Obter uma coleção
MongoCollection collection = database.getCollection("minha_colecao");

// Verificar se uma coleção existe
boolean exists = database.collectionExists("minha_colecao");

// Criar uma coleção
boolean success = database.createCollection("minha_colecao");

// Excluir uma coleção
boolean success = database.dropCollection("minha_colecao");

// Listar coleções
List<String> collections = database.listCollections();

// Executar um comando
Object result = database.runCommand(command);
```

## Solução de Problemas

### Problemas de Conexão

Se você estiver tendo problemas para conectar ao MongoDB:

1. Verifique se o servidor MongoDB está em execução
2. Verifique se a string de conexão está correta
3. Verifique se o firewall permite conexões na porta do MongoDB (padrão: 27017)
4. Aumente o timeout de conexão:

```java
MongoClient client = MongoClient.builder()
        .connectionString("mongodb://localhost:27017")
        .connectTimeout(10000) // 10 segundos
        .socketTimeout(10000) // 10 segundos
        .build();
```

### Problemas de Mapeamento

Se você estiver tendo problemas com o mapeamento objeto-documento:

1. Certifique-se de que a classe tem um construtor padrão sem parâmetros
2. Certifique-se de que todos os campos têm getters e setters
3. Verifique se as anotações estão corretas
4. Para tipos complexos, considere implementar conversores personalizados

### Problemas de Desempenho

Se você estiver tendo problemas de desempenho:

1. Habilite o cache:

```java
MongoClient client = MongoClient.builder()
        .cacheEnabled(true)
        .cacheExpiration(60, TimeUnit.SECONDS)
        .build();
```

2. Use operações assíncronas para operações que não bloqueiam o thread principal
3. Crie índices para campos frequentemente consultados
4. Limite o número de resultados retornados com `limit()`
5. Use projeções para retornar apenas os campos necessários

### Logs de Depuração

Para habilitar logs de depuração, configure o logger Java:

```java
Logger logger = Logger.getLogger("com.minecraft.mongodb");
logger.setLevel(Level.FINE);

Handler handler = new ConsoleHandler();
handler.setLevel(Level.FINE);
logger.addHandler(handler);
```

Isso mostrará logs detalhados de todas as operações MongoDB, incluindo consultas, atualizações e erros.
