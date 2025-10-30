package com.yagodaoud.VoxChain.modelo.enums;

public enum NivelAcesso {
    SUPER_ADMIN, // Pode tudo
    ADMIN_TSE, // Gerencia eleições
    OPERADOR, // Apenas consulta
    ELEITOR,
}