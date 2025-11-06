package com.yagodaoud.VoxChain.rede.api.v1;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.yagodaoud.VoxChain.blockchain.servicos.GerenciadorTokenVotacao;
import com.yagodaoud.VoxChain.modelo.TokenVotacao;

import java.util.Map;

import static spark.Spark.*;

/**
 * Controller para gerenciamento de tokens de votação anônima.
 */
public class TokenVotacaoController implements IApiController {

    private final GerenciadorTokenVotacao gerenciadorToken;

    public TokenVotacaoController(GerenciadorTokenVotacao gerenciadorToken) {
        this.gerenciadorToken = gerenciadorToken;
    }

    @Override
    public void registerRoutes(Gson gson) {
        // Filtro de autenticação para rotas de tokens
        before("/tokens/*", (req, res) -> {
            autenticarEleitorRequest(req, res, gson);
        });

        path("/tokens", () -> {
            post("/gerar", (req, res) -> {
                res.type("application/json");
                
                // Extrai cpfHash do token JWT
                String cpfHash = req.attribute("cpfHash");
                if (cpfHash == null) {
                    res.status(401);
                    return gson.toJson(Map.of("erro", "Token de autenticação inválido"));
                }

                // Lê eleicaoId do body
                JsonObject json = gson.fromJson(req.body(), JsonObject.class);
                if (json == null || !json.has("eleicaoId")) {
                    res.status(400);
                    return gson.toJson(Map.of("erro", "eleicaoId é obrigatório"));
                }

                String eleicaoId = json.get("eleicaoId").getAsString();

                try {
                    // Verifica se eleitor já tem token ativo
                    if (gerenciadorToken.eleitorPossuiTokenAtivo(cpfHash, eleicaoId)) {
                        res.status(409); // Conflict
                        return gson.toJson(Map.of(
                                "erro", "Eleitor já possui token ativo para esta eleição",
                                "mensagem", "Aguarde a expiração do token atual ou use o token existente"
                        ));
                    }

                    // Gera novo token
                    TokenVotacao token = gerenciadorToken.gerarToken(cpfHash, eleicaoId);

                    res.status(201);
                    return gson.toJson(Map.of(
                            "tokenAnonimo", token.getTokenAnonimo(),
                            "validoAte", token.getValidoAte(),
                            "eleicaoId", token.getEleicaoId()
                    ));
                } catch (IllegalStateException e) {
                    res.status(409); // Conflict
                    return gson.toJson(Map.of("erro", e.getMessage()));
                } catch (Exception e) {
                    res.status(500);
                    return gson.toJson(Map.of("erro", "Erro ao gerar token: " + e.getMessage()));
                }
            });
        });
    }

    /**
     * Middleware para autenticar requisições de eleitores
     */
    private void autenticarEleitorRequest(spark.Request req, spark.Response res, Gson gson) {
        String authHeader = req.headers("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            halt(401, gson.toJson(Map.of(
                    "error", "Token de autenticação não fornecido",
                    "message", "Use o header: Authorization: Bearer <token>"
            )));
        }

        String token = authHeader.substring(7); // Remove "Bearer "

        try {
            com.auth0.jwt.interfaces.DecodedJWT jwt = com.yagodaoud.VoxChain.utils.SecurityUtils.verificarToken(token);
            String cpfHash = jwt.getSubject();
            String nivelAcesso = jwt.getClaim("nivelAcesso").asString();

            // Verifica se é eleitor
            if (!"ELEITOR".equals(nivelAcesso)) {
                halt(403, gson.toJson(Map.of(
                        "error", "Acesso negado",
                        "message", "Apenas eleitores podem gerar tokens de votação"
                )));
            }

            req.attribute("cpfHash", cpfHash);
            req.attribute("nivelAcesso", nivelAcesso);
        } catch (com.auth0.jwt.exceptions.JWTVerificationException e) {
            halt(401, gson.toJson(Map.of(
                    "error", "Token inválido ou expirado",
                    "message", e.getMessage()
            )));
        }
    }
}

