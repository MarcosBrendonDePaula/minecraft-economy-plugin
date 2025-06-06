# EconomyPlugin - Sistema de Economia para Minecraft 1.20.1

Este plugin oferece um sistema completo de economia para servidores Minecraft 1.20.1, com suporte ao Vault, armazenamento em MongoDB, sistema de lojas do servidor e lojas de jogadores.

## Funcionalidades

### Sistema de Economia
- Compatível com Vault
- Armazenamento em MongoDB
- Comandos para gerenciar dinheiro
- Sistema de impostos
- Dinheiro rotativo

### Loja do Servidor
- Interface gráfica intuitiva
- Preços dinâmicos baseados em oferta e demanda
- Suporte completo para itens de mods
- Categorias organizadas

### Lojas de Jogadores
- Jogadores podem criar suas próprias lojas
- Opção de preço fixo ou dinâmico para cada item
- Suporte completo para itens de mods
- Sistema seguro que remove o item do inventário ao adicionar à loja
- Armazenamento no banco de dados MongoDB

### Sistema de Loteria
- Sorteios periódicos
- Acumulação de prêmios
- Compra de bilhetes

## Comandos

### Comandos de Economia
- `/money` - Mostra seu saldo atual
- `/money pay <jogador> <quantia>` - Transfere dinheiro para outro jogador
- `/eco give <jogador> <quantia>` - Dá dinheiro a um jogador (admin)
- `/eco take <jogador> <quantia>` - Remove dinheiro de um jogador (admin)
- `/eco set <jogador> <quantia>` - Define o saldo de um jogador (admin)

### Comandos de Loja
- `/shop` - Abre a loja do servidor
- `/playershop` - Abre o menu de lojas de jogadores
- `/playershop create <nome>` - Cria uma nova loja de jogador
- `/playershop delete <nome>` - Deleta uma loja de jogador

### Comandos de Loteria
- `/lottery buy <quantidade>` - Compra bilhetes de loteria
- `/lottery info` - Mostra informações sobre o próximo sorteio

## Configuração

### config.yml
```yaml
# Configuração do MongoDB e economia
mongodb:
  connection_string: mongodb://admin:password@localhost:27017/minecraft_economy?authSource=admin
  database: minecraft_economy
  pool_size: 10
  connect_timeout: 5000
  socket_timeout: 5000
  max_wait_time: 5000

economy:
  starting_balance: 1000.0
  currency_singular: "Moeda"
  currency_plural: "Moedas"
  tax_rate: 0.05
  tax_interval: 86400

shop:
  update_interval: 3600
  max_price_change: 0.2

playershop:
  max_shops_per_player: 3
  max_items_per_shop: 54
  creation_cost: 1000.0

lottery:
  ticket_price: 100.0
  draw_interval: 86400
  base_prize: 1000.0
```

## Instalação

1. Certifique-se de ter o MongoDB instalado e configurado
2. Coloque o arquivo JAR na pasta `plugins` do seu servidor
3. Configure o arquivo `config.yml` com suas informações de conexão do MongoDB
4. Reinicie o servidor

## Requisitos
- Minecraft 1.20.1
- Vault
- MongoDB

## Recursos Avançados

### Conexão Resiliente com MongoDB
O plugin implementa um sistema robusto de conexão com o MongoDB que:
- Tenta reconectar automaticamente em caso de falha
- Usa cache local para operações críticas
- Fornece fallback para operações quando o MongoDB está indisponível
- Garante que todas as operações sejam totalmente assíncronas

### Sistema de Preços Dinâmicos
Os preços dos itens na loja do servidor e nas lojas de jogadores (quando configurados como dinâmicos) são ajustados automaticamente com base na oferta e demanda:
- Quanto mais um item é vendido, menor seu preço
- Quanto menos um item está disponível, maior seu preço
- Limites configuráveis para evitar preços extremos

### Suporte a Mods
O plugin suporta completamente itens de mods em todas as operações:
- Identificação correta de itens de mods através de seus namespaces
- Armazenamento e recuperação adequados no banco de dados
- Exibição correta nas interfaces de loja
