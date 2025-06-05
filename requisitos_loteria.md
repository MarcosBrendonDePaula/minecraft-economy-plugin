# Requisitos do Sistema de Loteria - EconomyPlugin

## Visão Geral
O sistema de loteria será uma extensão do plugin de economia existente, permitindo que os jogadores comprem bilhetes para participar de sorteios periódicos com chance de ganhar prêmios em dinheiro. Este sistema contribuirá para a rotatividade do dinheiro no servidor, criando um mecanismo adicional de redistribuição de riqueza.

## Requisitos Funcionais

### 1. Tipos de Loteria
- **Loteria Diária**: Sorteio realizado uma vez por dia com prêmios menores
- **Loteria Semanal**: Sorteio realizado uma vez por semana com prêmios maiores
- **Loteria Especial**: Sorteio configurável para eventos especiais

### 2. Compra de Bilhetes
- Jogadores podem comprar bilhetes através de comandos ou interface gráfica
- Cada bilhete terá um custo configurável
- Possibilidade de limitar o número máximo de bilhetes por jogador
- Bilhetes serão armazenados no MongoDB junto com informações do comprador

### 3. Sorteios
- Sorteios automáticos em intervalos configuráveis
- Algoritmo de sorteio justo e transparente
- Anúncio dos vencedores no chat do servidor
- Distribuição automática dos prêmios

### 4. Prêmios
- Valor base do prêmio configurável para cada tipo de loteria
- Acúmulo do prêmio quando não há vencedores
- Possibilidade de múltiplos vencedores com divisão do prêmio
- Percentual do valor arrecadado que vai para o prêmio (ex: 80%)

### 5. Interface Gráfica
- Menu para compra de bilhetes
- Visualização dos bilhetes comprados
- Informações sobre próximos sorteios
- Histórico de sorteios anteriores

### 6. Comandos
- `/loteria comprar <tipo> <quantidade>` - Comprar bilhetes
- `/loteria bilhetes` - Ver bilhetes comprados
- `/loteria info` - Ver informações sobre as loterias ativas
- `/loteria admin` - Comandos administrativos (requer permissão)

### 7. Comandos Administrativos
- `/loteria admin criar <tipo> <nome> <preço_bilhete> <prêmio_base> <data_sorteio>` - Criar nova loteria
- `/loteria admin cancelar <id>` - Cancelar uma loteria
- `/loteria admin sortear <id>` - Forçar um sorteio
- `/loteria admin config <parâmetro> <valor>` - Configurar parâmetros do sistema

## Requisitos Não-Funcionais

### 1. Desempenho
- O sistema deve suportar um grande número de jogadores comprando bilhetes simultaneamente
- Os sorteios não devem causar lag no servidor

### 2. Persistência
- Todos os dados devem ser armazenados no MongoDB
- O sistema deve ser resistente a reinicializações do servidor

### 3. Configurabilidade
- Todos os parâmetros devem ser configuráveis via arquivo de configuração ou banco de dados
- Mensagens e textos devem ser personalizáveis

### 4. Integração
- Integração completa com o sistema de economia existente
- Compatibilidade com o sistema de dinheiro rotativo e impostos

## Estrutura de Dados

### Coleção: lotteries
```json
{
  "_id": "ObjectId",
  "type": "daily|weekly|special",
  "name": "Nome da Loteria",
  "ticket_price": 100.0,
  "base_prize": 1000.0,
  "current_prize": 1500.0,
  "start_time": "timestamp",
  "draw_time": "timestamp",
  "status": "active|completed|cancelled",
  "winner_count": 1,
  "winners": [
    {
      "uuid": "player-uuid",
      "name": "player-name",
      "prize": 1500.0,
      "ticket_id": "ticket-id"
    }
  ]
}
```

### Coleção: lottery_tickets
```json
{
  "_id": "ObjectId",
  "lottery_id": "lottery-id",
  "player_uuid": "player-uuid",
  "player_name": "player-name",
  "purchase_time": "timestamp",
  "ticket_number": "random-number",
  "status": "active|won|lost|refunded"
}
```

### Coleção: lottery_config
```json
{
  "_id": "ObjectId",
  "param": "param-name",
  "value": "param-value",
  "description": "Parameter description"
}
```

## Fluxo de Funcionamento

1. Administrador configura os tipos de loteria disponíveis
2. Jogadores compram bilhetes usando dinheiro do sistema de economia
3. O dinheiro é removido da economia e adicionado ao prêmio da loteria
4. No horário programado, o sistema realiza o sorteio automaticamente
5. O sistema seleciona aleatoriamente um ou mais bilhetes vencedores
6. Os prêmios são distribuídos automaticamente para os vencedores
7. O resultado é anunciado no chat do servidor
8. Uma nova rodada da loteria é iniciada automaticamente

## Considerações de Implementação

- Utilizar sistema de tarefas agendadas do Bukkit para os sorteios
- Implementar cache para melhorar o desempenho de operações frequentes
- Garantir que todas as operações de banco de dados sejam assíncronas
- Implementar sistema de logs detalhados para auditoria
- Criar mecanismos de backup e recuperação de dados
