package com.yagodaoud.VoxChain.blockchain.core;

import com.yagodaoud.VoxChain.blockchain.Bloco;

import java.util.List;

/**
 * Responsável pela validação de blocos e cadeias.
 * Centraliza toda a lógica de validação em um único lugar.
 */
public class BlockValidator {
    private final int dificuldade;

    public BlockValidator(int dificuldade) {
        this.dificuldade = dificuldade;
    }

    public ValidationResult validarBloco(Bloco bloco, Bloco blocoAnterior) {
        if (bloco == null) {
            return ValidationResult.erro("Bloco é nulo");
        }

        if (!validarHash(bloco)) {
            return ValidationResult.erro("Hash do bloco inválido");
        }

        if (!validarEncadeamento(bloco, blocoAnterior)) {
            return ValidationResult.erro("Hash anterior não corresponde");
        }

        if (!validarIndice(bloco, blocoAnterior)) {
            return ValidationResult.erro(
                    String.format("Índice inválido. Esperado: %d, Recebido: %d",
                            blocoAnterior.getIndice() + 1, bloco.getIndice())
            );
        }

        if (!validarProofOfWork(bloco)) {
            return ValidationResult.erro("Proof of Work inválido");
        }

        return ValidationResult.sucesso();
    }

    public ValidationResult validarCadeia(List<Bloco> cadeia) {
        if (cadeia == null || cadeia.isEmpty()) {
            return ValidationResult.erro("Cadeia vazia ou nula");
        }

        if (!validarBlocoGenesis(cadeia.get(0))) {
            return ValidationResult.erro("Bloco gênesis inválido");
        }

        for (int i = 1; i < cadeia.size(); i++) {
            Bloco blocoAtual = cadeia.get(i);
            Bloco blocoAnterior = cadeia.get(i - 1);

            ValidationResult resultado = validarBloco(blocoAtual, blocoAnterior);
            if (!resultado.isValido()) {
                return ValidationResult.erro(
                        String.format("Bloco %d inválido: %s", i, resultado.getMensagem())
                );
            }
        }

        return ValidationResult.sucesso();
    }

    private boolean validarHash(Bloco bloco) {
        return bloco.getHash().equals(bloco.calcularHash());
    }

    private boolean validarEncadeamento(Bloco bloco, Bloco blocoAnterior) {
        return bloco.getHashAnterior().equals(blocoAnterior.getHash());
    }

    private boolean validarIndice(Bloco bloco, Bloco blocoAnterior) {
        return bloco.getIndice() == blocoAnterior.getIndice() + 1;
    }

    private boolean validarProofOfWork(Bloco bloco) {
        String alvo = "0".repeat(dificuldade);
        return bloco.getHash().startsWith(alvo);
    }

    private boolean validarBlocoGenesis(Bloco genesis) {
        return genesis.getIndice() == 0 &&
                genesis.getHashAnterior().equals("0");
    }

    /**
     * Classe de resultado de validação.
     * Permite retornar tanto status quanto mensagem de erro.
     */
    public static class ValidationResult {
        private final boolean valido;
        private final String mensagem;

        private ValidationResult(boolean valido, String mensagem) {
            this.valido = valido;
            this.mensagem = mensagem;
        }

        public static ValidationResult sucesso() {
            return new ValidationResult(true, "");
        }

        public static ValidationResult erro(String mensagem) {
            return new ValidationResult(false, mensagem);
        }

        public boolean isValido() {
            return valido;
        }

        public String getMensagem() {
            return mensagem;
        }

        @Override
        public String toString() {
            return valido ? "✓ Válido" : "✗ Inválido: " + mensagem;
        }
    }
}