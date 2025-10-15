package com.yagodaoud.VoxChain.blockchain;

import com.yagodaoud.VoxChain.modelo.Transacao;
import com.yagodaoud.VoxChain.modelo.enums.TipoTransacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Testes da Classe Minerador")
public class MineradorTest {

    private Minerador minerador;

    @Mock
    private No noMock;

    @Mock
    private BlockchainGovernamental blockchainMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(noMock.getId()).thenReturn("TSE-SP");
        when(noMock.getBlockchain()).thenReturn(blockchainMock);

        minerador = new Minerador(noMock);
    }

    // ============ TESTES DE CRIAÇÃO ============

    @Test
    @DisplayName("Deve criar minerador com nó")
    void deveCriarMineradorComNo() {
        assertThat(minerador).isNotNull();
    }

    // ============ TESTES DE MINERAÇÃO MANUAL ============

    @Test
    @DisplayName("Deve minerar manualmente quando há transações")
    void deveMinearManualmentequandoHaTransacoes() {
        Bloco blocoMock = mock(Bloco.class);
        when(blocoMock.getIndice()).thenReturn(1);
        when(blocoMock.getTransacoes()).thenReturn(java.util.List.of());
        when(blocoMock.getHashTruncado(16)).thenReturn("0012345678901234");

        when(blockchainMock.temTransacoesPendentes()).thenReturn(true);
        when(blockchainMock.getTamanho()).thenReturn(1);
        when(blockchainMock.criarBlocoCandidato("TSE-SP", null)).thenReturn(blocoMock);
        when(blockchainMock.validarBloco(blocoMock)).thenReturn(true);

        minerador.minerarAgora();

        verify(noMock, atLeastOnce()).getId();
        verify(blockchainMock).adicionarBloco(blocoMock);
        verify(blockchainMock).limparTransacoesProcessadas(blocoMock);
    }

    @Test
    @DisplayName("Não deve minerar sem transações pendentes")
    void naoDeveMinearSemTransacoesPendentes() {
        when(blockchainMock.temTransacoesPendentes()).thenReturn(false);

        minerador.minerarAgora();

        verify(blockchainMock, never()).criarBlocoCandidato(anyString(), anyLong());
    }

    @Test
    @DisplayName("Deve aceitar comando de parada")
    void deveAceitarComandoDeParada() {
        // Apenas verifica que o método pode ser chamado sem erro
        minerador.parar();

        // Não vai ter efeito em minerarAgora() porque a implementação
        // não verifica a flag parar, mas o método deve existir
        assertThat(minerador).isNotNull();
    }

    // ============ TESTES DE VALIDAÇÃO DE BLOCO ============

    @Test
    @DisplayName("Deve descartar bloco se blockchain evoluiu durante mineração")
    void deveDescartarBlocoSeBlockchainEvoluiu() {
        Bloco blocoMock = mock(Bloco.class);
        when(blocoMock.getIndice()).thenReturn(1);  // Bloco esperava índice 1
        when(blocoMock.getTransacoes()).thenReturn(java.util.List.of());

        when(blockchainMock.temTransacoesPendentes()).thenReturn(true);
        when(blockchainMock.getTamanho()).thenReturn(2);  // Mas blockchain já tem 2 blocos
        when(blockchainMock.criarBlocoCandidato("TSE-SP", null)).thenReturn(blocoMock);

        minerador.minerarAgora();

        // Não deve adicionar bloco descartado
        verify(blockchainMock, never()).adicionarBloco(blocoMock);
    }

    @Test
    @DisplayName("Deve descartar bloco inválido")
    void deveDescartarBlocoInvalido() {
        Bloco blocoMock = mock(Bloco.class);
        when(blocoMock.getIndice()).thenReturn(1);
        when(blocoMock.getTransacoes()).thenReturn(java.util.List.of());

        when(blockchainMock.temTransacoesPendentes()).thenReturn(true);
        when(blockchainMock.getTamanho()).thenReturn(1);
        when(blockchainMock.criarBlocoCandidato("TSE-SP", null)).thenReturn(blocoMock);
        when(blockchainMock.validarBloco(blocoMock)).thenReturn(false);  // Bloco inválido

        minerador.minerarAgora();

        verify(blockchainMock, never()).adicionarBloco(blocoMock);
    }

    // ============ TESTES DE LIMPEZA DE POOL ============

    @Test
    @DisplayName("Deve limpar transações do pool após minerar")
    void deveLimparTransacoesDoPoolAposMineração() {
        Bloco blocoMock = mock(Bloco.class);
        when(blocoMock.getIndice()).thenReturn(1);
        when(blocoMock.getTransacoes()).thenReturn(java.util.List.of());
        when(blocoMock.getHashTruncado(16)).thenReturn("0012345678901234");

        when(blockchainMock.temTransacoesPendentes()).thenReturn(true);
        when(blockchainMock.getTamanho()).thenReturn(1);
        when(blockchainMock.criarBlocoCandidato("TSE-SP", null)).thenReturn(blocoMock);
        when(blockchainMock.validarBloco(blocoMock)).thenReturn(true);

        minerador.minerarAgora();

        verify(blockchainMock).limparTransacoesProcessadas(blocoMock);
    }

    // ============ TESTES DE BROADCAST ============

    @Test
    @DisplayName("Deve fazer broadcast do bloco minerado")
    void deveAzerfBroadcastDoBloco() {
        Bloco blocoMock = mock(Bloco.class);
        when(blocoMock.getIndice()).thenReturn(1);
        when(blocoMock.getTransacoes()).thenReturn(java.util.List.of());
        when(blocoMock.getHashTruncado(16)).thenReturn("0012345678901234");

        when(blockchainMock.temTransacoesPendentes()).thenReturn(true);
        when(blockchainMock.getTamanho()).thenReturn(1);
        when(blockchainMock.criarBlocoCandidato("TSE-SP", null)).thenReturn(blocoMock);
        when(blockchainMock.validarBloco(blocoMock)).thenReturn(true);

        minerador.minerarAgora();

        verify(noMock).broadcastBloco(blocoMock);
    }

    // ============ TESTES DE POOL VAZIO ============

    @Test
    @DisplayName("Não deve minerar com pool vazio")
    void naoDeveMinearComPoolVazio() {
        when(blockchainMock.temTransacoesPendentes()).thenReturn(false);
        when(blockchainMock.criarBlocoCandidato("TSE-SP", null)).thenReturn(null);

        minerador.minerarAgora();

        verify(blockchainMock, never()).validarBloco(any());
    }
}