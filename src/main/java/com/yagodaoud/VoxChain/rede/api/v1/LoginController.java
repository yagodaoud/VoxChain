package com.yagodaoud.VoxChain.rede.api.v1;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.yagodaoud.VoxChain.blockchain.No;
import com.yagodaoud.VoxChain.modelo.Administrador;
import com.yagodaoud.VoxChain.modelo.Eleitor;
import com.yagodaoud.VoxChain.modelo.enums.NivelAcesso;
import com.yagodaoud.VoxChain.utils.SecurityUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static spark.Spark.*;


public class LoginController implements IApiController {

    private final No no;
    private Map<String, LoginAttempt> tentativas = new ConcurrentHashMap<>();

    public LoginController(No no) {
        this.no = no;
    }

    @Override
    public void registerRoutes(Gson gson) {
        post("/auth/login", (req, res) -> {
            res.type("application/json");
            
            // Rate limiting - antes de validar credenciais
            String ip = req.ip();
            LoginAttempt attempt = tentativas.computeIfAbsent(ip, k -> new LoginAttempt());

            if (attempt.count >= 10000 && (System.currentTimeMillis() - attempt.lastAttempt) < 300000) {
                res.status(429);
                return gson.toJson(Map.of("erro", "Muitas tentativas. Tente em 5 minutos"));
            }

            attempt.count++;
            attempt.lastAttempt = System.currentTimeMillis();

            JsonObject json = gson.fromJson(req.body(), JsonObject.class);
            String cpfHash = Eleitor.hashCpf(json.get("cpf").getAsString());
            String senha = json.get("senha").getAsString();

            // Busca na blockchain
            Administrador admin = no.getBlockchain().buscarAdminPorCpfHashESenhaHash(cpfHash, senha);
            if (admin != null && admin.isAtivo()) {
                // Login bem-sucedido - remove tentativas
                tentativas.remove(ip);
                return gson.toJson(Map.of(
                        "tipo", mapearNivelParaTipo(admin.getNivel()),
                        "nome", admin.getHashCpf(),
                        "token", SecurityUtils.gerarToken(cpfHash, admin.getNivel())
                ));
            }

            Eleitor eleitor = no.getBlockchain().buscarEleitorPorCpfHashESenhaHash(cpfHash, senha);
            if (eleitor != null) {
                // Login bem-sucedido - remove tentativas
                tentativas.remove(ip);
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

    /**
     * Classe interna para rastrear tentativas de login
     */
    private static class LoginAttempt {
        int count = 0;
        long lastAttempt = System.currentTimeMillis();
    }
}
