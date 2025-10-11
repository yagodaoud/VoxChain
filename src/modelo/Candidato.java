package modelo;

public class Candidato {
    String numero;
    String nome;
    String partido;
    String foto;

    public Candidato(String numero, String nome, String partido, String foto) {
        this.numero = numero;
        this.nome = nome;
        this.partido = partido;
        this.foto = foto;
    }

    public String getNumero() {
        return numero;
    }

    public String getNome() {
        return nome;
    }

    public String getPartido() {
        return partido;
    }

    public String getFoto() {
        return foto;
    }
}
