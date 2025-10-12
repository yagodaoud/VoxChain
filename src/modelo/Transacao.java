package modelo;

import blockchain.BlockchainGovernamental;
import com.google.gson.Gson;
import modelo.enums.TipoTransacao;

import java.io.Serializable;
import java.time.Instant;

public class Transacao implements Serializable {
    private static final Gson gson = new Gson();

    private String id;
    private TipoTransacao tipo;
    private String payload;
    private String idOrigem;
    private long timestamp;

    public <T> Transacao(TipoTransacao tipo, T payload, String idOrigem) {
        this.timestamp = Instant.now().toEpochMilli();
        this.id = BlockchainGovernamental.gerarIdUnico(idOrigem, tipo, payload, timestamp);
        this.tipo = tipo;
        this.payload = gson.toJson(payload);
        this.idOrigem = idOrigem;
        this.timestamp = Instant.now().toEpochMilli();
    }

    public String getId() {
        return id;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public Serializable getPayload() {
        return payload;
    }

    public <T> T getPayloadAs(Class<T> clazz) {
        return gson.fromJson(payload, clazz);
    }

    public String getIdOrigem() {
        return idOrigem;
    }

    public long getTimestamp() {
        return timestamp;
    }
}