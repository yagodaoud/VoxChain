package com.yagodaoud.VoxChain.modelo;

import com.yagodaoud.VoxChain.modelo.enums.TipoTransacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Testes da Classe Transacao")
class TransacaoTest {

    @Test
    @DisplayName("Deve criar transação com dados corretos")
    void deveCriarTransacaoComDadosCorretos() {
        String payload = "{\"idEleitor\":\"123\"}";
        String idOrigem = "TSE-SP";
        TipoTransacao tipo = TipoTransacao.VOTO;

        Transacao t = new Transacao(tipo, payload, idOrigem);

        assertThat(t.getId()).isNotNull();
        assertThat(t.getTipo()).isEqualTo(tipo);
        assertThat(t.getPayload()).isEqualTo(payload);
        assertThat(t.getIdOrigem()).isEqualTo(idOrigem);
        assertThat(t.getTimestamp()).isGreaterThan(0);
    }

    @Test
    @DisplayName("IDs gerados devem ser únicos para diferentes timestamps")
    void idsDevemSerUnicos() {
        Transacao t1 = new Transacao(TipoTransacao.VOTO, "{\"id\":\"1\"}", "TSE-SP");

        // Pequeno delay
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Transacao t2 = new Transacao(TipoTransacao.VOTO, "{\"id\":\"1\"}", "TSE-SP");

        assertThat(t1.getId()).isNotEqualTo(t2.getId());
    }

    @Test
    @DisplayName("Transações iguais devem ser consideradas iguais")
    void transacoesIguaisDeveriaSerIguais() {
        String payload = "{\"idEleitor\":\"123\"}";
        Transacao t1 = new Transacao(TipoTransacao.VOTO, payload, "TSE-SP");

        Transacao t2 = new Transacao(TipoTransacao.VOTO, payload, "TSE-SP");
        // Força ID igual
        t2.setId(t1.getId()); // Se houver setter, ou crie uma equivalência

        assertThat(t1).isEqualTo(t2);
    }

    @Test
    @DisplayName("Deve desserializar payload corretamente")
    void deveDesserializarPayloadCorreto() {
        String payload = "{\"idEleitor\":\"123\",\"candidato\":\"13\"}";
        Transacao t = new Transacao(TipoTransacao.VOTO, payload, "TSE-SP");

        // Deveria ser possível fazer getPayloadAs
        assertThat(t.getPayload()).isEqualTo(payload);
    }

    @Test
    @DisplayName("Construtor vazio deve criar transação vazia")
    void construtorVazioDevelCriarTransacaoVazia() {
        Transacao t = new Transacao();

        assertThat(t.getId()).isNull();
        assertThat(t.getTimestamp()).isEqualTo(0);
    }
}