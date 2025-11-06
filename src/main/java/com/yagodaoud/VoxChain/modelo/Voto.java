package com.yagodaoud.VoxChain.modelo;

import java.io.Serializable;
import java.time.Instant;

public class Voto implements Serializable {
    private String tokenVotacao;
    private String idCandidato;
    private String tipoCandidato;
    private String idEleicao;
    private long timestamp;

    public Voto(String tokenVotacao, String idCandidato, String tipoCandidato, String idEleicao) {
        this.tokenVotacao = tokenVotacao;
        this.idCandidato = idCandidato;
        this.tipoCandidato = tipoCandidato;
        this.idEleicao = idEleicao;
        this.timestamp = Instant.now().toEpochMilli();
    }

    public String getTokenVotacao() { return tokenVotacao; }
    public String getIdCandidato() { return idCandidato; }
    public String getTipoCandidato() { return tipoCandidato; }
    public String getIdEleicao() { return idEleicao; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return tokenVotacao + idCandidato + timestamp;
    }

    public boolean validar(Eleicao eleicao, Candidato candidato) {
        return eleicao.estaAberta() && candidato != null;
    }
}
