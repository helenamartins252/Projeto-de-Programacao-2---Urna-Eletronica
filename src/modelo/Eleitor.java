package modelo;

public class Eleitor extends Pessoa {
    private final String tituloEleitor;
    private boolean jaVotou;

    /** Eleitor novo: ainda não votou. */
    public Eleitor(String nome, String cpf, String titulo) {
        this(nome, cpf, titulo, false);
    }

    /** Usado ao ler do banco: devolve o eleitor com o jaVotou que estava gravado. */
    public Eleitor(String nome, String cpf, String titulo, boolean jaVotou) {
        super(nome, cpf);
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("O título de eleitor não pode ficar vazio.");
        }
        this.tituloEleitor = titulo.trim();
        this.jaVotou = jaVotou;
    }

    public String getTituloEleitor() {
        return tituloEleitor;
    }

    public boolean jaVotou() {
        return jaVotou;
    }

    @Override
    public String getPapel() {
        return "Eleitor";
    }

    /** RN.1: confere o jaVotou antes de pedir o voto à urna. */
    public void verificarSePodeVotar() throws EleitorJaVotouException {
        if (jaVotou) {
            throw new EleitorJaVotouException(getNome());
        }
    }

    /** Vota direto num candidato. O jaVotou só muda depois que a urna aceitou o voto (RN.5). */
    public void votar(Urna urna, Candidato candidato)
            throws EleitorJaVotouException, UrnaEncerradaException, CandidatoInvalidoException {
        verificarSePodeVotar();
        urna.votar(candidato);
        registrarVoto();
    }

    /** Vota pelo número digitado. Número que não existe vira voto NULO. */
    public TipoVoto votar(Urna urna, int numero)
            throws EleitorJaVotouException, UrnaEncerradaException {
        verificarSePodeVotar();
        TipoVoto tipo = urna.votar(numero);
        registrarVoto();
        return tipo;
    }

    public void votarBranco(Urna urna) throws EleitorJaVotouException, UrnaEncerradaException {
        verificarSePodeVotar();
        urna.votarBranco();
        registrarVoto();
    }

    /** Privado: o jaVotou só muda por método do próprio eleitor. */
    private void registrarVoto() {
        jaVotou = true;
    }

    @Override
    public String toString() {
        return getNome() + " (título " + tituloEleitor + ")";
    }
}
