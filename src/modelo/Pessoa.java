package modelo;

/** Classe abstrata: toda pessoa do sistema e eleitor ou candidato. */
public abstract class Pessoa {
    private String nome;
    private String cpf;

    public Pessoa(String nome, String cpf) {
        this.nome = nome;
        this.cpf = cpf;
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    /** Metodo abstrato: cada classe filha diz o seu papel (polimorfismo). */
    public abstract String getPapel();
}
