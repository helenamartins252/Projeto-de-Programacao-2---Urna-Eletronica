package modelo;

/** Lancada quando o candidato nao pertence a urna (RN.6). */
public class CandidatoInvalidoException extends Exception {
    public CandidatoInvalidoException() {
        super("Esse candidato não pertence a esta urna.");
    }
}
