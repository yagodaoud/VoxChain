package com.yagodaoud.VoxChain.rede.api.v1;

import com.google.gson.Gson;

/**
 * Interface para padronizar todos os controllers da API.
 * Cada controller implementa esta interface para registrar suas próprias rotas.
 */
public interface IApiController {
    void registerRoutes(Gson gson);
}