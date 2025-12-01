package com.yagodaoud.VoxChain.rede.api.v1;

import com.google.gson.Gson;
import com.yagodaoud.VoxChain.blockchain.servicos.eleicao.ServicoFechamentoEleicao;
import com.yagodaoud.VoxChain.blockchain.servicos.eleicao.ServicoFechamentoEleicao.ResultadoEleicao;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static spark.Spark.*;

/**
 * Controller REST para gerenciamento de resultados de eleições.
 *
 * Endpoints:
 * - POST /resultados/fechar/:eleicaoId - Fecha uma eleição
 * - GET /resultados/:eleicaoId - Obtém resultado de uma eleição
 * - GET /resultados/:eleicaoId/pode-fechar - Verifica se pode fechar
 */
public class ResultadosController implements IApiController {

    private final ServicoFechamentoEleicao servicoFechamento;

    public ResultadosController(ServicoFechamentoEleicao servicoFechamento) {
        this.servicoFechamento = servicoFechamento;
    }

    @Override
    public void registerRoutes(Gson gson) {
        path("/resultados", () -> {

            // POST /api/v1/resultados/fechar/:eleicaoId
            // Fecha uma eleição (requer autenticação de admin)
            post("/fechar/:eleicaoId", (req, res) -> {
                res.type("application/json");

                String eleicaoId = req.params("eleicaoId");
                String solicitanteId = req.attribute("cpfHash"); // Do JWT

                try {
                    servicoFechamento.fecharEleicao(eleicaoId, solicitanteId);

                    res.status(200);
                    return gson.toJson(Map.of(
                            "mensagem", "Eleição fechada com sucesso",
                            "eleicaoId", eleicaoId
                    ));

                } catch (IllegalStateException e) {
                    res.status(400);
                    return gson.toJson(Map.of("erro", e.getMessage()));

                } catch (Exception e) {
                    res.status(500);
                    return gson.toJson(Map.of(
                            "erro", "Erro ao fechar eleição",
                            "mensagem", e.getMessage()
                    ));
                }
            });

            // GET /api/v1/resultados/:eleicaoId
            // Obtém resultado completo da eleição
            get("/:eleicaoId", (req, res) -> {
                res.type("application/json");

                String eleicaoId = req.params("eleicaoId");

                try {
                    ResultadoEleicao resultado = servicoFechamento.apurarResultados(eleicaoId);

                    // Converte para formato JSON amigável
                    Map<String, Object> resposta = new HashMap<>();
                    resposta.put("eleicaoId", resultado.getEleicaoId());
                    resposta.put("nomeEleicao", resultado.getNomeEleicao());
                    resposta.put("totalVotos", resultado.getTotalVotos());

                    // Resultados dos candidatos
                    resposta.put("candidatos", resultado.getResultados().stream()
                            .map(r -> Map.of(
                                    "numero", r.getNumero(),
                                    "nome", r.getNome(),
                                    "partido", r.getPartido(),
                                    "votos", r.getVotos(),
                                    "percentual", String.format("%.2f", r.getPercentual())
                            ))
                            .collect(Collectors.toList())
                    );

                    // Vencedor
                    if (resultado.getVencedor() != null) {
                        var v = resultado.getVencedor();
                        resposta.put("vencedor", Map.of(
                                "numero", v.getNumero(),
                                "nome", v.getNome(),
                                "partido", v.getPartido(),
                                "votos", v.getVotos(),
                                "percentual", String.format("%.2f", v.getPercentual())
                        ));
                    }

                    res.status(200);
                    return gson.toJson(resposta);

                } catch (IllegalArgumentException e) {
                    res.status(404);
                    return gson.toJson(Map.of("erro", e.getMessage()));

                } catch (Exception e) {
                    res.status(500);
                    return gson.toJson(Map.of(
                            "erro", "Erro ao apurar resultados",
                            "mensagem", e.getMessage()
                    ));
                }
            });

            // GET /api/v1/resultados/:eleicaoId/pode-fechar
            // Verifica se eleição pode ser fechada
            get("/:eleicaoId/pode-fechar", (req, res) -> {
                res.type("application/json");

                String eleicaoId = req.params("eleicaoId");

                try {
                    boolean podeFechar = servicoFechamento.podeFecharEleicao(eleicaoId);

                    res.status(200);
                    return gson.toJson(Map.of(
                            "eleicaoId", eleicaoId,
                            "podeFechar", podeFechar,
                            "mensagem", podeFechar
                                    ? "Eleição pode ser fechada"
                                    : "Eleição ainda não atingiu data de término"
                    ));

                } catch (IllegalArgumentException e) {
                    res.status(404);
                    return gson.toJson(Map.of("erro", e.getMessage()));

                } catch (Exception e) {
                    res.status(500);
                    return gson.toJson(Map.of("erro", e.getMessage()));
                }
            });

            // GET /api/v1/resultados/:eleicaoId/parcial
            // Obtém resultado parcial (mesmo com eleição aberta)
            get("/:eleicaoId/parcial", (req, res) -> {
                res.type("application/json");

                String eleicaoId = req.params("eleicaoId");

                try {
                    ResultadoEleicao resultado = servicoFechamento.apurarResultados(eleicaoId);

                    Map<String, Object> resposta = new HashMap<>();
                    resposta.put("eleicaoId", resultado.getEleicaoId());
                    resposta.put("nomeEleicao", resultado.getNomeEleicao());
                    resposta.put("totalVotos", resultado.getTotalVotos());
                    resposta.put("parcial", true);

                    // Apenas totais, sem detalhes de candidatos (para evitar influenciar)
                    resposta.put("candidatos", resultado.getResultados().stream()
                            .map(r -> Map.of(
                                    "numero", r.getNumero(),
                                    "votos", r.getVotos(),
                                    "percentual", String.format("%.2f", r.getPercentual())
                            ))
                            .collect(Collectors.toList())
                    );

                    res.status(200);
                    return gson.toJson(resposta);

                } catch (Exception e) {
                    res.status(500);
                    return gson.toJson(Map.of("erro", e.getMessage()));
                }
            });
        });
    }
}