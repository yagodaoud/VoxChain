package com.yagodaoud.VoxChain.rede;

import com.yagodaoud.VoxChain.blockchain.No;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.yagodaoud.VoxChain.modelo.Transacao;
import com.yagodaoud.VoxChain.modelo.enums.TipoTransacao;

import static spark.Spark.*;

public class ApiServidor {

    private No no;
    private NetworkMonitor monitor;

    public ApiServidor(No no) {
        this.no = no;
    }

    public ApiServidor comMonitor(NetworkMonitor monitor) {
        this.monitor = monitor;
        return this;
    }

    public void iniciar(int porta) {
        Gson gson = new Gson();
        port(porta);

        get("/blockchain", (req, res) -> {
            res.type("application/json");
            return gson.toJson(no.getBlockchain().getBlocos());
        });

        get("/blockchain/bloco/:index", (req, res) -> {
            int index = Integer.parseInt(req.params("index"));
            res.type("application/json");
            return gson.toJson(no.getBlockchain().getBloco(index));
        });

        get("/blockchain/voto/:hash", (req, res) -> {
            String hash = req.params("hash");
            res.type("application/json");
            return gson.toJson(no.getBlockchain().buscarVotoPorHash(hash));
        });

        get("/blockchain/status", (req, res) -> {
            res.type("application/json");
            return gson.toJson(no.getStatus());
        });

        post("/transacao", (req, res) -> {
            try {
                // Parse do JSON recebido
                JsonObject json = gson.fromJson(req.body(), JsonObject.class);

                String tipoStr = json.get("tipo").getAsString();
                TipoTransacao tipo = TipoTransacao.valueOf(tipoStr);

                // O payload vem como string JSON
                String payloadStr = json.get("payload").getAsString();

                String idOrigem = json.get("idOrigem").getAsString();

                System.out.println("[API] Recebeu POST /transacao");
                System.out.println("[API]   - Tipo: " + tipo);
                System.out.println("[API]   - Origem: " + idOrigem);
                System.out.println("[API]   - Payload: " + payloadStr);

                // Criar transação com construtor correto
                // Nota: O ID será gerado automaticamente pelo construtor
                Transacao t = new Transacao(tipo, payloadStr, idOrigem);

                System.out.println("[API]   - ID Gerado: " + t.getId());

                // Adicionar ao nó
                no.adicionarTransacao(t);

                res.status(201);
                res.type("application/json");

                JsonObject response = new JsonObject();
                response.addProperty("status", "success");
                response.addProperty("id", t.getId());
                response.addProperty("tipo", tipo.toString());
                response.addProperty("timestamp", t.getTimestamp());

                return response.toString();

            } catch (Exception e) {
                System.err.println("[API] Erro ao processar transação: " + e.getMessage());
                e.printStackTrace();

                res.status(400);
                res.type("application/json");

                JsonObject error = new JsonObject();
                error.addProperty("status", "error");
                error.addProperty("message", e.getMessage());

                return error.toString();
            }
        });

        get("/rede/status", (req, res) -> {
            res.type("application/json");
            JsonObject status = new JsonObject();

            status.addProperty("noId", no.getId());
            status.addProperty("blocksCount", no.getBlockchain().getTamanho());
            status.addProperty("poolSize", no.getBlockchain().getPoolSize());
            status.addProperty("peersTotal", no.getNumPeers());
            status.addProperty("peersConectados", (int) no.getPeers().stream()
                    .filter(Peer::isConectado).count());

            if (no.obterCatalogoPeers() != null) {
                status.addProperty("catalogoSize", no.obterCatalogoPeers().size());
            }

            return status.toString();
        });

        // Lista de peers conectados
        get("/rede/peers", (req, res) -> {
            res.type("application/json");
            JsonArray peers = new JsonArray();

            for (Peer p : no.getPeers()) {
                JsonObject peer = new JsonObject();
                peer.addProperty("id", p.getId());
                peer.addProperty("conectado", p.isConectado());
                peers.add(peer);
            }

            return peers.toString();
        });

        // Catálogo de peers (descobertos)
        get("/rede/catalogo", (req, res) -> {
            res.type("application/json");

            if (no.obterCatalogoPeers() == null) {
                return "[]";
            }

            JsonArray catalogo = new JsonArray();
            for (PeerDiscovery.PeerInfo info : no.obterCatalogoPeers()) {
                JsonObject peer = new JsonObject();
                peer.addProperty("id", info.id);
                peer.addProperty("ip", info.ip);
                peer.addProperty("porta", info.porta);
                peer.addProperty("ativo", info.ativo);
                catalogo.add(peer);
            }

            return catalogo.toString();
        });

        // Estatísticas da rede
        get("/rede/stats", (req, res) -> {
            res.type("application/json");
            JsonObject stats = new JsonObject();

            if (monitor != null) {
                stats.addProperty("transacoesEnviadas", monitor.getTransacoesEnviadas());
                stats.addProperty("blocosEnviados", monitor.getBlocosEnviados());
                stats.addProperty("pingsEnviados", monitor.getPingsEnviados());
                stats.addProperty("bytesEnviados", monitor.getBytesEnviados());
            }

            return stats.toString();
        });

        // Dashboard em HTML (opcional)
        get("/dashboard", (req, res) -> {
            res.type("text/html");
            return gerarHtmlDashboard();
        });

        System.out.println("✓ API REST iniciada com endpoints de monitoramento");
    }

    private String gerarHtmlDashboard() {
        int peersConectados = (int) no.getPeers().stream()
                .filter(Peer::isConectado).count();

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <title>Dashboard - " + no.getId() + "</title>\n" +
                "  <meta charset='UTF-8'>\n" +
                "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
                "  <style>\n" +
                "    * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
                "    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #0f172a; color: #e2e8f0; padding: 20px; }\n" +
                "    .container { max-width: 1200px; margin: 0 auto; }\n" +
                "    h1 { margin-bottom: 30px; text-align: center; color: #3b82f6; }\n" +
                "    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; margin-bottom: 30px; }\n" +
                "    .card { background: #1e293b; border: 1px solid #334155; border-radius: 8px; padding: 20px; }\n" +
                "    .card h2 { color: #60a5fa; margin-bottom: 15px; font-size: 14px; text-transform: uppercase; }\n" +
                "    .stat { font-size: 32px; font-weight: bold; color: #10b981; }\n" +
                "    .stat-label { font-size: 12px; color: #94a3b8; margin-top: 5px; }\n" +
                "    .peer { background: #0f172a; padding: 10px; margin: 5px 0; border-radius: 4px; border-left: 3px solid #10b981; }\n" +
                "    .peer.offline { border-left-color: #ef4444; }\n" +
                "    .refresh-info { text-align: center; color: #64748b; font-size: 12px; margin-top: 20px; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class='container'>\n" +
                "    <h1>🌐 Dashboard Blockchain - " + no.getId() + "</h1>\n" +
                "    \n" +
                "    <div class='grid'>\n" +
                "      <div class='card'>\n" +
                "        <h2>Blocos</h2>\n" +
                "        <div class='stat'>" + no.getBlockchain().getTamanho() + "</div>\n" +
                "        <div class='stat-label'>blocos na blockchain</div>\n" +
                "      </div>\n" +
                "      \n" +
                "      <div class='card'>\n" +
                "        <h2>Pool de Transações</h2>\n" +
                "        <div class='stat'>" + no.getBlockchain().getPoolSize() + "</div>\n" +
                "        <div class='stat-label'>aguardando mineração</div>\n" +
                "      </div>\n" +
                "      \n" +
                "      <div class='card'>\n" +
                "        <h2>Peers Conectados</h2>\n" +
                "        <div class='stat'>" + peersConectados + "</div>\n" +
                "        <div class='stat-label'>de " + no.getNumPeers() + " totais</div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div class='card'>\n" +
                "      <h2>Peers Ativos</h2>\n" +
                "      " + gerarListaPeers() + "\n" +
                "    </div>\n" +
                "    \n" +
                "    <div class='refresh-info'>\n" +
                "      ⏱️ Atualize a página para ver informações em tempo real\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>";
    }

    private String gerarListaPeers() {
        StringBuilder html = new StringBuilder();
        for (Peer p : no.getPeers()) {
            String clase = p.isConectado() ? "peer" : "peer offline";
            String icon = p.isConectado() ? "✓" : "✗";
            html.append("<div class='").append(clase).append("'>")
                    .append(icon).append(" ").append(p.getId()).append("</div>");
        }
        return html.toString();
    }
}