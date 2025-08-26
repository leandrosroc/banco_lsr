
# Banco LSR - API PIX

Projeto desenvolvido para praticar situações comuns do dia a dia com Spring Boot, Java e banco de dados H2. O objetivo é simular cenários de validação de limites de contas PIX, integração com banco, documentação OpenAPI e boas práticas de API REST.

## Objetivos
- Praticar desenvolvimento de APIs REST com Spring Boot
- Simular validação de transações PIX
- Exercitar integração com banco de dados em memória (H2)
- Implementar healthcheck e documentação automática (Swagger)

## Tecnologias Utilizadas
- Java 11
- Spring Boot
- H2 (banco de dados em memória)
- Maven

## Endpoints Principais

- `GET /pix/avaliar?cpf=...&agencia=...&conta=...&valorPix=...`
   - Avalia se a transação PIX pode ser realizada com base no limite da conta.
- `GET /healthcheck`
   - Retorna o status da API e da conexão com o banco de dados.
- `GET /`
   - Retorna informações básicas da API (título, versão, rota da documentação Swagger).
- `GET /swagger-ui.html`
   - Acessa a documentação interativa da API (Swagger UI).
- `GET /h2-console`
   - Console do banco H2 para consultas e visualização dos dados.

## Como Executar
1. Compile o projeto:
    ```shell
    mvn clean install
    ```
2. Execute a aplicação:
    ```shell
    mvn spring-boot:run
    ```
3. Acesse os endpoints conforme exemplos acima.

## Observações
- O banco de dados é em memória (H2) e será reiniciado a cada execução.
- Para acessar o console H2: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:pixdb`, usuário: `sa`, senha: _em branco_)
- A documentação Swagger está disponível em: `http://localhost:8080/swagger-ui.html`

---
Este projeto é apenas para fins de estudo e prática. Sinta-se à vontade para modificar, testar e experimentar novas funcionalidades!
