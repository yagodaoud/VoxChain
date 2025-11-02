package com.yagodaoud.VoxChain.modelo;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Registro que indica QUEM votou (comparecimento), mas não EM QUEM votou.
 * Separa o controle de comparecimento do voto em si, garantindo anonimato.
 */
public class RegistroComparecimento implements Serializable {
    private String id;
    private String eleitorHash;
    private String eleicaoId;
    private long timestamp;
    private String tokenGerado; // Referência ao token sem revelar o voto

    public RegistroComparecimento(String eleitorHash, String eleicaoId, String tokenGerado) {
        this.id = UUID.randomUUID().toString();
        this.eleitorHash = eleitorHash;
        this.eleicaoId = eleicaoId;
        this.timestamp = Instant.now().toEpochMilli();
        this.tokenGerado = tokenGerado;
    }

    public String getId() {
        return id;
    }

    public String getEleitorHash() {
        return eleitorHash;
    }

    public String getEleicaoId() {
        return eleicaoId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getTokenGerado() {
        return tokenGerado;
    }
}

