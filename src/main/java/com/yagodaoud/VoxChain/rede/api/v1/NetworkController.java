package com.yagodaoud.VoxChain.rede.api.v1;

import com.google.gson.Gson;
import com.yagodaoud.VoxChain.blockchain.No;
import com.yagodaoud.VoxChain.rede.NetworkMonitor;

import static spark.Spark.path;

public class NetworkController implements IApiController {
    private No no;
    private NetworkMonitor monitor;

    public NetworkController(No no, NetworkMonitor monitor) {
        this.no = no;
        this.monitor = monitor;
    }

    @Override
    public void registerRoutes(Gson gson) {
        path("/network", () -> {

        });
    }
}
