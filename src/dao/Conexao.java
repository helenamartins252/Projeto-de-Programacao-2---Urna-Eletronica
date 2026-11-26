package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Abre conexões com o SQLite (arquivo urna.db) e cria as tabelas na primeira execução. */
public final class Conexao {
    private static String url = "jdbc:sqlite:urna.db";

    private Conexao() {
    }

    /** Usado nos testes, para apontar para um banco temporário. */
    public static void setUrl(String novaUrl) {
        url = novaUrl;
    }

    public static String getUrl() {
        return url;
    }

    /** Abre uma conexão já com as chaves estrangeiras ligadas (no SQLite elas vêm desligadas). */
    public static Connection abrir() throws DaoException {
        Connection c = null;
        try {
            c = DriverManager.getConnection(url);
            try (Statement st = c.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
            return c;
        } catch (SQLException e) {
            fechar(c);
            throw new DaoException("Não foi possível abrir o banco de dados.", e);
        }
    }

    /** CREATE TABLE IF NOT EXISTS: se o banco já existe, nada é recriado e os dados continuam lá. */
    public static void criarTabelas() throws DaoException {
        String[] comandos = {
            "CREATE TABLE IF NOT EXISTS eleitor ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "nome TEXT NOT NULL, "
                + "cpf TEXT NOT NULL UNIQUE, "
                + "titulo TEXT NOT NULL UNIQUE, "
                + "ja_votou INTEGER NOT NULL DEFAULT 0 CHECK (ja_votou IN (0, 1)))",

            "CREATE TABLE IF NOT EXISTS candidato ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "eleitor_id INTEGER NOT NULL UNIQUE REFERENCES eleitor(id), "
                + "numero INTEGER NOT NULL UNIQUE, "
                + "cargo TEXT NOT NULL, "
                + "partido TEXT NOT NULL, "
                + "foto TEXT)",

            "CREATE TABLE IF NOT EXISTS urna ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "secao TEXT NOT NULL, "
                + "encerrada INTEGER NOT NULL DEFAULT 0 CHECK (encerrada IN (0, 1)))",

            "CREATE TABLE IF NOT EXISTS urna_candidato ("
                + "urna_id INTEGER NOT NULL REFERENCES urna(id), "
                + "candidato_id INTEGER NOT NULL REFERENCES candidato(id), "
                + "PRIMARY KEY (urna_id, candidato_id))",

            // Sem nenhuma coluna ligada ao eleitor: voto secreto (RN.2).
            "CREATE TABLE IF NOT EXISTS voto ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "urna_id INTEGER NOT NULL REFERENCES urna(id), "
                + "tipo TEXT NOT NULL CHECK (tipo IN ('VALIDO', 'BRANCO', 'NULO')), "
                + "candidato_id INTEGER REFERENCES candidato(id), "
                + "CHECK ((tipo = 'VALIDO' AND candidato_id IS NOT NULL) "
                + "OR (tipo <> 'VALIDO' AND candidato_id IS NULL)))"
        };
        try (Connection c = abrir(); Statement st = c.createStatement()) {
            for (String sql : comandos) {
                st.execute(sql);
            }
        } catch (SQLException e) {
            throw new DaoException("Não foi possível criar as tabelas do banco.", e);
        }
    }

    /** Fecha a conexão; se a operação não deu certo, desfaz a transação antes (rollback). */
    static void finalizar(Connection c, boolean sucesso) {
        if (c == null) {
            return;
        }
        if (!sucesso) {
            try {
                c.rollback();
            } catch (SQLException ignorada) {
                // sem o que fazer: a conexão será fechada e o SQLite descarta a transação aberta
            }
        }
        fechar(c);
    }

    static void fechar(Connection c) {
        if (c != null) {
            try {
                c.close();
            } catch (SQLException ignorada) {
                // nada a fazer
            }
        }
    }

    /** Transforma erro de campo repetido (UNIQUE) numa mensagem que o usuário entende. */
    static DaoException traduzir(SQLException e, String mensagemPadrao) {
        String msg = (e.getMessage() == null) ? "" : e.getMessage();
        if (msg.contains("UNIQUE constraint failed")) {
            if (msg.contains("eleitor.cpf")) {
                return new DaoException("Já existe um eleitor cadastrado com esse CPF.", e);
            }
            if (msg.contains("eleitor.titulo")) {
                return new DaoException("Já existe um eleitor cadastrado com esse título.", e);
            }
            if (msg.contains("candidato.numero")) {
                return new DaoException("Já existe um candidato com esse número.", e);
            }
            return new DaoException("Esse registro já existe no banco.", e);
        }
        return new DaoException(mensagemPadrao, e);
    }

    /** Deixa só os dígitos (CPF digitado com ponto e traço vira só números). */
    static String soDigitos(String texto) {
        return (texto == null) ? "" : texto.replaceAll("\\D", "");
    }
}
