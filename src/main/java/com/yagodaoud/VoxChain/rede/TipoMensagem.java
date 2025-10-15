package com.yagodaoud.VoxChain.rede;

public enum TipoMensagem {
    NOVA_TRANSACAO,      // "Tenho uma nova transação"
    NOVO_BLOCO,          // "Minerei um bloco"
    REQUISITAR_BLOCKCHAIN, // "Me manda tua blockchain"
    RESPOSTA_BLOCKCHAIN,  // "Aqui está"
    PING,                // Verificar se está vivo
    PONG,
    LISTAR_PEERS,
    RESPOSTA_PEERS
}