package com.yagodaoud.VoxChain.modelo;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Registro de auditoria para ações sensíveis no sistema.
 */
public class LogAuditoria implements Serializable {
    private String id;
    private long timestamp;
    private String acao; // LOGIN, CRIAR_ELEICAO, CADASTRAR_ADMIN, etc
    private String usuarioHash;
    private String detalhes; // JSON com informações adicionais
    private String ipOrigem;

    public LogAuditoria(String acao, String usuarioHash, String detalhes, String ipOrigem) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = Instant.now().toEpochMilli();
        this.acao = acao;
        this.usuarioHash = usuarioHash;
        this.detalhes = detalhes;
        this.ipOrigem = ipOrigem;
    }

    public String getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getAcao() {
        return acao;
    }

    public String getUsuarioHash() {
        return usuarioHash;
    }

    public String getDetalhes() {
        return detalhes;
    }

    public String getIpOrigem() {
        return ipOrigem;
    }
}

