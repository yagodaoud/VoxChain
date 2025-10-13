package com.yagodaoud.VoxChain.blockchain;

import com.yagodaoud.VoxChain.modelo.Transacao;
import com.yagodaoud.VoxChain.modelo.enums.TipoTransacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.yagodaoud.VoxChain.rede.Peer;
import com.yagodaoud.VoxChain.rede.PeerDiscovery;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Testes da Classe No (Nó da Rede)")
class NoTest {

    private No no;
    private static final String ID_NO = "TSE-SP";
    private static final int PORTA_NO = 9001;

    @BeforeEach
    void setUp() {
        no = new No(ID_NO, "localhost", PORTA_NO);
    }

    // ============ TESTES DE CRIAÇÃO ============

    @Test
    @DisplayName("Deve criar nó com dados corretos")
    void deveCriarNoComDadosCorretos() {
        assertThat(no.getId()).isEqualTo(ID_NO);
        assertThat(no.getBlockchain()).isNotNull();
        assertThat(no.getNumPeers()).isEqualTo(0);
    }

    @Test
    @DisplayName("Nó começa sem peers conectados")
    void noComecaSemPeersConectados() {
        assertThat(no.getNumPeers()).isEqualTo(0);
        assertThat(no.getPeers()).isEmpty();
    }

    @Test
    @DisplayName("Nó deve ter blockchain inicializada")
    void noDeveTermBlockchainInicializada() {
        BlockchainGovernamental blockchain = no.getBlockchain();

        assertThat(blockchain).isNotNull();
        assertThat(blockchain.getTamanho()).isEqualTo(1);  // Apenas bloco gênesis
    }

    // ============ TESTES DE ADIÇÃO DE TRANSAÇÕES ============

    @Test
    @DisplayName("Deve adicionar transação ao pool do nó")
    void deveAdicionarTransacaoAoPool() {
        Transacao t = new Transacao(TipoTransacao.VOTO,
                "{\"idEleitor\":\"123\"}", ID_NO);

        no.adicionarTransacao(t);

        assertThat(no.getBlockchain().getPoolSize()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Não deve adicionar transação duplicada")
    void naoDeveAdicionarTransacaoDuplicada() {
        Transacao t = new Transacao(TipoTransacao.VOTO,
                "{\"idEleitor\":\"123\"}", ID_NO);

        no.adicionarTransacao(t);
        int poolAntes = no.getBlockchain().getPoolSize();

        no.adicionarTransacao(t);
        int poolDepois = no.getBlockchain().getPoolSize();

        assertThat(poolDepois).isEqualTo(poolAntes);
    }

    @Test
    @DisplayName("Deve rejeitar transação nula")
    void deveRejeitarTransacaoNula() {
        no.adicionarTransacao(null);

        assertThat(no.getBlockchain().getPoolSize()).isEqualTo(0);
    }

    // ============ TESTES DE STATUS ============

    @Test
    @DisplayName("Deve retornar status do nó")
    void deveRetornarStatusDoNo() {
        String status = no.getStatus();

        assertThat(status).contains(ID_NO);
        assertThat(status).contains("Blockchain");
        assertThat(status).contains("Pool");
    }

    @Test
    @DisplayName("Status deve conter número de blocos")
    void statusDeveConterNumeroDesBlocos() {
        String status = no.getStatus();

        assertThat(status).contains("1 blocos");  // Bloco gênesis
    }

    @Test
    @DisplayName("Status deve conter número de peers")
    void statusDeveConterNumeroDePeers() {
        String status = no.getStatus();

        assertThat(status).contains("Peers: 0");
    }

    // ============ TESTES DE BLOCKCHAIN ============

    @Test
    @DisplayName("Deve ter bloco gênesis")
    void deveTermBlocoGenesis() {
        BlockchainGovernamental blockchain = no.getBlockchain();

        assertThat(blockchain.getTamanho()).isGreaterThanOrEqualTo(1);
        assertThat(blockchain.obterUltimoBloco().getIndice()).isEqualTo(0);
    }

    @Test
    @DisplayName("Deve processar bloco válido")
    void deveProcessarBlocoValido() {
        // Cria e minera um bloco
        Transacao t = new Transacao(TipoTransacao.VOTO,
                "{\"idEleitor\":\"123\"}", ID_NO);
        no.adicionarTransacao(t);

        BlockchainGovernamental blockchain = no.getBlockchain();
        Bloco bloco = blockchain.criarBlocoCandidato(ID_NO);
        bloco.minerarBloco(2);

        int tamanhoAntes = blockchain.getTamanho();

        no.processarNovoBloco(bloco, "PEER-TESTE");

        int tamanhoDepois = blockchain.getTamanho();
        assertThat(tamanhoDepois).isGreaterThan(tamanhoAntes);
    }

    @Test
    @DisplayName("Não deve processar bloco nulo")
    void naoDeveProcessarBlocoNulo() {
        int tamanhoAntes = no.getBlockchain().getTamanho();

        no.processarNovoBloco(null, "PEER-TESTE");

        int tamanhoDepois = no.getBlockchain().getTamanho();
        assertThat(tamanhoDepois).isEqualTo(tamanhoAntes);
    }

    @Test
    @DisplayName("Deve descartar bloco antigo (fork)")
    void deveDescartarBlocoAntigo() {
        // Cria bloco com índice 0 (já existe)
        Bloco blocoAntigo = new Bloco(0, java.util.List.of(), "0", "OUTRO-NO");
        blocoAntigo.minerarBloco(2);

        int tamanhoAntes = no.getBlockchain().getTamanho();

        no.processarNovoBloco(blocoAntigo, "PEER-TESTE");

        int tamanhoDepois = no.getBlockchain().getTamanho();
        assertThat(tamanhoDepois).isEqualTo(tamanhoAntes);  // Não mudou
    }

    // ============ TESTES DE SINCRONIZAÇÃO ============

    @Test
    @DisplayName("Deve sincronizar com blockchain mais longa")
    void deveSincronizarComBlockchainMaisLonga() {
        // Cria blockchain remota mais longa
        List<Bloco> blocosRemotos = no.getBlockchain().getBlocos();

        // Adiciona um bloco artificial
        Bloco novoBloco = new Bloco(1, java.util.List.of(),
                blocosRemotos.get(0).getHash(), "OUTRO-NO");
        novoBloco.minerarBloco(2);
        blocosRemotos.add(novoBloco);

        int tamanhoAntes = no.getBlockchain().getTamanho();

        no.sincronizarBlockchain(blocosRemotos);

        int tamanhoDepois = no.getBlockchain().getTamanho();
        assertThat(tamanhoDepois).isGreaterThan(tamanhoAntes);
    }

    @Test
    @DisplayName("Não deve sincronizar com blockchain mais curta")
    void naoDeveSincronizarComBlockchainMaisCurta() {
        int tamanhoAntes = no.getBlockchain().getTamanho();

        List<Bloco> blocosRemotos = java.util.List.of();  // Vazio
        no.sincronizarBlockchain(blocosRemotos);

        int tamanhoDepois = no.getBlockchain().getTamanho();
        assertThat(tamanhoDepois).isEqualTo(tamanhoAntes);
    }

    // ============ TESTES DE CATÁLOGO DE PEERS ============

    @Test
    @DisplayName("Deve obter catálogo de peers")
    void deveObterCatalogoDePeers() {
        List<PeerDiscovery.PeerInfo> catalogo = no.obterCatalogoPeers();

        assertThat(catalogo).isNotNull();
    }

    @Test
    @DisplayName("Deve atualizar catálogo de peers")
    void deveAtualizarCatalogoDePeers() {
        PeerDiscovery.PeerInfo peerInfo = new PeerDiscovery.PeerInfo("TSE-MG", "localhost", 8002);
        List<PeerDiscovery.PeerInfo> novosCatalogo = java.util.List.of(peerInfo);

        no.atualizarCatalogoPeers(novosCatalogo);

        List<PeerDiscovery.PeerInfo> catalogoAtualizado = no.obterCatalogoPeers();
        assertThat(catalogoAtualizado).contains(peerInfo);
    }

    // ============ TESTES DE PEERS ============

    @Test
    @DisplayName("Deve retornar lista de peers")
    void deveRetornarListaDePeers() {
        List<Peer> peers = no.getPeers();

        assertThat(peers).isNotNull();
        assertThat(peers).isEmpty();  // Começa sem peers
    }

    @Test
    @DisplayName("Número de peers deve ser consistente")
    void numeroDePeersDevelSerConsistente() {
        int numPeers1 = no.getNumPeers();
        int numPeers2 = no.getNumPeers();

        assertThat(numPeers1).isEqualTo(numPeers2);
    }

    // ============ TESTES DE GETTERS ============

    @Test
    @DisplayName("Deve retornar ID correto")
    void deveRetornarIDCorreto() {
        assertThat(no.getId()).isEqualTo(ID_NO);
    }

    @Test
    @DisplayName("Deve retornar blockchain não nula")
    void deveRetornarBlockchainNaoNula() {
        assertThat(no.getBlockchain()).isNotNull();
    }
}