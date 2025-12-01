package com.yagodaoud.VoxChain.blockchain;

import com.yagodaoud.VoxChain.modelo.Transacao;
import com.yagodaoud.VoxChain.utils.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TransacaoTracker {
    private static final Map<String, TransacaoInfo> historico = new ConcurrentHashMap<>();

    public static class TransacaoInfo {
        public String id;
        public int vezesSeen = 0;
        public List<String> eventos = new ArrayList<>();
        public long primeiraVez;

        public TransacaoInfo(String id) {
            this.id = id;
            this.primeiraVez = System.currentTimeMillis();
        }

        public void registrar(String evento) {
            vezesSeen++;
            eventos.add("[" + System.currentTimeMillis() + "] " + evento);
        }
    }

    public static void rastrearAdicao(String noId, Transacao t, String origem) {
        if (t == null || t.getId() == null) return;

        String id = t.getId();
        TransacaoInfo info = historico.computeIfAbsent(id, TransacaoInfo::new);

        String evento = noId + " <- " + origem + " (Pool)";
        info.registrar(evento);

        if (info.vezesSeen > 1) {
            Logger.debug(noId, "⚠️  DUPLICATA DETECTADA! ID: " + id + " | Vezes visto: " + info.vezesSeen);
            for (String e : info.eventos) {
                Logger.debug(noId, "   Histórico: " + e);
            }
        }
    }

    public static void rastrearBroadcast(String noId, Transacao t) {
        if (t == null || t.getId() == null) return;

        String id = t.getId();
        TransacaoInfo info = historico.computeIfAbsent(id, TransacaoInfo::new);
        info.registrar(noId + " -> BROADCAST");
    }

    public static void rastrearRebroadcast(String noId, Transacao t, String peerOrigem) {
        if (t == null || t.getId() == null) return;

        String id = t.getId();
        TransacaoInfo info = historico.computeIfAbsent(id, TransacaoInfo::new);
        info.registrar(noId + " -> REBROADCAST (evitando " + peerOrigem + ")");
    }

    public static void exibirRelatorio() {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║        📊 RELATÓRIO DE TRANSAÇÕES              ║");
        System.out.println("╠════════════════════════════════════════════════╣");

        for (TransacaoInfo info : historico.values()) {
            System.out.println("║ ID: " + info.id);
            System.out.println("║ Ocorrências: " + info.vezesSeen);
            System.out.println("║ Histórico:");
            for (String evento : info.eventos) {
                System.out.println("║   " + evento);
            }
            System.out.println("╠════════════════════════════════════════════════╣");
        }

        System.out.println("║ TOTAL DE TRANSAÇÕES ÚNICAS: " + historico.size());
        System.out.println("╚════════════════════════════════════════════════╝\n");
    }

    public static int getTotalDuplicatas() {
        int total = 0;
        for (TransacaoInfo info : historico.values()) {
            total += (info.vezesSeen - 1);
        }
        return total;
    }

    public static void limpar() {
        historico.clear();
    }
}