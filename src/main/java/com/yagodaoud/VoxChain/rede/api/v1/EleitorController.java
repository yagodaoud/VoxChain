package com.yagodaoud.VoxChain.rede.api.v1;

import com.google.gson.Gson;
import com.yagodaoud.VoxChain.blockchain.servicos.ServicoEleitor;
import com.yagodaoud.VoxChain.modelo.dto.NovoEleitorDTO;

import static spark.Spark.*;

public class EleitorController implements IApiController {

    private final ServicoEleitor servicoEleitor;

    public EleitorController(ServicoEleitor servicoEleitor) {
        this.servicoEleitor = servicoEleitor;
    }

    @Override
    public void registerRoutes(Gson gson) {
        path("/eleitores", () -> {
            post("/criar", (req, res) -> {
                res.type("application/json");
                String solicitanteCpfHash = req.attribute("cpfHash");
                NovoEleitorDTO dto = gson.fromJson(req.body(), NovoEleitorDTO.class);

                servicoEleitor.cadastrarEleitor(
                        solicitanteCpfHash,
                        dto.getCpf(),
                        dto.getZona(),
                        dto.getSecao()
                );

                res.status(201);
                return "{\"message\":\"Eleitor cadastrado com sucesso!\"}";
            });

            get("/listar", (req, res) -> {
                res.type("application/json");
                return gson.toJson(servicoEleitor.listarEleitores());
            });
        });
    }
}


