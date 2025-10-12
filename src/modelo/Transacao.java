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

    // Construtor vazio para desserialização
    public Transacao() {
    }

    // ★ Construtor com payload já como String JSON
    public Transacao(TipoTransacao tipo, String payloadJson, String idOrigem) {
        this.timestamp = Instant.now().toEpochMilli();
        this.tipo = tipo;
        this.payload = payloadJson; // ★ Já é string, não serializa de novo
        this.idOrigem = idOrigem;

        // ★ Gerar ID usando um hash simples
        this.id = gerarIdUnico(idOrigem, tipo, this.timestamp);
    }

    // ★ NOVO: ID simples e previsível
    private static String gerarIdUnico(String idOrigem, TipoTransacao tipo, long timestamp) {
        return idOrigem + "-" + tipo.name() + "-" + timestamp;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transacao transacao = (Transacao) o;
        return id != null && id.equals(transacao.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Transacao{" + "id='" + id + '\'' + ", tipo=" + tipo + ", timestamp=" + timestamp + '}';
    }
}