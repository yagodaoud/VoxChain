package com.yagodaoud.VoxChain.modelo.dto;

public class NovaEleicaoDTO {
    private String solicitanteId;
    private String descricao;
    private long dataInicio;
    private long dataFim;

    // Getters
    public String getSolicitanteId() { return solicitanteId; }
    public String getDescricao() { return descricao; }
    public long getDataInicio() { return dataInicio; }
    public long getDataFim() { return dataFim; }
}