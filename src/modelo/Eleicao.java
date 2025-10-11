package modelo;

import java.time.Instant;
import java.util.UUID;

public class Eleicao {
    String id;
    private String descricao;
    private long inicio;
    private long fim;
    private boolean ativa;

    public Eleicao(String descricao, long inicio, long fim) {
        this.id = UUID.randomUUID().toString();
        this.descricao = descricao;
        this.inicio = inicio;
        this.fim = fim;
        this.ativa = true;
    }

    public String getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }

    public long getInicio() {
        return inicio;
    }

    public long getFim() {
        return fim;
    }

    public boolean estaAberta() {
        long agora = Instant.now().toEpochMilli();
        return ativa && agora >= inicio && agora <= fim;
    }
}
