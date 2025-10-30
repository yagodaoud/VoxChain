package com.yagodaoud.VoxChain.blockchain;

import com.yagodaoud.VoxChain.blockchain.core.*;
import com.yagodaoud.VoxChain.blockchain.indices.EntityIndexManager;
import com.yagodaoud.VoxChain.blockchain.indices.VoteRegistry;
import com.yagodaoud.VoxChain.blockchain.sync.ChainSynchronizer;
import com.yagodaoud.VoxChain.blockchain.sync.ConflictResolver;
import com.yagodaoud.VoxChain.modelo.*;

import java.io.Serializable;
import java.util.List;

/**
 * Classe principal da Blockchain Governamental.
 * Orquestra os componentes: Chain, TransactionPool, Validator, Índices e Votos.
 *
 * Responsabilidades:
 * - Coordenar operações entre componentes
 * - Expor API simplificada para o restante do sistema
 * - Manter configurações globais (dificuldade, limites)
 */
public class BlockchainGovernamental implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Chain chain;
    private final TransactionPool pool;
    private final BlockValidator validator;
    private final EntityIndexManager indices;
    private final VoteRegistry votos;
    private final ChainSynchronizer synchronizer;
    private final ConflictResolver conflictResolver;
    private boolean modoTeste = false;

    public BlockchainGovernamental() {
        this(2, 5);
    }

    public BlockchainGovernamental(int dificuldade, int transacoesMaximasPorBloco) {
        this.chain = new Chain(dificuldade);
        this.pool = new TransactionPool(transacoesMaximasPorBloco);
        this.validator = new BlockValidator(dificuldade);
        this.indices = new EntityIndexManager();
        this.synchronizer = new ChainSynchronizer(chain, validator);
        this.votos = new VoteRegistry();
        this.conflictResolver = new ConflictResolver(validator);
    }

    // ========== OPERAÇÕES DE TRANSAÇÕES ==========

    public synchronized boolean adicionarAoPool(Transacao transacao) {
        if (!pool.adicionar(transacao)) {
            return false;
        }

        if (modoTeste && pool.temTransacoes()) {
            minerarImediato();
        }

        return true;
    }

    public synchronized boolean transacaoExiste(Transacao transacao) {
        if (transacao == null || transacao.getId() == null) {
            return false;
        }

        // Verifica no pool
        if (pool.existe(transacao.getId())) {
            return true;
        }

        // Verifica na blockchain
        return chain.obterTodosBlocos().stream()
                .flatMap(b -> b.getTransacoes().stream())
                .anyMatch(t -> t != null && transacao.getId().equals(t.getId()));
    }

    public int getPoolSize() {
        return pool.getTamanho();
    }

    public boolean temTransacoesPendentes() {
        return pool.temTransacoes();
    }

    // ========== OPERAÇÕES DE BLOCOS ==========

    public synchronized Bloco criarBlocoCandidato(String mineradoPor, Long timestampFixo) {
        List<Transacao> transacoes = pool.obterParaBloco();

        if (transacoes.isEmpty()) {
            return null;
        }

        return chain.criarBlocoCandidato(transacoes, mineradoPor, timestampFixo);
    }

    public synchronized void adicionarBloco(Bloco bloco) {
        chain.adicionarBloco(bloco);
        indices.atualizarComBloco(bloco);
    }

    public synchronized void limparTransacoesProcessadas(Bloco bloco) {
        pool.marcarComoProcessadas(bloco.getTransacoes());
    }

    public synchronized void minerarImediato() {
        Bloco bloco = criarBlocoCandidato("TEST-NODE", null);
        if (bloco != null) {
            bloco.minerarBloco(getDificuldade());

            if (validarBloco(bloco)) {
                adicionarBloco(bloco);
                limparTransacoesProcessadas(bloco);
            }
        }
    }

    // ========== VALIDAÇÃO ==========

    public synchronized boolean validarBloco(Bloco bloco) {
        Bloco ultimoBloco = chain.obterUltimoBloco();
        BlockValidator.ValidationResult resultado = validator.validarBloco(bloco, ultimoBloco);

        if (!resultado.isValido()) {
            System.err.println("[VALIDAÇÃO] " + resultado.getMensagem());
        }

        return resultado.isValido();
    }

    public synchronized boolean validarCadeia() {
        BlockValidator.ValidationResult resultado = validator.validarCadeia(chain.obterTodosBlocos());

        if (!resultado.isValido()) {
            System.err.println("[VALIDAÇÃO] " + resultado.getMensagem());
        }

        return resultado.isValido();
    }

    // ========== SINCRONIZAÇÃO ==========

    public synchronized void substituir(List<Bloco> cadeiaRemota) {
        ChainSynchronizer.SyncResult resultado = synchronizer.sincronizar(cadeiaRemota);

        System.out.println("[SYNC] " + resultado);

        if (resultado.isSucesso()) {
            pool.limpar();
            pool.reconstruirHistorico(chain.obterTodosBlocos());
            indices.reconstruirIndices(chain.obterTodosBlocos());
        }
    }

    // ========== CONSULTAS ==========

    public List<Bloco> getBlocos() {
        return chain.obterTodosBlocos();
    }

    public Bloco getBloco(int indice) {
        return chain.obterBloco(indice);
    }

    public Bloco obterUltimoBloco() {
        return chain.obterUltimoBloco();
    }

    public int getTamanho() {
        return chain.getTamanho();
    }

    public int getDificuldade() {
        return chain.getDificuldade();
    }

    public int getTotalTransacoes() {
        return chain.getTotalTransacoes();
    }

    // ========== CONSULTAS DE ENTIDADES ==========

    public Administrador buscarAdmin(String id) {
        return indices.buscarAdmin(id);
    }

    public Administrador buscarAdminPorCpfHash(String cpf) {
        return indices.buscarAdminPorCpfHash(cpf);
    }

    public Eleitor buscarEleitor(String cpfHash) {
        return indices.buscarEleitor(cpfHash);
    }

    public Candidato buscarCandidato(String numero) {
        return indices.buscarCandidato(numero);
    }

    public Eleicao buscarEleicao(String id) {
        return indices.buscarEleicao(id);
    }

    public List<Administrador> listarAdmins() {
        return indices.listarAdmins();
    }

    public List<Eleitor> listarEleitores() {
        return indices.listarEleitores();
    }

    public List<Candidato> listarCandidatos() {
        return indices.listarCandidatos();
    }

    public List<Candidato> listarCandidatos(String eleicaoId) {
        return indices.listarCandidatos(eleicaoId);
    }

    public List<Eleicao> listarEleicoes() {
        return indices.listarEleicoes();
    }

    public Voto buscarVotoPorHash(String hash) {
        return chain.obterTodosBlocos().stream()
                .flatMap(b -> b.getTransacoes().stream())
                .filter(t -> t.getTipo() == com.yagodaoud.VoxChain.modelo.enums.TipoTransacao.VOTO)
                .map(t -> t.getPayloadAs(Voto.class))
                .filter(v -> v != null && v.getIdEleitorHash().equals(hash))
                .findFirst()
                .orElse(null);
    }

    // ========== CONFIGURAÇÕES ==========

    public void setModoTeste(boolean modoTeste) {
        this.modoTeste = modoTeste;
    }

    // ========== STATUS ==========

    public String getStatus() {
        return String.format(
                "Tamanho: %d | Pool: %d | Total Transações: %d | Entidades: %d admins, %d eleitores, %d candidatos, %d eleições",
                getTamanho(),
                getPoolSize(),
                getTotalTransacoes(),
                indices.getTotalAdmins(),
                indices.getTotalEleitores(),
                indices.getTotalCandidatos(),
                indices.getTotalEleicoes()
        );
    }

    @Override
    public String toString() {
        return String.format(
                "BlockchainGovernamental{tamanho=%d, pool=%d, dificuldade=%d}",
                getTamanho(),
                getPoolSize(),
                getDificuldade()
        );
    }
}