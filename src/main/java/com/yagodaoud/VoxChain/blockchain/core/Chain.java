package com.yagodaoud.VoxChain.blockchain.core;

import com.yagodaoud.VoxChain.blockchain.Bloco;
import com.yagodaoud.VoxChain.modelo.Transacao;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsável apenas pelo gerenciamento da cadeia de blocos.
 * Mantém a lista de blocos e operações básicas.
 */
public class Chain {
    private final List<Bloco> blocos;
    private final int dificuldade;

    public Chain(int dificuldade) {
        this.blocos = new ArrayList<>();
        this.dificuldade = dificuldade;
        criarBlocoGenesis();
    }

    private void criarBlocoGenesis() {
        Bloco genesis = new Bloco(
                0,
                new ArrayList<>(),
                "0",
                "SYSTEM",
                1700000000000L
        );
        genesis.minerarBloco(dificuldade);
        blocos.add(genesis);
    }

    public void adicionarBloco(Bloco bloco) {
        if (bloco == null) {
            throw new IllegalArgumentException("Bloco não pode ser nulo");
        }
        blocos.add(bloco);
    }

    public Bloco obterUltimoBloco() {
        return blocos.get(blocos.size() - 1);
    }

    public Bloco obterBloco(int indice) {
        if (indice < 0 || indice >= blocos.size()) {
            throw new IndexOutOfBoundsException("Índice de bloco inválido: " + indice);
        }
        return blocos.get(indice);
    }

    public List<Bloco> obterTodosBlocos() {
        return new ArrayList<>(blocos);
    }

    public void substituirCadeia(List<Bloco> novaCadeia) {
        if (novaCadeia == null || novaCadeia.isEmpty()) {
            throw new IllegalArgumentException("Cadeia inválida");
        }
        blocos.clear();
        blocos.addAll(novaCadeia);
    }

    public int getTamanho() {
        return blocos.size();
    }

    public int getTotalTransacoes() {
        return blocos.stream()
                .skip(1) // Pula gênesis
                .mapToInt(b -> b.getTransacoes().size())
                .sum();
    }

    public int getDificuldade() {
        return dificuldade;
    }

    public Bloco criarBlocoCandidato(
            List<Transacao> transacoes,
            String mineradoPor,
            Long timestampFixo
    ) {
        return new Bloco(
                blocos.size(),
                transacoes,
                obterUltimoBloco().getHash(),
                mineradoPor,
                timestampFixo
        );
    }
}