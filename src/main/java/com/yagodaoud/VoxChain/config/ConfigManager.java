package com.yagodaoud.VoxChain.config;

import com.yagodaoud.VoxChain.rede.PeerDiscovery;
import com.yagodaoud.VoxChain.utils.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ConfigManager {
    private static Properties props = new Properties();
    private static boolean carregado = false;

    static {
        carregarConfig();
    }

    private static void carregarConfig() {
        try (InputStream input = ConfigManager.class
                .getClassLoader()
                .getResourceAsStream("bootstrap.properties")) {

            if (input != null) {
                props.load(input);
                carregado = true;
                System.out.println("✓ bootstrap.properties carregado");
            } else {
                System.out.println("⚠ bootstrap.properties não encontrado, usando defaults");
                carregarDefaults();
            }
        } catch (IOException e) {
            System.err.println("✗ Erro ao carregar bootstrap.properties: " + e.getMessage());
            carregarDefaults();
        }
    }

    private static void carregarDefaults() {
        props.setProperty("bootstrap.nodes", "TSE-SP|localhost|8001,TSE-RJ|localhost|8002,TSE-MG|localhost|8003");
        props.setProperty("discovery.interval", "5");
        props.setProperty("discovery.healthcheck.interval", "10");
        props.setProperty("discovery.sync.interval", "15");
        props.setProperty("discovery.timeout", "30000");
        props.setProperty("discovery.gossip.enabled", "true");
    }

    // ============ BOOTSTRAP NODES ============

    public static List<PeerDiscovery.PeerInfo> obterBootstrapNodes() {
        List<PeerDiscovery.PeerInfo> nodes = new ArrayList<>();
        String config = props.getProperty("bootstrap.nodes", "");

        if (config.isEmpty()) {
            System.out.println("⚠ Nenhum bootstrap node configurado");
            return nodes;
        }

        String[] pares = config.split(",");
        for (String par : pares) {
            String[] partes = par.trim().split("\\|");
            if (partes.length == 3) {
                try {
                    String id = partes[0].trim();
                    String ip = partes[1].trim();
                    int porta = Integer.parseInt(partes[2].trim());
                    nodes.add(new PeerDiscovery.PeerInfo(id, ip, porta));
                } catch (NumberFormatException e) {
                    System.err.println("✗ Erro ao parsear bootstrap node: " + par);
                }
            }
        }

        return nodes;
    }

    // ============ GETTERS ============

    public static int getDiscoveryInterval() {
        return Integer.parseInt(props.getProperty("discovery.interval", "5"));
    }

    public static int getHealthCheckInterval() {
        return Integer.parseInt(props.getProperty("discovery.healthcheck.interval", "10"));
    }

    public static int getSyncInterval() {
        return Integer.parseInt(props.getProperty("discovery.sync.interval", "15"));
    }

    public static long getDiscoveryTimeout() {
        return Long.parseLong(props.getProperty("discovery.timeout", "30000"));
    }

    public static boolean isGossipEnabled() {
        return Boolean.parseBoolean(props.getProperty("discovery.gossip.enabled", "true"));
    }

    public static void exibirConfiguracao() {
        Logger.apresentacao(null,
                "CONFIGURAÇÃO DE DISCOVERY",
                "Bootstrap nodes: " + props.getProperty("bootstrap.nodes"),
                "Discovery interval: " + getDiscoveryInterval() + "s",
                "Health check: " + getHealthCheckInterval() + "s",
                "Sync interval: " + getSyncInterval() + "s",
                "Gossip enabled: " + isGossipEnabled());
    }
}