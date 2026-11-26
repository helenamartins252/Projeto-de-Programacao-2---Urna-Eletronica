package modelo;

/** Classe abstrata: uma pessoa é sempre eleitor ou candidato. */
public abstract class Pessoa {
    private final String nome;
    private final String cpf;

    public Pessoa(String nome, String cpf) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome não pode ficar vazio.");
        }
        if (cpf == null || cpf.isBlank()) {
            throw new IllegalArgumentException("O CPF não pode ficar vazio.");
        }
        this.nome = nome.trim();
        this.cpf = cpf.trim();
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    /** Polimorfismo: cada classe filha diz o seu papel. */
    public abstract String getPapel();
}
