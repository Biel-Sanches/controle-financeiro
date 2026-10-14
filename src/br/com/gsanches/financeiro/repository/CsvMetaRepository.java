package br.com.gsanches.financeiro.repository;

import br.com.gsanches.financeiro.model.Meta;

import java.math.BigDecimal;

/**
 * Formato da linha: id;nome;valorObjetivo;valorAcumulado
 */
public class CsvMetaRepository extends CsvRepository<Meta> implements MetaRepository {

    public CsvMetaRepository() {
        super("metas.csv");
    }

    @Override
    protected Meta fromLinha(String[] c) {
        return new Meta(
                Long.parseLong(c[0]),
                c[1],
                new BigDecimal(c[2]),
                new BigDecimal(c[3]));
    }

    @Override
    protected String toLinha(Meta meta) {
        return String.join(";",
                String.valueOf(meta.id()),
                limpar(meta.nome()),
                meta.valorObjetivo().toPlainString(),
                meta.valorAcumulado().toPlainString());
    }

    @Override
    protected long idDe(Meta meta) {
        return meta.id();
    }

    @Override
    protected Meta comId(Meta meta, long novoId) {
        return new Meta(novoId, meta.nome(), meta.valorObjetivo(), meta.valorAcumulado());
    }
}
