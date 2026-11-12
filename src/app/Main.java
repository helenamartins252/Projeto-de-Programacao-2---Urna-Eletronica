package app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import modelo.Candidato;
import modelo.Eleitor;
import modelo.EleitorJaVotouException;
import modelo.EleitorNaoEncontradoException;
import modelo.TipoVoto;
import modelo.Urna;
import modelo.UrnaEncerradaException;

/** Versao interativa: menu no console (votar, encerrar e apurar). */
public class Main {

    public static void main(String[] args) {
        Candidato ana = new Candidato("Ana Souza", "111.111.111-11", "1001", 10, "Prefeito", "Partido X", "ana.png");
        Candidato bruno = new Candidato("Bruno Lima", "222.222.222-22", "1002", 20, "Prefeito", "Partido Y", "bruno.png");
        Candidato carla = new Candidato("Carla Dias", "333.333.333-33", "1003", 30, "Prefeito", "Partido Z", "carla.png");

        List<Candidato> candidatos = new ArrayList<>();
        candidatos.add(ana);
        candidatos.add(bruno);
        candidatos.add(carla);

        List<Eleitor> eleitores = new ArrayList<>();
        eleitores.addAll(candidatos);
        eleitores.add(new Eleitor("Diego Alves", "444.444.444-44", "2001"));
        eleitores.add(new Eleitor("Elisa Rocha", "555.555.555-55", "2002"));
        eleitores.add(new Eleitor("Fábio Costa", "666.666.666-66", "2003"));

        Urna urna = new Urna(1, "001", candidatos);
        Scanner teclado = new Scanner(System.in);
        int opcao = -1;

        while (opcao != 0) {
            System.out.println();
            System.out.println("=== URNA ELETRÔNICA - seção " + urna.getSecao() + " ===");
            System.out.println("1 - Votar");
            System.out.println("2 - Encerrar urna");
            System.out.println("3 - Apurar");
            System.out.println("0 - Sair");
            System.out.print("Opção: ");
            try {
                opcao = Integer.parseInt(teclado.nextLine().trim());
            } catch (NumberFormatException e) {
                opcao = -1;
            }
            switch (opcao) {
                case 1 -> votar(teclado, urna, eleitores);
                case 2 -> {
                    urna.encerrar();
                    System.out.println("Urna encerrada.");
                }
                case 3 -> apurar(urna);
                case 0 -> System.out.println("Até logo.");
                default -> System.out.println("Opção inválida.");
            }
        }
    }

    /** Procura o eleitor pelo titulo ou CPF. */
    public static Eleitor buscarEleitor(List<Eleitor> eleitores, String identificacao)
            throws EleitorNaoEncontradoException {
        for (Eleitor e : eleitores) {
            if (e.getTituloEleitor().equals(identificacao) || e.getCpf().equals(identificacao)) {
                return e;
            }
        }
        throw new EleitorNaoEncontradoException(identificacao);
    }

    private static void votar(Scanner teclado, Urna urna, List<Eleitor> eleitores) {
        try {
            System.out.print("Título de eleitor ou CPF: ");
            Eleitor eleitor = buscarEleitor(eleitores, teclado.nextLine().trim());

            System.out.println("Candidatos:");
            for (Candidato c : urna.getCandidatos()) {
                System.out.println("  " + c.getNumero() + " - " + c.getNome() + " (" + c.getPartido() + ")");
            }
            System.out.print("Digite o número do candidato (ou B para branco): ");
            String entrada = teclado.nextLine().trim();

            if (entrada.equalsIgnoreCase("B")) {
                eleitor.votarBranco(urna);
                System.out.println("Voto em branco registrado. FIM.");
            } else {
                TipoVoto tipo = eleitor.votar(urna, Integer.parseInt(entrada));
                if (tipo == TipoVoto.NULO) {
                    System.out.println("Número inexistente: o voto foi registrado como nulo. FIM.");
                } else {
                    System.out.println("Voto registrado. FIM.");
                }
            }
        } catch (EleitorNaoEncontradoException | EleitorJaVotouException | UrnaEncerradaException e) {
            System.out.println("ERRO: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("ERRO: digite apenas números (ou B para branco).");
        }
    }

    private static void apurar(Urna urna) {
        if (!urna.isEncerrada()) {
            System.out.println("A apuração só fica disponível com a urna encerrada.");
            return;
        }
        System.out.println("--- Apuração ---");
        for (Map.Entry<Candidato, Integer> e : urna.contabilizarVotos().entrySet()) {
            System.out.println(e.getKey().getNome() + " (" + e.getKey().getNumero() + "): " + e.getValue());
        }
        System.out.println("Brancos: " + urna.contarBrancos());
        System.out.println("Nulos: " + urna.contarNulos());
        System.out.println("Total de votos: " + urna.getTotalVotos());
    }
}
