package modelo;

/**
 * Um voto. Não guarda nenhuma referência ao eleitor (RN.2).
 * O construtor é visível só no pacote: quem cria votos é a Urna (RN.4).
 */
public class Voto {
    private final TipoVoto tipo;
    private final Candidato candidato; // null em voto branco ou nulo

    Voto(TipoVoto tipo, Candidato candidato) {
        if (tipo == TipoVoto.VALIDO && candidato == null) {
            throw new IllegalArgumentException("Voto válido precisa de candidato.");
        }
        if (tipo != TipoVoto.VALIDO && candidato != null) {
            throw new IllegalArgumentException("Voto branco ou nulo não tem candidato.");
        }
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
