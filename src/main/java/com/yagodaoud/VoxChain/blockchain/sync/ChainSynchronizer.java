package com.yagodaoud.VoxChain.blockchain.sync;

import com.yagodaoud.VoxChain.blockchain.Bloco;
import com.yagodaoud.VoxChain.blockchain.core.BlockValidator;
import com.yagodaoud.VoxChain.blockchain.core.Chain;

import java.util.List;

/**
 * Responsável pela sincronização de blockchains entre nós.
 * Implementa a regra da cadeia mais longa.
 */
public class ChainSynchronizer {
    private final Chain cadeiaLocal;
    private final BlockValidator validator;

    public ChainSynchronizer(Chain cadeiaLocal, BlockValidator validator) {
        this.cadeiaLocal = cadeiaLocal;
        this.validator = validator;
    }

    public SyncResult sincronizar(List<Bloco> cadeiaRemota) {
        if (cadeiaRemota == null || cadeiaRemota.isEmpty()) {
            return SyncResult.falha("Cadeia remota inválida (nula ou vazia)");
        }

        // Valida a cadeia remota
        BlockValidator.ValidationResult validacao = validator.validarCadeia(cadeiaRemota);
        if (!validacao.isValido()) {
            return SyncResult.falha("Cadeia remota inválida: " + validacao.getMensagem());
        }

        int tamanhoLocal = cadeiaLocal.getTamanho();
        int tamanhoRemoto = cadeiaRemota.size();

        // Regra da cadeia mais longa
        if (tamanhoRemoto <= tamanhoLocal) {
            return SyncResult.naoNecessario(
                    String.format("Cadeia local (%d) >= remota (%d)", tamanhoLocal, tamanhoRemoto)
            );
        }

        // Substitui a cadeia
        cadeiaLocal.substituirCadeia(cadeiaRemota);

        return SyncResult.sucesso(
                String.format("Cadeia substituída: %d -> %d blocos", tamanhoLocal, tamanhoRemoto)
        );
    }

    public boolean deveSincronizar(int tamanhoRemoto) {
        return tamanhoRemoto > cadeiaLocal.getTamanho();
    }

    /**
     * Resultado da sincronização
     */
    public static class SyncResult {
        private final SyncStatus status;
        private final String mensagem;

        private SyncResult(SyncStatus status, String mensagem) {
            this.status = status;
            this.mensagem = mensagem;
        }

        public static SyncResult sucesso(String mensagem) {
            return new SyncResult(SyncStatus.SUCESSO, mensagem);
        }

        public static SyncResult falha(String mensagem) {
            return new SyncResult(SyncStatus.FALHA, mensagem);
        }

        public static SyncResult naoNecessario(String mensagem) {
            return new SyncResult(SyncStatus.NAO_NECESSARIO, mensagem);
        }

        public boolean isSucesso() {
            return status == SyncStatus.SUCESSO;
        }

        public SyncStatus getStatus() {
            return status;
        }

        public String getMensagem() {
            return mensagem;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s", status, mensagem);
        }
    }

    public enum SyncStatus {
        SUCESSO,
        FALHA,
        NAO_NECESSARIO
    }
}