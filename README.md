# Guia de Instalação e Uso - EconomyPlugin

## Visão Geral

O EconomyPlugin é um sistema completo de economia para servidores Minecraft 1.20.1, oferecendo:

- Sistema de economia integrado ao Vault
- Armazenamento de dados em MongoDB
- Interface de shop com preços dinâmicos baseados em oferta e demanda
- Suporte a itens e blocos de mods
- Sistema de dinheiro rotativo e impostos
- Sistema de loteria para jogadores

## Requisitos

- Servidor Minecraft 1.20.1 (Paper/Spigot)
- Plugin Vault instalado
- MongoDB (local ou remoto)
- Java 17 ou superior

## Instalação

1. Baixe o arquivo `economy-1.0-SNAPSHOT.jar`
2. Coloque o arquivo na pasta `plugins` do seu servidor
3. Inicie o servidor para gerar os arquivos de configuração
4. Edite o arquivo `plugins/EconomyPlugin/config.yml` para configurar a conexão com o MongoDB
5. Reinicie o servidor

## Configuração

### MongoDB

```yaml
mongodb:
  uri: "mongodb://localhost:27017"
  database: "minecraft_economy"
  auth:
    enabled: false
    username: ""
    password: ""
    authSource: "admin"
  connection:
    timeout: 5000
    maxPoolSize: 20
    minPoolSize: 5
    maxWaitTime: 10000
```

### Economia

```yaml
economy:
  initial_balance: 1000.0
  currency_singular: "Moeda"
  currency_plural: "Moedas"
  currency_symbol: "$"
```

### Impostos

```yaml
taxes:
  transaction_tax: 0.02  # 2% de taxa em transações
  wealth_tax:
    enabled: true
    threshold: 100000.0  # Limite para aplicação do imposto
    rate: 0.01  # 1% sobre o valor acima do limite
  inactivity_decay:
    enabled: true
    days_threshold: 7  # Dias de inatividade
    daily_rate: 0.005  # 0.5% de decaimento diário
```

### Shop

```yaml
shop:
  update_interval: 30  # Minutos entre atualizações de preço
  price_volatility: 0.2  # Volatilidade dos preços (0.0 - 1.0)
  min_price_multiplier: 0.5  # Preço mínimo (% do preço base)
  max_price_multiplier: 2.0  # Preço máximo (% do preço base)
```

### Loteria

```yaml
lottery:
  ticket_price: 100.0  # Preço do bilhete
  initial_jackpot: 1000.0  # Prêmio inicial
  max_tickets_per_player: 5  # Máximo de bilhetes por jogador
  type: "daily"  # Tipo de loteria (daily, weekly, hourly)
```

## Comandos

### Economia Básica

- `/money` - Mostra seu saldo
- `/money <jogador>` - Mostra o saldo de outro jogador
- `/money pay <jogador> <quantia>` - Transfere dinheiro para outro jogador
- `/money top` - Mostra o ranking de jogadores mais ricos

### Administração

- `/eco give <jogador> <quantia>` - Dá dinheiro a um jogador
- `/eco take <jogador> <quantia>` - Retira dinheiro de um jogador
- `/eco set <jogador> <quantia>` - Define o saldo de um jogador
- `/eco reset <jogador>` - Redefine o saldo de um jogador para o valor inicial

### Shop

- `/shop` - Abre a interface de shop do servidor

### Impostos

- `/tax info` - Mostra informações sobre os impostos
- `/tax set <tipo> <valor>` - Define a taxa de um imposto
- `/tax enable <tipo>` - Ativa um tipo de imposto
- `/tax disable <tipo>` - Desativa um tipo de imposto
- `/tax apply <tipo>` - Aplica um imposto imediatamente

### Loteria

- `/lottery` - Mostra informações da loteria
- `/lottery buy` - Compra um bilhete de loteria
- `/lottery tickets` - Mostra seus bilhetes
- `/lottery draw` - Realiza o sorteio (admin)
- `/lottery setprice <valor>` - Define o preço do bilhete (admin)
- `/lottery settype <tipo>` - Define o tipo de loteria (admin)
- `/lottery enable` - Ativa a loteria (admin)
- `/lottery disable` - Desativa a loteria (admin)
- `/lottery help` - Mostra ajuda sobre os comandos

## Sistema de Loteria

O sistema de loteria permite que os jogadores comprem bilhetes para concorrer a prêmios em dinheiro. O prêmio aumenta conforme mais bilhetes são vendidos, e o sorteio pode ser realizado manualmente por administradores.

### Tipos de Loteria

- **Diária**: Sorteio a cada 24 horas
- **Semanal**: Sorteio a cada 7 dias
- **Horária**: Sorteio a cada hora

### Funcionamento

1. Jogadores compram bilhetes usando o comando `/lottery buy`
2. 80% do valor dos bilhetes vai para o prêmio
3. Quando chegar a hora do sorteio, o sistema notifica os administradores
4. Um administrador executa `/lottery draw` para realizar o sorteio
5. O vencedor recebe o prêmio automaticamente
6. Uma nova rodada da loteria é iniciada

### Nota Importante

Devido a limitações técnicas, o sorteio automático está temporariamente desativado. Os administradores devem usar o comando `/lottery draw` manualmente quando for hora do sorteio. O sistema notificará quando for o momento apropriado.

## Suporte a Mods

O sistema de shop suporta itens e blocos de mods. O plugin detecta automaticamente itens de mods instalados e os adiciona ao sistema de preços dinâmicos.

## Solução de Problemas

### Conexão com MongoDB

Se o plugin não conseguir se conectar ao MongoDB, verifique:

1. Se o MongoDB está em execução
2. Se as credenciais estão corretas
3. Se o firewall permite conexões na porta do MongoDB
4. Se o URI de conexão está correto

### Comandos Lentos

Se os comandos estiverem lentos ou travando o servidor:

1. Verifique a conexão com o MongoDB
2. Aumente os valores de `maxPoolSize` e `minPoolSize` na configuração
3. Verifique se o servidor MongoDB tem recursos suficientes

### Erros de Compilação

Se você estiver compilando o plugin a partir do código-fonte:

1. Certifique-se de usar Java 17 ou superior
2. Execute `mvn clean package -Dmaven.test.skip=true`
3. O arquivo JAR será gerado na pasta `target`

## Contato e Suporte

Para suporte adicional, entre em contato através do Discord ou abra uma issue no repositório do GitHub.

---

Desenvolvido por Manus - 2025
