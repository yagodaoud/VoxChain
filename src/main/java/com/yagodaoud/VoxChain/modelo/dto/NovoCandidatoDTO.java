package com.yagodaoud.VoxChain.modelo.dto;

import com.yagodaoud.VoxChain.modelo.enums.CargoCandidato;

public class NovoCandidatoDTO {
    private String solicitanteId;
    private String eleicaoId;
    private String numero;
    private String nome;
    private String partido;
    private CargoCandidato cargo;
    private String uf;
    private String fotoUrl;

    // Getters
    public String getSolicitanteId() { return solicitanteId; }
    public String getEleicaoId() { return eleicaoId; }
    public String getNumero() { return numero; }
    public String getNome() { return nome; }
    public String getPartido() { return partido; }
    public CargoCandidato getCargo() { return cargo; }
    public String getUf() { return uf; }
    public String getFotoUrl() { return fotoUrl; }
}