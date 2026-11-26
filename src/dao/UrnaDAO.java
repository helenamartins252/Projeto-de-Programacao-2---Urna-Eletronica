package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import modelo.Candidato;
import modelo.Urna;

public class UrnaDAO {

    /** Grava a urna e a lista de candidatos que concorrem nela (urna_candidato), numa transação. */
    public Urna inserir(String secao, List<Candidato> candidatos) throws DaoException {
        Connection c = Conexao.abrir();
        boolean sucesso = false;
        try {
            c.setAutoCommit(false);

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO urna (secao, encerrada) VALUES (?, 0)")) {
                ps.setString(1, secao);
                ps.executeUpdate();
            }

            int urnaId;
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                rs.next();
                urnaId = rs.getInt(1);
            }

            for (Candidato candidato : candidatos) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO urna_candidato (urna_id, candidato_id) "
                        + "SELECT ?, id FROM candidato WHERE numero = ?")) {
                    ps.setInt(1, urnaId);
                    ps.setInt(2, candidato.getNumero());
                    if (ps.executeUpdate() != 1) {
                        throw new DaoException("O candidato nº " + candidato.getNumero()
                                + " não está cadastrado.");
                    }
                }
            }

            c.commit();
            sucesso = true;
            return new Urna(urnaId, secao, candidatos);
        } catch (SQLException e) {
            throw Conexao.traduzir(e, "Erro ao gravar a urna.");
        } finally {
            Conexao.finalizar(c, sucesso);
        }
    }

    /**
     * Lê a urna do banco com o estado de encerrada e só os candidatos ligados a ela (RN.6).
     * Os votos antigos não são carregados: a contagem vem de consultas SQL (VotoDAO).
     */
    public Urna buscar(int id) throws DaoException {
        String secao;
        boolean encerrada;
        try (Connection c = Conexao.abrir()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT secao, encerrada FROM urna WHERE id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new DaoException("Urna nº " + id + " não encontrada.");
                    }
                    secao = rs.getString("secao");
                    encerrada = rs.getInt("encerrada") == 1;
                }
            }

            List<Candidato> candidatos = new ArrayList<>();
            String sql = "SELECT " + EleitorDAO.COLUNAS + " FROM urna_candidato uc "
                       + "JOIN candidato c ON c.id = uc.candidato_id "
                       + "JOIN eleitor e ON e.id = c.eleitor_id "
                       + "WHERE uc.urna_id = ? ORDER BY c.numero";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        candidatos.add((Candidato) EleitorDAO.montar(rs));
                    }
                }
            }
            return new Urna(id, secao, candidatos, encerrada);
        } catch (SQLException e) {
            throw new DaoException("Erro ao buscar a urna.", e);
        }
    }

    public List<Urna> listar() throws DaoException {
        List<Integer> ids = new ArrayList<>();
        try (Connection c = Conexao.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT id FROM urna ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            throw new DaoException("Erro ao listar as urnas.", e);
        }
        List<Urna> urnas = new ArrayList<>();
        for (int id : ids) {
            urnas.add(buscar(id));
        }
        return urnas;
    }

    public void encerrar(int id) throws DaoException {
        try (Connection c = Conexao.abrir();
             PreparedStatement ps = c.prepareStatement("UPDATE urna SET encerrada = 1 WHERE id = ?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() != 1) {
                throw new DaoException("Urna nº " + id + " não encontrada.");
            }
        } catch (SQLException e) {
            throw new DaoException("Erro ao encerrar a urna.", e);
        }
    }
}
