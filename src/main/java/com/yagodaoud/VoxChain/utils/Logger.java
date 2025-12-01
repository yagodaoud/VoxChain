package com.yagodaoud.VoxChain.utils;

/**
 * Sistema de logs com níveis configuráveis para facilitar apresentação
 * e debugging do sistema de blockchain.
 *
 * Níveis:
 * - ERROR: Apenas erros críticos
 * - INFO: Informações importantes (mineração, novos blocos)
 * - DEBUG: Detalhes de operações
 * - NETWORK: Tráfego de rede (muito verboso)
 */
public class Logger {

    public enum Level {
        ERROR(0),
        INFO(1),
        DEBUG(2),
        NETWORK(3);

        private final int priority;

        Level(int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }
    }

    // Nível padrão: INFO (para apresentação)
    private static Level currentLevel = Level.INFO;

    public static void setLevel(Level level) {
        currentLevel = level;
        System.out.println("📋 Nível de log alterado para: " + level);
    }

    public static void error(String nodeId, String message) {
        log(Level.ERROR, nodeId, "❌ " + message);
    }

    public static void info(String nodeId, String message) {
        log(Level.INFO, nodeId, "ℹ️  " + message);
    }

    public static void debug(String nodeId, String message) {
        log(Level.DEBUG, nodeId, "🔍 " + message);
    }

    public static void network(String nodeId, String message) {
        log(Level.NETWORK, nodeId, "🌐 " + message);
    }

    public static void blockchain(String nodeId, String message) {
        log(Level.INFO, nodeId, "⛓️  " + message);
    }

    public static void mining(String nodeId, String message) {
        log(Level.INFO, nodeId, "⛏️  " + message);
    }

    public static void vote(String nodeId, String message) {
        log(Level.INFO, nodeId, "🗳️  " + message);
    }

    private static void log(Level level, String nodeId, String message) {
        if (level.getPriority() <= currentLevel.getPriority()) {
            String timestamp = java.time.LocalTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            System.out.println(String.format("[%s] [%s] %s",
                    timestamp, nodeId, message));
        }
    }

    // Método especial para apresentação - exibe resumo visual
    public static void apresentacao(String nodeId, String titulo, String... detalhes) {
        if (currentLevel.getPriority() >= Level.INFO.getPriority()) {
            System.out.println("\n╔═══════════════════════════════════════════════════════╗");
            System.out.println("║ " + String.format("%-53s", titulo) + " ║");
            System.out.println("╠═══════════════════════════════════════════════════════╣");
            for (String detalhe : detalhes) {
                System.out.println("║ " + String.format("%-53s", detalhe) + " ║");
            }
            System.out.println("╚═══════════════════════════════════════════════════════╝\n");
        }
    }
}