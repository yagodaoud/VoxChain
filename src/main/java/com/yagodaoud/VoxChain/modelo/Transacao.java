package com.yagodaoud.VoxChain.modelo;

import com.google.gson.Gson;
import com.yagodaoud.VoxChain.modelo.enums.TipoTransacao;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public class Transacao implements Serializable {
    private static final Gson gson = new Gson();

    private String id;
    private TipoTransacao tipo;
    private String payload;  // ★ Sempre String (JSON)
    private String idOrigem;
    private long timestamp;

    // ============ CONSTRUTORES ============

    // Construtor vazio para desserialização
    public Transacao() {
    }

    public <T> Transacao(TipoTransacao tipo, T payloadObject,
                         String idOrigem, long timestampFixo) {
        this.timestamp = timestampFixo;
        this.tipo = tipo;
        this.idOrigem = idOrigem;
        this.payload = gson.toJson(payloadObject);
        this.id = gerarIdUnico(idOrigem, tipo, this.timestamp, true);
    }

    // ★ NOVO: Aceita qualquer Object e converte para JSON
    public <T> Transacao(TipoTransacao tipo, T payloadObject, String idOrigem) {
        this.timestamp = Instant.now().toEpochMilli();
        this.tipo = tipo;
        this.idOrigem = idOrigem;

        // ★ Converte objeto para JSON automaticamente
        this.payload = gson.toJson(payloadObject);

        // ★ Gera ID único
        this.id = gerarIdUnico(idOrigem, tipo, this.timestamp, false);
    }

    // ============ GERAÇÃO DE ID ============

    private static String gerarIdUnico(String idOrigem, TipoTransacao tipo, long timestamp, boolean isFixed) {
        if (isFixed) {
            String input = idOrigem + tipo.name() + timestamp;
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                byte[] hashBytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                // Take first 8 characters of the hex string
                String hashHex = bytesToHex(hashBytes).substring(0, 8);
                return idOrigem + "-" + tipo.name() + "-" + timestamp + "-" + hashHex;
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new RuntimeException("Failed to generate hash for fixed ID", e);
            }
        } else {
            String uuid = UUID.randomUUID().toString().substring(0, 8);
            return idOrigem + "-" + tipo.name() + "-" + timestamp + "-" + uuid;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // ============ SETTERS ============

    public void setId(String id) {
        this.id = id;
    }

    // ============ GETTERS ============

    public String getId() {
        return id;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    // ★ Retorna o JSON string do payload
    public String getPayloadJson() {
        return payload;
    }

    // ★ Converte JSON para um objeto específico
    public <T> T getPayloadAs(Class<T> clazz) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(payload, clazz);
        } catch (Exception e) {
            System.err.println("Erro ao desserializar payload: " + e.getMessage());
            return null;
        }
    }

    public String getIdOrigem() {
        return idOrigem;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // ============ EQUALS E HASHCODE ============

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transacao transacao = (Transacao) o;
        return id != null && id.equals(transacao.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    // ============ TOSTRING ============

    @Override
    public String toString() {
        return "Transacao{" +
                "id='" + id + '\'' +
                ", tipo=" + tipo +
                ", idOrigem='" + idOrigem + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}