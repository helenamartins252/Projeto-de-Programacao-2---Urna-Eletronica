package modelo;

public class Eleitor extends Pessoa {
    private String tituloEleitor;
    private boolean jaVotou;

    public Eleitor(String nome, String cpf, String tituloEleitor) {
        super(nome, cpf);
        this.tituloEleitor = tituloEleitor;
        this.jaVotou = false;
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

    /** Vota direto num candidato. */
    public void votar(Urna urna, Candidato candidato)
            throws EleitorJaVotouException, UrnaEncerradaException, CandidatoInvalidoException {
        verificarSePodeVotar();
        urna.votar(candidato);   // se a urna recusar, o jaVotou nao muda (RN.5)
        registrarVoto();
    }

    /** Vota pelo numero digitado. Devolve o tipo (VALIDO ou NULO). */
    public TipoVoto votar(Urna urna, int numero)
            throws EleitorJaVotouException, UrnaEncerradaException {
        verificarSePodeVotar();
        TipoVoto tipo = urna.votar(numero);
        registrarVoto();
        return tipo;
    }

    public void votarBranco(Urna urna)
            throws EleitorJaVotouException, UrnaEncerradaException {
        verificarSePodeVotar();
        urna.votarBranco();
        registrarVoto();
    }

    /** So o proprio eleitor muda o jaVotou, depois que a urna aceitou o voto. */
    private void registrarVoto() {
        jaVotou = true;
    }

    private void verificarSePodeVotar() throws EleitorJaVotouException {
        if (jaVotou) {
            throw new EleitorJaVotouException(getNome());
        }
    }

    @Override
    public String toString() {
        return getNome() + " (título " + tituloEleitor + ")";
    }
}
