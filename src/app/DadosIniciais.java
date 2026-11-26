package app;

import java.util.List;

import dao.CandidatoDAO;
import dao.DaoException;
import dao.EleitorDAO;
import dao.UrnaDAO;
import modelo.Candidato;
import modelo.Eleitor;
import modelo.EleitorNaoEncontradoException;

/** Dados de exemplo (menu 8): 3 candidatos, 4 eleitores e a urna da seção 001. */
public class DadosIniciais {

    public static void carregar(EleitorDAO eleitorDAO, CandidatoDAO candidatoDAO, UrnaDAO urnaDAO)
            throws DaoException {
        if (jaCarregados(eleitorDAO)) {
            System.out.println("Os dados de exemplo já estão no banco. Nada foi alterado.");
            return;
        }

        Candidato ana = new Candidato("Ana Souza", "11111111111", "1001", 10, "Prefeito", "Partido X", "ana.png");
        Candidato bruno = new Candidato("Bruno Lima", "22222222222", "1002", 20, "Prefeito", "Partido Y", "bruno.png");
        Candidato carla = new Candidato("Carla Dias", "33333333333", "1003", 30, "Prefeito", "Partido Z", "carla.png");
        candidatoDAO.inserir(ana);
        candidatoDAO.inserir(bruno);
        candidatoDAO.inserir(carla);

        eleitorDAO.inserir(new Eleitor("Diego Alves", "44444444444", "2001"));
        eleitorDAO.inserir(new Eleitor("Elisa Rocha", "55555555555", "2002"));
        eleitorDAO.inserir(new Eleitor("Fábio Costa", "66666666666", "2003"));
        eleitorDAO.inserir(new Eleitor("Gabriela Nunes", "77777777777", "2004"));

        urnaDAO.inserir("001", List.of(ana, bruno, carla));

        System.out.println("Dados de exemplo carregados:");
        System.out.println("  Candidatos: Ana Souza (10), Bruno Lima (20), Carla Dias (30) - títulos 1001, 1002, 1003");
        System.out.println("  Eleitores: Diego Alves (2001), Elisa Rocha (2002), Fábio Costa (2003), Gabriela Nunes (2004)");
        System.out.println("  Urna 1, seção 001, com os três candidatos.");
    }

    private static boolean jaCarregados(EleitorDAO eleitorDAO) throws DaoException {
        try {
            eleitorDAO.buscar("1001");
            return true;
        } catch (EleitorNaoEncontradoException e) {
            return false;
        }
    }
}
