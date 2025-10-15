package com.yagodaoud.VoxChain.blockchain.core;

import com.yagodaoud.VoxChain.blockchain.Bloco;
import com.yagodaoud.VoxChain.modelo.Transacao;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia o pool de transações pendentes.
 * Thread-safe para operações concorrentes.
 */
public class TransactionPool {
    private final List<Transacao> transacoesPendentes;
    private final Set<String> idsProcessados;
    private final int limiteTransacoesPorBloco;

    public TransactionPool(int limiteTransacoesPorBloco) {
        this.transacoesPendentes = new ArrayList<>();
        this.idsProcessados = ConcurrentHashMap.newKeySet();
        this.limiteTransacoesPorBloco = limiteTransacoesPorBloco;
    }

    public synchronized boolean adicionar(Transacao transacao) {
        if (transacao == null || transacao.getId() == null) {
            return false;
        }

        if (existe(transacao.getId())) {
            return false;
        }

        transacoesPendentes.add(transacao);
        return true;
    }

    public synchronized boolean existe(String idTransacao) {
        if (idTransacao == null) {
            return false;
        }

        // Verifica no pool
        for (Transacao t : transacoesPendentes) {
            if (t != null && idTransacao.equals(t.getId())) {
                return true;
            }
        }

        // Verifica no histórico de processadas
        return idsProcessados.contains(idTransacao);
    }

    public synchronized List<Transacao> obterParaBloco() {
        if (transacoesPendentes.isEmpty()) {
            return new ArrayList<>();
        }

        int quantidade = Math.min(limiteTransacoesPorBloco, transacoesPendentes.size());
        return new ArrayList<>(transacoesPendentes.subList(0, quantidade));
    }

    public synchronized void marcarComoProcessadas(List<Transacao> transacoes) {
        for (Transacao t : transacoes) {
            if (t != null && t.getId() != null) {
                transacoesPendentes.remove(t);
                idsProcessados.add(t.getId());
            }
        }
    }

    public synchronized void limpar() {
        transacoesPendentes.clear();
    }

    public synchronized int getTamanho() {
        return transacoesPendentes.size();
    }

    public synchronized boolean temTransacoes() {
        return !transacoesPendentes.isEmpty();
    }

    public synchronized List<Transacao> obterTodas() {
        return new ArrayList<>(transacoesPendentes);
    }

    /**
     * Reconstrói o histórico de transações processadas a partir da blockchain.
     * Útil após sincronização.
     */
    public synchronized void reconstruirHistorico(List<Bloco> blocos) {
        idsProcessados.clear();

        for (Bloco bloco : blocos) {
            for (Transacao t : bloco.getTransacoes()) {
                if (t != null && t.getId() != null) {
                    idsProcessados.add(t.getId());
                }
            }
        }
    }
}