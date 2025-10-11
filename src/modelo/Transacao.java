package modelo;

import java.io.Serializable;
import java.time.Instant;

import modelo.enums.TipoTransacao;

class Transacao implements Serializable {
    private TipoTransacao tipo;
    private Object dados;
    private String idAdmin;
    private long timestamp;

    public Transacao(TipoTransacao tipo, Object dados, String idAdmin) {
        this.tipo = tipo;
        this.dados = dados;
        this.idAdmin = idAdmin;
        this.timestamp = Instant.now().toEpochMilli();
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