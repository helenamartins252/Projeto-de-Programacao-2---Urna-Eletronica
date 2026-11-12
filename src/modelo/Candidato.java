package modelo;

public class Candidato extends Eleitor {
    private int numero;
    private String cargo;
    private String partido;
    private String foto;

    public Candidato(String nome, String cpf, String tituloEleitor,
                     int numero, String cargo, String partido, String foto) {
        super(nome, cpf, tituloEleitor);
        this.numero = numero;
        this.cargo = cargo;
        this.partido = partido;
        this.foto = foto;
    }

    public int getNumero() {
        return numero;
    }

    public String getCargo() {
        return cargo;
    }

    public String getPartido() {
        return partido;
    }

    public String getFoto() {
        return foto;
    }

    @Override
    public String getPapel() {
        return "Candidato";
    }

    @Override
    public String toString() {
        return getNome() + " (" + numero + ") - " + cargo + " - " + partido;
    }
}
