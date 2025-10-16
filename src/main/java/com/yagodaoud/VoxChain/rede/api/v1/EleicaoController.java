package com.yagodaoud.VoxChain.rede.api.v1;

import com.google.gson.Gson;
import com.yagodaoud.VoxChain.blockchain.servicos.eleicao.ServicoEleicao;
import com.yagodaoud.VoxChain.modelo.dto.NovaEleicaoDTO;
import com.yagodaoud.VoxChain.modelo.dto.NovoCandidatoDTO;
import static spark.Spark.*;

public class EleicaoController implements IApiController {

    private final ServicoEleicao servicoEleicao;

    public EleicaoController(ServicoEleicao servicoEleicao) {
        this.servicoEleicao = servicoEleicao;
    }

    @Override
    public void registerRoutes(Gson gson) {
        // Agrupa rotas relacionadas a eleições e candidatos
        // Essas rotas também devem ser protegidas pelo filtro de autenticação
        path("/eleicao", () -> {
            post("/criar", (req, res) -> {
                res.type("application/json");
                String solicitanteId = req.attribute("adminId");
                NovaEleicaoDTO eleicaoDTO = gson.fromJson(req.body(), NovaEleicaoDTO.class);

                // O servicoEleicao precisará do solicitanteId para validar permissões
                servicoEleicao.criarEleicao(
                        solicitanteId,
                        eleicaoDTO.getDescricao(),
                        eleicaoDTO.getDataInicio(),
                        eleicaoDTO.getDataFim()
                );

                res.status(201);
                return "{\"message\":\"Eleição criada e registrada na blockchain.\"}";
            });
        });

        path("/candidato", () -> {
            post("/cadastrar", (req, res) -> {
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
                        candidatoDTO.getUf()
                );

                res.status(201);
                return "{\"message\":\"Candidato cadastrado com sucesso!\"}";
            });
        });
    }
}