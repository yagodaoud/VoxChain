package com.yagodaoud.VoxChain.blockchain.servicos;

import com.yagodaoud.VoxChain.blockchain.BlockchainGovernamental;
import com.yagodaoud.VoxChain.modelo.Administrador;
import com.yagodaoud.VoxChain.modelo.Eleitor;
import com.yagodaoud.VoxChain.modelo.Transacao;
import com.yagodaoud.VoxChain.modelo.enums.TipoTransacao;

import java.util.List;

public class ServicoEleitor {
    private final BlockchainGovernamental blockchain;

    public ServicoEleitor(BlockchainGovernamental blockchain) {
        this.blockchain = blockchain;
    }

    public Eleitor cadastrarEleitor(String solicitanteCpfHash, String cpf, int zona, int secao) {
        Administrador admin = blockchain.buscarAdminPorCpfHash(solicitanteCpfHash);
        if (admin == null || !admin.isAtivo()) {
            throw new SecurityException("Apenas administradores podem cadastrar eleitores");
        }

        Eleitor existente = blockchain.buscarEleitor(Eleitor.hashCpf(cpf));
        if (existente != null) {
            throw new IllegalArgumentException("Eleitor já cadastrado");
        }

        Eleitor eleitor = new Eleitor(cpf, zona, secao);

        Transacao t = new Transacao(
                TipoTransacao.CADASTRO_ELEITOR,
                eleitor,
                admin.getId());

        blockchain.adicionarAoPool(t);

        return eleitor;
    }

    public List<Eleitor> listarEleitores() {
        return blockchain.listarEleitores();
    }
}
