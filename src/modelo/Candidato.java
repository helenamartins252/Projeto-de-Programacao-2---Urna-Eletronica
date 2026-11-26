package modelo;

public class Candidato extends Eleitor {
    private final int numero;
    private final String cargo;
    private final String partido;
    private final String foto;

    public Candidato(String nome, String cpf, String titulo,
                     int numero, String cargo, String partido, String foto) {
        this(nome, cpf, titulo, numero, cargo, partido, foto, false);
    }

    /** Usado ao ler do banco: preserva o jaVotou gravado. */
    public Candidato(String nome, String cpf, String titulo,
                     int numero, String cargo, String partido, String foto, boolean jaVotou) {
        super(nome, cpf, titulo, jaVotou);
        if (numero <= 0) {
            throw new IllegalArgumentException("O número do candidato deve ser maior que zero.");
        }
        if (cargo == null || cargo.isBlank() || partido == null || partido.isBlank()) {
            throw new IllegalArgumentException("Cargo e partido não podem ficar vazios.");
        }
        this.numero = numero;
        this.cargo = cargo.trim();
        this.partido = partido.trim();
        this.foto = (foto == null) ? "" : foto.trim();
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

    /** O número identifica o candidato (é único no banco). */
    @Override
    public boolean equals(Object o) {
        return o instanceof Candidato outro && outro.numero == numero;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(numero);
    }

    @Override
    public String toString() {
        return getNome() + " (" + numero + ") - " + cargo + " - " + partido;
    }
}
