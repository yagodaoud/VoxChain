package com.yagodaoud.VoxChain.rede.api.v1;

import com.google.gson.Gson;
import com.yagodaoud.VoxChain.blockchain.servicos.ServicoAdministracao;
import com.yagodaoud.VoxChain.blockchain.servicos.eleicao.ServicoEleicao;
import com.yagodaoud.VoxChain.modelo.dto.NovoCandidatoDTO;

import static spark.Spark.*;

public class VotoController implements IApiController {

    private final ServicoEleicao servicoEleicao;

    public VotoController(ServicoEleicao servicoEleicao, ServicoAdministracao servicoAdministracao) {
        this.servicoEleicao = servicoEleicao;
    }

    @Override
    public void registerRoutes(Gson gson) {
        path("/votos", () -> {
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
