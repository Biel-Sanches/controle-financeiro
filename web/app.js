"use strict";

// ===== Utilidades =====

const fmtMoeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const fmtData = (iso) => new Date(iso + (iso.length === 10 ? "T12:00:00" : "")).toLocaleDateString("pt-BR");
const fmtMes = (iso) => {
  const [ano, mes] = iso.split("-");
  const nomes = ["jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"];
  return `${nomes[Number(mes) - 1]}/${ano}`;
};

function esc(texto) {
  const div = document.createElement("div");
  div.textContent = texto == null ? "" : String(texto);
  return div.innerHTML;
}

function toast(mensagem, erro = false) {
  const el = document.getElementById("toast");
  el.textContent = mensagem;
  el.classList.toggle("erro", erro);
  el.classList.add("visivel");
  clearTimeout(el._timer);
  el._timer = setTimeout(() => el.classList.remove("visivel"), 3200);
}

async function api(caminho, opcoes = {}) {
  const resposta = await fetch(caminho, {
    headers: { "Content-Type": "application/json" },
    ...opcoes,
  });
  if (!resposta.ok) {
    let mensagem = `Erro ${resposta.status}`;
    try {
      const erro = await resposta.json();
      if (erro && erro.erro) mensagem = erro.erro;
    } catch (_) { /* mantém mensagem padrão */ }
    throw new Error(mensagem);
  }
  return resposta.json();
}

// ===== Estado =====

const estado = {
  mes: new Date().toISOString().slice(0, 7),
  aba: "resumo",
  filtro: "todas",
  transacoes: [],
};

// ===== Resumo =====

async function carregarResumo() {
  const resumo = await api(`/api/resumo?mes=${estado.mes}`);

  const cartoes = [
    { rotulo: "Saldo atual", valor: fmtMoeda.format(resumo.saldoGeral), classe: classeValor(resumo.saldoGeral), detalhe: "todas as transações" },
    { rotulo: "Receitas do mês", valor: fmtMoeda.format(resumo.receitas), classe: "positivo", detalhe: fmtMes(resumo.mes) },
    { rotulo: "Despesas do mês", valor: fmtMoeda.format(resumo.despesas), classe: "negativo", detalhe: fmtMes(resumo.mes) },
    { rotulo: "Sobrou no mês", valor: fmtMoeda.format(resumo.resultado), classe: classeValor(resumo.resultado), detalhe: "receitas − despesas" },
    {
      rotulo: "Contas pendentes",
      valor: String(resumo.pendentes.quantidade),
      classe: "",
      destaque: true,
      detalhe: resumo.pendentes.quantidade
        ? `${fmtMoeda.format(resumo.pendentes.total)}${resumo.pendentes.vencidas ? ` · ${resumo.pendentes.vencidas} vencida(s)!` : ""}`
        : "nenhuma",
    },
  ];
  document.getElementById("cartoes-resumo").innerHTML = cartoes.map((c) => `
    <div class="cartao${c.destaque ? " destaque-amarelo" : ""}">
      <span class="rotulo">${c.rotulo}</span>
      <span class="valor ${c.classe}">${c.valor}</span>
      <span class="detalhe">${esc(c.detalhe)}</span>
    </div>`).join("");

  // gastos por categoria
  const categorias = resumo.porCategoria;
  document.getElementById("grafico-categorias").innerHTML = categorias.length
    ? categorias.map((c) => `
        <div class="barra-item">
          <div class="barra-info">
            <span class="nome">${esc(c.categoria)}</span>
            <span class="numero">${fmtMoeda.format(c.total)} · ${Number(c.percentual).toLocaleString("pt-BR", { maximumFractionDigits: 1 })}%</span>
          </div>
          <div class="trilho"><div class="preenchimento" style="width:${Math.min(c.percentual, 100)}%"></div></div>
        </div>`).join("")
    : `<p class="vazio">Nenhuma despesa neste mês.</p>`;

  // evolução (6 meses)
  const evolucao = resumo.evolucao;
  const maximo = Math.max(...evolucao.map((e) => e.total), 0.01);
  document.getElementById("grafico-evolucao").innerHTML = evolucao.map((e) => `
    <div class="coluna-item" title="${fmtMes(e.mes)}: ${fmtMoeda.format(e.total)}">
      <span class="valor-coluna">${fmtMoeda.format(e.total)}</span>
      <div class="coluna" style="height:${Math.max((e.total / maximo) * 100, 2)}%"></div>
      <span class="mes">${fmtMes(e.mes)}</span>
    </div>`).join("");
}

function classeValor(numero) {
  return numero > 0 ? "positivo" : numero < 0 ? "negativo" : "";
}

// ===== Transações =====

async function carregarTransacoes() {
  estado.transacoes = await api(`/api/transacoes?mes=${estado.mes}`);
  document.getElementById("titulo-transacoes").textContent = `Transações de ${fmtMes(estado.mes)}`;
  renderizarTransacoes();
}

function renderizarTransacoes() {
  const lista = estado.transacoes.filter((t) =>
    estado.filtro === "todas" || t.tipo === estado.filtro);
  const corpo = document.getElementById("corpo-transacoes");
  corpo.innerHTML = lista.length
    ? lista.map((t) => `
      <tr>
        <td>${fmtData(t.data)}</td>
        <td>${esc(t.descricao)}${t.observacao ? `<br><small class="obs">${esc(t.observacao)}</small>` : ""}</td>
        <td>${esc(t.categoria)}</td>
        <td>${esc(t.formaRotulo || t.formaPagamento)}</td>
        <td class="numero valor ${t.tipo === "RECEITA" ? "receita" : "despesa"}">
          ${t.tipo === "RECEITA" ? "+" : "−"} ${fmtMoeda.format(t.valor)}
        </td>
        <td>
          <div class="acao-icones">
            <button class="botao-icones" data-editar="${t.id}" title="Editar">✎</button>
            <button class="botao-icones perigo" data-excluir="${t.id}" title="Excluir">✕</button>
          </div>
        </td>
      </tr>`).join("")
    : `<tr><td colspan="6" class="vazio">Nenhuma transação ${estado.filtro !== "todas" ? "deste tipo " : ""}neste mês.</td></tr>`;
}

function abrirModalTransacao(transacao) {
  const modal = document.getElementById("modal-transacao");
  document.getElementById("titulo-modal").textContent = transacao ? "Editar transação" : "Nova transação";
  document.getElementById("t-id").value = transacao ? transacao.id : "";
  document.getElementById("t-tipo").value = transacao ? transacao.tipo : "DESPESA";
  document.getElementById("t-data").value = transacao ? transacao.data : new Date().toISOString().slice(0, 10);
  document.getElementById("t-valor").value = transacao ? transacao.valor : "";
  document.getElementById("t-descricao").value = transacao ? transacao.descricao : "";
  document.getElementById("t-categoria").value = transacao ? transacao.categoria : "";
  document.getElementById("t-forma").value = transacao ? transacao.formaPagamento : "PIX";
  document.getElementById("t-observacao").value = transacao && transacao.observacao ? transacao.observacao : "";
  modal.showModal();
}

async function salvarTransacao(evento) {
  evento.preventDefault();
  const id = document.getElementById("t-id").value;
  const corpo = JSON.stringify({
    data: document.getElementById("t-data").value,
    tipo: document.getElementById("t-tipo").value,
    valor: Number(document.getElementById("t-valor").value),
    descricao: document.getElementById("t-descricao").value.trim(),
    categoria: document.getElementById("t-categoria").value.trim(),
    formaPagamento: document.getElementById("t-forma").value,
    observacao: document.getElementById("t-observacao").value.trim(),
  });
  try {
    await api(id ? `/api/transacoes/${id}` : "/api/transacoes", {
      method: id ? "PUT" : "POST",
      body: corpo,
    });
    document.getElementById("modal-transacao").close();
    toast(id ? "Transação atualizada!" : "Transação registrada!");
    await atualizarAba();
  } catch (e) {
    toast(e.message, true);
  }
}

// ===== Contas =====

async function carregarContas() {
  const contas = await api("/api/contas");
  const corpo = document.getElementById("corpo-contas");
  corpo.innerHTML = contas.length
    ? contas.map((c) => {
      const vencida = c.status === "PENDENTE" && new Date(c.vencimento) < new Date(new Date().toDateString());
      const status = vencida
        ? `<span class="etiqueta alerta">! Vencida</span>`
        : `<span class="etiqueta ${c.status === "PENDENTE" ? "neutra" : "ok"}">${c.status === "PENDENTE" ? "Pendente" : c.status === "PAGA" ? "Paga" : "Recebida"}</span>`;
      return `
      <tr class="${vencida ? "vencida" : ""}">
        <td>${fmtData(c.vencimento)}</td>
        <td>${esc(c.nome)}</td>
        <td><span class="etiqueta ${c.tipo === "A_PAGAR" ? "despesa" : "receita"}">${c.tipo === "A_PAGAR" ? "A pagar" : "A receber"}</span></td>
        <td>${status}${c.dataPagamento ? `<br><small class="obs">em ${fmtData(c.dataPagamento)}</small>` : ""}</td>
        <td class="numero">${fmtMoeda.format(c.valor)}</td>
        <td>
          <div class="acao-icones">
            ${c.status === "PENDENTE" ? `<button class="botao-icones" data-pagar="${c.id}" title="Registrar pagamento">✓</button>` : ""}
            <button class="botao-icones perigo" data-excluir-conta="${c.id}" title="Excluir">✕</button>
          </div>
        </td>
      </tr>`;
    }).join("")
    : `<tr><td colspan="6" class="vazio">Nenhuma conta registrada.</td></tr>`;
}

async function salvarConta(evento) {
  evento.preventDefault();
  try {
    await api("/api/contas", {
      method: "POST",
      body: JSON.stringify({
        nome: document.getElementById("c-nome").value.trim(),
        tipo: document.getElementById("c-tipo").value,
        valor: Number(document.getElementById("c-valor").value),
        vencimento: document.getElementById("c-vencimento").value,
      }),
    });
    document.getElementById("modal-conta").close();
    toast("Conta registrada!");
    await carregarContas();
  } catch (e) {
    toast(e.message, true);
  }
}

// ===== Metas =====

async function carregarMetas() {
  const metas = await api("/api/metas");
  document.getElementById("lista-metas").innerHTML = metas.length
    ? metas.map((m) => {
      const falta = Math.max(m.valorObjetivo - m.valorAcumulado, 0);
      const pctNumero = m.valorObjetivo > 0 ? (m.valorAcumulado / m.valorObjetivo) * 100 : 0;
      const pct = Math.min(pctNumero, 100);
      const concluida = pctNumero >= 100;
      return `
      <div class="meta-card">
        <h3>${esc(m.nome)}${concluida ? " — concluída!" : ""}</h3>
        <div class="meta-numeros">
          <span>Objetivo: <strong>${fmtMoeda.format(m.valorObjetivo)}</strong></span>
          <span>Juntado: <strong>${fmtMoeda.format(m.valorAcumulado)}</strong></span>
          <span>Falta: <strong>${fmtMoeda.format(falta)}</strong></span>
        </div>
        <div class="meta-progresso">
          <div class="trilho" style="flex:1"><div class="preenchimento${concluida ? " cheio" : ""}" style="width:${pct}%"></div></div>
          <span class="percentual">${pct.toLocaleString("pt-BR", { maximumFractionDigits: 1 })}%</span>
        </div>
        <div class="meta-acoes">
          <button class="botao secundario" data-depositar="${m.id}">+ Depositar</button>
          <button class="botao secundario" data-excluir-meta="${m.id}">Excluir</button>
        </div>
      </div>`;
    }).join("")
    : `<p class="vazio">Nenhuma meta criada ainda.</p>`;
}

async function salvarMeta(evento) {
  evento.preventDefault();
  try {
    await api("/api/metas", {
      method: "POST",
      body: JSON.stringify({
        nome: document.getElementById("m-nome").value.trim(),
        valorObjetivo: Number(document.getElementById("m-objetivo").value),
      }),
    });
    document.getElementById("modal-meta").close();
    toast("Meta criada!");
    await carregarMetas();
  } catch (e) {
    toast(e.message, true);
  }
}

async function depositarNaMeta(id) {
  const resposta = window.prompt("Valor do depósito (R$):", "");
  if (resposta === null) return;
  const valor = Number(resposta.replace(",", "."));
  if (!valor || valor <= 0) {
    toast("Valor inválido.", true);
    return;
  }
  try {
    const meta = await api(`/api/metas/${id}/depositar`, {
      method: "POST",
      body: JSON.stringify({ valor }),
    });
    toast(`Depósito feito! ${meta.nome}: ${fmtMoeda.format(meta.valorAcumulado)} de ${fmtMoeda.format(meta.valorObjetivo)}`);
    await carregarMetas();
  } catch (e) {
    toast(e.message, true);
  }
}

// ===== Navegação e eventos =====

async function atualizarAba() {
  await Promise.all([carregarResumo(), carregarTransacoes()]);
  if (estado.aba === "contas") await carregarContas();
  if (estado.aba === "metas") await carregarMetas();
}

function trocarAba(aba) {
  estado.aba = aba;
  document.querySelectorAll(".aba").forEach((el) => el.classList.toggle("ativa", el.dataset.aba === aba));
  document.querySelectorAll(".secao").forEach((el) => el.classList.toggle("ativa", el.id === `aba-${aba}`));
  if (aba === "contas") carregarContas();
  if (aba === "metas") carregarMetas();
}

document.querySelectorAll(".aba").forEach((el) =>
  el.addEventListener("click", () => trocarAba(el.dataset.aba)));

document.querySelectorAll(".filtro").forEach((el) =>
  el.addEventListener("click", () => {
    estado.filtro = el.dataset.filtro;
    document.querySelectorAll(".filtro").forEach((f) => f.classList.toggle("ativo", f === el));
    renderizarTransacoes();
  }));

document.getElementById("mes").addEventListener("change", (e) => {
  estado.mes = e.target.value || estado.mes;
  atualizarAba().catch((erro) => toast(erro.message, true));
});

document.getElementById("btn-nova").addEventListener("click", () => abrirModalTransacao(null));
document.getElementById("btn-nova-conta").addEventListener("click", () =>
  document.getElementById("modal-conta").showModal());
document.getElementById("btn-nova-meta").addEventListener("click", () =>
  document.getElementById("modal-meta").showModal());

document.getElementById("btn-exportar").addEventListener("click", () => {
  window.location.href = `/api/exportar?mes=${estado.mes}`;
});

document.getElementById("form-transacao").addEventListener("submit", salvarTransacao);
document.getElementById("form-conta").addEventListener("submit", salvarConta);
document.getElementById("form-meta").addEventListener("submit", salvarMeta);

["t", "c", "m"].forEach((prefixo) =>
  document.getElementById(`${prefixo}-cancelar`).addEventListener("click", () =>
    document.getElementById(`modal-${prefixo === "t" ? "transacao" : prefixo === "c" ? "conta" : "meta"}`).close()));

// ações das tabelas (delegação de eventos)
document.body.addEventListener("click", (evento) => {
  const alvo = evento.target.closest("button");
  if (!alvo) return;
  if (alvo.dataset.editar) {
    const transacao = estado.transacoes.find((t) => t.id === Number(alvo.dataset.editar));
    if (transacao) abrirModalTransacao(transacao);
  }
  if (alvo.dataset.excluir) {
    const id = Number(alvo.dataset.excluir);
    if (window.confirm("Excluir esta transação?")) {
      api(`/api/transacoes/${id}`, { method: "DELETE" })
        .then(() => { toast("Transação excluída."); atualizarAba(); })
        .catch((e) => toast(e.message, true));
    }
  }
  if (alvo.dataset.pagar) {
    api(`/api/contas/${alvo.dataset.pagar}/pagar`, { method: "POST", body: "{}" })
      .then(() => { toast("Conta resolvida!"); carregarContas(); carregarResumo(); })
      .catch((e) => toast(e.message, true));
  }
  if (alvo.dataset.excluirConta) {
    if (window.confirm("Excluir esta conta?")) {
      api(`/api/contas/${alvo.dataset.excluirConta}`, { method: "DELETE" })
        .then(() => { toast("Conta excluída."); carregarContas(); carregarResumo(); })
        .catch((e) => toast(e.message, true));
    }
  }
  if (alvo.dataset.depositar) {
    depositarNaMeta(Number(alvo.dataset.depositar));
  }
  if (alvo.dataset.excluirMeta) {
    if (window.confirm("Excluir esta meta?")) {
      api(`/api/metas/${alvo.dataset.excluirMeta}`, { method: "DELETE" })
        .then(() => { toast("Meta excluída."); carregarMetas(); })
        .catch((e) => toast(e.message, true));
    }
  }
});

// ===== Início =====

document.getElementById("mes").value = estado.mes;
atualizarAba().catch((erro) => toast(erro.message, true));
