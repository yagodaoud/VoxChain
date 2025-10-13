package com.yagodaoud.VoxChain.blockchain;

import com.yagodaoud.VoxChain.modelo.Transacao;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Bloco implements Serializable {
    private static final long serialVersionUID = 1L;

    private int indice;
    private final long timestamp;
    private final List<Transacao> transacoes;
    private final String hashAnterior;
    private String hash;
    private int nonce;
    private final String mineradoPor;
    private String assinaturaMinerador; // opcional — simulando chave pública do nó minerador

    public Bloco(int indice, List<Transacao> transacoes, String hashAnterior, String mineradoPor) {
        this.indice = indice;
        this.timestamp = Instant.now().toEpochMilli();
        this.transacoes = new ArrayList<>(transacoes);
        this.hashAnterior = hashAnterior;
        this.nonce = 0;
        this.mineradoPor = mineradoPor;
        this.hash = calcularHash();
    }

    // ==================== CÁLCULO DE HASH ====================

    public String calcularHash() {
        try {
            String dados = indice + hashAnterior + timestamp + transacoesParaString() + nonce + mineradoPor;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(dados.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao calcular hash do bloco", e);
        }
    }

    private String transacoesParaString() {
        StringBuilder sb = new StringBuilder();
        for (Transacao t : transacoes) {
            sb.append(t.toString());
        }
        return sb.toString();
    }

    // ==================== PROVA DE TRABALHO ====================

    public void minerarBloco(int dificuldade) {
        String alvo = "0".repeat(Math.max(0, dificuldade));
        while (!hash.startsWith(alvo)) {
            nonce++;
            hash = calcularHash();
        }
        this.assinaturaMinerador = gerarAssinaturaMinerador();
        System.out.println("⛏️  Bloco " + indice + " minerado por " + mineradoPor +
                " | Hash: " + getHashTruncado(12));
    }

    private String gerarAssinaturaMinerador() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] assinatura = digest.digest((mineradoPor + hash).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : assinatura) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public void setIndice(int indice) {
        this.indice = indice;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    // ==================== GETTERS ====================

    public int getIndice() {
        return indice;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getHash() {
        return hash;
    }

    public String getHashAnterior() {
        return hashAnterior;
    }

    public int getNonce() {
        return nonce;
    }

    public String getMineradoPor() {
        return mineradoPor;
    }

    public List<Transacao> getTransacoes() {
        return new ArrayList<>(transacoes);
    }

    public String getAssinaturaMinerador() {
        return assinaturaMinerador;
    }

    // ==================== FORMATAÇÃO ====================

    public String getHashTruncado(int tamanho) {
        return (hash == null || hash.isEmpty()) ? "0"
                : hash.substring(0, Math.min(tamanho, hash.length()));
    }

    public String getHashAnteriorTruncado(int tamanho) {
        if (hashAnterior == null || hashAnterior.equals("0") || hashAnterior.isEmpty()) {
            return "[GENESIS]";
        }
        return hashAnterior.substring(0, Math.min(tamanho, hashAnterior.length()));
    }

    @Override
    public String toString() {
        return "Bloco{" +
                "indice=" + indice +
                ", hash=" + getHashTruncado(12) +
                ", transacoes=" + transacoes.size() +
                ", mineradoPor='" + mineradoPor + '\'' +
                '}';
    }
}
