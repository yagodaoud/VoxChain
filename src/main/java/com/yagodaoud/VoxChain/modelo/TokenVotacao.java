package com.yagodaoud.VoxChain.modelo;

import com.yagodaoud.VoxChain.utils.SecurityUtils;

import java.io.Serializable;
import java.util.UUID;

public class TokenVotacao implements Serializable {
    private String tokenAnonimo;
    private String eleicaoId;
    private long validoAte;
    private boolean usado;
    private String hashAutorizacao;
    private String eleitorHash; // Armazenado apenas para validação, não usado no voto

    public TokenVotacao(String eleitorHash, String eleicaoId, long validoAte) {
        this.tokenAnonimo = UUID.randomUUID().toString();
        this.eleicaoId = eleicaoId;
        this.validoAte = validoAte;
        this.usado = false;
        this.eleitorHash = eleitorHash;
        
        // Gera hash de autorização: SHA-256 de eleitorHash+eleicaoId+salt
        String salt = "TOKEN_SALT_2025";
        this.hashAutorizacao = SecurityUtils.hash(eleitorHash + eleicaoId + salt, "");
    }

    public String getTokenAnonimo() {
        return tokenAnonimo;
    }

    public String getEleicaoId() {
        return eleicaoId;
    }

    public long getValidoAte() {
        return validoAte;
    }

    public boolean isUsado() {
        return usado;
    }

    public void marcarComoUsado() {
        this.usado = true;
    }

    public String getHashAutorizacao() {
        return hashAutorizacao;
    }

    public String getEleitorHash() {
        return eleitorHash;
    }

    /**
     * Verifica se o token é válido (não está usado e não expirou)
     */
    public boolean isValido() {
        long agora = System.currentTimeMillis();
        return !usado && agora < validoAte;
    }
}

