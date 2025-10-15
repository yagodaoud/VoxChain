package com.yagodaoud.VoxChain;

import com.yagodaoud.VoxChain.blockchain.No;
import com.yagodaoud.VoxChain.modelo.Transacao;
import com.yagodaoud.VoxChain.modelo.Voto;
import com.yagodaoud.VoxChain.modelo.enums.TipoTransacao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Teste de Integração da Rede P2P e Blockchain")
public class RedeP2PIntegracaoTest {

    private No noSP;
    private No noRJ;
    private No noMG;

    // Constantes de atraso para simular o tempo de rede e mineração
    private final long DELAY_INICIAL = 4000;
    private final long DELAY_SINCRONIZACAO_MAX = 5000; // Tempo máximo para propagação na rede

    @BeforeEach
    void setUp() throws Exception {
        System.out.println("\n--- Configurando Nós de Teste ---");

        // 1. CRIAR 3 NÓS
        noSP = new No("TSE-SP", "localhost", 8001);
        noRJ = new No("TSE-RJ", "localhost", 8002);
        noMG = new No("TSE-MG", "localhost", 8003);

        // 2. INICIAR NÓS
        noSP.iniciar();
        noRJ.iniciar();
        noMG.iniciar();

        Thread.sleep(DELAY_INICIAL);
        System.out.println("Nós iniciados com sucesso.");

        // 3. CONECTAR NÓS EM REDE (Topologia: SP <-> RJ <-> MG)
        noSP.conectarPeer("localhost", 8002, "TSE-RJ");
        Thread.sleep(1000);
        noRJ.conectarPeer("localhost", 8003, "TSE-MG");
        Thread.sleep(DELAY_SINCRONIZACAO_MAX); // Espera a rede estabilizar e sincronizar a blockchain inicial

        System.out.println("Conexões P2P estabelecidas.");

        // 4. VALIDAÇÃO INICIAL: A blockchain deve ter 2 blocos (Gênesis + Super Admin)
        int tamanhoInicial = 2;
        assertThat(noSP.getBlockchain().getTamanho()).isEqualTo(tamanhoInicial);
        assertThat(noRJ.getBlockchain().getTamanho()).isEqualTo(tamanhoInicial);
        assertThat(noMG.getBlockchain().getTamanho()).isEqualTo(tamanhoInicial);
        System.out.println("Blockchain inicial sincronizada em " + tamanhoInicial + " blocos.");
    }

    @AfterEach
    void tearDown() {
        System.out.println("\n--- Encerrando Nós de Teste ---");
        noSP.parar();
        noRJ.parar();
        noMG.parar();
    }

    // ====================================================================
    // FUNÇÃO REATIVA DE ESPERA (AVOID FLAKINESS)
    // ====================================================================
    private void awaitPoolSize(int expectedSize) throws Exception {
        long startTime = System.currentTimeMillis();
        long maxWaitTime = 10000; // Tempo máximo de 10 segundos para a propagação

        while (System.currentTimeMillis() - startTime < maxWaitTime) {
            int spSize = noSP.getBlockchain().getPoolSize();
            int rjSize = noRJ.getBlockchain().getPoolSize();
            int mgSize = noMG.getBlockchain().getPoolSize();

            if (spSize == expectedSize && rjSize == expectedSize && mgSize == expectedSize) {
                System.out.println("✓ Condição de Pool Size (" + expectedSize + ") alcançada em " +
                        (System.currentTimeMillis() - startTime) + "ms.");
                return;
            }
            Thread.sleep(200); // Espera um pouco e tenta de novo
        }

        // Se o loop de espera for excedido, a asserção final falhará com a mensagem do JUnit
        System.out.println("✗ Timeout! Pool Size atual - SP:" + noSP.getBlockchain().getPoolSize() +
                " | RJ:" + noRJ.getBlockchain().getPoolSize() +
                " | MG:" + noMG.getBlockchain().getPoolSize());
    }

    // ====================================================================

    @Test
    void testeFluxoP2PCompleto() throws Exception {
        int tamanhoBlockchainAntes = noSP.getBlockchain().getTamanho();

        // ==================== A. ADICIONAR 1ª TRANSAÇÃO ====================
        System.out.println("\n=== A. ADICIONANDO 1ª TRANSAÇÃO (Nó SP) ===");

        // Transação 1: Voto em 13
        Voto voto1 = new Voto("ELEITOR001", "13", "Presidente", "ELEICAO2026");
        Transacao t1 = new Transacao(TipoTransacao.VOTO, voto1, "ELEITOR");
        noSP.adicionarTransacao(t1);

        // 5. VALIDAÇÃO 1: Pools devem ter 1 transação (espera ativa)
        awaitPoolSize(1);

        assertThat(noSP.getBlockchain().getPoolSize()).isEqualTo(1);
        assertThat(noRJ.getBlockchain().getPoolSize()).isEqualTo(1);
        assertThat(noMG.getBlockchain().getPoolSize()).isEqualTo(1);
        System.out.println("Pools Sincronizados com 1 transação.");


        // ==================== B. ADICIONAR MAIS 2 TRANSAÇÕES ====================
        System.out.println("\n=== B. ADICIONANDO MAIS 2 TRANSAÇÕES (Totalizando 3) ===");

        // Transação 2 (no RJ): Voto em 45
        Voto voto2 = new Voto("ELEITOR002", "45", "Presidente", "ELEICAO2026");
        Transacao t2 = new Transacao(TipoTransacao.VOTO, voto2, "ELEITOR");
        noRJ.adicionarTransacao(t2);

        Thread.sleep(500); // Pequeno atraso para a thread de broadcast iniciar

        // Transação 3 (no MG): Voto em 22
        Voto voto3 = new Voto("ELEITOR003", "22", "Presidente", "ELEICAO2026");
        Transacao t3 = new Transacao(TipoTransacao.VOTO, voto3, "ELEITOR");
        noMG.adicionarTransacao(t3);

        // 6. VALIDAÇÃO 2: Pools devem ter 3 transações
        awaitPoolSize(3);

        assertThat(noSP.getBlockchain().getPoolSize()).isEqualTo(3);
        assertThat(noRJ.getBlockchain().getPoolSize()).isEqualTo(3);
        assertThat(noMG.getBlockchain().getPoolSize()).isEqualTo(3);
        System.out.println("Pools Sincronizados com 3 transações.");


        // ==================== C. MINERAÇÃO E VALIDAÇÃO FINAL ====================
        System.out.println("\n=== C. FORÇANDO MINERAÇÃO MANUAL (Nó SP) ===");

        // Força mineração no Nó SP
        noSP.minerarManualmente();

        System.out.println("Aguardando mineração e propagação do Bloco 2...");
        // Tempo suficiente para a mineração (PoW + Broadcast)
        Thread.sleep(25000);

        // 7. VALIDAÇÃO FINAL: Blockchain sincronizada com 1 bloco novo e pools vazios
        int tamanhoFinal = tamanhoBlockchainAntes + 1;

        // Todos os nós devem ter o mesmo tamanho (um bloco a mais)
        assertThat(noSP.getBlockchain().getTamanho()).isEqualTo(tamanhoFinal);
        assertThat(noRJ.getBlockchain().getTamanho()).isEqualTo(tamanhoFinal);
        assertThat(noMG.getBlockchain().getTamanho()).isEqualTo(tamanhoFinal);

        // Os pools devem estar vazios
        assertThat(noSP.getBlockchain().getPoolSize()).isEqualTo(0);
        assertThat(noRJ.getBlockchain().getPoolSize()).isEqualTo(0);
        assertThat(noMG.getBlockchain().getPoolSize()).isEqualTo(0);

        // O hash do último bloco deve ser o mesmo em todos os nós (prova de sincronização)
        String hashUltimoBlocoSP = noSP.getBlockchain().obterUltimoBloco().getHash();
        String hashUltimoBlocoRJ = noRJ.getBlockchain().obterUltimoBloco().getHash();
        String hashUltimoBlocoMG = noMG.getBlockchain().obterUltimoBloco().getHash();

        assertThat(hashUltimoBlocoRJ).isEqualTo(hashUltimoBlocoSP);
        assertThat(hashUltimoBlocoMG).isEqualTo(hashUltimoBlocoSP);

        System.out.println("\n✓ Teste de Fluxo P2P Concluído com Sucesso.");
    }
}