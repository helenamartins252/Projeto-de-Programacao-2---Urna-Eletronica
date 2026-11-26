package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import modelo.Candidato;
import modelo.Eleitor;

public class CandidatoDAO {

    /**
     * Todo candidato é eleitor: grava em eleitor e em candidato na mesma transação.
     * Se uma das gravações falhar, a outra é desfeita.
     */
    public void inserir(Candidato candidato) throws DaoException {
        Connection c = Conexao.abrir();
        boolean sucesso = false;
        try {
            c.setAutoCommit(false);

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO eleitor (nome, cpf, titulo, ja_votou) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, candidato.getNome());
                ps.setString(2, Conexao.soDigitos(candidato.getCpf()));
                ps.setString(3, candidato.getTituloEleitor());
                ps.setInt(4, candidato.jaVotou() ? 1 : 0);
                ps.executeUpdate();
            }

            long eleitorId;
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                rs.next();
                eleitorId = rs.getLong(1);
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO candidato (eleitor_id, numero, cargo, partido, foto) "
                    + "VALUES (?, ?, ?, ?, ?)")) {
                ps.setLong(1, eleitorId);
                ps.setInt(2, candidato.getNumero());
                ps.setString(3, candidato.getCargo());
                ps.setString(4, candidato.getPartido());
                ps.setString(5, candidato.getFoto());
                ps.executeUpdate();
            }

            c.commit();
            sucesso = true;
        } catch (SQLException e) {
            throw Conexao.traduzir(e, "Erro ao gravar o candidato.");
        } finally {
            Conexao.finalizar(c, sucesso);
        }
    }

    public Candidato buscarPorNumero(int numero) throws DaoException {
        String sql = "SELECT " + EleitorDAO.COLUNAS + " FROM candidato c "
                   + "JOIN eleitor e ON e.id = c.eleitor_id WHERE c.numero = ?";
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, numero);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return (Candidato) EleitorDAO.montar(rs);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Erro ao buscar o candidato.", e);
        }
        return null;
    }

    public List<Candidato> listar() throws DaoException {
        String sql = "SELECT " + EleitorDAO.COLUNAS + " FROM candidato c "
                   + "JOIN eleitor e ON e.id = c.eleitor_id ORDER BY c.numero";
        List<Candidato> lista = new ArrayList<>();
        try (Connection c = Conexao.abrir();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Eleitor e = EleitorDAO.montar(rs);
                lista.add((Candidato) e);
            }
        } catch (SQLException e) {
            throw new DaoException("Erro ao listar os candidatos.", e);
        }
        return lista;
    }
}
