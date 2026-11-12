package app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import modelo.Candidato;
import modelo.CandidatoInvalidoException;
import modelo.Eleitor;
import modelo.EleitorJaVotouException;
import modelo.EleitorNaoEncontradoException;
import modelo.Urna;
import modelo.UrnaEncerradaException;

/** Demonstracao automatica: nao pede nada no teclado. */
public class Demo {

    public static void main(String[] args) {
        Candidato ana = new Candidato("Ana Souza", "111.111.111-11", "1001", 10, "Prefeito", "Partido X", "ana.png");
        Candidato bruno = new Candidato("Bruno Lima", "222.222.222-22", "1002", 20, "Prefeito", "Partido Y", "bruno.png");
        Candidato carla = new Candidato("Carla Dias", "333.333.333-33", "1003", 30, "Prefeito", "Partido Z", "carla.png");

        List<Candidato> candidatos = new ArrayList<>();
        candidatos.add(ana);
        candidatos.add(bruno);
        candidatos.add(carla);

        // Liskov: um Candidato entra onde se espera um Eleitor
        List<Eleitor> eleitores = new ArrayList<>();
        eleitores.addAll(candidatos);
        eleitores.add(new Eleitor("Diego Alves", "444.444.444-44", "2001"));
        eleitores.add(new Eleitor("Elisa Rocha", "555.555.555-55", "2002"));
        eleitores.add(new Eleitor("Fábio Costa", "666.666.666-66", "2003"));

        Urna urna = new Urna(1, "001", candidatos);

        System.out.println("=== DEMONSTRAÇÃO - Urna da seção " + urna.getSecao() + " ===");
        System.out.println("Cadastro (polimorfismo: getPapel() muda conforme o objeto):");
        for (Eleitor e : eleitores) {
            System.out.println("[" + e.getPapel() + "] " + e);
        }

        System.out.println("--- Votação (o voto é secreto: só aparece quem votou, nunca em quem) ---");
        try {
            Main.buscarEleitor(eleitores, "2001").votar(urna, 20);       // voto valido
            System.out.println("OK: Diego Alves votou.");
            Main.buscarEleitor(eleitores, "2002").votarBranco(urna);     // voto em branco
            System.out.println("OK: Elisa Rocha votou.");
            Main.buscarEleitor(eleitores, "2003").votar(urna, 20);       // voto valido
            System.out.println("OK: Fábio Costa votou.");
            Main.buscarEleitor(eleitores, "1001").votar(urna, 99);       // numero inexistente: nulo
            System.out.println("OK: Ana Souza votou.");
        } catch (EleitorJaVotouException | UrnaEncerradaException | EleitorNaoEncontradoException e) {
            System.out.println("ERRO inesperado: " + e.getMessage());
        }

        System.out.println("--- Exceções tratadas ---");
        try {
            Main.buscarEleitor(eleitores, "2001").votar(urna, 10);       // Diego de novo
        } catch (EleitorJaVotouException | UrnaEncerradaException | EleitorNaoEncontradoException e) {
            System.out.println("ERRO (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
        try {
            Main.buscarEleitor(eleitores, "9999").votar(urna, 10);       // titulo inexistente
        } catch (EleitorJaVotouException | UrnaEncerradaException | EleitorNaoEncontradoException e) {
            System.out.println("ERRO (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
        try {
            Candidato forasteiro = new Candidato("Gustavo Reis", "777.777.777-77", "3001", 40, "Prefeito", "Partido W", "gustavo.png");
            carla.votar(urna, forasteiro);                                // candidato de fora da urna
        } catch (EleitorJaVotouException | UrnaEncerradaException | CandidatoInvalidoException e) {
            System.out.println("ERRO (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
        try {
            // o voto da Carla foi recusado, entao o jaVotou dela continua falso (RN.5)
            carla.votar(urna, ana);
            System.out.println("OK: Carla Dias votou.");
        } catch (EleitorJaVotouException | UrnaEncerradaException | CandidatoInvalidoException e) {
            System.out.println("ERRO inesperado: " + e.getMessage());
        }

        urna.encerrar();
        System.out.println("--- Apuração (urna encerrada) ---");
        Map<Candidato, Integer> resultado = urna.contabilizarVotos();
        int votosCandidatos = 0;
        for (Map.Entry<Candidato, Integer> entrada : resultado.entrySet()) {
            Candidato c = entrada.getKey();
            System.out.println(c.getNome() + " (" + c.getNumero() + "): " + entrada.getValue());
            votosCandidatos += entrada.getValue();
        }
        System.out.println("Brancos: " + urna.contarBrancos());
        System.out.println("Nulos: " + urna.contarNulos());
        System.out.println("Total de votos: " + urna.getTotalVotos());
        System.out.println("Conferência: " + votosCandidatos + " + " + urna.contarBrancos()
                + " + " + urna.contarNulos() + " = " + urna.getTotalVotos());

        System.out.println("--- Tentativa depois de encerrar ---");
        try {
            Eleitor atrasado = new Eleitor("Hugo Pires", "888.888.888-88", "2004");
            atrasado.votar(urna, 10);
        } catch (EleitorJaVotouException | UrnaEncerradaException e) {
            System.out.println("ERRO (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }
}
