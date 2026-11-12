package modelo;

/** Lancada quando o eleitor tenta votar de novo (RN.1). */
public class EleitorJaVotouException extends Exception {
    public EleitorJaVotouException(String nome) {
        super(nome + " já votou.");
    }
}
