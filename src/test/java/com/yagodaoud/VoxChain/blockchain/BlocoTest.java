package com.yagodaoud.VoxChain.blockchain;

import com.yagodaoud.VoxChain.modelo.Transacao;
import com.yagodaoud.VoxChain.modelo.enums.TipoTransacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Testes da Classe Bloco")
class BlocoTest {

    private Bloco bloco;
    private List<Transacao> transacoes;

    @BeforeEach
    void setUp() {
        transacoes = new ArrayList<>();
        Transacao t = new Transacao(TipoTransacao.VOTO, "{\"id\":\"1\"}", "TSE-SP");
        transacoes.add(t);

        bloco = new Bloco(1, transacoes, "hash_anterior_123", "TSE-SP", null);
    }

    @Test
    @DisplayName("Deve criar bloco com dados corretos")
    void deveCriarBlocoComDadosCorretos() {
        assertThat(bloco.getIndice()).isEqualTo(1);
        assertThat(bloco.getTransacoes()).hasSize(1);
        assertThat(bloco.getHashAnterior()).isEqualTo("hash_anterior_123");
        assertThat(bloco.getMineradoPor()).isEqualTo("TSE-SP");
    }

    @Test
    @DisplayName("Deve calcular hash do bloco")
    void deveCalcularHashDoBloco() {
        String hash = bloco.calcularHash();

        assertThat(hash).isNotNull();
        assertThat(hash).isNotEmpty();
        assertThat(hash.length()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Hash deve ser determinístico")
    void hashDeveSerDeterministico() {
        String hash1 = bloco.calcularHash();
        String hash2 = bloco.calcularHash();

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("Blocos diferentes devem ter hashes diferentes")
    void blocosDiferentesDevemTerHashesDiferentes() {
        Transacao t2 = new Transacao(TipoTransacao.VOTO, "{\"id\":\"2\"}", "TSE-MG");
        List<Transacao> transacoes2 = new ArrayList<>();
        transacoes2.add(t2);

        Bloco bloco2 = new Bloco(1, transacoes2, "hash_anterior_123", "TSE-SP", null);

        String hash1 = bloco.calcularHash();
        String hash2 = bloco2.calcularHash();

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("Deve minerar bloco com proof of work")
    void deveMinerarBlocoComProofOfWork() {
        int dificuldade = 2;
        bloco.minerarBloco(dificuldade);

        String hash = bloco.getHash();
        assertThat(hash).startsWith("00"); // 2 zeros para dificuldade 2
    }

    @Test
    @DisplayName("Nonce deve aumentar durante mineração")
    void nonceDevelAumentarDuranteMineracao() {
        int nonceAntes = bloco.getNonce();
        bloco.minerarBloco(2);
        int nonceDepois = bloco.getNonce();

        assertThat(nonceDepois).isGreaterThanOrEqualTo(nonceAntes);
    }

    @Test
    @DisplayName("Deve truncar hash para exibição")
    void deveTruncarHashParaExibicao() {
        bloco.minerarBloco(2);
        String hashTruncado = bloco.getHashTruncado(8);

        assertThat(hashTruncado).hasSize(8);
    }

    @Test
    @DisplayName("Deve incluir timestamp")
    void deveIncluirTimestamp() {
        assertThat(bloco.getTimestamp()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Bloco com mais transações deve ter hash diferente")
    void blocoComMaisTransacoesDeveTerHashDiferente() {
        Bloco bloco1 = new Bloco(1, transacoes, "hash_anterior", "TSE-SP", null);

        List<Transacao> transacoes2 = new ArrayList<>(transacoes);
        Transacao t2 = new Transacao(TipoTransacao.VOTO, "{\"id\":\"2\"}", "TSE-SP");
        transacoes2.add(t2);
        Bloco bloco2 = new Bloco(1, transacoes2, "hash_anterior", "TSE-SP", null);

        assertThat(bloco1.calcularHash()).isNotEqualTo(bloco2.calcularHash());
    }
}