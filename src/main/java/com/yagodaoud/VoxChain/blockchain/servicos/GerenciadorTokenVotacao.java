package com.yagodaoud.VoxChain.blockchain.servicos;

import com.yagodaoud.VoxChain.modelo.TokenVotacao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Gerenciador de tokens de votação anônima.
 * Responsável por gerar, validar e gerenciar tokens que permitem votação sem revelar identidade do eleitor.
 *
 * ATUALIZAÇÃO: Agora mantém histórico de tokens para permitir que eleitores consultem seus votos.
 */
public class GerenciadorTokenVotacao {

    // Chave: tokenAnonimo -> TokenVotacao
    private final Map<String, TokenVotacao> tokensAtivos;

    // Chave: eleitorHash+eleicaoId -> TokenVotacao (para verificar se já tem token)
    private final Map<String, TokenVotacao> tokensPorEleitor;

    // NOVO: Histórico de todos os tokens gerados (mesmo após uso)
    // Chave: eleitorHash -> Lista de tokens gerados
    private final Map<String, List<String>> historicoTokensPorEleitor;

    private static final long VALIDADE_TOKEN_MS = 30 * 60 * 1000; // 30 minutos

    public GerenciadorTokenVotacao() {
        this.tokensAtivos = new ConcurrentHashMap<>();
        this.tokensPorEleitor = new ConcurrentHashMap<>();
        this.historicoTokensPorEleitor = new ConcurrentHashMap<>();
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

        // NOVO: Adiciona ao histórico
        historicoTokensPorEleitor
                .computeIfAbsent(eleitorHash, k -> new ArrayList<>())
                .add(novoToken.getTokenAnonimo());

        System.out.println("[TOKEN] Token gerado para eleitor " +
                eleitorHash.substring(0, 8) + "... | Token: " +
                novoToken.getTokenAnonimo().substring(0, 8) + "...");

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

            System.out.println("[TOKEN] Token marcado como usado: " +
                    tokenAnonimo.substring(0, 8) + "...");

            // Remove das listas ativas
            tokensAtivos.remove(tokenAnonimo);
            String chaveEleitor = criarChaveEleitor(token.getEleitorHash(), token.getEleicaoId());
            tokensPorEleitor.remove(chaveEleitor);

            // MAS mantém no histórico (não remove de historicoTokensPorEleitor)
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
     * NOVO: Retorna todos os tokens gerados por um eleitor
     * Usado para permitir que o eleitor consulte seus votos
     *
     * @param eleitorHash Hash do CPF do eleitor
     * @return Lista de tokens anônimos gerados pelo eleitor
     */
    public List<String> obterTokensDoEleitor(String eleitorHash) {
        List<String> tokens = historicoTokensPorEleitor.get(eleitorHash);

        if (tokens == null) {
            System.out.println("[TOKEN] Nenhum token encontrado para eleitor: " +
                    eleitorHash.substring(0, 8) + "...");
            return new ArrayList<>();
        }

        System.out.println("[TOKEN] Encontrados " + tokens.size() +
                " tokens para eleitor: " + eleitorHash.substring(0, 8) + "...");

        return new ArrayList<>(tokens); // Retorna cópia para segurança
    }

    /**
     * NOVO: Verifica se um token pertence a um eleitor específico
     *
     * @param tokenAnonimo Token a ser verificado
     * @param eleitorHash Hash do CPF do eleitor
     * @return true se o token pertence ao eleitor
     */
    public boolean tokenPertenceAoEleitor(String tokenAnonimo, String eleitorHash) {
        List<String> tokens = historicoTokensPorEleitor.get(eleitorHash);
        return tokens != null && tokens.contains(tokenAnonimo);
    }

    /**
     * Remove tokens expirados (limpeza periódica)
     * ATUALIZAÇÃO: Não remove do histórico, apenas dos tokens ativos
     */
    public void limparTokensExpirados() {
        long agora = System.currentTimeMillis();
        int removidos = 0;

        tokensAtivos.entrySet().removeIf(entry -> {
            TokenVotacao token = entry.getValue();
            if (!token.isValido() || agora >= token.getValidoAte()) {
                // Remove também do mapa por eleitor
                String chaveEleitor = criarChaveEleitor(token.getEleitorHash(), token.getEleicaoId());
                tokensPorEleitor.remove(chaveEleitor);

                // MAS NÃO remove do histórico!
                return true;
            }
            return false;
        });

        if (removidos > 0) {
            System.out.println("[TOKEN] Limpeza: " + removidos + " tokens expirados removidos");
        }
    }

    /**
     * NOVO: Estatísticas do gerenciador
     */
    public Map<String, Object> obterEstatisticas() {
        int totalTokensGerados = historicoTokensPorEleitor.values().stream()
                .mapToInt(List::size)
                .sum();

        return Map.of(
                "tokensAtivos", tokensAtivos.size(),
                "eleitoresComToken", tokensPorEleitor.size(),
                "totalTokensGerados", totalTokensGerados,
                "totalEleitores", historicoTokensPorEleitor.size()
        );
    }

    private String criarChaveEleitor(String eleitorHash, String eleicaoId) {
        return eleitorHash + ":" + eleicaoId;
    }
}