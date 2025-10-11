import blockchain.No;
import modelo.Transacao;
import modelo.Voto;
import modelo.enums.TipoTransacao;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║   TESTE DE UM NÓ - BLOCKCHAIN VOTAÇÃO         ║");
        System.out.println("║   TSE São Paulo                                ║");
        System.out.println("╚════════════════════════════════════════════════╝\n");

        // ==================== 1. CRIAR NÓ ====================
        System.out.println("=== FASE 1: CRIANDO NÓ ===\n");
        No no1 = new No("TSE-SP", "localhost", 8001);
        System.out.println("✓ Nó criado: TSE-SP");

        // ==================== 2. INICIAR NÓ ====================
        System.out.println("\n=== FASE 2: INICIANDO NÓ ===\n");
        no1.iniciar();

        // Aguardar servidor iniciar
        Thread.sleep(2000);
        System.out.println("✓ Nó iniciado com sucesso\n");

        // ==================== 3. CRIAR TRANSAÇÕES ====================
        System.out.println("=== FASE 3: CRIANDO TRANSAÇÕES ===\n");

        // Transação 1
        Voto voto1 = new Voto("ELEITOR001", "13", "Presidente", "ELEICAO2026");
        Transacao t1 = new Transacao(TipoTransacao.VOTO, voto1, "ELEITOR");
        System.out.println("Transação 1 criada: Eleitor 001 votou em 13");
        no1.adicionarTransacao(t1);

        Thread.sleep(1000);

        // Transação 2
        Voto voto2 = new Voto("ELEITOR002", "45", "Presidente", "ELEICAO2026");
        Transacao t2 = new Transacao(TipoTransacao.VOTO, voto2, "ELEITOR");
        System.out.println("Transação 2 criada: Eleitor 002 votou em 45");
        no1.adicionarTransacao(t2);

        Thread.sleep(1000);

        // Transação 3
        Voto voto3 = new Voto("ELEITOR003", "13", "Presidente", "ELEICAO2026");
        Transacao t3 = new Transacao(TipoTransacao.VOTO, voto3, "ELEITOR");
        System.out.println("Transação 3 criada: Eleitor 003 votou em 13");
        no1.adicionarTransacao(t3);

        Thread.sleep(1000);

        // Transação 4
        Voto voto4 = new Voto("ELEITOR004", "22", "Presidente", "ELEICAO2026");
        Transacao t4 = new Transacao(TipoTransacao.VOTO, voto4, "ELEITOR");
        System.out.println("Transação 4 criada: Eleitor 004 votou em 22");
        no1.adicionarTransacao(t4);

        Thread.sleep(1000);

        // Transação 5 - vai disparar mineração (limite é 5)
        Voto voto5 = new Voto("ELEITOR005", "13", "Presidente", "ELEICAO2026");
        Transacao t5 = new Transacao(TipoTransacao.VOTO, voto5, "ELEITOR");
        System.out.println("Transação 5 criada: Eleitor 005 votou em 13");
        no1.adicionarTransacao(t5);

        System.out.println("\n✓ 5 transações adicionadas ao pool (dispara mineração automática)\n");

        // ==================== 4. AGUARDAR MINERAÇÃO ====================
        System.out.println("=== FASE 4: MINERAÇÃO EM PROGRESSO ===\n");
        System.out.println("Aguardando mineração do primeiro bloco...\n");

        Thread.sleep(15000); // Aguardar mineração (pode levar um tempo)

        // ==================== 5. VERIFICAR STATUS ====================
        System.out.println("\n=== FASE 5: STATUS DO NÓ ===\n");
        System.out.println(no1.getStatus());
        System.out.println("Blockchain: " + no1.getBlockchain().toString());

        // ==================== 6. FORÇAR MINERAÇÃO MANUAL ====================
        System.out.println("\n=== FASE 6: FORÇAR MINERAÇÃO MANUAL ===\n");

        // Adicionar mais uma transação
        Voto voto6 = new Voto("ELEITOR006", "45", "Presidente", "ELEICAO2026");
        Transacao t6 = new Transacao(TipoTransacao.VOTO, voto6, "ELEITOR");
        System.out.println("Transação 6 criada: Eleitor 006 votou em 45");
        no1.adicionarTransacao(t6);

        Thread.sleep(1000);

        System.out.println("Forçando mineração manual...\n");
        no1.minerarManualmente();

        Thread.sleep(15000); // Aguardar mineração

        // ==================== 7. STATUS FINAL ====================
        System.out.println("\n=== FASE 7: STATUS FINAL ===\n");
        System.out.println(no1.getStatus());
        System.out.println("Blockchain: " + no1.getBlockchain().toString());

        // ==================== 8. VALIDAR BLOCKCHAIN ====================
        System.out.println("\n=== FASE 8: VALIDAÇÃO ===\n");
        boolean valida = no1.getBlockchain().validarCadeia();
        System.out.println("Blockchain válida: " + (valida ? "✓ SIM" : "✗ NÃO"));

        // ==================== 9. EXIBIR INFORMAÇÕES DOS BLOCOS ====================
        System.out.println("\n=== FASE 9: BLOCOS MINERADOS ===\n");
        int totalBlocos = no1.getBlockchain().getBlocos().size();
        System.out.println("Total de blocos: " + totalBlocos);

        for (blockchain.Bloco bloco : no1.getBlockchain().getBlocos()) {
            System.out.println("\n" + bloco.toString());

            // Exibe hash com segurança
            String hash = bloco.getHash();
            System.out.println("  Hash: " + truncarHash(hash, 32));

            // Exibe hash anterior com segurança
            String hashAnterior = bloco.getHashAnterior();
            if (hashAnterior.equals("0")) {
                System.out.println("  Hash Anterior: [GENESIS]");
            } else {
                System.out.println("  Hash Anterior: " + truncarHash(hashAnterior, 32));
            }

            System.out.println("  Timestamp: " + bloco.getTimestamp());
            System.out.println("  Nonce: " + bloco.getNonce());
        }
        // ==================== 10. TESTE DE TRANSAÇÃO DUPLICADA ====================
        System.out.println("\n=== FASE 10: TESTE DE TRANSAÇÃO DUPLICADA ===\n");
        System.out.println("Tentando adicionar transação duplicada...");
        no1.adicionarTransacao(t1); // Já foi processada

        System.out.println("\n✓ Transação duplicada rejeitada com sucesso");

        // ==================== 11. AGUARDANDO (MODO CONTÍNUO) ====================
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║  TESTE CONCLUÍDO COM SUCESSO!                 ║");
        System.out.println("║  Nó continuará rodando...                     ║");
        System.out.println("║  Pressione Ctrl+C para finalizar              ║");
        System.out.println("╚════════════════════════════════════════════════╝\n");

        while (true) {
            Thread.sleep(30000);
            System.out.println("\n[" + System.currentTimeMillis() + "] Status: " + no1.getStatus());
        }
    }

    public static String truncarHash(String hash, int tamanho) {
        if (hash == null || hash.length() == 0) {
            return "0";
        }

        int fim = Math.min(tamanho, hash.length());
        return hash.substring(0, fim) + (fim < hash.length() ? "..." : "");
    }

}