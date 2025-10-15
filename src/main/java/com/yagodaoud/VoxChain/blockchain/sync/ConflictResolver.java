package com.yagodaoud.VoxChain.blockchain.sync;

import com.yagodaoud.VoxChain.blockchain.Bloco;
import com.yagodaoud.VoxChain.blockchain.core.BlockValidator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Resolve conflitos entre cadeias divergentes (forks).
 * Implementa estratégias de consenso para escolher a cadeia válida.
 */
public class ConflictResolver {
    private final BlockValidator validator;
    private ConflictResolutionStrategy strategy;

    public ConflictResolver(BlockValidator validator) {
        this(validator, ConflictResolutionStrategy.LONGEST_CHAIN);
    }

    public ConflictResolver(BlockValidator validator, ConflictResolutionStrategy strategy) {
        this.validator = validator;
        this.strategy = strategy;
    }

    /**
     * Resolve conflito entre múltiplas cadeias
     */
    public ResolutionResult resolverConflito(List<Bloco> cadeiaLocal, List<Bloco> cadeiaRemota) {
        // Valida ambas as cadeias
        BlockValidator.ValidationResult validacaoLocal = validator.validarCadeia(cadeiaLocal);
        BlockValidator.ValidationResult validacaoRemota = validator.validarCadeia(cadeiaRemota);

        if (!validacaoLocal.isValido()) {
            return ResolutionResult.erro("Cadeia local inválida: " + validacaoLocal.getMensagem());
        }

        if (!validacaoRemota.isValido()) {
            return ResolutionResult.manter("Cadeia remota inválida: " + validacaoRemota.getMensagem());
        }

        // Detecta tipo de conflito
        ConflictType tipo = detectarTipoConflito(cadeiaLocal, cadeiaRemota);

        switch (tipo) {
            case NO_CONFLICT:
                return ResolutionResult.manter("Sem conflito - cadeias idênticas");

            case REMOTE_AHEAD:
                // Remota é continuação da local
                return ResolutionResult.sincronizar(
                        cadeiaRemota,
                        "Cadeia remota é continuação da local"
                );

            case LOCAL_AHEAD:
                // Local está à frente
                return ResolutionResult.manter("Cadeia local está à frente");

            case FORK:
                // Fork detectado - aplica estratégia
                return resolverFork(cadeiaLocal, cadeiaRemota);

            default:
                return ResolutionResult.erro("Tipo de conflito desconhecido");
        }
    }

    /**
     * Resolve fork usando a estratégia configurada
     */
    private ResolutionResult resolverFork(List<Bloco> cadeiaLocal, List<Bloco> cadeiaRemota) {
        System.out.println("[FORK] Fork detectado! Aplicando estratégia: " + strategy);

        switch (strategy) {
            case LONGEST_CHAIN:
                return resolverPorTamanho(cadeiaLocal, cadeiaRemota);

            case MOST_WORK:
                return resolverPorTrabalho(cadeiaLocal, cadeiaRemota);

            case FIRST_SEEN:
                return ResolutionResult.manter("Mantendo primeira cadeia vista (local)");

            default:
                return ResolutionResult.erro("Estratégia não implementada: " + strategy);
        }
    }

    /**
     * Detecta o tipo de conflito entre cadeias
     */
    private ConflictType detectarTipoConflito(List<Bloco> cadeiaLocal, List<Bloco> cadeiaRemota) {
        if (cadeiaLocal.size() == cadeiaRemota.size() &&
                cadeiaLocal.get(cadeiaLocal.size() - 1).getHash()
                        .equals(cadeiaRemota.get(cadeiaRemota.size() - 1).getHash())) {
            return ConflictType.NO_CONFLICT;
        }

        // Encontra ponto de divergência
        int pontoDivergencia = encontrarPontoDivergencia(cadeiaLocal, cadeiaRemota);

        if (pontoDivergencia == -1) {
            // Gênesis diferente - cadeias incompatíveis
            return ConflictType.FORK;
        }

        // Verifica se é continuação ou fork
        if (pontoDivergencia == cadeiaLocal.size() - 1) {
            return ConflictType.REMOTE_AHEAD;
        }

        if (pontoDivergencia == cadeiaRemota.size() - 1) {
            return ConflictType.LOCAL_AHEAD;
        }

        return ConflictType.FORK;
    }

    /**
     * Encontra o índice onde as cadeias divergem
     */
    private int encontrarPontoDivergencia(List<Bloco> cadeiaLocal, List<Bloco> cadeiaRemota) {
        int tamanhoMinimo = Math.min(cadeiaLocal.size(), cadeiaRemota.size());

        for (int i = 0; i < tamanhoMinimo; i++) {
            if (!cadeiaLocal.get(i).getHash().equals(cadeiaRemota.get(i).getHash())) {
                return i - 1; // Último bloco em comum
            }
        }

        return tamanhoMinimo - 1;
    }

    /**
     * Resolve por tamanho (regra da cadeia mais longa)
     */
    private ResolutionResult resolverPorTamanho(List<Bloco> cadeiaLocal, List<Bloco> cadeiaRemota) {
        if (cadeiaRemota.size() > cadeiaLocal.size()) {
            return ResolutionResult.sincronizar(
                    cadeiaRemota,
                    String.format("Cadeia remota mais longa (%d vs %d blocos)",
                            cadeiaRemota.size(), cadeiaLocal.size())
            );
        } else if (cadeiaRemota.size() == cadeiaLocal.size()) {
            // Empate - usa hash como desempate
            String hashLocal = cadeiaLocal.get(cadeiaLocal.size() - 1).getHash();
            String hashRemoto = cadeiaRemota.get(cadeiaRemota.size() - 1).getHash();

            if (hashRemoto.compareTo(hashLocal) < 0) {
                return ResolutionResult.sincronizar(
                        cadeiaRemota,
                        "Empate no tamanho - hash remoto menor (desempate)"
                );
            }
        }

        return ResolutionResult.manter(
                String.format("Cadeia local mantida (%d >= %d blocos)",
                        cadeiaLocal.size(), cadeiaRemota.size())
        );
    }

    /**
     * Resolve por trabalho acumulado (dificuldade)
     */
    private ResolutionResult resolverPorTrabalho(List<Bloco> cadeiaLocal, List<Bloco> cadeiaRemota) {
        long trabalhoLocal = calcularTrabalhoAcumulado(cadeiaLocal);
        long trabalhoRemoto = calcularTrabalhoAcumulado(cadeiaRemota);

        if (trabalhoRemoto > trabalhoLocal) {
            return ResolutionResult.sincronizar(
                    cadeiaRemota,
                    String.format("Cadeia remota com mais trabalho (%d vs %d)",
                            trabalhoRemoto, trabalhoLocal)
            );
        }

        return ResolutionResult.manter(
                String.format("Cadeia local mantida (trabalho: %d >= %d)",
                        trabalhoLocal, trabalhoRemoto)
        );
    }

    /**
     * Calcula trabalho acumulado (baseado em dificuldade)
     */
    private long calcularTrabalhoAcumulado(List<Bloco> cadeia) {
        return cadeia.stream()
                .skip(1) // Pula gênesis
                .mapToLong(bloco -> {
                    // Trabalho = 2^dificuldade para cada bloco
                    int zeros = contarZerosIniciais(bloco.getHash());
                    return (long) Math.pow(2, zeros);
                })
                .sum();
    }

    /**
     * Conta zeros iniciais no hash (indicador de dificuldade)
     */
    private int contarZerosIniciais(String hash) {
        int count = 0;
        for (char c : hash.toCharArray()) {
            if (c == '0') {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    /**
     * Analisa fork e retorna informações detalhadas
     */
    public ForkAnalysis analisarFork(List<Bloco> cadeiaLocal, List<Bloco> cadeiaRemota) {
        int pontoDivergencia = encontrarPontoDivergencia(cadeiaLocal, cadeiaRemota);

        List<Bloco> blocosLocal = pontoDivergencia >= 0
                ? cadeiaLocal.subList(pontoDivergencia + 1, cadeiaLocal.size())
                : cadeiaLocal;

        List<Bloco> blocosRemoto = pontoDivergencia >= 0
                ? cadeiaRemota.subList(pontoDivergencia + 1, cadeiaRemota.size())
                : cadeiaRemota;

        return new ForkAnalysis(
                pontoDivergencia,
                blocosLocal,
                blocosRemoto,
                calcularTrabalhoAcumulado(blocosLocal),
                calcularTrabalhoAcumulado(blocosRemoto)
        );
    }

    public void setStrategy(ConflictResolutionStrategy strategy) {
        this.strategy = strategy;
    }

    // ========== ENUMS E CLASSES AUXILIARES ==========

    public enum ConflictType {
        NO_CONFLICT,      // Cadeias idênticas
        REMOTE_AHEAD,     // Remota é continuação da local
        LOCAL_AHEAD,      // Local está à frente
        FORK              // Divergência real (fork)
    }

    public enum ConflictResolutionStrategy {
        LONGEST_CHAIN,    // Cadeia mais longa vence (Bitcoin)
        MOST_WORK,        // Maior trabalho acumulado
        FIRST_SEEN        // Primeira cadeia vista (mantém local)
    }

    /**
     * Resultado da resolução de conflito
     */
    public static class ResolutionResult {
        private final ResolutionAction action;
        private final List<Bloco> cadeiaEscolhida;
        private final String mensagem;

        private ResolutionResult(ResolutionAction action, List<Bloco> cadeiaEscolhida, String mensagem) {
            this.action = action;
            this.cadeiaEscolhida = cadeiaEscolhida;
            this.mensagem = mensagem;
        }

        public static ResolutionResult sincronizar(List<Bloco> cadeia, String mensagem) {
            return new ResolutionResult(ResolutionAction.SYNC, cadeia, mensagem);
        }

        public static ResolutionResult manter(String mensagem) {
            return new ResolutionResult(ResolutionAction.KEEP, null, mensagem);
        }

        public static ResolutionResult erro(String mensagem) {
            return new ResolutionResult(ResolutionAction.ERROR, null, mensagem);
        }

        public boolean deveSincronizar() {
            return action == ResolutionAction.SYNC;
        }

        public ResolutionAction getAction() {
            return action;
        }

        public List<Bloco> getCadeiaEscolhida() {
            return cadeiaEscolhida;
        }

        public String getMensagem() {
            return mensagem;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s", action, mensagem);
        }
    }

    public enum ResolutionAction {
        SYNC,   // Sincronizar com cadeia remota
        KEEP,   // Manter cadeia local
        ERROR   // Erro na resolução
    }

    /**
     * Análise detalhada de um fork
     */
    public static class ForkAnalysis {
        private final int pontoDivergencia;
        private final List<Bloco> blocosLocal;
        private final List<Bloco> blocosRemoto;
        private final long trabalhoLocal;
        private final long trabalhoRemoto;

        public ForkAnalysis(int pontoDivergencia, List<Bloco> blocosLocal,
                            List<Bloco> blocosRemoto, long trabalhoLocal, long trabalhoRemoto) {
            this.pontoDivergencia = pontoDivergencia;
            this.blocosLocal = blocosLocal;
            this.blocosRemoto = blocosRemoto;
            this.trabalhoLocal = trabalhoLocal;
            this.trabalhoRemoto = trabalhoRemoto;
        }

        public int getPontoDivergencia() {
            return pontoDivergencia;
        }

        public int getTamanhoBranchLocal() {
            return blocosLocal.size();
        }

        public int getTamanhoBranchRemoto() {
            return blocosRemoto.size();
        }

        public long getTrabalhoLocal() {
            return trabalhoLocal;
        }

        public long getTrabalhoRemoto() {
            return trabalhoRemoto;
        }

        @Override
        public String toString() {
            return String.format(
                    "Fork Analysis:\n" +
                            "  Ponto de divergência: bloco #%d\n" +
                            "  Branch local: %d blocos, trabalho=%d\n" +
                            "  Branch remoto: %d blocos, trabalho=%d",
                    pontoDivergencia,
                    blocosLocal.size(), trabalhoLocal,
                    blocosRemoto.size(), trabalhoRemoto
            );
        }
    }
}