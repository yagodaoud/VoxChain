package com.yagodaoud.VoxChain.blockchain.servicos.eleicao;

import com.yagodaoud.VoxChain.blockchain.Bloco;
import com.yagodaoud.VoxChain.blockchain.BlockchainGovernamental;
import com.yagodaoud.VoxChain.modelo.Candidato;
import com.yagodaoud.VoxChain.modelo.Eleicao;
import com.yagodaoud.VoxChain.modelo.Transacao;
import com.yagodaoud.VoxChain.modelo.Voto;
import com.yagodaoud.VoxChain.modelo.enums.TipoTransacao;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço responsável pelo fechamento e apuração de eleições.
 *
 * Funcionalidades:
 * - Verifica se eleição pode ser fechada (data fim ultrapassada)
 * - Registra transação de fechamento na blockchain
 * - Apura resultados (contagem de votos por candidato)
 * - Calcula percentuais e determina vencedor
 */
public class ServicoFechamentoEleicao {

    private final BlockchainGovernamental blockchain;

    public ServicoFechamentoEleicao(BlockchainGovernamental blockchain) {
        this.blockchain = blockchain;
    }

    /**
     * Verifica se uma eleição pode ser fechada
     */
    public boolean podeFecharEleicao(String eleicaoId) {
        Eleicao eleicao = blockchain.buscarEleicao(eleicaoId);

        if (eleicao == null) {
            throw new IllegalArgumentException("Eleição não encontrada: " + eleicaoId);
        }

        long agora = System.currentTimeMillis();
        return agora > eleicao.getDataFim();
    }

    /**
     * Fecha uma eleição adicionando transação de fechamento à blockchain
     */
    public void fecharEleicao(String eleicaoId, String solicitanteId) {
        if (!podeFecharEleicao(eleicaoId)) {
            throw new IllegalStateException("Eleição ainda não pode ser fechada - data fim não alcançada");
        }

        Eleicao eleicao = blockchain.buscarEleicao(eleicaoId);

        // Cria transação de fechamento
        Transacao transacao = new Transacao(
                TipoTransacao.FIM_ELEICAO,
                eleicao,
                solicitanteId
        );

        blockchain.adicionarAoPool(transacao);

        System.out.println("[ELEIÇÃO] Eleição " + eleicaoId + " fechada por " + solicitanteId);
    }

    /**
     * Apura resultados de uma eleição
     */
    public ResultadoEleicao apurarResultados(String eleicaoId) {
        Eleicao eleicao = blockchain.buscarEleicao(eleicaoId);

        if (eleicao == null) {
            throw new IllegalArgumentException("Eleição não encontrada");
        }

        // Coleta todos os votos da eleição
        Map<String, Integer> votosPorCandidato = new HashMap<>();
        int totalVotos = 0;

        for (Bloco bloco : blockchain.getBlocos()) {
            for (Transacao transacao : bloco.getTransacoes()) {
                if (transacao.getTipo() == TipoTransacao.VOTO) {
                    Voto voto = transacao.getPayloadAs(Voto.class);

                    if (voto != null && voto.getIdEleicao().equals(eleicaoId)) {
                        String candidatoId = voto.getIdCandidato();
                        votosPorCandidato.merge(candidatoId, 1, Integer::sum);
                        totalVotos++;
                    }
                }
            }
        }

        // Calcula percentuais
        List<ResultadoCandidato> resultados = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : votosPorCandidato.entrySet()) {
            String candidatoNumero = entry.getKey();
            int votos = entry.getValue();
            double percentual = totalVotos > 0 ? (votos * 100.0) / totalVotos : 0.0;

            Candidato candidato = blockchain.buscarCandidato(candidatoNumero);

            resultados.add(new ResultadoCandidato(
                    candidatoNumero,
                    candidato != null ? candidato.getNome() : "Desconhecido",
                    candidato != null ? candidato.getPartido() : "N/A",
                    votos,
                    percentual
            ));
        }

        // Ordena por votos (decrescente)
        resultados.sort((a, b) -> Integer.compare(b.getVotos(), a.getVotos()));

        // Determina vencedor
        ResultadoCandidato vencedor = resultados.isEmpty() ? null : resultados.get(0);

        return new ResultadoEleicao(
                eleicaoId,
                eleicao.getNome(),
                totalVotos,
                resultados,
                vencedor
        );
    }

    /**
     * Classe que representa o resultado de um candidato
     */
    public static class ResultadoCandidato {
        private final String numero;
        private final String nome;
        private final String partido;
        private final int votos;
        private final double percentual;

        public ResultadoCandidato(String numero, String nome, String partido,
                                  int votos, double percentual) {
            this.numero = numero;
            this.nome = nome;
            this.partido = partido;
            this.votos = votos;
            this.percentual = percentual;
        }

        public String getNumero() { return numero; }
        public String getNome() { return nome; }
        public String getPartido() { return partido; }
        public int getVotos() { return votos; }
        public double getPercentual() { return percentual; }

        @Override
        public String toString() {
            return String.format("%s (%s - %s): %d votos (%.2f%%)",
                    nome, numero, partido, votos, percentual);
        }
    }

    /**
     * Classe que representa o resultado completo de uma eleição
     */
    public static class ResultadoEleicao {
        private final String eleicaoId;
        private final String nomeEleicao;
        private final int totalVotos;
        private final List<ResultadoCandidato> resultados;
        private final ResultadoCandidato vencedor;

        public ResultadoEleicao(String eleicaoId, String nomeEleicao, int totalVotos,
                                List<ResultadoCandidato> resultados, ResultadoCandidato vencedor) {
            this.eleicaoId = eleicaoId;
            this.nomeEleicao = nomeEleicao;
            this.totalVotos = totalVotos;
            this.resultados = resultados;
            this.vencedor = vencedor;
        }

        public String getEleicaoId() { return eleicaoId; }
        public String getNomeEleicao() { return nomeEleicao; }
        public int getTotalVotos() { return totalVotos; }
        public List<ResultadoCandidato> getResultados() { return resultados; }
        public ResultadoCandidato getVencedor() { return vencedor; }

        /**
         * Exibe resultado formatado para apresentação
         */
        public void exibirResultado() {
            System.out.println("\n╔════════════════════════════════════════════════════════╗");
            System.out.println("║         📊 RESULTADO DA ELEIÇÃO                        ║");
            System.out.println("╠════════════════════════════════════════════════════════╣");
            System.out.println("║ Eleição: " + String.format("%-43s", nomeEleicao) + " ║");
            System.out.println("║ Total de Votos: " + String.format("%-37s", totalVotos) + " ║");
            System.out.println("╠════════════════════════════════════════════════════════╣");

            int posicao = 1;
            for (ResultadoCandidato resultado : resultados) {
                String linha = String.format("%dº %s: %d votos (%.1f%%)",
                        posicao++,
                        resultado.getNome(),
                        resultado.getVotos(),
                        resultado.getPercentual()
                );
                System.out.println("║ " + String.format("%-53s", linha) + " ║");
            }

            if (vencedor != null) {
                System.out.println("╠════════════════════════════════════════════════════════╣");
                System.out.println("║ 🏆 VENCEDOR: " + String.format("%-41s", vencedor.getNome()) + " ║");
            }

            System.out.println("╚════════════════════════════════════════════════════════╝\n");
        }
    }
}