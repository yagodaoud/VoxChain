package com.yagodaoud.VoxChain.modelo;

import com.yagodaoud.VoxChain.modelo.enums.CategoriaEleicao;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Eleicao {
    String id;
    private String nome;
    private String descricao;
    private List<CategoriaEleicao> categorias;
    private long dataInicio;
    private long dataFim;
    private boolean ativa;

    public Eleicao(String nome, String descricao, List<CategoriaEleicao> categorias, long dataInicio, long dataFim) {
        this.id = UUID.randomUUID().toString();
        this.nome = nome;
        this.descricao = descricao;
        this.categorias = categorias;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.ativa = true;
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public List<CategoriaEleicao> getCategorias() {
        return categorias;
    }

    public long getDataInicio() {
        return dataInicio;
    }

    public long getDataFim() {
        return dataFim;
    }

    public boolean estaAberta() {
        long agora = Instant.now().toEpochMilli();
        return ativa && agora >= dataInicio && agora <= dataFim;
    }
}
