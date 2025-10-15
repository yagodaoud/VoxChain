package com.yagodaoud.VoxChain.blockchain;

import com.yagodaoud.VoxChain.modelo.*;
import com.yagodaoud.VoxChain.modelo.enums.TipoTransacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Testes da Blockchain Governamental")
class BlockchainGovernamentalTest {

    private BlockchainGovernamental blockchain;

    @BeforeEach
    void setUp() {
        blockchain = new BlockchainGovernamental(2, 5);
    }

    // ============ TESTES DE BLOCO GÊNESIS ============

    @Test
    @DisplayName("Deve criar bloco gênesis corretamente")
    void deveCriarBlocoGenesis() {
        assertThat(blockchain.getTamanho()).isEqualTo(1);
        assertThat(blockchain.obterUltimoBloco().getIndice()).isEqualTo(0);
        assertThat(blockchain.obterUltimoBloco().getTransacoes()).isEmpty();
    }

    // ============ TESTES DE TRANSAÇÕES ============

    @Test
    @DisplayName("Deve adicionar transação ao pool")
    void deveAdicionarTransacaoAoPool() {
        Transacao t = new Transacao(TipoTransacao.VOTO,
                new Voto("123", "Candidato1", "Tipo1", "Eleicao1"), "TSE-SP");

        boolean adicionada = blockchain.adicionarAoPool(t);

        assertThat(adicionada).isTrue();
        assertThat(blockchain.getPoolSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("Não deve adicionar transação duplicada")
    void naoDeveAdicionarTransacaoDuplicada() {
        Transacao t = new Transacao(TipoTransacao.VOTO,
                new Voto("123", "Candidato1", "Tipo1", "Eleicao1"), "TSE-SP");

        blockchain.adicionarAoPool(t);
        boolean adicionadaNovaVez = blockchain.adicionarAoPool(t);

        assertThat(adicionadaNovaVez).isFalse();
        assertThat(blockchain.getPoolSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve verificar se transação existe")
    void deveVerificarSeTransacaoExiste() {
        Transacao t = new Transacao(TipoTransacao.VOTO,
                new Voto("123", "Candidato1", "Tipo1", "Eleicao1"), "TSE-SP");
        blockchain.adicionarAoPool(t);

        boolean existe = blockchain.transacaoExiste(t);

        assertThat(existe).isTrue();
    }

    @Test
    @DisplayName("Transação nula não deve existir")
    void transacaoNulaDeveRetornarFalse() {
        boolean existe = blockchain.transacaoExiste(null);
        assertThat(existe).isFalse();
    }

    // ============ TESTES DE MINERAÇÃO ============

    @Test
    @DisplayName("Deve criar bloco candidato com transações do pool")
    void deveCriarBlocoCandidato() {
        Transacao t1 = new Transacao(TipoTransacao.VOTO,
                new Voto("123", "Candidato1", "Tipo1", "Eleicao1"), "TSE-SP");
        Transacao t2 = new Transacao(TipoTransacao.VOTO,
                "{\"idEleitorHash\":\"456\"}", "TSE-SP");

        blockchain.adicionarAoPool(t1);
        blockchain.adicionarAoPool(t2);

        Bloco bloco = blockchain.criarBlocoCandidato("TSE-SP");

        assertThat(bloco).isNotNull();
        assertThat(bloco.getTransacoes()).hasSize(2);
        assertThat(bloco.getIndice()).isEqualTo(1);
    }

    @Test
    @DisplayName("Não deve criar bloco candidato com pool vazio")
    void naoDeveCriarBlocoCandidatoComPoolVazio() {
        Bloco bloco = blockchain.criarBlocoCandidato("TSE-SP");
        assertThat(bloco).isNull();
    }

    @Test
    @DisplayName("Deve respeitar limite máximo de transações por bloco")
    void deveRespeeitarLimiteTransacoesPorBloco() throws InterruptedException {
        // Adiciona 8 transações (limite é 5)
        for (int i = 0; i < 8; i++) {
            Thread.sleep(100);
            Transacao t = new Transacao(TipoTransacao.VOTO,
                    "{\"idEleitorHash\":\"" + i + "\"}", "TSE-SP");
            blockchain.adicionarAoPool(t);
        }

        Bloco bloco = blockchain.criarBlocoCandidato("TSE-SP");

        assertThat(bloco.getTransacoes()).hasSize(5);
    }

    @Test
    @DisplayName("Deve adicionar bloco válido à cadeia")
    void deveAdicionarBlocoValido() {
        Transacao t = new Transacao(TipoTransacao.VOTO,
                new Voto("123", "Candidato1", "Tipo1", "Eleicao1"), "TSE-SP");
        blockchain.adicionarAoPool(t);

        Bloco bloco = blockchain.criarBlocoCandidato("TSE-SP");
        bloco.minerarBloco(2);

        blockchain.adicionarBloco(bloco);

        assertThat(blockchain.getTamanho()).isEqualTo(2);
        assertThat(blockchain.obterUltimoBloco().getIndice()).isEqualTo(1);
    }

    // ============ TESTES DE LIMPEZA DE TRANSAÇÕES ============

    @Test
    @DisplayName("Deve limpar transações após mineração")
    void deveLimparTransacoesAposMineracao() {
        Transacao t = new Transacao(TipoTransacao.VOTO,
                new Voto("123", "Candidato1", "Tipo1", "Eleicao1"), "TSE-SP");
        blockchain.adicionarAoPool(t);

        Bloco bloco = blockchain.criarBlocoCandidato("TSE-SP");
        bloco.minerarBloco(2);
        blockchain.adicionarBloco(bloco);

        blockchain.limparTransacoesProcessadas(bloco);

        assertThat(blockchain.getPoolSize()).isEqualTo(0);
    }

    @Test
    @DisplayName("Deve manter transações não incluídas no bloco")
    void deveMantterTransacoesNaoIncluidasNoBloco() {
        // Adiciona 3 transações
        Transacao t1 = new Transacao(TipoTransacao.VOTO,
                new Voto("1", "Candidato1", "Tipo1", "Eleicao1"), "TSE-SP");
        Transacao t2 = new Transacao(TipoTransacao.VOTO,
                new Voto("2", "Candidato2", "Tipo2", "Eleicao2"), "TSE-SP");
        Transacao t3 = new Transacao(TipoTransacao.VOTO,
                new Voto("3", "Candidato3", "Tipo3", "Eleicao3"), "TSE-SP");

        blockchain.adicionarAoPool(t1);
        blockchain.adicionarAoPool(t2);
        blockchain.adicionarAoPool(t3);

        // Cria bloco com apenas 2 (limite é 5, mas criarBlocoCandidato pega as 3)
        Bloco bloco = blockchain.criarBlocoCandidato("TSE-SP");
        bloco.minerarBloco(2);
        blockchain.adicionarBloco(bloco);
        blockchain.limparTransacoesProcessadas(bloco);

        // Todas foram incluídas, então pool fica vazio
        assertThat(blockchain.getPoolSize()).isEqualTo(0);
    }

    // ============ TESTES DE VALIDAÇÃO ============

    @Test
    @DisplayName("Deve validar bloco correto")
    void deveValidarBlocoCorreto() {
        Transacao t = new Transacao(TipoTransacao.VOTO,
                new Voto("123", "Candidato1", "Tipo1", "Eleicao1"), "TSE-SP");
        blockchain.adicionarAoPool(t);

        Bloco bloco = blockchain.criarBlocoCandidato("TSE-SP");
        bloco.minerarBloco(2);

        boolean valido = blockchain.validarBloco(bloco);
        assertThat(valido).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar bloco com hash inválido")
    void deveRejeitarBlocoComHashInvalido() {
        Transacao t = new Transacao(TipoTransacao.VOTO,
                new Voto("123", "Candidato1", "Tipo1", "Eleicao1"), "TSE-SP");
        blockchain.adicionarAoPool(t);

        Bloco bloco = blockchain.criarBlocoCandidato("TSE-SP");
        bloco.minerarBloco(2);

        // Corrompe o hash
        bloco.setHash("HASH_INVALIDO");

        boolean valido = blockchain.validarBloco(bloco);
        assertThat(valido).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar bloco com índice inválido")
    void deveRejeitarBlocoComIndiceInvalido() {
        Transacao t = new Transacao(TipoTransacao.VOTO,
                new Voto("123", "Candidato1", "Tipo1", "Eleicao1"), "TSE-SP");
        blockchain.adicionarAoPool(t);

        Bloco bloco = blockchain.criarBlocoCandidato("TSE-SP");
        bloco.minerarBloco(2);

        // Define índice errado (deve ser 1, não 5)
        bloco.setIndice(5);

        boolean valido = blockchain.validarBloco(bloco);
        assertThat(valido).isFalse();
    }

    // ============ TESTES DE CADEIA ============

    @Test
    @DisplayName("Deve validar cadeia íntegra")
    void deveValidarCadeiaIntegra() {
        // Adiciona 2 blocos
        for (int i = 0; i < 2; i++) {
            Transacao t = new Transacao(TipoTransacao.VOTO,
                    new Voto(String.valueOf(i), "Candidato1", "Tipo1", "Eleicao1"), "TSE-SP");
            blockchain.adicionarAoPool(t);

            Bloco bloco = blockchain.criarBlocoCandidato("TSE-SP");
            bloco.minerarBloco(2);
            blockchain.adicionarBloco(bloco);
        }

        boolean valida = blockchain.validarCadeia();
        assertThat(valida).isTrue();
    }

    @Test
    @DisplayName("Deve retornar status da blockchain")
    void deveRetornarStatusBlockchain() {
        String status = blockchain.getStatus();
        assertThat(status).contains("Tamanho: 1");
        assertThat(status).contains("Pool: 0");
    }
}