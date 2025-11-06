package com.yagodaoud.VoxChain.rede.api.v1;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.yagodaoud.VoxChain.blockchain.servicos.ServicoAdministracao;
import com.yagodaoud.VoxChain.blockchain.servicos.eleicao.ServicoEleicao;

import java.util.Map;

import static spark.Spark.*;

public class VotoController implements IApiController {

    private final ServicoEleicao servicoEleicao;

    public VotoController(ServicoEleicao servicoEleicao, ServicoAdministracao servicoAdministracao) {
        this.servicoEleicao = servicoEleicao;
    }

    @Override
    public void registerRoutes(Gson gson) {
        path("/votos", () -> {
            post("/registrar", (req, res) -> {
                res.type("application/json");
                
                // Lê dados do body (SEM autenticação - voto é anônimo)
                JsonObject json = gson.fromJson(req.body(), JsonObject.class);
                if (json == null) {
                    res.status(400);
                    return gson.toJson(Map.of("erro", "Body inválido"));
                }

                String tokenVotacao = json.has("tokenVotacao") ? json.get("tokenVotacao").getAsString() : null;
                String numeroCandidato = json.has("numeroCandidato") ? json.get("numeroCandidato").getAsString() : null;
                String eleicaoId = json.has("eleicaoId") ? json.get("eleicaoId").getAsString() : null;

                if (tokenVotacao == null || numeroCandidato == null || eleicaoId == null) {
                    res.status(400);
                    return gson.toJson(Map.of("erro", "tokenVotacao, numeroCandidato e eleicaoId são obrigatórios"));
                }

                try {
                    // Chama servicoEleicao.registrarVoto com tokenVotacao do body
                    servicoEleicao.registrarVoto(tokenVotacao, numeroCandidato, eleicaoId);
                    
                    res.status(201);
                    return gson.toJson(Map.of("mensagem", "Voto registrado com sucesso"));
                } catch (IllegalStateException e) {
                    res.status(400);
                    return gson.toJson(Map.of("erro", e.getMessage()));
                } catch (IllegalArgumentException e) {
                    res.status(400);
                    return gson.toJson(Map.of("erro", e.getMessage()));
                } catch (Exception e) {
                    res.status(500);
                    return gson.toJson(Map.of("erro", "Erro ao registrar voto: " + e.getMessage()));
                }
            });

            post("/batch", (req, res) -> {
                res.type("application/json");
                String solicitanteId = req.attribute("adminId");

                res.status(201);
                return "{\"message\":\"Votos cadastrados com sucesso!\"}";
            });

            get("/listar", (req, res) -> {
                res.type("application/json");
                String eleicaoId = req.queryParams("eleicaoId");

                if (eleicaoId != null) {
                    return gson.toJson(servicoEleicao.listarCandidatos(eleicaoId));
                }

                return gson.toJson(servicoEleicao.listarCandidatos());
            });
        });
    }
}
