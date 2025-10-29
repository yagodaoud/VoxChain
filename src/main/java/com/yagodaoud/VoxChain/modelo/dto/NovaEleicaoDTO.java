package com.yagodaoud.VoxChain.modelo.dto;

import com.yagodaoud.VoxChain.modelo.enums.CategoriaEleicao;

import java.util.List;

public class NovaEleicaoDTO {
    private String nome;
    private String descricao;
    private List<CategoriaEleicao> categorias;
    private long dataInicio;
    private long dataFim;

    // Getters
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public List<CategoriaEleicao> getCategorias() { return categorias; }
    public long getDataInicio() { return dataInicio; }
    public long getDataFim() { return dataFim; }
}