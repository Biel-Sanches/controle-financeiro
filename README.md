# 💰 Controle Financeiro Pessoal

Sistema de controle financeiro pessoal em **Java**, com **duas interfaces**:

- 🖥️ **Terminal** — menu de console com persistência em CSV;
- 🌐 **Web** — interface HTML/CSS/JS servida por um servidor HTTP em Java puro,
  com API JSON e **banco de dados MySQL**.

## ✨ Funcionalidades

1. **Resumo financeiro** — dashboard com a situação do mês selecionado:
   saldo atual, total de receitas, total de despesas, contas pendentes e
   quanto sobrou no mês.
2. **Cadastro de receitas e despesas** — descrição, valor, tipo, categoria,
   data, forma de pagamento e observações opcionais. Editar, excluir e
   consultar (por mês ou período).
3. **Contas a pagar e a receber** — nome, valor, vencimento, status
   (pendente/paga/recebida), **aviso de conta vencida** e registro da data
   de pagamento.
4. **Categorias e análise de gastos** — total por categoria (com gráficos),
   comparação com o mês anterior, categoria com maior gasto e evolução das
   despesas ao longo do tempo.
5. **Metas financeiras** — crie objetivos, faça depósitos e acompanhe o
   progresso com porcentagem.
6. **Relatórios financeiros** — relatório por período com saldo final,
   gastos por categoria, histórico e **exportação para CSV** (abre no Excel).

## 🛠️ Tecnologias

- Java 21 (records, switch expressions, text blocks, `HttpServer` do JDK)
- MySQL 8 (JDBC puro, sem ORM) · driver MySQL Connector/J
- Gson (serialização JSON da API)
- HTML5 + CSS3 + JavaScript puro no front (sem frameworks)
- `BigDecimal` para valores monetários

## ▶️ Como executar

Requisito: JDK 17+ (testado com o 21).

### Versão web (recomendada)

1. Tenha um MySQL rodando e crie o banco e o usuário:

   ```sql
   CREATE DATABASE financeiro;
   CREATE USER 'financeiro'@'localhost' IDENTIFIED BY 'financeiro123';
   GRANT ALL PRIVILEGES ON financeiro.* TO 'financeiro'@'localhost';
   ```

2. Baixe as dependências para a pasta `lib/`
   (MySQL Connector e Gson, disponíveis no Maven Central):

   ```
   lib/mysql-connector-j-8.4.0.jar
   lib/gson-2.11.0.jar
   ```

3. Copie `db.properties.example` para `db.properties` e ajuste as credenciais.

4. Compile e rode:

   ```bash
   javac -encoding UTF-8 -cp "lib/*" -d out -sourcepath src src/br/com/gsanches/financeiro/Main.java src/br/com/gsanches/financeiro/web/MainWeb.java
   java -cp "out;lib/*" br.com.gsanches.financeiro.web.MainWeb
   ```

5. Abra **http://localhost:8080** — as tabelas são criadas automaticamente.

   No Windows com tudo configurado, basta executar o **`run-web.bat`**.

### Versão terminal

```bash
javac -encoding UTF-8 -d out -sourcepath src src/br/com/gsanches/financeiro/Main.java
java -cp out br.com.gsanches.financeiro.Main      # ou run.bat
```

## 🏛️ Arquitetura

```
src/br/com/gsanches/financeiro/
├── Main.java            → ponto de entrada (console, CSV)
├── web/MainWeb.java     → servidor HTTP + API JSON (web, MySQL)
├── infra/               → conexão JDBC e criação de tabelas
├── model/               → entidades (records) e enums
├── repository/          → contratos + implementações CSV e JDBC
├── service/             → regras de negócio
└── ui/                  → interface de console

web/                     → front-end (index.html, estilo.css, app.js)
```

A persistência fica isolada atrás de interfaces (`TransacaoRepository`,
`ContaRepository`, `MetaRepository`). Existem duas implementações:
`Csv*` (arquivos, usada pela versão terminal) e `Jdbc*` (MySQL, usada pela
web) — os serviços e as regras não mudam.

## 🗺️ Roadmap

- [x] Versão terminal com CSV
- [x] Interface web + MySQL
- [ ] Autenticação de usuário
- [ ] Deploy na nuvem

## 👨‍💻 Autor

**Gabriel Sanches** — estudante de Análise e Desenvolvimento de Sistemas.
Portfólio: [biel-sanches.github.io/portfolio](https://biel-sanches.github.io/portfolio/)
