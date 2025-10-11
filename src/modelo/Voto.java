package modelo;

import java.io.Serializable;
import java.time.Instant;

public class Voto implements Serializable {
    private String idEleitorHash;
    private String idCandidato;
    private String tipoCandidato;
    private String idEleicao;
    private long timestamp;

    public Voto(String idEleitorHash, String idCandidato, String tipoCandidato, String idEleicao) {
        this.idEleitorHash = idEleitorHash;
        this.idCandidato = idCandidato;
        this.tipoCandidato = tipoCandidato;
        this.idEleicao = idEleicao;
        this.timestamp = Instant.now().toEpochMilli();
    }

    public String getIdEleitorHash() { return idEleitorHash; }
    public String getIdCandidato() { return idCandidato; }
    public String getTipoCandidato() { return tipoCandidato; }
    public String getIdEleicao() { return idEleicao; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return idEleitorHash + idCandidato + timestamp;
    }

    public boolean validar(Eleicao eleicao, Candidato candidato) {
        return eleicao.estaAberta() && candidato != null;
    }
}
