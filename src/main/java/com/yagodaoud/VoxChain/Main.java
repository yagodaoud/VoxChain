package com.yagodaoud.VoxChain;

import com.yagodaoud.VoxChain.blockchain.No;
import com.yagodaoud.VoxChain.config.ConfigManager;
import com.yagodaoud.VoxChain.rede.ApiServidor;
import com.yagodaoud.VoxChain.rede.NetworkMonitor;

public class Main {
    public static void main(String[] args) {
        String id = args.length > 0 ? args[0] : "NO-DEFAULT";
        int portaNo = args.length > 1 ? Integer.parseInt(args[1]) : 8001;
        int portaApi = args.length > 2 ? Integer.parseInt(args[2]) : 8080;

        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  🌐 BLOCKCHAIN GOVERNAMENTAL - SISTEMA DE VOTAÇÃO SEGURA 🌐 ║");
        System.out.println("║                                                            ║");
        System.out.println("║  Nó: " + String.format("%-50s", id) + " ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");

        // Exibir configuração
        ConfigManager.exibirConfiguracao();

        // Criar e iniciar nó
        No no = new No(id, "localhost", portaNo);
        no.iniciar();

        System.out.println("[" + id + "] ✓ Nó iniciado em localhost:" + portaNo);
        System.out.println("[" + id + "] ✓ P2P Network iniciada");

        // Inicializar Monitor de Rede
        NetworkMonitor monitor = new NetworkMonitor(no);
        monitor.iniciar(10); // Dashboard a cada 10 segundos

        // Inicializar API com endpoints de monitoramento
        ApiServidor api = new ApiServidor(no).comMonitor(monitor);
        api.iniciar(portaApi);
        System.out.println("[" + id + "] ✓ API REST disponível em localhost:" + portaApi);
        System.out.println("[" + id + "] ✓ Dashboard disponível em http://localhost:" + portaApi + "/dashboard\n");

        // Handler para shutdown gracioso
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[" + id + "] 🛑 Encerrando nó...");
            monitor.parar();
            no.parar();
            System.out.println("[" + id + "] ✓ Nó encerrado corretamente");
        }));

        System.out.println("[" + id + "] Sistema pronto. Pressione Ctrl+C para encerrar.\n");

        // Manter aplicação rodando
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}