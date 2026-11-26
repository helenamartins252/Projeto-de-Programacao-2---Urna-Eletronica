package modelo;

/** Lançada quando o eleitor tenta votar de novo (RN.1). */
public class EleitorJaVotouException extends Exception {
    public EleitorJaVotouException(String nomeEleitor) {
        super(nomeEleitor + " já votou.");
    }
}
