# Requisitos para o Sistema de Lojas de Jogadores

## Visão Geral
O sistema de lojas de jogadores permitirá que cada jogador crie sua própria loja para vender itens a outros jogadores. Diferente da loja do servidor, onde os preços são dinâmicos baseados em oferta e demanda global, as lojas de jogadores terão preços definidos pelos próprios jogadores.

## Funcionalidades Principais

### 1. Criação de Lojas
- Jogadores podem criar suas próprias lojas
- Cada jogador pode ter uma quantidade limitada de lojas (configurável)
- Criação de loja pode ter um custo (configurável)
- Lojas podem ser criadas em locais específicos ou em qualquer lugar

### 2. Gerenciamento de Itens
- Jogadores podem adicionar itens à venda em suas lojas
- Jogadores definem os preços dos itens
- Jogadores podem remover itens de suas lojas
- Sistema de estoque baseado nos itens que o jogador possui

### 3. Interface de Usuário
- Menu intuitivo para gerenciar lojas
- Visualização de itens à venda
- Estatísticas de vendas
- Configuração de preços

### 4. Transações
- Compra segura de itens entre jogadores
- Transferência automática de fundos
- Registro de transações
- Notificações de vendas para o dono da loja

### 5. Administração
- Comandos para administradores gerenciarem lojas
- Configurações globais para limites e taxas
- Logs de transações

## Estrutura de Dados

### Loja de Jogador
- ID único
- Dono (UUID do jogador)
- Nome da loja
- Localização (opcional)
- Lista de itens à venda
- Saldo acumulado
- Data de criação
- Status (aberta/fechada)

### Item à Venda
- ID único
- Tipo de item
- Quantidade
- Preço unitário
- Metadados (encantamentos, durabilidade, etc.)
- Status (disponível/vendido)

## Comandos

### Jogadores
- `/playershop create <nome>` - Cria uma nova loja
- `/playershop list` - Lista suas lojas
- `/playershop open <id>` - Abre sua loja para gerenciamento
- `/playershop add <preço>` - Adiciona o item na mão à loja
- `/playershop remove <id>` - Remove um item da loja
- `/playershop setprice <id> <preço>` - Altera o preço de um item
- `/playershop withdraw` - Retira o dinheiro acumulado na loja

### Administradores
- `/playershop admin list <jogador>` - Lista lojas de um jogador
- `/playershop admin delete <id>` - Remove uma loja
- `/playershop admin setlimit <jogador> <limite>` - Define limite de lojas para um jogador

## Permissões
- `economy.playershop.create` - Permissão para criar lojas
- `economy.playershop.use` - Permissão para usar lojas
- `economy.playershop.admin` - Permissão para comandos administrativos

## Configurações
- Limite de lojas por jogador
- Custo para criar uma loja
- Taxa de transação (porcentagem do valor da venda)
- Tempo máximo de inatividade antes da loja ser removida
- Limite de itens por loja

## Integração
- Integração com o sistema de economia existente
- Integração com o sistema de banco de dados MongoDB
- Compatibilidade com itens de mods

## Considerações Técnicas
- Armazenamento assíncrono no MongoDB
- Cache local para melhor performance
- Tratamento de erros robusto
- Proteção contra exploits e duplicação de itens
