package com.yagodaoud.VoxChain.rede;

import java.io.Serializable;

public class MensagemP2P implements Serializable {
    private static final long serialVersionUID = 1L;

    private TipoMensagem tipo;
    private Object payload;
    private String remetente;
    private long timestamp;

    public MensagemP2P(TipoMensagem tipo, Object payload, String remetente) {
        this.tipo = tipo;
        this.payload = payload;
        this.remetente = remetente;
        this.timestamp = System.currentTimeMillis();
    }

    public TipoMensagem getTipo() { return tipo; }
    public Object getPayload() { return payload; }
    public String getRemetente() { return remetente; }
    public long getTimestamp() { return timestamp; }
}
