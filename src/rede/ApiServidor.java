package rede;

import blockchain.No;
import com.google.gson.Gson;
import modelo.Transacao;

import static spark.Spark.*;

public class ApiServidor {

    private No no;

    public ApiServidor(No no) {
        this.no = no;
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
            Transacao t = gson.fromJson(req.body(), Transacao.class);
            no.adicionarTransacao(t); // agora funciona
            res.status(201);
            return "Transação recebida e broadcast iniciada";
        });
    }
}
