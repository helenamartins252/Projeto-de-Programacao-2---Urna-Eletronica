package modelo;

/** Lançada quando o candidato não pertence à urna (RN.6). */
public class CandidatoInvalidoException extends Exception {
    public CandidatoInvalidoException() {
        super("Esse candidato não pertence a esta urna.");
    }
}
