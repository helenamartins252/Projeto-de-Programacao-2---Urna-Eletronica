package modelo;

/** Lancada quando alguem tenta votar numa urna encerrada (RN.3). */
public class UrnaEncerradaException extends Exception {
    public UrnaEncerradaException(String secao) {
        super("A urna da seção " + secao + " está encerrada.");
    }
}
