package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import modelo.Candidato;
import modelo.Eleitor;
import modelo.TipoVoto;
import modelo.Urna;
import modelo.Voto;

public class VotoDAO {

    /**
     * RN.5: grava o voto e marca ja_votou = 1 na MESMA transação (RN.3 e RN.6 também são conferidas no banco).
     * Se qualquer passo falhar, faz rollback e nada fica gravado.
     * A tabela voto não recebe nenhuma informação do eleitor (RN.2): o eleitor só é usado
     * para marcar o ja_votou dele.
     */
    public void gravar(Urna urna, Voto voto, Eleitor eleitor) throws DaoException {
        Connection c = Conexao.abrir();
        boolean sucesso = false;
        try {
            c.setAutoCommit(false);

            // 1) insere o voto (candidato_id fica vazio em voto branco ou nulo).
            //    O próprio SELECT confere no banco que a urna ainda está aberta (RN.3) e,
            //    no voto válido, que o candidato está ligado à urna em urna_candidato (RN.6).
            if (voto.getTipo() == TipoVoto.VALIDO) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO voto (urna_id, tipo, candidato_id) "
                        + "SELECT u.id, 'VALIDO', ca.id FROM urna u "
                        + "JOIN urna_candidato uc ON uc.urna_id = u.id "
                        + "JOIN candidato ca ON ca.id = uc.candidato_id "
                        + "WHERE u.id = ? AND u.encerrada = 0 AND ca.numero = ?")) {
                    ps.setInt(1, urna.getId());
                    ps.setInt(2, voto.getCandidato().getNumero());
                    if (ps.executeUpdate() != 1) {
                        throw new DaoException("Não foi possível registrar o voto: a urna está "
                                + "encerrada ou o candidato não pertence a ela. Nada foi gravado.");
                    }
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO voto (urna_id, tipo, candidato_id) "
                        + "SELECT id, ?, NULL FROM urna WHERE id = ? AND encerrada = 0")) {
                    ps.setString(1, voto.getTipo().name());
                    ps.setInt(2, urna.getId());
                    if (ps.executeUpdate() != 1) {
                        throw new DaoException("Não foi possível registrar o voto: a urna está "
                                + "encerrada ou não existe. Nada foi gravado.");
                    }
                }
            }

            // 2) marca ja_votou = 1; se não alterar exatamente uma linha, desfaz tudo
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE eleitor SET ja_votou = 1 WHERE titulo = ? AND ja_votou = 0")) {
                ps.setString(1, eleitor.getTituloEleitor());
                if (ps.executeUpdate() != 1) {
                    throw new DaoException("Não foi possível registrar o voto: o eleitor "
                            + "não está cadastrado ou já votou. Nada foi gravado.");
                }
            }

            c.commit();
            sucesso = true;
        } catch (SQLException e) {
            throw new DaoException("Erro ao gravar o voto. Nada foi gravado.", e);
        } finally {
            Conexao.finalizar(c, sucesso);
        }
    }

    /** Votos válidos de cada candidato da urna (COUNT + GROUP BY). Quem não teve voto aparece com 0. */
    public Map<Candidato, Integer> contarPorCandidato(Urna urna) throws DaoException {
        Map<Candidato, Integer> resultado = new LinkedHashMap<>();
        for (Candidato candidato : urna.getCandidatos()) {
            resultado.put(candidato, 0);
        }
        String sql = "SELECT c.numero AS numero, COUNT(*) AS total FROM voto v "
                   + "JOIN candidato c ON c.id = v.candidato_id "
                   + "WHERE v.urna_id = ? AND v.tipo = 'VALIDO' GROUP BY c.numero";
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, urna.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Candidato candidato = urna.buscarCandidato(rs.getInt("numero"));
                    if (candidato != null) {
                        resultado.put(candidato, rs.getInt("total"));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Erro ao contar os votos.", e);
        }
        return resultado;
    }

    public int contar(Urna urna, TipoVoto tipo) throws DaoException {
        return contarVotos("SELECT COUNT(*) FROM voto WHERE urna_id = ? AND tipo = ?",
                urna.getId(), tipo.name());
    }

    public int contarTotal(Urna urna) throws DaoException {
        return contarVotos("SELECT COUNT(*) FROM voto WHERE urna_id = ?", urna.getId(), null);
    }

    private int contarVotos(String sql, int urnaId, String tipo) throws DaoException {
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, urnaId);
            if (tipo != null) {
                ps.setString(2, tipo);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DaoException("Erro ao contar os votos.", e);
        }
    }
}
