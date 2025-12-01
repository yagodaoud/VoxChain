package com.yagodaoud.VoxChain;

import com.yagodaoud.VoxChain.blockchain.No;
import com.yagodaoud.VoxChain.config.ConfigManager;
import com.yagodaoud.VoxChain.rede.ApiServidor;
import com.yagodaoud.VoxChain.rede.NetworkMonitor;
import com.yagodaoud.VoxChain.utils.Logger;

public class Main {
    public static void main(String[] args) {
        String id = args.length > 0 ? args[0] : "NO-DEFAULT";
        int portaNo = args.length > 1 ? Integer.parseInt(args[1]) : 8001;
        int portaApi = args.length > 2 ? Integer.parseInt(args[2]) : 8080;

        Logger.apresentacao(id, "BLOCKCHAIN GOVERNAMENTAL - SISTEMA DE VOTAÇÃO", "Nó: " + id);

        // Exibir configuração
        ConfigManager.exibirConfiguracao();

        // Criar e iniciar nó
        No no = new No(id, "localhost", portaNo);
        no.iniciar();

        Logger.network(id, "✓ Nó iniciado em localhost:" + portaNo);
        Logger.network(id, "✓ P2P Network iniciada");

        // Inicializar Monitor de Rede
        NetworkMonitor monitor = new NetworkMonitor(no);
        monitor.iniciar(10); // Dashboard a cada 10 segundos

        // Inicializar API com endpoints de monitoramento
        ApiServidor api = new ApiServidor(no).comMonitor(monitor);
        api.iniciar(portaApi);

        Logger.info(id, "✓ API REST disponível em localhost:" + portaApi);
        Logger.info(id, "✓ Dashboard disponível em http://localhost:" + portaApi + "/dashboard\n");

        // Handler para shutdown gracioso
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Logger.info(id, "🛑 Encerrando nó...");
            monitor.parar();
            no.parar();
            Logger.info(id, "✓ Nó encerrado corretamente");
        }));

        Logger.info(id, "Sistema pronto. Pressione Ctrl+C para encerrar.\n");

        // Manter aplicação rodando
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}