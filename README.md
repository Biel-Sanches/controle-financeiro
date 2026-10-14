# 💰 Controle Financeiro Pessoal

Sistema de controle financeiro pessoal em **Java**, rodando no terminal.
Projeto em evolução — hoje com persistência em CSV, com arquitetura pronta
para migrar para banco de dados (MySQL) e, no futuro, ganhar uma interface web.

## ✨ Funcionalidades

1. **Resumo financeiro** — tela inicial com a situação do mês selecionado:
   saldo atual, total de receitas, total de despesas, contas pendentes e
   quanto sobrou no mês.
2. **Cadastro de receitas e despesas** — descrição, valor, tipo, categoria,
   data, forma de pagamento e observações opcionais. Editar, excluir e
   consultar (por mês ou período).
3. **Contas a pagar e a receber** — nome, valor, vencimento, status
   (pendente/paga/recebida), **aviso de conta vencida** e registro da data
   de pagamento.
4. **Categorias e análise de gastos** — total por categoria, comparação com
   o mês anterior, categoria com maior gasto e evolução das despesas dos
   últimos 6 meses (com gráfico de barras no terminal).
5. **Metas financeiras** — crie objetivos (ex: juntar R$ 1.500 para um
   celular), faça depósitos e acompanhe o progresso com porcentagem.
6. **Relatórios financeiros** — relatório do mês ou de um período qualquer,
   com saldo final, gastos por categoria, histórico e **exportação para
   CSV** pronta para abrir no Excel.

## 🛠️ Tecnologias

- Java 21 (records, switch expressions, text blocks)
- `BigDecimal` para valores monetários (sem erros de arredondamento)
- Persistência em arquivos CSV (pasta `data/`)

## ▶️ Como executar

Requisito: JDK 17+ (testado com o 21).

**No Windows**, basta dar dois cliques (ou executar) o `run.bat` após compilar —
ele configura o terminal para UTF-8 e inicia o programa.

Manualmente:

```bash
# compilar
javac -encoding UTF-8 -d out -sourcepath src src/br/com/gsanches/financeiro/Main.java

# executar
java -cp out br.com.gsanches.financeiro.Main
```

> 💡 Se os acentos aparecerem estranhos no Windows, rode `chcp 65001` antes
> de executar (é o que o `run.bat` faz).

## 🏛️ Arquitetura

```
src/br/com/gsanches/financeiro/
├── Main.java            → ponto de entrada
├── model/               → entidades (Transacao, Conta, Meta) e enums
├── repository/          → contratos de persistência + implementações CSV
├── service/             → regras de negócio
└── ui/                  → interface de console
```

A persistência fica isolada atrás de interfaces (`TransacaoRepository`,
`ContaRepository`, `MetaRepository`). A versão CSV está em
`Csv*Repository`; a futura versão com MySQL só precisa criar novas
implementações — o resto do sistema não muda.

## 🗺️ Roadmap

- [ ] Migração da persistência para MySQL (JDBC)
- [ ] Interface web para acesso pelo navegador
- [ ] Autenticação de usuário

## 👨‍💻 Autor

**Gabriel Sanches** — estudante de Análise e Desenvolvimento de Sistemas.
Portfólio: [biel-sanches.github.io/portfolio](https://biel-sanches.github.io/portfolio/)
