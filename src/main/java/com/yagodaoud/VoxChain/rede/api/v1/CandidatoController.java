package com.yagodaoud.VoxChain.rede.api.v1;

import com.google.gson.Gson;
import com.yagodaoud.VoxChain.blockchain.servicos.ServicoAdministracao;
import com.yagodaoud.VoxChain.blockchain.servicos.eleicao.ServicoEleicao;
import com.yagodaoud.VoxChain.modelo.dto.NovoCandidatoDTO;

import static spark.Spark.*;

public class CandidatoController implements IApiController {

    private final ServicoEleicao servicoEleicao;

    public CandidatoController(ServicoEleicao servicoEleicao, ServicoAdministracao servicoAdministracao) {
        this.servicoEleicao = servicoEleicao;
    }

    @Override
    public void registerRoutes(Gson gson) {
        path("/candidatos", () -> {
            post("/criar", (req, res) -> {
                res.type("application/json");
                String solicitanteId = req.attribute("adminId");
                NovoCandidatoDTO candidatoDTO = gson.fromJson(req.body(), NovoCandidatoDTO.class);

                servicoEleicao.cadastrarCandidato(
                        solicitanteId,
                        candidatoDTO.getEleicaoId(),
                        candidatoDTO.getNumero(),
                        candidatoDTO.getNome(),
                        candidatoDTO.getPartido(),
                        candidatoDTO.getCargo(),
                        candidatoDTO.getUf(),
                        candidatoDTO.getFotoUrl()
                );

                res.status(201);
                return "{\"message\":\"Candidato cadastrado com sucesso!\"}";
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
