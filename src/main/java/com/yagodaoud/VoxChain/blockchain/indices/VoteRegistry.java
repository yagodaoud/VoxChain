package com.yagodaoud.VoxChain.blockchain.indices;

import com.yagodaoud.VoxChain.blockchain.Bloco;
import com.yagodaoud.VoxChain.modelo.Transacao;
import com.yagodaoud.VoxChain.modelo.Voto;
import com.yagodaoud.VoxChain.modelo.enums.TipoTransacao;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registro especializado para controle de votos.
 * Garante a integridade do processo eleitoral:
 * - Contabilização de votos por candidato
 * - Total de votos por eleição
 * - Resultados agregados (sem identificar votos individuais)
 */
public class VoteRegistry {
    // Chave: tokenVotacao -> Voto (para evitar duplicatas)
    private final Map<String, Voto> votosRegistrados;

    // Chave: eleicaoId -> contador de votos
    private final Map<String, Integer> totalVotosPorEleicao;

    // Chave: eleicaoId+candidatoNumero -> contador
    private final Map<String, Integer> contagemVotos;

    public VoteRegistry() {
        this.votosRegistrados = new ConcurrentHashMap<>();
        this.totalVotosPorEleicao = new ConcurrentHashMap<>();
        this.contagemVotos = new ConcurrentHashMap<>();
    }

    /**
     * Atualiza o registro com votos de um novo bloco
     */
    public void atualizarComBloco(Bloco bloco) {
        if (bloco == null) return;

        bloco.getTransacoes().stream()
                .filter(t -> t.getTipo() == TipoTransacao.VOTO)
                .map(t -> t.getPayloadAs(Voto.class))
                .filter(Objects::nonNull)
                .forEach(this::registrarVoto);
    }

    /**
     * Reconstrói o registro a partir da blockchain
     */
    public void reconstruirRegistro(List<Bloco> blocos) {
        limpar();

        System.out.println("[VOTOS] Reconstruindo registro de votos...");

        blocos.stream()
                .skip(1) // Pula gênesis
                .forEach(this::atualizarComBloco);

        System.out.println(String.format(
                "[VOTOS] Reconstruídos: %d votos em %d eleições",
                votosRegistrados.size(),
                totalVotosPorEleicao.size()
        ));
    }

    /**
     * Registra um voto no sistema
     */
    private void registrarVoto(Voto voto) {
        // Usa tokenVotacao como chave para evitar duplicatas
        votosRegistrados.put(voto.getTokenVotacao(), voto);

        // Incrementa total de votos na eleição
        totalVotosPorEleicao.merge(voto.getIdEleicao(), 1, Integer::sum);

        // Incrementa contagem do candidato
        String chaveContagem = criarChaveContagem(voto.getIdEleicao(), voto.getIdCandidato());
        contagemVotos.merge(chaveContagem, 1, Integer::sum);
    }

    /**
     * Conta quantos votos um candidato recebeu em uma eleição
     */
    public int contarVotosCandidato(String eleicaoId, String numeroCandidato) {
        String chave = criarChaveContagem(eleicaoId, numeroCandidato);
        return contagemVotos.getOrDefault(chave, 0);
    }

    /**
     * Obtém o total de votos em uma eleição
     */
    public int getTotalVotosEleicao(String eleicaoId) {
        return totalVotosPorEleicao.getOrDefault(eleicaoId, 0);
    }

    /**
     * Obtém resultado completo de uma eleição
     */
    public Map<String, Integer> obterResultadoEleicao(String eleicaoId) {
        return contagemVotos.entrySet().stream()
                .filter(e -> e.getKey().startsWith(eleicaoId + ":"))
                .collect(Collectors.toMap(
                        e -> e.getKey().split(":")[1], // Número do candidato
                        Map.Entry::getValue
                ));
    }

    /**
     * Valida a integridade dos votos (detecta duplicatas ou inconsistências)
     */
    public ValidationReport validarIntegridade() {
        ValidationReport report = new ValidationReport();

        // Verifica duplicatas por token
        Map<String, Long> votosPorToken = votosRegistrados.values().stream()
                .collect(Collectors.groupingBy(Voto::getTokenVotacao, Collectors.counting()));

        votosPorToken.forEach((token, count) -> {
            if (count > 1) {
                report.adicionarErro("Duplicata detectada: token " + token);
            }
        });

        // Verifica consistência da contagem
        Map<String, Long> contagemReal = votosRegistrados.values().stream()
                .collect(Collectors.groupingBy(
                        v -> criarChaveContagem(v.getIdEleicao(), v.getIdCandidato()),
                        Collectors.counting()
                ));

        contagemReal.forEach((chave, contagemEsperada) -> {
            Integer contagemRegistrada = contagemVotos.get(chave);
            if (contagemRegistrada == null || contagemRegistrada != contagemEsperada.intValue()) {
                report.adicionarErro(String.format(
                        "Inconsistência na contagem: %s (esperado: %d, registrado: %d)",
                        chave, contagemEsperada, contagemRegistrada
                ));
            }
        });

        return report;
    }

    /**
     * Limpa todos os registros
     */
    public void limpar() {
        votosRegistrados.clear();
        totalVotosPorEleicao.clear();
        contagemVotos.clear();
    }

    /**
     * Estatísticas gerais
     */
    public VoteStatistics getStatistics() {
        return new VoteStatistics(
                votosRegistrados.size(),
                totalVotosPorEleicao.size(),
                votosRegistrados.size() // Total de votos (tokens únicos)
        );
    }

    // ========== MÉTODOS AUXILIARES ==========

    private String criarChaveContagem(String eleicaoId, String numeroCandidato) {
        return eleicaoId + ":" + numeroCandidato;
    }

    // ========== CLASSES AUXILIARES ==========

    /**
     * Relatório de validação
     */
    public static class ValidationReport {
        private final List<String> erros;

        public ValidationReport() {
            this.erros = new ArrayList<>();
        }

        public void adicionarErro(String erro) {
            erros.add(erro);
        }

        public boolean isValido() {
            return erros.isEmpty();
        }

        public List<String> getErros() {
            return new ArrayList<>(erros);
        }

        @Override
        public String toString() {
            if (isValido()) {
                return "✓ Registro de votos íntegro";
            }
            return String.format("✗ %d erro(s) encontrado(s):\n- %s",
                    erros.size(),
                    String.join("\n- ", erros)
            );
        }
    }

    /**
     * Estatísticas de votos
     */
    public static class VoteStatistics {
        private final int totalVotos;
        private final int totalEleicoes;
        private final int totalEleitoresUnicos;

        public VoteStatistics(int totalVotos, int totalEleicoes, int totalEleitoresUnicos) {
            this.totalVotos = totalVotos;
            this.totalEleicoes = totalEleicoes;
            this.totalEleitoresUnicos = totalEleitoresUnicos;
        }

        public int getTotalVotos() {
            return totalVotos;
        }

        public int getTotalEleicoes() {
            return totalEleicoes;
        }

        public int getTotalEleitoresUnicos() {
            return totalEleitoresUnicos;
        }

        @Override
        public String toString() {
            return String.format(
                    "Estatísticas: %d votos | %d eleições | %d eleitores únicos",
                    totalVotos, totalEleicoes, totalEleitoresUnicos
            );
        }
    }
}