package modelo;

import java.io.Serializable;
import java.time.Instant;

import blockchain.BlockchainGovernamental;
import modelo.enums.TipoTransacao;

public class Transacao implements Serializable {
    private String id;
    private TipoTransacao tipo;
    private Object dados;
    private String idAdmin;
    private long timestamp;

    public Transacao(TipoTransacao tipo, Object dados, String idAdmin) {
        this.id = BlockchainGovernamental.gerarIdUnico(idAdmin, tipo, dados, timestamp);
        this.tipo = tipo;
        this.dados = dados;
        this.idAdmin = idAdmin;
        this.timestamp = Instant.now().toEpochMilli();
    }

    public String getId() {
        return id;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public Object getDados() {
        return dados;
    }

    public String getIdAdmin() {
        return idAdmin;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return tipo + dados.toString() + idAdmin + timestamp;
    }
}