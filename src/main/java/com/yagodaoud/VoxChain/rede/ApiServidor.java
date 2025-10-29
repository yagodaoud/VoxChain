package com.yagodaoud.VoxChain.rede;

import com.google.gson.Gson;
import com.yagodaoud.VoxChain.blockchain.No;
import com.yagodaoud.VoxChain.blockchain.servicos.ServicoAdministracao;
import com.yagodaoud.VoxChain.blockchain.servicos.eleicao.ServicoEleicao;
import com.yagodaoud.VoxChain.modelo.Administrador;
import com.yagodaoud.VoxChain.rede.api.v1.AdminController;
import com.yagodaoud.VoxChain.rede.api.v1.BlockchainController;
import com.yagodaoud.VoxChain.rede.api.v1.EleicaoController;
import com.yagodaoud.VoxChain.rede.api.v1.IApiController;
import com.yagodaoud.VoxChain.rede.api.v1.NetworkController;
import spark.Spark;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static spark.Spark.*;

/**
 * Orquestrador principal do servidor da API REST.
 * Responsabilidades:
 * - Configurar a porta e o GSON.
 * - Inicializar os serviços de negócio.
 * - Configurar middleware, como filtros de autenticação.
 * - Registrar todos os controllers da API.
 * - Definir tratamento de exceções globais.
 */
public class ApiServidor {

    private final No no;
    private NetworkMonitor monitor;

    public ApiServidor(No no) {
        this.no = no;
    }

    public ApiServidor comMonitor(NetworkMonitor monitor) {
        this.monitor = monitor;
        return this;
    }

    /**
     * Inicia o servidor Spark e configura todas as rotas e filtros.
     * @param porta A porta em que o servidor irá escutar.
     */
    public void iniciar(int porta) {
        Gson gson = new Gson();
        port(porta);

        // ==================== MIDDLEWARE E FILTROS ====================

        // Aplica o filtro de autenticação a todas as rotas que exigem privilégios de administrador.
        before("/api/v1/admin/*", this::autenticarAdminRequest);
        before("/api/v1/eleicao/criar", this::autenticarAdminRequest);
        before("/api/v1/candidato/*", this::autenticarAdminRequest);

        // ==================== INICIALIZAÇÃO DE SERVIÇOS ====================

        // Instancia os serviços que contêm a lógica de negócio.
        ServicoAdministracao servicoAdmin = new ServicoAdministracao(no.getBlockchain());
        ServicoEleicao servicoEleicao = new ServicoEleicao(no.getBlockchain(), servicoAdmin);

        // ==================== REGISTRO DE CONTROLLERS ====================

        // Cria uma lista de todos os controllers que compõem a API.
        List<IApiController> controllers = List.of(
                new AdminController(servicoAdmin),
                new EleicaoController(servicoEleicao, servicoAdmin),
                new BlockchainController(no),
                new NetworkController(no, monitor)
        );

        // Define um prefixo global para todas as rotas da API versionada.
        path("/api/v1", () -> {
            controllers.forEach(controller -> controller.registerRoutes(gson));
        });

        // ==================== TRATAMENTO DE ERROS GLOBAIS ====================

        // Captura exceções não tratadas em qualquer rota para fornecer respostas de erro consistentes.
        exception(Exception.class, (exception, req, res) -> {
            res.type("application/json");
            // Logar a stack trace completa no console para depuração.
            exception.printStackTrace();

            // Retorna uma mensagem de erro genérica e segura para o cliente.
            if (exception instanceof SecurityException) {
                res.status(403); // Forbidden
                res.body("{\"error\":\"Acesso negado: permissão insuficiente.\"}");
            } else if (exception instanceof IllegalArgumentException) {
                res.status(400); // Bad Request
                res.body("{\"error\":\"Requisição inválida: " + exception.getMessage() + "\"}");
            } else {
                res.status(500); // Internal Server Error
                res.body("{\"error\":\"Ocorreu um erro interno no servidor.\"}");
            }
        });

        System.out.println("✓ Servidor API modularizado iniciado. Rotas disponíveis em http://localhost:" + porta + "/api/v1");
    }

    /**
     * Middleware de autenticação que processa o header "Authorization" (Basic Auth).
     * Se a autenticação for bem-sucedida, o ID do admin é anexado à requisição.
     * Caso contrário, a requisição é interrompida com um status 401 Unauthorized.
     */
    private void autenticarAdminRequest(spark.Request req, spark.Response res) {
        Optional<String> adminIdAutenticado = autenticarViaBasicHeader(req.headers("Authorization"));

        if (adminIdAutenticado.isEmpty()) {
            halt(401, "{\"error\":\"Autenticação falhou ou é necessária.\"}");
        }

        // Anexa o ID do admin à requisição para que os controllers possam usá-lo.
        req.attribute("adminId", adminIdAutenticado.get());
    }

    /**
     * Valida as credenciais do cabeçalho de Autorização (Basic Auth) contra a blockchain.
     * @param authHeader Conteúdo do cabeçalho "Authorization".
     * @return Um Optional contendo o ID do admin se a autenticação for bem-sucedida.
     */
    private Optional<String> autenticarViaBasicHeader(String authHeader) {
        if (authHeader == null || !authHeader.toLowerCase().startsWith("basic ")) {
            return Optional.empty();
        }
        try {
            String base64Credentials = authHeader.substring("Basic".length()).trim();
            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(credDecoded); // Formato "id:senha"

            final String[] values = credentials.split(":", 2);
            if (values.length != 2) return Optional.empty();

            String adminId = values[0];
            String senha = values[1];

            Administrador admin = no.getBlockchain().buscarAdmin(adminId);
            // Verifica se o admin existe, se a senha confere e se a conta está ativa.
            if (admin != null && admin.verificarSenha(senha) && admin.isAtivo()) {
                return Optional.of(adminId); // Sucesso!
            }
        } catch (Exception e) {
            // Se houver qualquer erro (ex: Base64 malformado), a autenticação falha.
            return Optional.empty();
        }
        return Optional.empty();
    }
}