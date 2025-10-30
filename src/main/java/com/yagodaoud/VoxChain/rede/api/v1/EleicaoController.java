package com.yagodaoud.VoxChain.rede.api.v1;

import com.google.gson.Gson;
import com.yagodaoud.VoxChain.blockchain.servicos.ServicoAdministracao;
import com.yagodaoud.VoxChain.blockchain.servicos.eleicao.ServicoEleicao;
import com.yagodaoud.VoxChain.modelo.Eleicao;
import com.yagodaoud.VoxChain.modelo.dto.NovaEleicaoDTO;
import com.yagodaoud.VoxChain.modelo.dto.NovoCandidatoDTO;

import java.util.List;
import java.util.stream.Collectors;

import static spark.Spark.*;

public class EleicaoController implements IApiController {

    private final ServicoEleicao servicoEleicao;

    public EleicaoController(ServicoEleicao servicoEleicao, ServicoAdministracao servicoAdministracao) {
        this.servicoEleicao = servicoEleicao;
    }

    @Override
    public void registerRoutes(Gson gson) {
        // Agrupa rotas relacionadas a eleições e candidatos
        // Essas rotas também devem ser protegidas pelo filtro de autenticação
        path("/eleicoes", () -> {
            post("/criar", (req, res) -> {
                res.type("application/json");
                String cpfHash = req.attribute("cpfHash");
                NovaEleicaoDTO eleicaoDTO = gson.fromJson(req.body(), NovaEleicaoDTO.class);

                // O servicoEleicao precisará do cpfHash para validar permissões
                Eleicao eleicao = servicoEleicao.criarEleicao(
                        cpfHash,
                        eleicaoDTO.getNome(),
                        eleicaoDTO.getDescricao(),
                        eleicaoDTO.getCategorias(),
                        eleicaoDTO.getDataInicio(),
                        eleicaoDTO.getDataFim()
                );

                res.status(201);
                return gson.toJson(eleicao);
            });


            get("/listar", (req, res) -> {
                res.type("application/json");
                boolean incluirFinalizadas = Boolean.parseBoolean(req.queryParams("finished"));
                long agora = System.currentTimeMillis();

                List<Eleicao> eleicoes = servicoEleicao.listarEleicoes();

                if (!incluirFinalizadas) {
                    eleicoes = eleicoes.stream()
                            .filter(e -> e.getDataFim() > agora) // Filtra apenas eleições ativas ou futuras
                            .collect(Collectors.toList());
                }

                return gson.toJson(eleicoes);
            });
        });
    }
}