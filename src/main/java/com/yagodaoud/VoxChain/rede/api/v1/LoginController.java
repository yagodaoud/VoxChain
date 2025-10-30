package com.yagodaoud.VoxChain.rede.api.v1;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.yagodaoud.VoxChain.blockchain.No;
import com.yagodaoud.VoxChain.modelo.Administrador;
import com.yagodaoud.VoxChain.modelo.Eleitor;
import com.yagodaoud.VoxChain.modelo.enums.NivelAcesso;
import com.yagodaoud.VoxChain.utils.SecurityUtils;

import java.util.Map;

import static spark.Spark.*;


public class LoginController implements IApiController {

    private final No no;

    public LoginController(No no) {
        this.no = no;
    }

    @Override
    public void registerRoutes(Gson gson) {
        post("/auth/login", (req, res) -> {
            JsonObject json = gson.fromJson(req.body(), JsonObject.class);
            String cpfHash = Eleitor.hashCpf(json.get("cpf").getAsString());

            // Busca na blockchain
            Administrador admin = no.getBlockchain().buscarAdminPorCpfHash(cpfHash);
            if (admin != null && admin.isAtivo()) {
                return gson.toJson(Map.of(
                        "tipo", mapearNivelParaTipo(admin.getNivel()),
                        "nome", admin.getHashCpf(),
                        "token", SecurityUtils.gerarToken(cpfHash, admin.getNivel())
                ));
            }

            Eleitor eleitor = no.getBlockchain().buscarEleitor(cpfHash);
            if (eleitor != null) {
                return gson.toJson(Map.of(
                        "tipo", NivelAcesso.ELEITOR,
                        "nome", "Fulano de tal" /*eleitor.getNome()*/,
                        "token", SecurityUtils.gerarToken(cpfHash, NivelAcesso.ELEITOR)
                ));
            }

            res.status(401);
            return gson.toJson(Map.of("erro", "Usuário não encontrado"));
        });
    }

    private String mapearNivelParaTipo(NivelAcesso nivel) {
        switch(nivel) {
            case SUPER_ADMIN: return "super-admin";
            case ADMIN_TSE: return "admin";
            case OPERADOR: return "operador";
            default: return "eleitor";
        }
    }
}
