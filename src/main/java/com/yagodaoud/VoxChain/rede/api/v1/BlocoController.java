package com.yagodaoud.VoxChain.rede.api.v1;

import com.google.gson.Gson;
import com.yagodaoud.VoxChain.blockchain.Bloco;
import com.yagodaoud.VoxChain.blockchain.No;
import com.yagodaoud.VoxChain.modelo.Transacao;
import com.yagodaoud.VoxChain.modelo.Voto;
import com.yagodaoud.VoxChain.modelo.enums.TipoTransacao;

import java.util.*;
import java.util.stream.Collectors;

import static spark.Spark.*;

/**
 * Controller para operações relacionadas a blocos individuais
 */
public class BlocoController implements IApiController {

    private final No no;

    public BlocoController(No no) {
        this.no = no;
    }

    @Override
    public void registerRoutes(Gson gson) {
        path("/blocos", () -> {

            // GET /api/v1/blocos/:hash - Busca bloco por hash
            get("/:hash", (req, res) -> {
                res.type("application/json");
                String hash = req.params("hash");

                try {
                    Bloco bloco = buscarBlocoPorHash(hash);

                    if (bloco == null) {
                        res.status(404);
                        return gson.toJson(Map.of(
                                "erro", "Bloco não encontrado",
                                "hash", hash));
                    }

                    // Converte bloco para formato com votos
                    Map<String, Object> blocoData = new HashMap<>();
                    blocoData.put("indice", bloco.getIndice());
                    blocoData.put("hash", bloco.getHash());
                    blocoData.put("hashAnterior", bloco.getHashAnterior());
                    blocoData.put("timestamp", bloco.getTimestamp());
                    blocoData.put("nonce", bloco.getNonce());
                    blocoData.put("mineradoPor", bloco.getMineradoPor());
                    blocoData.put("votos", extrairVotosDoBloco(bloco));
                    blocoData.put("totalTransacoes", bloco.getTransacoes().size());

                    res.status(200);
                    return gson.toJson(blocoData);

                } catch (Exception e) {
                    res.status(500);
                    return gson.toJson(Map.of(
                            "erro", "Erro ao buscar bloco",
                            "mensagem", e.getMessage()));
                }
            });

            // POST /api/v1/blocos/:hash/validar - Valida um bloco específico
            post("/:hash/validar", (req, res) -> {
                res.type("application/json");
                String hash = req.params("hash");

                try {
                    Bloco bloco = buscarBlocoPorHash(hash);

                    if (bloco == null) {
                        res.status(404);
                        return gson.toJson(Map.of(
                                "valido", false,
                                "mensagem", "Bloco não encontrado"));
                    }

                    boolean valido = no.getBlockchain().validarBlocoContextual(bloco);

                    Map<String, Object> resultado = new HashMap<>();
                    resultado.put("valido", valido);
                    resultado.put("mensagem", valido
                            ? "Bloco estruturalmente válido em seu contexto original"
                            : "Bloco inválido ou corrompido");
                    resultado.put("hash", hash);
                    resultado.put("indice", bloco.getIndice());

                    res.status(200);
                    return gson.toJson(resultado);

                } catch (Exception e) {
                    res.status(500);
                    return gson.toJson(Map.of(
                            "valido", false,
                            "mensagem", "Erro ao validar bloco: " + e.getMessage()));
                }
            });

            // GET /api/v1/blocos/:hash/votos - Busca todos os votos de um bloco
            get("/:hash/votos", (req, res) -> {
                res.type("application/json");
                String hash = req.params("hash");

                try {
                    Bloco bloco = buscarBlocoPorHash(hash);

                    if (bloco == null) {
                        res.status(404);
                        return gson.toJson(Map.of(
                                "erro", "Bloco não encontrado",
                                "hash", hash));
                    }

                    List<Map<String, Object>> votos = extrairVotosDoBloco(bloco);

                    Map<String, Object> resultado = new HashMap<>();
                    resultado.put("hash", hash);
                    resultado.put("indice", bloco.getIndice());
                    resultado.put("totalVotos", votos.size());
                    resultado.put("votos", votos);

                    res.status(200);
                    return gson.toJson(resultado);

                } catch (Exception e) {
                    res.status(500);
                    return gson.toJson(Map.of(
                            "erro", "Erro ao buscar votos do bloco",
                            "mensagem", e.getMessage()));
                }
            });

            // GET /api/v1/blocos/:hash/transacoes - Busca todas as transações de um bloco
            get("/:hash/transacoes", (req, res) -> {
                res.type("application/json");
                String hash = req.params("hash");

                try {
                    Bloco bloco = buscarBlocoPorHash(hash);

                    if (bloco == null) {
                        res.status(404);
                        return gson.toJson(Map.of(
                                "erro", "Bloco não encontrado"));
                    }

                    List<Map<String, Object>> transacoes = bloco.getTransacoes().stream()
                            .map(t -> {
                                Map<String, Object> tMap = new HashMap<>();
                                tMap.put("id", t.getId());
                                tMap.put("tipo", t.getTipo().name());
                                tMap.put("timestamp", t.getTimestamp());
                                tMap.put("idOrigem", t.getIdOrigem());
                                return tMap;
                            })
                            .collect(Collectors.toList());

                    Map<String, Object> resultado = new HashMap<>();
                    resultado.put("hash", hash);
                    resultado.put("totalTransacoes", transacoes.size());
                    resultado.put("transacoes", transacoes);

                    res.status(200);
                    return gson.toJson(resultado);

                } catch (Exception e) {
                    res.status(500);
                    return gson.toJson(Map.of(
                            "erro", "Erro ao buscar transações",
                            "mensagem", e.getMessage()));
                }
            });
        });
    }

    /**
     * Busca um bloco pelo hash na blockchain
     */
    private Bloco buscarBlocoPorHash(String hash) {
        if (hash == null || hash.trim().isEmpty()) {
            return null;
        }

        return no.getBlockchain().getBlocos().stream()
                .filter(b -> b.getHash().equals(hash))
                .findFirst()
                .orElse(null);
    }

    /**
     * Extrai votos de um bloco, convertendo para formato adequado para API
     */
    private List<Map<String, Object>> extrairVotosDoBloco(Bloco bloco) {
        return bloco.getTransacoes().stream()
                .filter(t -> t.getTipo() == TipoTransacao.VOTO)
                .map(t -> {
                    Voto voto = t.getPayloadAs(Voto.class);
                    if (voto != null) {
                        Map<String, Object> votoMap = new HashMap<>();
                        votoMap.put("tokenVotacao", voto.getTokenVotacao());
                        votoMap.put("idCandidato", voto.getIdCandidato());
                        votoMap.put("tipoCandidato", voto.getTipoCandidato());
                        votoMap.put("idEleicao", voto.getIdEleicao());
                        votoMap.put("timestamp", voto.getTimestamp());
                        return votoMap;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}