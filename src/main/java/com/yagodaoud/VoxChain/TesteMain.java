package com.yagodaoud.VoxChain;

public class TesteMain {
//    public static void main(String[] args) throws InterruptedException {
//
//        System.out.println("╔════════════════════════════════════════════════╗");
//        System.out.println("║   TESTE DE UM NÓ - BLOCKCHAIN VOTAÇÃO         ║");
//        System.out.println("║   TSE São Paulo                                ║");
//        System.out.println("╚════════════════════════════════════════════════╝\n");
//
//        // ==================== 1. CRIAR NÓ ====================
//        System.out.println("=== FASE 1: CRIANDO NÓ ===\n");
//        No no1 = new No("TSE-SP", "localhost", 8001);
//        System.out.println("✓ Nó criado: TSE-SP");
//
//        // ==================== 2. INICIAR NÓ ====================
//        System.out.println("\n=== FASE 2: INICIANDO NÓ ===\n");
//        no1.iniciar();
//
//        // Aguardar servidor iniciar
//        Thread.sleep(2000);
//        System.out.println("✓ Nó iniciado com sucesso\n");
//
//        // ==================== 3. CRIAR TRANSAÇÕES ====================
//        System.out.println("=== FASE 3: CRIANDO TRANSAÇÕES ===\n");
//
//        // Transação 1
//        Voto voto1 = new Voto("ELEITOR001", "13", "Presidente", "ELEICAO2026");
//        Transacao t1 = new Transacao(TipoTransacao.VOTO, voto1, "ELEITOR");
//        System.out.println("Transação 1 criada: Eleitor 001 votou em 13");
//        no1.adicionarTransacao(t1);
//
//        Thread.sleep(1000);
//
//        // Transação 2
//        Voto voto2 = new Voto("ELEITOR002", "45", "Presidente", "ELEICAO2026");
//        Transacao t2 = new Transacao(TipoTransacao.VOTO, voto2, "ELEITOR");
//        System.out.println("Transação 2 criada: Eleitor 002 votou em 45");
//        no1.adicionarTransacao(t2);
//
//        Thread.sleep(1000);
//
//        // Transação 3
//        Voto voto3 = new Voto("ELEITOR003", "13", "Presidente", "ELEICAO2026");
//        Transacao t3 = new Transacao(TipoTransacao.VOTO, voto3, "ELEITOR");
//        System.out.println("Transação 3 criada: Eleitor 003 votou em 13");
//        no1.adicionarTransacao(t3);
//
//        Thread.sleep(1000);
//
//        // Transação 4
//        Voto voto4 = new Voto("ELEITOR004", "22", "Presidente", "ELEICAO2026");
//        Transacao t4 = new Transacao(TipoTransacao.VOTO, voto4, "ELEITOR");
//        System.out.println("Transação 4 criada: Eleitor 004 votou em 22");
//        no1.adicionarTransacao(t4);
//
//        Thread.sleep(1000);
//
//        // Transação 5 - vai disparar mineração (limite é 5)
//        Voto voto5 = new Voto("ELEITOR005", "13", "Presidente", "ELEICAO2026");
//        Transacao t5 = new Transacao(TipoTransacao.VOTO, voto5, "ELEITOR");
//        System.out.println("Transação 5 criada: Eleitor 005 votou em 13");
//        no1.adicionarTransacao(t5);
//
//        System.out.println("\n✓ 5 transações adicionadas ao pool (dispara mineração automática)\n");
//
//        // ==================== 4. AGUARDAR MINERAÇÃO ====================
//        System.out.println("=== FASE 4: MINERAÇÃO EM PROGRESSO ===\n");
//        System.out.println("Aguardando mineração do primeiro bloco...\n");
//
//        Thread.sleep(15000); // Aguardar mineração (pode levar um tempo)
//
//        // ==================== 5. VERIFICAR STATUS ====================
//        System.out.println("\n=== FASE 5: STATUS DO NÓ ===\n");
//        System.out.println(no1.getStatus());
//        System.out.println("Blockchain: " + no1.getBlockchain().toString());
//
//        // ==================== 6. FORÇAR MINERAÇÃO MANUAL ====================
//        System.out.println("\n=== FASE 6: FORÇAR MINERAÇÃO MANUAL ===\n");
//
//        // Adicionar mais uma transação
//        Voto voto6 = new Voto("ELEITOR006", "45", "Presidente", "ELEICAO2026");
//        Transacao t6 = new Transacao(TipoTransacao.VOTO, voto6, "ELEITOR");
//        System.out.println("Transação 6 criada: Eleitor 006 votou em 45");
//        no1.adicionarTransacao(t6);
//
//        Thread.sleep(1000);
//
//        System.out.println("Forçando mineração manual...\n");
//        no1.minerarManualmente();
//
//        Thread.sleep(15000); // Aguardar mineração
//
//        // ==================== 7. STATUS FINAL ====================
//        System.out.println("\n=== FASE 7: STATUS FINAL ===\n");
//        System.out.println(no1.getStatus());
//        System.out.println("Blockchain: " + no1.getBlockchain().toString());
//
//        // ==================== 8. VALIDAR BLOCKCHAIN ====================
//        System.out.println("\n=== FASE 8: VALIDAÇÃO ===\n");
//        boolean valida = no1.getBlockchain().validarCadeia();
//        System.out.println("Blockchain válida: " + (valida ? "✓ SIM" : "✗ NÃO"));
//
//        // ==================== 9. EXIBIR INFORMAÇÕES DOS BLOCOS ====================
//        System.out.println("\n=== FASE 9: BLOCOS MINERADOS ===\n");
//        int totalBlocos = no1.getBlockchain().getBlocos().size();
//        System.out.println("Total de blocos: " + totalBlocos);
//
//        for (blockchain.Bloco bloco : no1.getBlockchain().getBlocos()) {
//            System.out.println("\n" + bloco.toString());
//
//            // Exibe hash com segurança
//            String hash = bloco.getHash();
//            System.out.println("  Hash: " + truncarHash(hash, 32));
//
//            // Exibe hash anterior com segurança
//            String hashAnterior = bloco.getHashAnterior();
//            if (hashAnterior.equals("0")) {
//                System.out.println("  Hash Anterior: [GENESIS]");
//            } else {
//                System.out.println("  Hash Anterior: " + truncarHash(hashAnterior, 32));
//            }
//
//            System.out.println("  Timestamp: " + bloco.getTimestamp());
//            System.out.println("  Nonce: " + bloco.getNonce());
//        }
//        // ==================== 10. TESTE DE TRANSAÇÃO DUPLICADA ====================
//        System.out.println("\n=== FASE 10: TESTE DE TRANSAÇÃO DUPLICADA ===\n");
//        System.out.println("Tentando adicionar transação duplicada...");
//        no1.adicionarTransacao(t1); // Já foi processada
//
//        System.out.println("\n✓ Transação duplicada rejeitada com sucesso");
//
//        // ==================== 11. AGUARDANDO (MODO CONTÍNUO) ====================
//        System.out.println("\n╔════════════════════════════════════════════════╗");
//        System.out.println("║  TESTE CONCLUÍDO COM SUCESSO!                 ║");
//        System.out.println("║  Nó continuará rodando...                     ║");
//        System.out.println("║  Pressione Ctrl+C para finalizar              ║");
//        System.out.println("╚════════════════════════════════════════════════╝\n");
//
//        while (true) {
//            Thread.sleep(30000);
//            System.out.println("\n[" + System.currentTimeMillis() + "] Status: " + no1.getStatus());
//        }
//    }

//    public static void main(String[] args) throws InterruptedException {
//
//        System.out.println("╔═══════════════════════════════════════════════════╗");
//        System.out.println("║   TESTE DE 3 NÓS - BLOCKCHAIN VOTAÇÃO P2P       ║");
//        System.out.println("║   Simulando Rede Distribuída TSE                 ║");
//        System.out.println("╚═══════════════════════════════════════════════════╝\n");
//
//        // ==================== 1. CRIAR 3 NÓS ====================
//        System.out.println("=== FASE 1: CRIANDO 3 NÓS ===\n");
//
//        No no1 = new No("TSE-SP", "localhost", 8001);
//        No no2 = new No("TSE-RJ", "localhost", 8002);
//        No no3 = new No("TSE-MG", "localhost", 8003);
//
//        System.out.println("✓ Nó 1 criado: TSE-SP (porta 8001)");
//        System.out.println("✓ Nó 2 criado: TSE-RJ (porta 8002)");
//        System.out.println("✓ Nó 3 criado: TSE-MG (porta 8003)");
//
//        // ==================== 2. INICIAR 3 NÓS ====================
//        System.out.println("\n=== FASE 2: INICIANDO 3 NÓS ===\n");
//
//        no1.iniciar();
//        no2.iniciar();
//        no3.iniciar();
//
//        // Aguardar servidores ficarem prontos
//        Thread.sleep(3000);
//        System.out.println("\n✓ Todos os nós iniciados com sucesso");
//
//        // ==================== 3. CONECTAR NÓS EM REDE ====================
//        System.out.println("\n=== FASE 3: CONECTANDO NÓS EM REDE P2P ===\n");
//        System.out.println("Topologia: SP --> RJ --> MG\n");
//
//        // SP conecta em RJ
//        no1.conectarPeer("localhost", 8002, "TSE-RJ");
//        Thread.sleep(2000);
//
//        // RJ conecta em MG
//        no2.conectarPeer("localhost", 8003, "TSE-MG");
//        Thread.sleep(2000);
//
//        System.out.println("\n✓ Conexões P2P estabelecidas");
//
//        // ==================== 4. SINCRONIZAR BLOCKCHAINS ====================
//        System.out.println("\n=== FASE 4: SINCRONIZANDO BLOCKCHAINS ===\n");
//        System.out.println("Aguardando sincronização entre nós...");
//        Thread.sleep(3000);
//
//        System.out.println("\n✓ Blockchains sincronizadas");
//        System.out.println("[TSE-SP] " + no1.getStatus());
//        System.out.println("[TSE-RJ] " + no2.getStatus());
//        System.out.println("[TSE-MG] " + no3.getStatus());
//
//        // ==================== 5. NÓ 1 ADICIONA TRANSAÇÕES ====================
//        System.out.println("\n=== FASE 5: NÓ 1 (SP) ADICIONANDO TRANSAÇÕES ===\n");
//
//        Voto voto1 = new Voto("ELEITOR001", "13", "Presidente", "ELEICAO2026");
//        Transacao t1 = new Transacao(TipoTransacao.VOTO, voto1, "ELEITOR");
//        System.out.println("SP: Criando transação 1 - Eleitor 001 votou em 13");
//        no1.adicionarTransacao(t1);
//
//        Thread.sleep(1500);
//
//        Voto voto2 = new Voto("ELEITOR002", "45", "Presidente", "ELEICAO2026");
//        Transacao t2 = new Transacao(TipoTransacao.VOTO, voto2, "ELEITOR");
//        System.out.println("SP: Criando transação 2 - Eleitor 002 votou em 45");
//        no1.adicionarTransacao(t2);
//
//        Thread.sleep(1500);
//
//        Voto voto3 = new Voto("ELEITOR003", "13", "Presidente", "ELEICAO2026");
//        Transacao t3 = new Transacao(TipoTransacao.VOTO, voto3, "ELEITOR");
//        System.out.println("SP: Criando transação 3 - Eleitor 003 votou em 13");
//        no1.adicionarTransacao(t3);
//
//        Thread.sleep(1500);
//
//        // ==================== 6. NÓ 2 ADICIONA TRANSAÇÃO ====================
//        System.out.println("\n=== FASE 6: NÓ 2 (RJ) ADICIONANDO TRANSAÇÃO ===\n");
//
//        Voto voto4 = new Voto("ELEITOR004", "22", "Presidente", "ELEICAO2026");
//        Transacao t4 = new Transacao(TipoTransacao.VOTO, voto4, "ELEITOR");
//        System.out.println("RJ: Criando transação 4 - Eleitor 004 votou em 22");
//        no2.adicionarTransacao(t4);
//
//        Thread.sleep(1500);
//
//        // ==================== 7. NÓ 3 ADICIONA TRANSAÇÃO ====================
//        System.out.println("\n=== FASE 7: NÓ 3 (MG) ADICIONANDO TRANSAÇÃO ===\n");
//
//        Voto voto5 = new Voto("ELEITOR005", "45", "Presidente", "ELEICAO2026");
//        Transacao t5 = new Transacao(TipoTransacao.VOTO, voto5, "ELEITOR");
//        System.out.println("MG: Criando transação 5 - Eleitor 005 votou em 45");
//        no3.adicionarTransacao(t5);
//
//        System.out.println("\n✓ 5 transações criadas em diferentes nós (dispara mineração automática)");
//
//        // ==================== 8. AGUARDAR MINERAÇÃO ====================
//        System.out.println("\n=== FASE 8: MINERAÇÃO E PROPAGAÇÃO ===\n");
//        System.out.println("Aguardando mineração do primeiro bloco...\n");
//
//        Thread.sleep(20000); // Aguardar mineração
//
//        // ==================== 9. VERIFICAR SINCRONIZAÇÃO ====================
//        System.out.println("\n=== FASE 9: VERIFICAR SINCRONIZAÇÃO P2P ===\n");
//
//        System.out.println("Status após mineração:");
//        System.out.println("[TSE-SP] " + no1.getStatus());
//        System.out.println("[TSE-RJ] " + no2.getStatus());
//        System.out.println("[TSE-MG] " + no3.getStatus());
//
//        int tamanhoSP = no1.getBlockchain().getTamanho();
//        int tamanhoRJ = no2.getBlockchain().getTamanho();
//        int tamanhoMG = no3.getBlockchain().getTamanho();
//
//        if (tamanhoSP == tamanhoRJ && tamanhoRJ == tamanhoMG) {
//            System.out.println("\n✓ SUCESSO! Todos os nós têm a mesma blockchain (" + tamanhoSP + " blocos)");
//        } else {
//            System.out.println("\n⚠ AVISO: Tamanhos diferentes!");
//            System.out.println("  SP: " + tamanhoSP + " blocos");
//            System.out.println("  RJ: " + tamanhoRJ + " blocos");
//            System.out.println("  MG: " + tamanhoMG + " blocos");
//        }
//
//        // ==================== 10. ADICIONAR MAIS TRANSAÇÕES ====================
//        System.out.println("\n=== FASE 10: ADICIONAR MAIS TRANSAÇÕES ===\n");
//
//        Voto voto6 = new Voto("ELEITOR006", "13", "Presidente", "ELEICAO2026");
//        Transacao t6 = new Transacao(TipoTransacao.VOTO, voto6, "ELEITOR");
//        System.out.println("SP: Criando transação 6 - Eleitor 006 votou em 13");
//        no1.adicionarTransacao(t6);
//
//        Thread.sleep(1500);
//
//        Voto voto7 = new Voto("ELEITOR007", "45", "Presidente", "ELEICAO2026");
//        Transacao t7 = new Transacao(TipoTransacao.VOTO, voto7, "ELEITOR");
//        System.out.println("RJ: Criando transação 7 - Eleitor 007 votou em 45");
//        no2.adicionarTransacao(t7);
//
//        Thread.sleep(1500);
//
//        Voto voto8 = new Voto("ELEITOR008", "22", "Presidente", "ELEICAO2026");
//        Transacao t8 = new Transacao(TipoTransacao.VOTO, voto8, "ELEITOR");
//        System.out.println("MG: Criando transação 8 - Eleitor 008 votou em 22");
//        no3.adicionarTransacao(t8);
//
//        Thread.sleep(1500);
//
//        Voto voto9 = new Voto("ELEITOR009", "13", "Presidente", "ELEICAO2026");
//        Transacao t9 = new Transacao(TipoTransacao.VOTO, voto9, "ELEITOR");
//        System.out.println("SP: Criando transação 9 - Eleitor 009 votou em 13");
//        no1.adicionarTransacao(t9);
//
//        System.out.println("\n✓ 4 novas transações criadas (vai disparar novo bloco)");
//
//        // ==================== 11. AGUARDAR SEGUNDA MINERAÇÃO ====================
//        System.out.println("\n=== FASE 11: SEGUNDA RODADA DE MINERAÇÃO ===\n");
//        System.out.println("Aguardando mineração do segundo bloco...\n");
//
//        Thread.sleep(20000);
//
//        // ==================== 12. VERIFICAR SINCRONIZAÇÃO NOVAMENTE ====================
//        System.out.println("\n=== FASE 12: VERIFICAR SINCRONIZAÇÃO FINAL ===\n");
//
//        System.out.println("Status após segunda mineração:");
//        System.out.println("[TSE-SP] " + no1.getStatus());
//        System.out.println("[TSE-RJ] " + no2.getStatus());
//        System.out.println("[TSE-MG] " + no3.getStatus());
//
//        // ==================== 13. VALIDAR TODAS AS BLOCKCHAINS ====================
//        System.out.println("\n=== FASE 13: VALIDAÇÃO DAS BLOCKCHAINS ===\n");
//
//        boolean validaSP = no1.getBlockchain().validarCadeia();
//        boolean validaRJ = no2.getBlockchain().validarCadeia();
//        boolean validaMG = no3.getBlockchain().validarCadeia();
//
//        System.out.println("Blockchain SP válida: " + (validaSP ? "✓ SIM" : "✗ NÃO"));
//        System.out.println("Blockchain RJ válida: " + (validaRJ ? "✓ SIM" : "✗ NÃO"));
//        System.out.println("Blockchain MG válida: " + (validaMG ? "✓ SIM" : "✗ NÃO"));
//
//        // ==================== 14. EXIBIR BLOCOS ====================
//        System.out.println("\n=== FASE 14: BLOCOS NO NÓ SP ===\n");
//
//        int totalBlocos = no1.getBlockchain().getBlocos().size();
//        System.out.println("Total de blocos: " + totalBlocos);
//
//        for (blockchain.Bloco bloco : no1.getBlockchain().getBlocos()) {
//            System.out.println("\n" + bloco.toString());
//            System.out.println("  Hash: " + bloco.getHashTruncado(32));
//            System.out.println("  Hash Anterior: " + bloco.getHashAnteriorTruncado(32));
//            System.out.println("  Timestamp: " + bloco.getTimestamp());
//            System.out.println("  Nonce: " + bloco.getNonce());
//        }
//
//        // ==================== 15. TESTE DE TRANSAÇÃO DUPLICADA ====================
//        System.out.println("\n=== FASE 15: TESTE DE TRANSAÇÃO DUPLICADA ===\n");
//
//        System.out.println("Tentando adicionar transação duplicada em RJ...");
//        no2.adicionarTransacao(t1); // Já foi processada
//
//        System.out.println("\n✓ Transação duplicada rejeitada com sucesso");
//
//        // ==================== 16. RESUMO FINAL ====================
//        System.out.println("\n╔═══════════════════════════════════════════════════╗");
//        System.out.println("║          TESTE CONCLUÍDO COM SUCESSO!            ║");
//        System.out.println("╚═══════════════════════════════════════════════════╝");
//
//        System.out.println("\nRESUMO FINAL:");
//        System.out.println("✓ 3 nós criados e iniciados");
//        System.out.println("✓ Conectados em com.yagodaoud.VoxChain.rede P2P");
//        System.out.println("✓ 9 transações processadas");
//        System.out.println("✓ 2 blocos minerados");
//        System.out.println("✓ Sincronização P2P funcionando");
//        System.out.println("✓ Todas blockchains válidas e sincronizadas");
//        System.out.println("✓ Validação de transações duplicadas funcionando");
//
//        System.out.println("\nNós continuando rodando...");
//        System.out.println("Pressione Ctrl+C para finalizar\n");
//
//        // ==================== 17. LOOP DE MONITORAMENTO ====================
//        while (true) {
//            Thread.sleep(30000);
//            System.out.println("\n[MONITOR] " + System.currentTimeMillis());
//            System.out.println("[TSE-SP] " + no1.getStatus() + " | Peers: " + no1.getNumPeers());
//            System.out.println("[TSE-RJ] " + no2.getStatus() + " | Peers: " + no2.getNumPeers());
//            System.out.println("[TSE-MG] " + no3.getStatus() + " | Peers: " + no3.getNumPeers());
//        }
//    }

    public static String truncarHash(String hash, int tamanho) {
        if (hash == null || hash.length() == 0) {
            return "0";
        }

        int fim = Math.min(tamanho, hash.length());
        return hash.substring(0, fim) + (fim < hash.length() ? "..." : "");
    }

}