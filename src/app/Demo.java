package app;

import java.util.ArrayList;
import java.util.List;

import modelo.Candidato;
import modelo.CandidatoInvalidoException;
import modelo.Eleitor;
import modelo.EleitorJaVotouException;
import modelo.EleitorNaoEncontradoException;
import modelo.Urna;
import modelo.UrnaEncerradaException;

/**
 * Demonstração automática (não pede nada no teclado), só em memória, sem banco.
 * O voto é secreto: a saída mostra só quem votou, nunca em quem.
 */
public class Demo {

    public static void main(String[] args) {
        Candidato ana = new Candidato("Ana Souza", "11111111111", "1001", 10, "Prefeito", "Partido X", "");
        Candidato bruno = new Candidato("Bruno Lima", "22222222222", "1002", 20, "Prefeito", "Partido Y", "");
        Candidato carla = new Candidato("Carla Dias", "33333333333", "1003", 30, "Prefeito", "Partido Z", "");
        Candidato foraDaUrna = new Candidato("Zeca Pires", "99999999999", "1009", 90, "Prefeito", "Partido W", "");
        Eleitor diego = new Eleitor("Diego Alves", "44444444444", "2001");
        Eleitor elisa = new Eleitor("Elisa Rocha", "55555555555", "2002");
        Eleitor fabio = new Eleitor("Fábio Costa", "66666666666", "2003");

        Urna urna = new Urna(1, "001", List.of(ana, bruno, carla));

        System.out.println("=== DEMONSTRAÇÃO - Urna da seção " + urna.getSecao() + " ===");
        System.out.println("Cadastro (polimorfismo: getPapel() muda conforme o objeto):");
        List<Eleitor> eleitores = new ArrayList<>();
        eleitores.addAll(List.of(ana, bruno, carla)); // Liskov: Candidato entra onde se espera Eleitor
        eleitores.addAll(List.of(diego, elisa, fabio));
        for (Eleitor e : eleitores) {
            System.out.println("[" + e.getPapel() + "] " + e);
        }

        System.out.println("--- Votação (o voto é secreto: só aparece quem votou, nunca em quem) ---");
        try {
            diego.votar(urna, 20);          // válido (Bruno)
            System.out.println("OK: " + diego.getNome() + " votou.");
            elisa.votarBranco(urna);        // branco
            System.out.println("OK: " + elisa.getNome() + " votou.");
            fabio.votar(urna, 99);          // número que não existe: vira voto nulo
            System.out.println("OK: " + fabio.getNome() + " votou.");
            ana.votar(urna, 10);            // um candidato também vota (é um Eleitor)
            System.out.println("OK: " + ana.getNome() + " votou.");
        } catch (EleitorJaVotouException | UrnaEncerradaException e) {
            System.out.println("ERRO (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }

        System.out.println("--- Exceções tratadas ---");
        try {
            diego.votar(urna, 10);
        } catch (EleitorJaVotouException | UrnaEncerradaException e) {
            System.out.println("ERRO (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
        try {
            buscarEleitor(eleitores, "9999");
        } catch (EleitorNaoEncontradoException e) {
            System.out.println("ERRO (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
        try {
            carla.votar(urna, foraDaUrna);
        } catch (EleitorJaVotouException | UrnaEncerradaException | CandidatoInvalidoException e) {
            System.out.println("ERRO (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
        try {
            carla.votar(urna, 20);          // o erro anterior não marcou o jaVotou: agora vota
            System.out.println("OK: " + carla.getNome() + " votou.");
        } catch (EleitorJaVotouException | UrnaEncerradaException e) {
            System.out.println("ERRO (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }

        urna.encerrar();
        System.out.println("--- Apuração (urna encerrada) ---");
        int validos = 0;
        for (var entrada : urna.contabilizarVotos().entrySet()) {
            Candidato c = entrada.getKey();
            System.out.println(c.getNome() + " (" + c.getNumero() + "): " + entrada.getValue());
            validos += entrada.getValue();
        }
        System.out.println("Brancos: " + urna.contarBrancos());
        System.out.println("Nulos: " + urna.contarNulos());
        System.out.println("Total de votos: " + urna.getTotalVotos());
        System.out.println("Conferência: " + validos + " + " + urna.contarBrancos() + " + "
                + urna.contarNulos() + " = " + (validos + urna.contarBrancos() + urna.contarNulos()));

        System.out.println("--- Tentativa depois de encerrar ---");
        try {
            bruno.votar(urna, 10);
        } catch (EleitorJaVotouException | UrnaEncerradaException e) {
            System.out.println("ERRO (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    /** Na Demo (só em memória) a busca é na própria lista; no Main ela é o EleitorDAO.buscar(). */
    private static Eleitor buscarEleitor(List<Eleitor> eleitores, String tituloOuCpf)
            throws EleitorNaoEncontradoException {
        for (Eleitor e : eleitores) {
            if (e.getTituloEleitor().equals(tituloOuCpf) || e.getCpf().equals(tituloOuCpf)) {
                return e;
            }
        }
        throw new EleitorNaoEncontradoException(tituloOuCpf);
    }
}
