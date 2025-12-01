package com.yagodaoud.VoxChain.rede;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.yagodaoud.VoxChain.blockchain.No;
import com.yagodaoud.VoxChain.blockchain.servicos.GerenciadorTokenVotacao;
import com.yagodaoud.VoxChain.blockchain.servicos.ServicoAdministracao;
import com.yagodaoud.VoxChain.blockchain.servicos.eleicao.ServicoEleicao;
import com.yagodaoud.VoxChain.blockchain.servicos.ServicoEleitor;
import com.yagodaoud.VoxChain.blockchain.servicos.eleicao.ServicoFechamentoEleicao;
import com.yagodaoud.VoxChain.modelo.Administrador;
import com.yagodaoud.VoxChain.modelo.LogAuditoria;
import com.yagodaoud.VoxChain.modelo.Transacao;
import com.yagodaoud.VoxChain.modelo.enums.TipoTransacao;
import com.yagodaoud.VoxChain.rede.api.v1.*;
import com.yagodaoud.VoxChain.utils.Logger;
import com.yagodaoud.VoxChain.utils.SecurityUtils;
import spark.Spark;

import java.util.Base64;
import java.util.List;
import java.util.Map;
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

    private final Gson gson = new Gson();

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
        before("/api/v1/admin/*", this::autenticarSuperAdminRequest);
        before("/api/v1/eleicoes/criar", this::autenticarAdminRequest);
        before("/api/v1/candidatos/criar", this::autenticarAdminRequest);
        before("/api/v1/eleitores/criar", this::autenticarAdminRequest);

        // ==================== INICIALIZAÇÃO DE SERVIÇOS ====================

        // Instancia os serviços que contêm a lógica de negócio.
        ServicoAdministracao servicoAdmin = new ServicoAdministracao(no.getBlockchain());
        GerenciadorTokenVotacao gerenciadorToken = new GerenciadorTokenVotacao();
        ServicoEleicao servicoEleicao = new ServicoEleicao(no.getBlockchain(), servicoAdmin, gerenciadorToken);
        ServicoEleitor servicoEleitor = new ServicoEleitor(no.getBlockchain());
        ServicoFechamentoEleicao servicoFechamentoEleicao = new ServicoFechamentoEleicao(no.getBlockchain());

        // ==================== REGISTRO DE CONTROLLERS ====================

        // Cria uma lista de todos os controllers que compõem a API.
        List<IApiController> controllers = List.of(
                new AdminController(servicoAdmin),
                new EleicaoController(servicoEleicao, servicoAdmin),
                new BlockchainController(no),
                new NetworkController(no, monitor),
                new LoginController(no),
                new CandidatoController(servicoEleicao, servicoAdmin),
                new VotoController(servicoEleicao, servicoAdmin, no, gerenciadorToken),
                new EleitorController(servicoEleitor),
                new TokenVotacaoController(gerenciadorToken),
                new BlocoController(no),
                new ConsultaVotoAnonimaController(no, gerenciadorToken),
                new ResultadosController(servicoFechamentoEleicao)
        );

        // Define um prefixo global para todas as rotas da API versionada.
        path("/api/v1", () -> {
            controllers.forEach(controller -> controller.registerRoutes(gson));
        });

        // ==================== AUDITORIA ====================

        // Filtro after para registrar auditoria em ações administrativas
        after("/api/v1/admin/*", (req, res) -> {
            String cpfHash = req.attribute("cpfHash");
            String ipOrigem = req.ip();
            String acao = req.requestMethod() + " " + req.pathInfo();
            
            if (cpfHash != null) {
                String detalhes = gson.toJson(Map.of(
                        "method", req.requestMethod(),
                        "path", req.pathInfo(),
                        "status", res.status()
                ));
                
                LogAuditoria log = new LogAuditoria(acao, cpfHash, detalhes, ipOrigem);
                // Registra na blockchain como transação de auditoria
                Transacao transacaoAuditoria = new Transacao(TipoTransacao.CADASTRO_ADMIN, log, cpfHash);
                no.getBlockchain().adicionarAoPool(transacaoAuditoria);
            }
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

        Logger.info(no.getId(), "✓ Servidor API modularizado iniciado. Rotas disponíveis em http://localhost:" + porta + "/api/v1");
    }

    /**
     * Middleware para autenticar requisições via JWT
     * Extrai o token do header Authorization e valida
     */
    private void autenticarRequest(spark.Request req, spark.Response res) {
        String authHeader = req.headers("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            halt(401, gson.toJson(Map.of(
                    "error", "Token de autenticação não fornecido",
                    "message", "Use o header: Authorization: Bearer <token>"
            )));
        }

        String token = authHeader.substring(7); // Remove "Bearer "

        try {
            DecodedJWT jwt = SecurityUtils.verificarToken(token);

            // Extrai informações do token
            String cpfHash = jwt.getSubject();
            String tipo = jwt.getClaim("nivelAcesso").asString();
//            String nome = jwt.getClaim("nome").asString();

            // Anexa à requisição para uso nos controllers
            req.attribute("cpfHash", cpfHash);
            req.attribute("nivelAcesso", tipo);
            req.attribute("userNome", cpfHash);

        } catch (JWTVerificationException e) {
            halt(401, gson.toJson(Map.of(
                    "error", "Token inválido ou expirado",
                    "message", e.getMessage()
            )));
        }
    }

    /**
     * Middleware específico para rotas administrativas
     * Verifica se o usuário tem permissão de admin
     */
    private void autenticarAdminRequest(spark.Request req, spark.Response res) {
        // Primeiro autentica o token
        autenticarRequest(req, res);

        // Depois verifica se é admin
        String tipo = req.attribute("nivelAcesso").toString().toLowerCase();

        if (!"admin".equals(tipo) && !"super_admin".equals(tipo)) {
            halt(403, gson.toJson(Map.of(
                    "error", "Acesso negado",
                    "message", "Apenas administradores podem acessar este recurso"
            )));
        }
    }

    /**
     * Middleware para super-admin apenas
     */
    private void autenticarSuperAdminRequest(spark.Request req, spark.Response res) {
        autenticarRequest(req, res);

        String tipo = req.attribute("nivelAcesso");

        if (!"super-admin".equals(tipo)) {
            halt(403, gson.toJson(Map.of(
                    "error", "Acesso negado",
                    "message", "Apenas super administradores podem acessar este recurso"
            )));
        }
    }
}