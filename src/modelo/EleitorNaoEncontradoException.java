package modelo;

/** Lançada quando o título ou CPF informado não está cadastrado (RN.6). */
public class EleitorNaoEncontradoException extends Exception {
    public EleitorNaoEncontradoException(String identificador) {
        super("Título ou CPF " + identificador + " não cadastrado.");
    }
}
