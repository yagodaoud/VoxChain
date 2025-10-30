package com.yagodaoud.VoxChain.blockchain.servicos.eleicao;

import com.yagodaoud.VoxChain.blockchain.BlockchainGovernamental;
import com.yagodaoud.VoxChain.blockchain.servicos.ServicoAdministracao;
import com.yagodaoud.VoxChain.modelo.Candidato;
import com.yagodaoud.VoxChain.modelo.Eleicao;
import com.yagodaoud.VoxChain.modelo.Transacao;
import com.yagodaoud.VoxChain.modelo.Voto;
import com.yagodaoud.VoxChain.modelo.enums.CargoCandidato;
import com.yagodaoud.VoxChain.modelo.enums.CategoriaEleicao;
import com.yagodaoud.VoxChain.modelo.enums.TipoTransacao;

import java.util.List;

public class ServicoEleicao {

    private final BlockchainGovernamental blockchain;
    private final ServicoAdministracao servicoAdministracao;

    public ServicoEleicao(BlockchainGovernamental blockchain, ServicoAdministracao servicoAdministracao) {
        this.blockchain = blockchain;
        this.servicoAdministracao = servicoAdministracao;
    }

    public List<Eleicao> listarEleicoes() {
        return blockchain.listarEleicoes();
    }

    public Eleicao criarEleicao(String cpfHash, String nome, String descricao, List<CategoriaEleicao> categorias, long dataInicio, long dataFim) {
        if (!servicoAdministracao.temPermissao(cpfHash, TipoTransacao.CRIACAO_ELEICAO)) {
            throw new SecurityException(
                    "Admin " + cpfHash + " não tem permissão para criar eleições"
            );
        }

        if (dataFim <= dataInicio) {
            throw new IllegalArgumentException("Data de fim deve ser posterior à data de início.");
        }

        Eleicao novaEleicao = new Eleicao(nome, descricao, categorias, dataInicio, dataFim);
        Transacao transacao = new Transacao(TipoTransacao.CRIACAO_ELEICAO, novaEleicao, cpfHash);
        blockchain.adicionarAoPool(transacao);

        return novaEleicao;
    }

    public void finalizarEleicao(String solicitanteId, String eleicaoId) {
        Eleicao eleicao = blockchain.buscarEleicao(eleicaoId);
        if (eleicao == null) {
            throw new IllegalArgumentException("Eleição não encontrada.");
        }
        // Lógica para finalizar eleição (ex: mudar status)
        Transacao transacao = new Transacao(TipoTransacao.FIM_ELEICAO, eleicao, solicitanteId);
        blockchain.adicionarAoPool(transacao);
    }

    public List<Candidato> listarCandidatos() {
        return blockchain.listarCandidatos();
    }

    public List<Candidato> listarCandidatos(String eleicaoId) {
        return blockchain.listarCandidatos(eleicaoId);
    }

    public Candidato cadastrarCandidato(String solicitanteId, String eleicaoId, String numero, String nome, String partido, CargoCandidato cargo, String uf, String fotoUrl) {
        Eleicao eleicao = blockchain.buscarEleicao(eleicaoId);
        if (eleicao == null) {
            throw new IllegalArgumentException("Eleição não encontrada para cadastrar candidato.");
        }

        Candidato novoCandidato = new Candidato(eleicaoId, numero, nome, partido, cargo, uf, fotoUrl);
        Transacao transacao = new Transacao(TipoTransacao.CADASTRO_CANDIDATO, novoCandidato, solicitanteId);
        blockchain.adicionarAoPool(transacao);

        return novoCandidato;
    }

    public Voto registrarVoto(String eleitorHash, String numeroCandidato, String eleicaoId) {
        Eleicao eleicao = blockchain.buscarEleicao(eleicaoId);
        Candidato candidato = blockchain.buscarCandidato(numeroCandidato);

        if (eleicao == null || !eleicao.estaAberta()) {
            throw new IllegalStateException("A eleição não está aberta para votação.");
        }
        if (candidato == null) {
            throw new IllegalArgumentException("Candidato não encontrado.");
        }

        Voto voto = new Voto(eleitorHash, numeroCandidato, candidato.getCargo().toString(), eleicaoId);
        Transacao transacao = new Transacao(TipoTransacao.VOTO, voto, eleitorHash);
        blockchain.adicionarAoPool(transacao);

        return voto;
    }
}