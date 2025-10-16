package com.yagodaoud.VoxChain.rede.api.v1;

import com.google.gson.Gson;
import com.yagodaoud.VoxChain.blockchain.servicos.ServicoAdministracao;
import com.yagodaoud.VoxChain.modelo.Administrador;
import com.yagodaoud.VoxChain.modelo.dto.NovoAdminDTO;
import static spark.Spark.*;

public class AdminController implements IApiController {

    private final ServicoAdministracao servicoAdmin;

    public AdminController(ServicoAdministracao servicoAdmin) {
        this.servicoAdmin = servicoAdmin;
    }

    @Override
    public void registerRoutes(Gson gson) {
        // Agrupa todas as rotas sob o prefixo /admin
        path("/admin", () -> {

            // Rota para criar um novo administrador
            // POST /admin
            post("/criar", (req, res) -> {
                res.type("application/json");

                String solicitanteId = req.attribute("adminId"); // Pego do filtro de auth
                NovoAdminDTO adminDTO = gson.fromJson(req.body(), NovoAdminDTO.class);

                Administrador novoAdmin = servicoAdmin.cadastrarNovoAdmin(
                        solicitanteId,
                        adminDTO.getNome(),
                        adminDTO.getSenha(),
                        adminDTO.getNivel(),
                        adminDTO.getJurisdicao()
                );

                res.status(201);
                return gson.toJson(novoAdmin);
            });
        });
    }
}