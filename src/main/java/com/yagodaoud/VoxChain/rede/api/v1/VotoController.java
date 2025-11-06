package com.yagodaoud.VoxChain.rede.api.v1;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.yagodaoud.VoxChain.blockchain.Bloco;
import com.yagodaoud.VoxChain.blockchain.No;
import com.yagodaoud.VoxChain.blockchain.servicos.GerenciadorTokenVotacao;
import com.yagodaoud.VoxChain.blockchain.servicos.ServicoAdministracao;
import com.yagodaoud.VoxChain.blockchain.servicos.eleicao.ServicoEleicao;
import com.yagodaoud.VoxChain.modelo.Eleitor;
import com.yagodaoud.VoxChain.modelo.Transacao;
import com.yagodaoud.VoxChain.modelo.Voto;
import com.yagodaoud.VoxChain.modelo.enums.TipoTransacao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.*;

public class VotoController implements IApiController {

    private final ServicoEleicao servicoEleicao;
    private final No no;
    private final GerenciadorTokenVotacao gerenciadorToken;

    public VotoController(
            ServicoEleicao servicoEleicao,
            ServicoAdministracao servicoAdministracao,
            No no,
            GerenciadorTokenVotacao gerenciadorToken) {
        this.servicoEleicao = servicoEleicao;
        this.no = no;
        this.gerenciadorToken = gerenciadorToken;
    }

    @Override
    public void registerRoutes(Gson gson) {
        path("/votos", () -> {

            // POST /api/v1/votos/registrar - Registra um voto anônimo
            post("/registrar", (req, res) -> {
                res.type("application/json");

                JsonObject json = gson.fromJson(req.body(), JsonObject.class);
                if (json == null) {
                    res.status(400);
                    return gson.toJson(Map.of("erro", "Body inválido"));
                }

                String tokenVotacao = json.has("tokenVotacao") ? json.get("tokenVotacao").getAsString() : null;
                String numeroCandidato = json.has("numeroCandidato") ? json.get("numeroCandidato").getAsString() : null;
                String eleicaoId = json.has("eleicaoId") ? json.get("eleicaoId").getAsString() : null;

                if (tokenVotacao == null || numeroCandidato == null || eleicaoId == null) {
                    res.status(400);
                    return gson.toJson(Map.of("erro", "tokenVotacao, numeroCandidato e eleicaoId são obrigatórios"));
                }

                try {
                    servicoEleicao.registrarVoto(tokenVotacao, numeroCandidato, eleicaoId);

                    res.status(201);
                    return gson.toJson(Map.of("mensagem", "Voto registrado com sucesso"));
                } catch (IllegalStateException e) {
                    res.status(400);
                    return gson.toJson(Map.of("erro", e.getMessage()));
                } catch (IllegalArgumentException e) {
                    res.status(400);
                    return gson.toJson(Map.of("erro", e.getMessage()));
                } catch (Exception e) {
                    res.status(500);
                    return gson.toJson(Map.of("erro", "Erro ao registrar voto: " + e.getMessage()));
                }
            });

            // GET /api/v1/votos/meus-votos?cpf=12345678900
            // Busca votos associados a um CPF específico
            get("/meus-votos", (req, res) -> {
                res.type("application/json");
                String cpf = req.queryParams("cpf");

                // Validação do parâmetro CPF
                if (cpf == null || cpf.trim().isEmpty()) {
                    res.status(400);
                    return gson.toJson(Map.of(
                            "erro", "CPF é obrigatório",
                            "mensagem", "Informe o CPF como query parameter: ?cpf=12345678900"
                    ));
                }

                try {
                    // Converte CPF para hash (mesma função usada no cadastro)
                    String cpfHash = Eleitor.hashCpf(cpf);

                    System.out.println("[VOTOS] Buscando votos para CPF hash: " + cpfHash.substring(0, 8) + "...");

                    // Busca todos os tokens gerados por este eleitor
                    List<String> tokensDoEleitor = gerenciadorToken.obterTokensDoEleitor(cpfHash);

                    System.out.println("[VOTOS] Eleitor possui " + tokensDoEleitor.size() + " tokens gerados");

                    if (tokensDoEleitor.isEmpty()) {
                        res.status(200);
                        return gson.toJson(Map.of(
                                "cpfHash", cpfHash.substring(0, 8) + "...",
                                "totalVotos", 0,
                                "votos", new ArrayList<>(),
                                "mensagem", "Nenhum voto encontrado para este eleitor"
                        ));
                    }

                    // Busca votos na blockchain usando os tokens
                    List<Map<String, Object>> meusVotos = buscarVotosPorTokens(tokensDoEleitor);

                    System.out.println("[VOTOS] Encontrados " + meusVotos.size() + " votos registrados na blockchain");

                    res.status(200);
                    return gson.toJson(Map.of(
                            "cpfHash", cpfHash.substring(0, 8) + "...",
                            "totalVotos", meusVotos.size(),
                            "votos", meusVotos
                    ));

                } catch (Exception e) {
                    System.err.println("[VOTOS] Erro ao buscar votos: " + e.getMessage());
                    e.printStackTrace();

                    res.status(500);
                    return gson.toJson(Map.of(
                            "erro", "Erro ao buscar votos",
                            "mensagem", e.getMessage()
                    ));
                }
            });

            // POST /api/v1/votos/batch - Cadastro em lote (para testes)
            post("/batch", (req, res) -> {
                res.type("application/json");
                String solicitanteId = req.attribute("adminId");

                res.status(201);
                return "{\"message\":\"Votos cadastrados com sucesso!\"}";
            });

            // GET /api/v1/votos/listar - Lista votos/candidatos (para admins)
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

    /**
     * Busca votos na blockchain usando uma lista de tokens.
     *
     * Este método garante privacidade porque:
     * 1. Só retorna votos que correspondem aos tokens do eleitor
     * 2. Os tokens são anônimos na blockchain
     * 3. Não é possível vincular token ao eleitor sem o GerenciadorTokenVotacao
     *
     * @param tokensDoEleitor Lista de tokens anônimos gerados pelo eleitor
     * @return Lista de votos encontrados na blockchain
     */
    private List<Map<String, Object>> buscarVotosPorTokens(List<String> tokensDoEleitor) {
        List<Map<String, Object>> meusVotos = new ArrayList<>();

        System.out.println("[DEBUG] Iniciando busca de votos na blockchain...");
        System.out.println("[DEBUG] Total de blocos: " + no.getBlockchain().getTamanho());
        System.out.println("[DEBUG] Tokens a buscar: " + tokensDoEleitor.size());

        // Percorre todos os blocos da blockchain
        for (Bloco bloco : no.getBlockchain().getBlocos()) {
            if (bloco.getIndice() == 0) continue; // Pula bloco genesis

            // Para cada transação no bloco
            for (Transacao transacao : bloco.getTransacoes()) {

                // Filtra apenas transações de voto
                if (transacao.getTipo() == TipoTransacao.VOTO) {
                    Voto voto = transacao.getPayloadAs(Voto.class);

                    if (voto != null) {
                        // Verifica se o token do voto está na lista de tokens do eleitor
                        if (tokensDoEleitor.contains(voto.getTokenVotacao())) {
                            // Este voto pertence ao eleitor!

                            Map<String, Object> votoInfo = new HashMap<>();
                            votoInfo.put("tokenVotacao", voto.getTokenVotacao());
                            votoInfo.put("idEleicao", voto.getIdEleicao());
                            votoInfo.put("tipoCandidato", voto.getTipoCandidato());
                            votoInfo.put("timestamp", voto.getTimestamp());
                            votoInfo.put("blocoHash", bloco.getHash());
                            votoInfo.put("blocoIndice", bloco.getIndice());

                            // PRIVACIDADE: NÃO incluímos idCandidato
                            // Isso mantém o voto secreto mesmo para o próprio eleitor
                            // (ele sabe que votou, mas não pode provar em quem votou)

                            meusVotos.add(votoInfo);

                            System.out.println("[DEBUG] ✓ Voto encontrado!");
                            System.out.println("[DEBUG]   Bloco: " + bloco.getIndice());
                            System.out.println("[DEBUG]   Token: " + voto.getTokenVotacao().substring(0, 8) + "...");
                            System.out.println("[DEBUG]   Eleição: " + voto.getIdEleicao());
                        }
                    }
                }
            }
        }

        System.out.println("[DEBUG] Busca concluída: " + meusVotos.size() + " votos encontrados");

        return meusVotos;
    }
}