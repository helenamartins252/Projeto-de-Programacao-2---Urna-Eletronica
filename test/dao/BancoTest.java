package dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import modelo.Candidato;
import modelo.Eleitor;
import modelo.EleitorNaoEncontradoException;
import modelo.TipoVoto;
import modelo.Urna;

/** Testes da persistência: cada teste usa um banco SQLite temporário, separado do urna.db. */
class BancoTest {
    @TempDir
    Path pasta;

    private String urlAnterior;
    private EleitorDAO eleitorDAO;
    private CandidatoDAO candidatoDAO;
    private UrnaDAO urnaDAO;
    private VotoDAO votoDAO;
    private BoletimDAO boletimDAO;
    private Urna urnaCriada;

    @BeforeEach
    void preparar() throws Exception {
        urlAnterior = Conexao.getUrl();
        Conexao.setUrl("jdbc:sqlite:" + pasta.resolve("teste.db"));
        Conexao.criarTabelas();

        eleitorDAO = new EleitorDAO();
        candidatoDAO = new CandidatoDAO();
        urnaDAO = new UrnaDAO();
        votoDAO = new VotoDAO();
        boletimDAO = new BoletimDAO();

        Candidato ana = new Candidato("Ana Souza", "11111111111", "1001", 10, "Prefeito", "Partido X", "");
        Candidato bruno = new Candidato("Bruno Lima", "22222222222", "1002", 20, "Prefeito", "Partido Y", "");
        Candidato carla = new Candidato("Carla Dias", "33333333333", "1003", 30, "Prefeito", "Partido Z", "");
        candidatoDAO.inserir(ana);
        candidatoDAO.inserir(bruno);
        candidatoDAO.inserir(carla); // cadastrada, mas fora da urna
        eleitorDAO.inserir(new Eleitor("Diego Alves", "44444444444", "2001"));
        urnaCriada = urnaDAO.inserir("001", List.of(ana, bruno));
    }

    @AfterEach
    void restaurar() {
        Conexao.setUrl(urlAnterior);
    }

    private Urna urna() throws Exception {
        return urnaDAO.buscar(urnaCriada.getId());
    }

    @Test
    @DisplayName("criarTabelas() chamado de novo não apaga os dados")
    void criarTabelasDeNovoNaoApagaDados() throws Exception {
        Conexao.criarTabelas();
        assertEquals("Diego Alves", eleitorDAO.buscar("2001").getNome());
    }

    @Test
    @DisplayName("RN.2: a tabela voto não tem coluna ligada ao eleitor")
    void tabelaVotoNaoTemColunaDoEleitor() throws Exception {
        Set<String> colunas = new HashSet<>();
        try (Connection c = Conexao.abrir();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(voto)")) {
            while (rs.next()) {
                colunas.add(rs.getString("name"));
            }
        }
        assertEquals(Set.of("id", "urna_id", "tipo", "candidato_id"), colunas);
    }

    @Test
    @DisplayName("RN.5: voto e ja_votou são gravados juntos e continuam lá depois de reabrir")
    void gravarVotoMarcaJaVotou() throws Exception {
        Eleitor diego = eleitorDAO.buscar("2001");
        Urna urna = urna();
        diego.votar(urna, 20);
        votoDAO.gravar(urna, urna.getVotos().get(0), diego);

        // "reabrir o programa": objetos e DAOs novos, só o arquivo do banco continua
        EleitorDAO novoEleitorDAO = new EleitorDAO();
        VotoDAO novoVotoDAO = new VotoDAO();
        Urna relida = urna();

        assertTrue(novoEleitorDAO.buscar("2001").jaVotou());
        assertEquals(1, novoVotoDAO.contarTotal(relida));
        Map<Candidato, Integer> porCandidato = novoVotoDAO.contarPorCandidato(relida);
        assertEquals(1, porCandidato.get(relida.buscarCandidato(20)));
        assertEquals(0, porCandidato.get(relida.buscarCandidato(10)));
    }

    @Test
    @DisplayName("RF.4: voto em branco e voto nulo são gravados sem candidato")
    void gravarBrancoENulo() throws Exception {
        eleitorDAO.inserir(new Eleitor("Elisa Rocha", "55555555555", "2002"));
        eleitorDAO.inserir(new Eleitor("Fábio Costa", "66666666666", "2003"));

        Eleitor elisa = eleitorDAO.buscar("2002");
        Urna u1 = urna();
        elisa.votarBranco(u1);
        votoDAO.gravar(u1, u1.getVotos().get(0), elisa);

        Eleitor fabio = eleitorDAO.buscar("2003");
        Urna u2 = urna();
        assertEquals(TipoVoto.NULO, fabio.votar(u2, 99));
        votoDAO.gravar(u2, u2.getVotos().get(0), fabio);

        Urna relida = urna();
        assertEquals(1, votoDAO.contar(relida, TipoVoto.BRANCO));
        assertEquals(1, votoDAO.contar(relida, TipoVoto.NULO));
        assertEquals(2, votoDAO.contarTotal(relida));
    }

    @Test
    @DisplayName("RN.5: se o ja_votou não puder ser marcado, o voto também não fica gravado (rollback)")
    void rollbackQuandoEleitorJaVotouNoBanco() throws Exception {
        Eleitor diego = eleitorDAO.buscar("2001");   // em memória: ainda não votou
        try (Connection c = Conexao.abrir(); Statement st = c.createStatement()) {
            st.executeUpdate("UPDATE eleitor SET ja_votou = 1 WHERE titulo = '2001'"); // outro acesso votou antes
        }

        Urna urna = urna();
        diego.votar(urna, 20);                         // o modelo em memória aceita
        assertThrows(DaoException.class, () -> votoDAO.gravar(urna, urna.getVotos().get(0), diego));

        assertEquals(0, votoDAO.contarTotal(urna()));  // o INSERT do voto foi desfeito
    }

    @Test
    @DisplayName("RN.3: se a urna foi encerrada no banco, o voto não é gravado e o ja_votou continua 0")
    void bancoRecusaVotoEmUrnaEncerrada() throws Exception {
        Eleitor diego = eleitorDAO.buscar("2001");
        Urna urna = urna();                            // lida ainda aberta
        diego.votar(urna, 20);                         // o modelo em memória aceita
        urnaDAO.encerrar(urnaCriada.getId());          // o mesário encerra antes da gravação

        assertThrows(DaoException.class, () -> votoDAO.gravar(urna, urna.getVotos().get(0), diego));

        assertEquals(0, votoDAO.contarTotal(urna()));
        assertFalse(eleitorDAO.buscar("2001").jaVotou());
    }

    @Test
    @DisplayName("RN.6: o banco recusa voto em candidato que não está ligado à urna")
    void bancoRecusaCandidatoForaDaUrna() throws Exception {
        Candidato carla = candidatoDAO.buscarPorNumero(30);   // cadastrada, mas fora da urna
        Urna comCarla = new Urna(urnaCriada.getId(), "001", List.of(carla)); // urna adulterada em memória
        Eleitor diego = eleitorDAO.buscar("2001");
        diego.votar(comCarla, 30);                     // a memória aceita, o banco não

        assertThrows(DaoException.class,
                () -> votoDAO.gravar(comCarla, comCarla.getVotos().get(0), diego));

        assertEquals(0, votoDAO.contarTotal(urna()));
        assertFalse(eleitorDAO.buscar("2001").jaVotou());
    }

    @Test
    @DisplayName("RN.6: título ou CPF não cadastrado lança EleitorNaoEncontradoException")
    void eleitorNaoCadastrado() {
        assertThrows(EleitorNaoEncontradoException.class, () -> eleitorDAO.buscar("9999"));
        assertThrows(EleitorNaoEncontradoException.class, () -> eleitorDAO.buscar("   "));
    }

    @Test
    @DisplayName("A busca aceita o CPF com pontos e traço")
    void buscaPorCpfComPontuacao() throws Exception {
        assertEquals("Diego Alves", eleitorDAO.buscar("444.444.444-44").getNome());
    }

    @Test
    @DisplayName("Polimorfismo na leitura: candidato volta como Candidato e eleitor comum como Eleitor")
    void candidatoVoltaComoCandidato() throws Exception {
        Eleitor ana = eleitorDAO.buscar("1001");
        Eleitor diego = eleitorDAO.buscar("2001");

        assertInstanceOf(Candidato.class, ana);
        assertEquals("Candidato", ana.getPapel());
        assertEquals(Eleitor.class, diego.getClass());
        assertEquals("Eleitor", diego.getPapel());
    }

    @Test
    @DisplayName("RN.6: a urna só traz os candidatos ligados a ela")
    void urnaSoTemSeusCandidatos() throws Exception {
        Urna urna = urna();
        assertEquals(2, urna.getCandidatos().size());
        assertNull(urna.buscarCandidato(30)); // Carla existe, mas não concorre nesta urna
        assertEquals(3, candidatoDAO.listar().size());
    }

    @Test
    @DisplayName("O estado encerrada da urna fica gravado")
    void encerramentoPersiste() throws Exception {
        assertFalse(urna().isEncerrada());
        urnaDAO.encerrar(urnaCriada.getId());
        assertTrue(urna().isEncerrada());
    }

    @Test
    @DisplayName("RF.6: o boletim só é gerado com a urna encerrada e traz o total de votos")
    void boletimSoComUrnaEncerrada() throws Exception {
        Eleitor diego = eleitorDAO.buscar("2001");
        Urna urna = urna();
        diego.votar(urna, 10);
        votoDAO.gravar(urna, urna.getVotos().get(0), diego);

        assertThrows(DaoException.class, () -> boletimDAO.gerar(urna(), pasta));

        urnaDAO.encerrar(urnaCriada.getId());
        Path arquivo = boletimDAO.gerar(urna(), pasta);

        assertEquals("boletim_urna_" + urnaCriada.getId() + ".txt", arquivo.getFileName().toString());
        String texto = Files.readString(arquivo);
        assertTrue(texto.contains("Total de votos: 1"));
        assertTrue(texto.contains("Ana Souza"));
        assertTrue(texto.contains("Votos em branco: 0"));
        assertFalse(texto.contains("Diego"), "O boletim não pode citar eleitores");
    }

    @Test
    @DisplayName("Número de candidato repetido é recusado e o eleitor correspondente também é desfeito")
    void candidatoRepetidoFazRollback() {
        Candidato repetido = new Candidato("Outro Nome", "88888888888", "1008", 10, "Prefeito", "Partido K", "");

        DaoException erro = assertThrows(DaoException.class, () -> candidatoDAO.inserir(repetido));

        assertTrue(erro.getMessage().contains("número"));
        assertThrows(EleitorNaoEncontradoException.class, () -> eleitorDAO.buscar("1008"));
    }

    @Test
    @DisplayName("CPF repetido é recusado com mensagem clara")
    void cpfRepetido() {
        Eleitor repetido = new Eleitor("Outro", "44444444444", "2999");
        DaoException erro = assertThrows(DaoException.class, () -> eleitorDAO.inserir(repetido));
        assertTrue(erro.getMessage().contains("CPF"));
    }
}
