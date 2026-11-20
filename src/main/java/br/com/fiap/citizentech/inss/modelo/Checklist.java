package br.com.fiap.citizentech.inss.modelo;

import java.util.List;

/** Lista de documentos de uma exigência, com as regras de "está pronto para enviar?". */
public class Checklist {
    private final List<ItemChecklist> itens;

    public Checklist(List<ItemChecklist> itens) {
        this.itens = List.copyOf(itens);
    }

    public List<ItemChecklist> itens() {
        return itens;
    }

    public boolean obrigatoriosProntos() {
        return itens.stream().filter(ItemChecklist::isObrigatorio).allMatch(i -> i.getStatus().pronto());
    }

    public List<ItemChecklist> obrigatoriosPendentes() {
        return itens.stream().filter(i -> i.isObrigatorio() && !i.getStatus().pronto()).toList();
    }

    public List<ItemChecklist> prontos() {
        return itens.stream().filter(i -> i.getStatus().pronto()).toList();
    }

    /** Há documento sem solução clara (dúvida ou ilegível)? */
    public boolean temDuvidaOuIlegivel() {
        return itens.stream().anyMatch(i -> i.getStatus() == StatusItem.DUVIDA || i.getStatus() == StatusItem.ILEGIVEL);
    }
}
