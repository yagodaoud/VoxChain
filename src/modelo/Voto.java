package modelo;

import java.io.Serializable;
import java.time.Instant;

public class Voto implements Serializable {
    private String idEleitor;
    private String candidato;
    private String tipoCandidato;
    private String idEleicao;
    private long timestamp;

    public Voto(String idEleitor, String candidato, String tipoCandidato, String idEleicao) {
        this.idEleitor = idEleitor;
        this.candidato = candidato;
        this.tipoCandidato = tipoCandidato;
        this.idEleicao = idEleicao;
        this.timestamp = Instant.now().toEpochMilli();
    }

    public String getIdEleitor() { return idEleitor; }
    public String getCandidato() { return candidato; }
    public String getTipoCandidato() { return tipoCandidato; }
    public String getIdEleicao() { return idEleicao; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return idEleitor + candidato + timestamp;
    }
}
