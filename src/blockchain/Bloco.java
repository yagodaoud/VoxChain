package blockchain;

import modelo.Transacao;

import java.io.Serializable;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Bloco implements Serializable {
    private static final long serialVersionUID = 1L;

    private int indice;
    private long timestamp;
    private List<Transacao> transacoes;
    private String hashAnterior;
    private String hash;
    private int nonce;
    private String mineradoPor;

    public Bloco(int indice, List<Transacao> transacoes, String hashAnterior, String mineradoPor) {
        this.indice = indice;
        this.timestamp = Instant.now().toEpochMilli();
        this.transacoes = new ArrayList<>(transacoes);
        this.hashAnterior = hashAnterior;
        this.nonce = 0;
        this.mineradoPor = mineradoPor;
        this.hash = calcularHash();
    }

    public String calcularHash() {
        try {
            String dados = indice + timestamp + transacoesParaString() + hashAnterior + nonce;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(dados.getBytes("UTF-8"));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String transacoesParaString() {
        StringBuilder sb = new StringBuilder();
        for (Transacao t : transacoes) {
            sb.append(t.toString());
        }
        return sb.toString();
    }

    public void minerarBloco(int dificuldade) {
        String alvo = new String(new char[dificuldade]).replace('\0', '0');
        while (!hash.substring(0, dificuldade).equals(alvo)) {
            nonce++;
            hash = calcularHash();
        }
    }

    // ==================== GETTERS ====================

    public int getIndice() {
        return indice;
    }

    public String getHash() {
        return hash;
    }

    public String getHashAnterior() {
        return hashAnterior;
    }

    public List<Transacao> getTransacoes() {
        return new ArrayList<>(transacoes);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getNonce() {
        return nonce;
    }

    public String getMineradoPor() {
        return mineradoPor;
    }

    public String getHashTruncado(int tamanho) {
        if (hash == null || hash.length() == 0) {
            return "0";
        }
        int fim = Math.min(tamanho, hash.length());
        return hash.substring(0, fim) + (fim < hash.length() ? "..." : "");
    }

    public String getHashAnteriorTruncado(int tamanho) {
        if (hashAnterior == null || hashAnterior.length() == 0) {
            return "[GENESIS]";
        }
        if (hashAnterior.equals("0")) {
            return "[GENESIS]";
        }
        int fim = Math.min(tamanho, hashAnterior.length());
        return hashAnterior.substring(0, fim) + (fim < hashAnterior.length() ? "..." : "");
    }


    @Override
    public String toString() {
        return "Bloco{" +
                "indice=" + indice +
                ", hash=" + hash.substring(0, 16) + "..." +
                ", transacoes=" + transacoes.size() +
                ", mineradoPor='" + mineradoPor + '\'' +
                '}';
    }
}