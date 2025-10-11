package modelo;

import java.io.Serializable;
import java.time.Instant;

import blockchain.BlockchainGovernamental;
import modelo.enums.TipoTransacao;

public class Transacao implements Serializable {
    private String id;
    private TipoTransacao tipo;
    private Serializable payload;
    private String idOrigem;
    private long timestamp;

    public Transacao(TipoTransacao tipo, Serializable payload, String idOrigem) {
        this.timestamp = Instant.now().toEpochMilli();
        this.id = BlockchainGovernamental.gerarIdUnico(idOrigem, tipo, payload, timestamp);
        this.tipo = tipo;
        this.payload = payload;
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

    public String getIdOrigem() {
        return idOrigem;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return tipo + payload.toString() + idOrigem + timestamp;
    }
}