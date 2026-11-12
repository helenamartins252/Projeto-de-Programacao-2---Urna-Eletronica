package modelo;

/** O voto nao tem ligacao com o eleitor (RN.2). So a Urna cria votos (RN.4). */
public class Voto {
    private TipoVoto tipo;
    private Candidato candidato;   // null se for branco ou nulo

    // sem "public": visivel so no pacote modelo, ou seja, so a Urna cria Voto
    Voto(TipoVoto tipo, Candidato candidato) {
        this.tipo = tipo;
        this.candidato = candidato;
    }

    public TipoVoto getTipo() {
        return tipo;
    }

    public Candidato getCandidato() {
        return candidato;
    }
}
