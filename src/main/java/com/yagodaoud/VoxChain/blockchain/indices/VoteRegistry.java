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
 * - Um eleitor vota apenas uma vez por eleição
 * - Rastreamento de votos por eleição
 * - Contabilização de votos por candidato
 */
public class VoteRegistry {
    // Chave: eleitorHash+eleicaoId -> Voto
    private final Map<String, Voto> votosRegistrados;

    // Chave: eleicaoId -> Set<eleitorHash>
    private final Map<String, Set<String>> eleitoresPorEleicao;

    // Chave: eleicaoId+candidatoNumero -> contador
    private final Map<String, Integer> contagemVotos;

    public VoteRegistry() {
        this.votosRegistrados = new ConcurrentHashMap<>();
        this.eleitoresPorEleicao = new ConcurrentHashMap<>();
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
                eleitoresPorEleicao.size()
        ));
    }

    /**
     * Registra um voto no sistema
     */
    private void registrarVoto(Voto voto) {
        String chave = criarChaveVoto(voto.getIdEleitorHash(), voto.getIdEleicao());
        votosRegistrados.put(chave, voto);

        // Registra eleitor na eleição
        eleitoresPorEleicao
                .computeIfAbsent(voto.getIdEleicao(), k -> ConcurrentHashMap.newKeySet())
                .add(voto.getIdEleitorHash());

        // Incrementa contagem do candidato
        String chaveContagem = criarChaveContagem(voto.getIdEleicao(), voto.getIdCandidato());
        contagemVotos.merge(chaveContagem, 1, Integer::sum);
    }

    /**
     * Verifica se um eleitor já votou em determinada eleição
     */
    public boolean eleitorJaVotou(String eleitorHash, String eleicaoId) {
        String chave = criarChaveVoto(eleitorHash, eleicaoId);
        return votosRegistrados.containsKey(chave);
    }

    /**
     * Busca o voto de um eleitor em uma eleição específica
     */
    public Voto buscarVoto(String eleitorHash, String eleicaoId) {
        String chave = criarChaveVoto(eleitorHash, eleicaoId);
        return votosRegistrados.get(chave);
    }

    /**
     * Obtém todos os votos de uma eleição
     */
    public List<Voto> obterVotosPorEleicao(String eleicaoId) {
        return votosRegistrados.values().stream()
                .filter(v -> v.getIdEleicao().equals(eleicaoId))
                .collect(Collectors.toList());
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
        Set<String> eleitores = eleitoresPorEleicao.get(eleicaoId);
        return eleitores != null ? eleitores.size() : 0;
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
     * Lista os eleitores que votaram em uma eleição
     */
    public Set<String> obterEleitoresQueVotaram(String eleicaoId) {
        Set<String> eleitores = eleitoresPorEleicao.get(eleicaoId);
        return eleitores != null ? new HashSet<>(eleitores) : Collections.emptySet();
    }

    /**
     * Valida a integridade dos votos (detecta duplicatas ou inconsistências)
     */
    public ValidationReport validarIntegridade() {
        ValidationReport report = new ValidationReport();

        // Verifica duplicatas
        Map<String, List<Voto>> votosPorEleitor = votosRegistrados.values().stream()
                .collect(Collectors.groupingBy(v -> v.getIdEleitorHash() + ":" + v.getIdEleicao()));

        votosPorEleitor.forEach((chave, votos) -> {
            if (votos.size() > 1) {
                report.adicionarErro("Duplicata detectada: " + chave);
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
        eleitoresPorEleicao.clear();
        contagemVotos.clear();
    }

    /**
     * Estatísticas gerais
     */
    public VoteStatistics getStatistics() {
        return new VoteStatistics(
                votosRegistrados.size(),
                eleitoresPorEleicao.size(),
                eleitoresPorEleicao.values().stream()
                        .mapToInt(Set::size)
                        .sum()
        );
    }

    // ========== MÉTODOS AUXILIARES ==========

    private String criarChaveVoto(String eleitorHash, String eleicaoId) {
        return eleitorHash + ":" + eleicaoId;
    }

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