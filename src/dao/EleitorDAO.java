package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import modelo.Candidato;
import modelo.Eleitor;
import modelo.EleitorNaoEncontradoException;

public class EleitorDAO {

    /** Colunas lidas em toda consulta que devolve eleitor ou candidato (veja montar). */
    static final String COLUNAS =
        "e.nome AS nome, e.cpf AS cpf, e.titulo AS titulo, e.ja_votou AS ja_votou, "
        + "c.numero AS numero, c.cargo AS cargo, c.partido AS partido, c.foto AS foto";

    public void inserir(Eleitor eleitor) throws DaoException {
        String sql = "INSERT INTO eleitor (nome, cpf, titulo, ja_votou) VALUES (?, ?, ?, ?)";
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, eleitor.getNome());
            ps.setString(2, Conexao.soDigitos(eleitor.getCpf()));
            ps.setString(3, eleitor.getTituloEleitor());
            ps.setInt(4, eleitor.jaVotou() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw Conexao.traduzir(e, "Erro ao gravar o eleitor.");
        }
    }

    /**
     * Procura pelo título ou pelo CPF (RN.6). O LEFT JOIN devolve um Candidato quando existe
     * linha em candidato e um Eleitor comum quando não existe: o polimorfismo continua valendo.
     */
    public Eleitor buscar(String tituloOuCpf) throws EleitorNaoEncontradoException, DaoException {
        String entrada = (tituloOuCpf == null) ? "" : tituloOuCpf.trim();
        if (entrada.isEmpty()) {
            throw new EleitorNaoEncontradoException(entrada);
        }
        String digitos = Conexao.soDigitos(entrada);
        String sql = "SELECT " + COLUNAS + " FROM eleitor e "
                   + "LEFT JOIN candidato c ON c.eleitor_id = e.id "
                   + "WHERE e.titulo = ? OR e.cpf = ? LIMIT 1";
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, entrada);
            ps.setString(2, digitos.isEmpty() ? entrada : digitos);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return montar(rs);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Erro ao buscar o eleitor.", e);
        }
        throw new EleitorNaoEncontradoException(entrada);
    }

    /** Monta um Candidato (se a linha tem numero) ou um Eleitor, já com o jaVotou gravado. */
    static Eleitor montar(ResultSet rs) throws SQLException {
        String nome = rs.getString("nome");
        String cpf = rs.getString("cpf");
        String titulo = rs.getString("titulo");
        boolean jaVotou = rs.getInt("ja_votou") == 1;
        int numero = rs.getInt("numero");
        if (rs.wasNull()) {
            return new Eleitor(nome, cpf, titulo, jaVotou);
        }
        return new Candidato(nome, cpf, titulo, numero,
                rs.getString("cargo"), rs.getString("partido"), rs.getString("foto"), jaVotou);
    }
}
