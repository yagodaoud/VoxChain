package com.yagodaoud.VoxChain.blockchain.servicos.eleicao;

import com.yagodaoud.VoxChain.blockchain.BlockchainGovernamental;
import com.yagodaoud.VoxChain.blockchain.servicos.GerenciadorTokenVotacao;
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
    private final GerenciadorTokenVotacao gerenciadorToken;

    public ServicoEleicao(BlockchainGovernamental blockchain, ServicoAdministracao servicoAdministracao,
            GerenciadorTokenVotacao gerenciadorToken) {
        this.blockchain = blockchain;
        this.servicoAdministracao = servicoAdministracao;
        this.gerenciadorToken = gerenciadorToken;
    }

    public List<Eleicao> listarEleicoes() {
        return blockchain.listarEleicoes();
    }

    public Eleicao criarEleicao(String cpfHash, String nome, String descricao, List<CategoriaEleicao> categorias,
            long dataInicio, long dataFim) {
        if (!servicoAdministracao.temPermissao(cpfHash, TipoTransacao.CRIACAO_ELEICAO)) {
            throw new SecurityException(
                    "Admin " + cpfHash + " não tem permissão para criar eleições");
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

    public Candidato cadastrarCandidato(String solicitanteId, String eleicaoId, String numero, String nome,
            String partido, CargoCandidato cargo, String uf, String fotoUrl) {
        Eleicao eleicao = blockchain.buscarEleicao(eleicaoId);
        if (eleicao == null) {
            throw new IllegalArgumentException("Eleição não encontrada para cadastrar candidato.");
        }

        Candidato novoCandidato = new Candidato(eleicaoId, numero, nome, partido, cargo, uf, fotoUrl);
        Transacao transacao = new Transacao(TipoTransacao.CADASTRO_CANDIDATO, novoCandidato, solicitanteId);
        blockchain.adicionarAoPool(transacao);

        return novoCandidato;
    }

    public Voto registrarVoto(String tokenVotacao, String numeroCandidato, String eleicaoId) {
        // 1. Validar token usando GerenciadorTokenVotacao
        if (!gerenciadorToken.validarToken(tokenVotacao, eleicaoId)) {
            throw new IllegalStateException("Token de votação inválido ou expirado");
        }

        // 2. Verificar se eleição está aberta e validações temporais
        Eleicao eleicao = blockchain.buscarEleicao(eleicaoId);
        if (eleicao == null) {
            throw new IllegalArgumentException("Eleição não encontrada.");
        }

        long agora = System.currentTimeMillis();
        if (agora < eleicao.getDataInicio()) {
            throw new IllegalStateException("Eleição ainda não iniciou");
        }
        if (agora > eleicao.getDataFim()) {
            throw new IllegalStateException("Eleição já encerrou");
        }

        // 3. Verificar se candidato existe
        Candidato candidato = blockchain.buscarCandidato(numeroCandidato);
        if (candidato == null) {
            throw new IllegalArgumentException("Candidato não encontrado.");
        }

        // 4. Criar Voto usando tokenVotacao (não eleitorHash)
        Voto voto = new Voto(tokenVotacao, numeroCandidato, candidato.getCargo().toString(), eleicaoId);

        // 5. Marcar token como usado
        gerenciadorToken.marcarTokenComoUsado(tokenVotacao);

        // 6. Adicionar transação ao pool (idOrigem será 'ANONIMO' automaticamente)
        Transacao transacao = new Transacao(TipoTransacao.VOTO, voto, "ANONIMO");
        blockchain.adicionarAoPool(transacao);

        // 7. Retornar voto
        return voto;
    }
}