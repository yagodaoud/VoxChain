package com.yagodaoud.VoxChain.blockchain.servicos;

import com.yagodaoud.VoxChain.modelo.TokenVotacao;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de tokens de votação anônima.
 * Responsável por gerar, validar e gerenciar tokens que permitem votação sem revelar identidade do eleitor.
 */
public class GerenciadorTokenVotacao {
    
    // Chave: tokenAnonimo -> TokenVotacao
    private final Map<String, TokenVotacao> tokensAtivos;
    
    // Chave: eleitorHash+eleicaoId -> TokenVotacao (para verificar se já tem token)
    private final Map<String, TokenVotacao> tokensPorEleitor;
    
    private static final long VALIDADE_TOKEN_MS = 30 * 60 * 1000; // 30 minutos

    public GerenciadorTokenVotacao() {
        this.tokensAtivos = new ConcurrentHashMap<>();
        this.tokensPorEleitor = new ConcurrentHashMap<>();
    }

    /**
     * Gera um token anônimo para votação
     * @param eleitorHash Hash do CPF do eleitor
     * @param eleicaoId ID da eleição
     * @return TokenVotacao gerado
     * @throws IllegalStateException se o eleitor já possui token ativo para esta eleição
     */
    public TokenVotacao gerarToken(String eleitorHash, String eleicaoId) {
        String chaveEleitor = criarChaveEleitor(eleitorHash, eleicaoId);
        
        // Verifica se eleitor já tem token ativo
        TokenVotacao tokenExistente = tokensPorEleitor.get(chaveEleitor);
        if (tokenExistente != null && tokenExistente.isValido()) {
            throw new IllegalStateException("Eleitor já possui token ativo para esta eleição");
        }
        
        // Gera novo token válido por 30 minutos
        long validoAte = System.currentTimeMillis() + VALIDADE_TOKEN_MS;
        TokenVotacao novoToken = new TokenVotacao(eleitorHash, eleicaoId, validoAte);
        
        // Armazena o token
        tokensAtivos.put(novoToken.getTokenAnonimo(), novoToken);
        tokensPorEleitor.put(chaveEleitor, novoToken);
        
        return novoToken;
    }

    /**
     * Valida um token de votação
     * @param tokenAnonimo Token anônimo a ser validado
     * @param eleicaoId ID da eleição
     * @return true se o token é válido, false caso contrário
     */
    public boolean validarToken(String tokenAnonimo, String eleicaoId) {
        TokenVotacao token = tokensAtivos.get(tokenAnonimo);
        
        if (token == null) {
            return false;
        }
        
        // Verifica se o token é da eleição correta
        if (!token.getEleicaoId().equals(eleicaoId)) {
            return false;
        }
        
        // Verifica se o token é válido (não usado e não expirado)
        return token.isValido();
    }

    /**
     * Marca um token como usado após a votação
     * @param tokenAnonimo Token a ser marcado como usado
     */
    public void marcarTokenComoUsado(String tokenAnonimo) {
        TokenVotacao token = tokensAtivos.get(tokenAnonimo);
        if (token != null) {
            token.marcarComoUsado();
            
            // Remove das listas ativas mas mantém no histórico
            tokensAtivos.remove(tokenAnonimo);
            String chaveEleitor = criarChaveEleitor(token.getEleitorHash(), token.getEleicaoId());
            tokensPorEleitor.remove(chaveEleitor);
        }
    }

    /**
     * Verifica se um eleitor já possui token ativo para uma eleição
     * @param eleitorHash Hash do CPF do eleitor
     * @param eleicaoId ID da eleição
     * @return true se já possui token ativo
     */
    public boolean eleitorPossuiTokenAtivo(String eleitorHash, String eleicaoId) {
        String chaveEleitor = criarChaveEleitor(eleitorHash, eleicaoId);
        TokenVotacao token = tokensPorEleitor.get(chaveEleitor);
        return token != null && token.isValido();
    }

    /**
     * Remove tokens expirados (limpeza periódica)
     */
    public void limparTokensExpirados() {
        long agora = System.currentTimeMillis();
        tokensAtivos.entrySet().removeIf(entry -> {
            TokenVotacao token = entry.getValue();
            if (!token.isValido() || agora >= token.getValidoAte()) {
                // Remove também do mapa por eleitor
                String chaveEleitor = criarChaveEleitor(token.getEleitorHash(), token.getEleicaoId());
                tokensPorEleitor.remove(chaveEleitor);
                return true;
            }
            return false;
        });
    }

    private String criarChaveEleitor(String eleitorHash, String eleicaoId) {
        return eleitorHash + ":" + eleicaoId;
    }
}

