package modelo;

/** Lancada quando o titulo (ou CPF) nao esta cadastrado (RN.6). */
public class EleitorNaoEncontradoException extends Exception {
    public EleitorNaoEncontradoException(String identificacao) {
        super("Título " + identificacao + " não cadastrado.");
    }
}
