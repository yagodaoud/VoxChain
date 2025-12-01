package com.yagodaoud.VoxChain.rede.api.v1;

import com.google.gson.Gson;
import com.yagodaoud.VoxChain.blockchain.Bloco;
import com.yagodaoud.VoxChain.blockchain.No;
import com.yagodaoud.VoxChain.blockchain.servicos.GerenciadorTokenVotacao;
import com.yagodaoud.VoxChain.modelo.Eleitor;
import com.yagodaoud.VoxChain.modelo.Transacao;
import com.yagodaoud.VoxChain.modelo.Voto;
import com.yagodaoud.VoxChain.modelo.enums.TipoTransacao;

import java.util.*;
import java.util.stream.Collectors;

import static spark.Spark.*;

/**
 * Controller para consulta ANÔNIMA de votos.
 *
 * Garante privacidade ao:
 * 1. Retornar apenas o BLOCO onde o voto está (sem identificar qual voto)
 * 2. Mostrar TODOS os votos do bloco (não só o do eleitor)
 * 3. Não vincular eleitor a voto específico
 *
 * Assim o eleitor pode verificar que seu voto foi registrado,
 * mas não pode provar em quem votou (mantém anonimato).
 */
public class ConsultaVotoAnonimaController implements IApiController {

    private final No no;
    private final GerenciadorTokenVotacao gerenciadorToken;

    public ConsultaVotoAnonimaController(No no, GerenciadorTokenVotacao gerenciadorToken) {
        this.no = no;
        this.gerenciadorToken = gerenciadorToken;
    }

    @Override
    public void registerRoutes(Gson gson) {
        path("/votos-anonimos", () -> {

            // GET /api/v1/votos-anonimos/meus-blocos?cpf=12345678900
            // Retorna os BLOCOS que contêm votos do eleitor (não os votos específicos)
            get("/meus-blocos", (req, res) -> {
                res.type("application/json");

                String cpf = req.queryParams("cpf");

                if (cpf == null || cpf.trim().isEmpty()) {
                    res.status(400);
                    return gson.toJson(Map.of(
                            "erro", "CPF é obrigatório",
                            "mensagem", "Informe o CPF como query parameter: ?cpf=12345678900"
                    ));
                }

                try {
                    String cpfHash = Eleitor.hashCpf(cpf);

                    // Busca tokens do eleitor
                    List<String> tokens = gerenciadorToken.obterTokensDoEleitor(cpfHash);

                    if (tokens.isEmpty()) {
                        res.status(200);
                        return gson.toJson(Map.of(
                                "totalBlocos", 0,
                                "blocos", new ArrayList<>(),
                                "mensagem", "Nenhum voto registrado para este eleitor"
                        ));
                    }

                    // Busca blocos que contêm os tokens
                    List<Map<String, Object>> blocosComVotos = buscarBlocosComTokens(tokens);

                    res.status(200);
                    return gson.toJson(Map.of(
                            "totalBlocos", blocosComVotos.size(),
                            "blocos", blocosComVotos,
                            "aviso", "Por privacidade, mostramos todos os votos do bloco, não apenas o seu"
                    ));

                } catch (Exception e) {
                    res.status(500);
                    return gson.toJson(Map.of(
                            "erro", "Erro ao buscar blocos",
                            "mensagem", e.getMessage()
                    ));
                }
            });

            // GET /api/v1/votos-anonimos/bloco/:hash
            // Retorna TODOS os votos de um bloco específico
            get("/bloco/:hash", (req, res) -> {
                res.type("application/json");

                String hash = req.params("hash");

                try {
                    Bloco bloco = buscarBlocoPorHash(hash);

                    if (bloco == null) {
                        res.status(404);
                        return gson.toJson(Map.of("erro", "Bloco não encontrado"));
                    }

                    // Extrai TODOS os votos do bloco
                    List<Map<String, Object>> votos = extrairVotosDoBloco(bloco);

                    Map<String, Object> resposta = new HashMap<>();
                    resposta.put("blocoHash", bloco.getHash());
                    resposta.put("blocoIndice", bloco.getIndice());
                    resposta.put("timestamp", bloco.getTimestamp());
                    resposta.put("mineradoPor", bloco.getMineradoPor());
                    resposta.put("totalVotos", votos.size());
                    resposta.put("votos", votos);

                    res.status(200);
                    return gson.toJson(resposta);

                } catch (Exception e) {
                    res.status(500);
                    return gson.toJson(Map.of("erro", e.getMessage()));
                }
            });

            // GET /api/v1/votos-anonimos/verificar?cpf=xxx&eleicaoId=yyy
            // Verifica se eleitor votou em uma eleição (SIM/NÃO, sem detalhes)
            get("/verificar", (req, res) -> {
                res.type("application/json");

                String cpf = req.queryParams("cpf");
                String eleicaoId = req.queryParams("eleicaoId");

                if (cpf == null || eleicaoId == null) {
                    res.status(400);
                    return gson.toJson(Map.of(
                            "erro", "CPF e eleicaoId são obrigatórios"
                    ));
                }

                try {
                    String cpfHash = Eleitor.hashCpf(cpf);
                    List<String> tokens = gerenciadorToken.obterTokensDoEleitor(cpfHash);

                    // Verifica se existe voto para a eleição
                    boolean votou = verificarVotoNaEleicao(tokens, eleicaoId);

                    res.status(200);
                    return gson.toJson(Map.of(
                            "votou", votou,
                            "eleicaoId", eleicaoId,
                            "mensagem", votou
                                    ? "✓ Voto registrado nesta eleição"
                                    : "✗ Nenhum voto registrado nesta eleição"
                    ));

                } catch (Exception e) {
                    res.status(500);
                    return gson.toJson(Map.of("erro", e.getMessage()));
                }
            });
        });
    }

    /**
     * Busca blocos que contêm votos com os tokens fornecidos
     */
    private List<Map<String, Object>> buscarBlocosComTokens(List<String> tokens) {
        List<Map<String, Object>> blocos = new ArrayList<>();

        for (Bloco bloco : no.getBlockchain().getBlocos()) {
            if (bloco.getIndice() == 0) continue; // Pula genesis

            // Verifica se o bloco contém algum dos tokens
            boolean contemToken = bloco.getTransacoes().stream()
                    .filter(t -> t.getTipo() == TipoTransacao.VOTO)
                    .map(t -> t.getPayloadAs(Voto.class))
                    .filter(Objects::nonNull)
                    .anyMatch(v -> tokens.contains(v.getTokenVotacao()));

            if (contemToken) {
                Map<String, Object> blocoInfo = new HashMap<>();
                blocoInfo.put("hash", bloco.getHash());
                blocoInfo.put("indice", bloco.getIndice());
                blocoInfo.put("timestamp", bloco.getTimestamp());

                // TODOS os votos do bloco (não só o do eleitor)
                List<Map<String, Object>> votosDoBloco = extrairVotosDoBloco(bloco);
                blocoInfo.put("totalVotos", votosDoBloco.size());
                blocoInfo.put("votos", votosDoBloco);

                blocos.add(blocoInfo);
            }
        }

        return blocos;
    }

    /**
     * Extrai TODOS os votos de um bloco (mantém anonimato)
     */
    private List<Map<String, Object>> extrairVotosDoBloco(Bloco bloco) {
        return bloco.getTransacoes().stream()
                .filter(t -> t.getTipo() == TipoTransacao.VOTO)
                .map(t -> {
                    Voto voto = t.getPayloadAs(Voto.class);
                    if (voto != null) {
                        Map<String, Object> votoMap = new HashMap<>();
                        // NÃO incluímos o token completo (privacidade)
                        votoMap.put("tokenHash", voto.getTokenVotacao().substring(0, 8) + "...");
                        votoMap.put("eleicaoId", voto.getIdEleicao());
                        votoMap.put("tipoCandidato", voto.getTipoCandidato());
                        votoMap.put("timestamp", voto.getTimestamp());
                        // NÃO incluímos idCandidato (privacidade total)
                        return votoMap;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Busca bloco por hash
     */
    private Bloco buscarBlocoPorHash(String hash) {
        return no.getBlockchain().getBlocos().stream()
                .filter(b -> b.getHash().equals(hash))
                .findFirst()
                .orElse(null);
    }

    /**
     * Verifica se eleitor votou em uma eleição específica
     */
    private boolean verificarVotoNaEleicao(List<String> tokens, String eleicaoId) {
        for (Bloco bloco : no.getBlockchain().getBlocos()) {
            for (Transacao t : bloco.getTransacoes()) {
                if (t.getTipo() == TipoTransacao.VOTO) {
                    Voto voto = t.getPayloadAs(Voto.class);
                    if (voto != null &&
                            tokens.contains(voto.getTokenVotacao()) &&
                            voto.getIdEleicao().equals(eleicaoId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}