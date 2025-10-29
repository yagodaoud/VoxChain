package com.yagodaoud.VoxChain.rede.api.v1;

import com.google.gson.Gson;
import com.yagodaoud.VoxChain.blockchain.No;
import com.yagodaoud.VoxChain.rede.Peer;

import static spark.Spark.*;

public class BlockchainController implements IApiController {

    private final No no;

    public BlockchainController(No no) {
        this.no = no;
    }

    @Override
    public void registerRoutes(Gson gson) {
        path("/blockchain", () -> {
            get("", (req, res) -> {
                res.type("application/json");
                return gson.toJson(no.getBlockchain().getBlocos());
            });

            get("/status", (req, res) -> {
                res.type("application/json");
                return gson.toJson(no.getStatus());
            });

            get("/blockchain/bloco/:index", (req, res) -> {
                int index = Integer.parseInt(req.params("index"));
                res.type("application/json");
                return gson.toJson(no.getBlockchain().getBloco(index));
            });

            get("/blockchain/status", (req, res) -> {
                res.type("application/json");
                return gson.toJson(no.getStatus());
            });

            get("/dashboard", (req, res) -> {
                res.type("text/html");
                return gerarHtmlDashboard();
            });
        });

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