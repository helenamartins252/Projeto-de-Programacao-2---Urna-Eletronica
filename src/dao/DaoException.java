package dao;

/** Exceção do próprio sistema: esconde a SQLException para a tela não saber nada de SQL. */
public class DaoException extends Exception {
    public DaoException(String mensagem) {
        super(mensagem);
    }

    public DaoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
