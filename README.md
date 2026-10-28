# Banco LSR - API PIX

API desenvolvida em **Java 11**, **Spring Boot 2.7** e **Spring Data JPA** com banco de dados em memória **H2**, focada na gestão segura de limites e execução de transferências PIX com proteção de nível bancário contra transações duplicadas, concorrência e fraudes.

---

## 🛡️ Diagnóstico & Problema: *"E se vier PIX duplicado? Vou pagar 2 vezes?"*

### ⚠️ Como era no código anterior (Vulnerabilidade Crítica)
Na versão anterior, a operação era realizada via:
```http
GET /pix/avaliar?cpf=...&agencia=...&conta=...&valorPix=...
```
Isso causava sérios riscos:
1. **Pagar 2 vezes por reenvio de rede ou duplo clique**: Se o usuário clicasse duas vezes no botão de pagamento ou houvesse retentativas automáticas de rede/webhook, o limite da conta era debitado **duas vezes**.
2. **Violação do protocolo HTTP (RFC 7231)**: O método `GET` deve ser somente leitura e idempotente. Alterar o saldo/limite em um `GET` fazia com que navegadores, proxies ou crawlers pudessem debitar a conta ao fazer pré-carregamento de links.
3. **Condição de Corrida (Race Condition / Double-Spending)**: Duas requisições simultâneas liam o mesmo saldo antes da gravação, permitindo gastar mais do que o limite disponível.
4. **Fraude de Valor Negativo**: Não havia checagem se o valor era positivo; valores negativos podiam inflar o saldo do usuário.
5. **Inexistência de Registro**: Não havia histórico, comprovante nem identificador ponta a ponta (`endToEndId`).

---

## 🚀 O que foi implementado: As 4 Travas de Proteção

Com as novas implementações, a resposta é: **NÃO, você NÃO vai pagar 2 vezes!** A API agora conta com quatro camadas de defesa:

### 1. Trava de Idempotência Estrita (`chaveIdempotencia`)
- Cada requisição de pagamento recebe ou gera uma **chave de idempotência única** (UUID).
- Caso o aplicativo, gateway ou webhook reenvie a mesma requisição por timeout de rede ou duplo clique:
  - O sistema identifica a chave já existente na tabela `transacao_pix`.
  - **Nenhum valor é debitado novamente**.
  - A API retorna imediatamente o comprovante da transação anterior com a flag `"reprocessada": true`.

### 2. Trava Antifraude de Suspeita de Duplicidade Temporal (Janela de 60s)
- Protege o cenário em que o cliente ou sistema gera um novo ID de requisição, mas tenta enviar **o mesmo valor** para **o mesmo destinatário** em menos de **60 segundos**.
- A API rejeita com status `409 Conflict` e status `NEGADA_SUSPEITA_DUPLICIDADE`.
- Para autorizar uma segunda transferência idêntica intencional, deve-se enviar `"confirmarSuspeitaDuplicada": true`.

### 3. Bloqueio Pessimista de Concorrência (`PESSIMISTIC_WRITE`)
- Ao debitar o limite, a linha do cliente no banco de dados é bloqueada exclusivamente (`SELECT ... FOR UPDATE`).
- Garante que transações concorrentes não causem saldo negativo nem percam atualizações de limite.

### 4. Validação de Integridade e Rastreabilidade E2E
- Rejeição de valores menores ou iguais a zero (`NEGADA_VALOR_INVALIDO`).
- Obrigatoriedade de chave PIX de destino.
- Geração de código ponta a ponta exclusivo no formato BACEN: `E00000000 + yyyyMMddHHmmss + hash`.
- Registro histórico completo de todas as transações (sucesso ou falha) para auditoria e conciliação.

---

## 🛠️ Tecnologias Utilizadas

- **Java 11**
- **Spring Boot 2.7.18**
- **Spring Data JPA & Hibernate** (Lock Pessimista e Índices)
- **H2 Database** (em memória para desenvolvimento e testes)
- **Springdoc OpenAPI / Swagger UI** (OpenAPI 3)
- **JUnit 5 & Mockito** (Testes unitários automatizados)

---

## 📡 Endpoints da API

### 1. Transferência PIX Segura
- **Rota:** `POST /pix/pagar`
- **Descrição:** Executa a transferência PIX com todas as travas ativadas.

#### Exemplo de Requisição (Body JSON):
```json
{
  "cpf": "12345678900",
  "agencia": "0001",
  "conta": "12345-6",
  "chavePixDestino": "recebedor@exemplo.com",
  "valor": 150.00,
  "chaveIdempotencia": "e3f9479b-2c49-411f-8239-cf4d86c2f0a1",
  "confirmarSuspeitaDuplicada": false
}
```

#### Exemplo de Resposta de Sucesso (`200 OK`):
```json
{
  "sucesso": true,
  "status": "SUCESSO",
  "chaveIdempotencia": "e3f9479b-2c49-411f-8239-cf4d86c2f0a1",
  "endToEndId": "E0000000020261027220500AB12CD34EF",
  "mensagem": "Transação PIX realizada com sucesso.",
  "valor": 150.0,
  "limiteRestante": 850.0,
  "dataHora": "2026-10-27T22:05:00",
  "reprocessada": false,
  "alertaDuplicidade": false
}
```

#### Exemplo de Resposta Repetida com Mesma Chave (Idempotência - Sem Cobrar 2 Vezes):
```json
{
  "sucesso": true,
  "status": "SUCESSO",
  "chaveIdempotencia": "e3f9479b-2c49-411f-8239-cf4d86c2f0a1",
  "endToEndId": "E0000000020261027220500AB12CD34EF",
  "mensagem": "Transação já processada com sucesso anteriormente. Débito NÃO foi duplicado (Idempotência garantida).",
  "valor": 150.0,
  "limiteRestante": 850.0,
  "dataHora": "2026-10-27T22:05:00",
  "reprocessada": true,
  "alertaDuplicidade": false
}
```

#### Exemplo de Bloqueio por Suspeita de Duplicidade (`409 Conflict`):
```json
{
  "sucesso": false,
  "status": "NEGADA_SUSPEITA_DUPLICIDADE",
  "mensagem": "ALERTA DE SUSPEITA DE DUPLICIDADE: Uma transação idêntica (mesmo valor e destino) foi concluída nos últimos 60 segundos. Para confirmar um segundo pagamento idêntico intencional, envie 'confirmarSuspeitaDuplicada: true'.",
  "alertaDuplicidade": true
}
```

---

### 2. Avaliação de Limite (Consulta Segura - Somente Leitura)
- **Rota:** `GET /pix/avaliar?cpf=...&agencia=...&conta=...&valorPix=...`
- **Descrição:** Verifica se a conta possui limite suficiente para o valor informado **sem alterar o limite nem debitar saldo**.

### 3. Histórico de Transações do Cliente
- **Rota:** `GET /pix/transacoes?cpf=12345678900`
- **Descrição:** Retorna a lista de transações realizadas pela conta com status, horários e valores.

### 4. Consulta de Comprovante por Idempotência
- **Rota:** `GET /pix/transacoes/idempotencia/{chave}`
- **Descrição:** Recupera os dados da transação vinculada à chave de idempotência.

### 5. Gestão de Clientes
- `POST /clientes` - Cadastra cliente com CPF, agência, conta e limite PIX inicial.
- `GET /clientes?cpf=...&agencia=...&conta=...` - Consulta cadastro do cliente.
- `PUT /clientes/limite?cpf=...&agencia=...&conta=...&novoLimite=...` - Altera limite da conta.
- `DELETE /clientes?cpf=...&agencia=...&conta=...` - Remove cliente.

### 6. Monitoramento & Documentação
- `GET /healthcheck` - Verifica integridade da aplicação e conexão com banco de dados.
- `GET /swagger-ui.html` - Swagger UI com documentação interativa OpenAPI 3.
- `GET /h2-console` - Acesso ao console do banco H2 (`jdbc:h2:mem:pixdb`, user `sa`, senha em branco).

---

## 🧪 Testes Automatizados

Foram implementados testes de unidade com JUnit 5 e Mockito para garantir as regras de negócio:
- ✅ **Garantia de Idempotência**: Testa que o reenvio da mesma requisição não debita duas vezes.
- ✅ **Bloqueio de Duplicidade Temporal**: Valida a recusa de transações idênticas na janela de 60s.
- ✅ **Bypass Seguro de Duplicidade**: Valida que a flag de confirmação permite transferências intencionais.
- ✅ **Rejeição de Valores Negativos**: Valida proteção contra fraude de saldo.
- ✅ **Validação de Limite Insuficiente**: Garante integridade do saldo em tentativas acima do limite.

---

## 🏃 Como Executar

1. Compile o projeto:
   ```shell
   mvn clean install
   ```
2. Execute a aplicação:
   ```shell
   mvn spring-boot:run
   ```
3. Acesse a documentação interativa no navegador:
   ```
   http://localhost:8080/swagger-ui.html
   ```
