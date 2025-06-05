# Requisitos Detalhados - Plugin de Economia para Minecraft 1.20.1

## 1. Integração com Vault
- Implementar hooks para a API Vault
- Registrar o plugin como provedor de economia
- Implementar métodos obrigatórios: hasAccount, createAccount, deleteAccount, getBalance, has, withdraw, deposit, transferMoney
- Garantir compatibilidade com outros plugins que dependem do Vault

## 2. Armazenamento em MongoDB
- Estrutura do banco de dados:
  - Coleção "players": armazenar informações de contas dos jogadores
  - Coleção "transactions": histórico de transações
  - Coleção "market_items": itens disponíveis no mercado e seus preços
  - Coleção "market_history": histórico de preços para análise de tendências
- Implementar conexão assíncrona para não impactar o desempenho do servidor
- Criar sistema de cache para reduzir consultas ao banco de dados
- Implementar sistema de backup automático

## 3. Sistema de Shop
- Interface gráfica usando inventários do Minecraft
- Categorias de itens para facilitar navegação
- Sistema de paginação para lidar com muitos itens
- Visualização de preços atuais e tendências
- Opções de compra em diferentes quantidades
- Sistema de busca/filtro de itens

## 4. Sistema de Preços Dinâmicos
- Algoritmo de precificação baseado em:
  - Quantidade do item no mercado
  - Frequência de compra/venda do item
  - Raridade base do item
  - Tempo desde a última transação
- Fórmula para cálculo de preço: `Preço = PreçoBase * (1 + FatorEscassez - FatorAbundância) * FatorDemanda`
- Sistema de limites para evitar preços extremamente altos ou baixos
- Atualização periódica de preços (a cada X minutos)
- Registro de histórico de preços para análise

## 5. Sistema de Dinheiro Rotativo e Impostos
- Imposto sobre transações de compra/venda (configurável, padrão: 2-5%)
- Imposto progressivo sobre grandes fortunas
- Sistema de decaimento de dinheiro inativo (taxa sobre contas que não realizam transações por X dias)
- Redistribuição parcial dos impostos para jogadores ativos
- Eventos econômicos periódicos (inflação, deflação, etc.)

## 6. Comandos e Permissões
- Comandos básicos:
  - `/money` - Ver saldo próprio
  - `/money pay <jogador> <quantia>` - Transferir dinheiro
  - `/money top` - Ver ranking de jogadores mais ricos
  - `/shop` - Abrir interface do shop
- Comandos administrativos:
  - `/eco give <jogador> <quantia>` - Dar dinheiro a um jogador
  - `/eco take <jogador> <quantia>` - Remover dinheiro de um jogador
  - `/eco reset <jogador>` - Resetar conta de um jogador
  - `/market setprice <item> <preço>` - Definir preço base de um item
  - `/tax set <taxa>` - Definir taxa de imposto
- Sistema de permissões detalhado para cada comando

## 7. Configuração e Personalização
- Arquivo de configuração para todas as variáveis do sistema
- Mensagens personalizáveis
- Opção para desativar recursos específicos
- Configuração de limites de preços mínimos e máximos
- Ajuste de parâmetros do algoritmo de precificação

## 8. Compatibilidade e Desempenho
- Compatibilidade com Minecraft 1.20.1
- Otimização para servidores com muitos jogadores
- Compatibilidade com Windows e Linux
- Operações assíncronas para não impactar o desempenho do servidor
- Sistema de cache para reduzir consultas ao banco de dados
