package app;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;

import dao.BoletimDAO;
import dao.CandidatoDAO;
import dao.Conexao;
import dao.DaoException;
import dao.EleitorDAO;
import dao.UrnaDAO;
import dao.VotoDAO;
import modelo.Candidato;
import modelo.CandidatoInvalidoException;
import modelo.Eleitor;
import modelo.EleitorJaVotouException;
import modelo.EleitorNaoEncontradoException;
import modelo.TipoVoto;
import modelo.Urna;
import modelo.UrnaEncerradaException;
import modelo.Voto;

/**
 * Menu de console. Só lê o teclado, chama o modelo e os DAOs e mostra mensagens:
 * as regras ficam no pacote modelo e o SQL fica no pacote dao.
 */
public class Main {
    private static final Scanner teclado = new Scanner(System.in);
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private static final EleitorDAO eleitorDAO = new EleitorDAO();
    private static final CandidatoDAO candidatoDAO = new CandidatoDAO();
    private static final UrnaDAO urnaDAO = new UrnaDAO();
    private static final VotoDAO votoDAO = new VotoDAO();
    private static final BoletimDAO boletimDAO = new BoletimDAO();

    public static void main(String[] args) {
        try {
            Conexao.criarTabelas();
        } catch (DaoException e) {
            System.out.println("ERRO: " + e.getMessage());
            return;
        }

        int opcao = -1;
        try {
            while (opcao != 0) {
                mostrarMenu();
                opcao = lerInteiro("Escolha uma opção: ");
                try {
                    executar(opcao);
                } catch (EleitorNaoEncontradoException | EleitorJaVotouException
                         | UrnaEncerradaException | CandidatoInvalidoException
                         | DaoException | IllegalArgumentException e) {
                    System.out.println("ERRO (" + e.getClass().getSimpleName() + "): " + e.getMessage());
                }
            }
        } catch (NoSuchElementException fimDaEntrada) {
            System.out.println();
        }
        System.out.println("Programa encerrado. Os dados continuam salvos em urna.db.");
    }

    private static void mostrarMenu() {
        System.out.println();
        System.out.println("========== URNA ELETRÔNICA ==========");
        System.out.println("1 - Cadastrar eleitor");
        System.out.println("2 - Cadastrar candidato");
        System.out.println("3 - Cadastrar urna");
        System.out.println("4 - Votar");
        System.out.println("5 - Encerrar urna (mesário)");
        System.out.println("6 - Apurar votos");
        System.out.println("7 - Gerar boletim de urna (TXT)");
        System.out.println("8 - Carregar dados de exemplo");
        System.out.println("0 - Sair");
        System.out.println("=====================================");
    }

    private static void executar(int opcao) throws EleitorNaoEncontradoException,
            EleitorJaVotouException, UrnaEncerradaException, CandidatoInvalidoException, DaoException {
        switch (opcao) {
            case 1 -> cadastrarEleitor();
            case 2 -> cadastrarCandidato();
            case 3 -> cadastrarUrna();
            case 4 -> votar();
            case 5 -> encerrarUrna();
            case 6 -> apurar();
            case 7 -> gerarBoletim();
            case 8 -> DadosIniciais.carregar(eleitorDAO, candidatoDAO, urnaDAO);
            case 0 -> { }
            default -> System.out.println("Opção inválida.");
        }
    }

    // ---------------------------------------------------------------- cadastros (RF.1 e RF.2)

    private static void cadastrarEleitor() throws DaoException {
        System.out.println("--- Cadastrar eleitor ---");
        String nome = lerTexto("Nome: ");
        String cpf = lerTexto("CPF (só números): ");
        String titulo = lerTexto("Título de eleitor: ");
        eleitorDAO.inserir(new Eleitor(nome, cpf, titulo));
        System.out.println("Eleitor cadastrado: " + nome + ".");
    }

    private static void cadastrarCandidato() throws DaoException {
        System.out.println("--- Cadastrar candidato (todo candidato também é eleitor) ---");
        String nome = lerTexto("Nome: ");
        String cpf = lerTexto("CPF (só números): ");
        String titulo = lerTexto("Título de eleitor: ");
        int numero = lerInteiro("Número do candidato: ");
        String cargo = lerTexto("Cargo: ");
        String partido = lerTexto("Partido: ");
        String foto = lerTexto("Arquivo da foto (opcional, Enter para pular): ", true);
        candidatoDAO.inserir(new Candidato(nome, cpf, titulo, numero, cargo, partido, foto));
        System.out.println("Candidato cadastrado: " + nome + " (" + numero + ").");
    }

    private static void cadastrarUrna() throws DaoException {
        System.out.println("--- Cadastrar urna ---");
        List<Candidato> disponiveis = candidatoDAO.listar();
        if (disponiveis.isEmpty()) {
            System.out.println("Cadastre pelo menos um candidato antes (menu 2).");
            return;
        }
        String secao = lerTexto("Seção (ex.: 001): ");
        System.out.println("Candidatos cadastrados:");
        for (Candidato c : disponiveis) {
            System.out.println("  " + c);
        }
        String linha = lerTexto("Números dos candidatos que concorrem nesta urna, separados por vírgula: ");
        Set<Integer> numeros = new LinkedHashSet<>();
        for (String parte : linha.split(",")) {
            try {
                numeros.add(Integer.parseInt(parte.trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("\"" + parte.trim() + "\" não é um número válido.");
            }
        }
        List<Candidato> escolhidos = new ArrayList<>();
        for (int numero : numeros) {
            Candidato c = candidatoDAO.buscarPorNumero(numero);
            if (c == null) {
                throw new IllegalArgumentException("Não existe candidato com o número " + numero + ".");
            }
            escolhidos.add(c);
        }
        Urna urna = urnaDAO.inserir(secao, escolhidos);
        System.out.println("Urna cadastrada: nº " + urna.getId() + ", seção " + urna.getSecao()
                + ", com " + escolhidos.size() + " candidato(s).");
    }

    // ---------------------------------------------------------------- votação (RF.3 e RF.4)

    private static void votar() throws EleitorNaoEncontradoException, EleitorJaVotouException,
            UrnaEncerradaException, CandidatoInvalidoException, DaoException {
        System.out.println("--- Votar ---");
        Urna urna = escolherUrna();

        // Objetos relidos do banco a cada voto: se a gravação falhar, os de memória são descartados.
        String identificacao = lerTexto("Título de eleitor ou CPF: ");
        Eleitor eleitor = eleitorDAO.buscar(identificacao);

        // Confere antes de pedir o número (o modelo confere de novo na hora de votar).
        if (urna.isEncerrada()) {
            throw new UrnaEncerradaException(urna.getSecao());
        }
        eleitor.verificarSePodeVotar();

        System.out.println("Olá, " + eleitor.getNome() + ".");
        boolean votoRegistrado = false;
        while (!votoRegistrado) {
            String entrada = lerTexto("Número do candidato (ou B para voto em BRANCO): ");
            if (entrada.equalsIgnoreCase("B")) {
                if (confirmar("Confirma voto em BRANCO?")) {
                    eleitor.votarBranco(urna);
                    votoRegistrado = true;
                } else {
                    System.out.println("Voto corrigido. Digite de novo.");
                }
                continue;
            }

            int numero;
            try {
                numero = Integer.parseInt(entrada);
            } catch (NumberFormatException e) {
                System.out.println("Digite só números ou B para branco.");
                continue;
            }

            Candidato candidato = urna.buscarCandidato(numero);
            if (candidato == null) {
                System.out.println("ATENÇÃO: número " + numero + " não existe nesta urna. O voto será NULO.");
            } else {
                System.out.println("Candidato: " + candidato.getNome() + " | " + candidato.getCargo()
                        + " | " + candidato.getPartido());
            }
            if (confirmar("CONFIRMA?")) {
                TipoVoto tipo = eleitor.votar(urna, numero);
                if (tipo == TipoVoto.NULO) {
                    System.out.println("Aviso: número inexistente, voto computado como NULO.");
                }
                votoRegistrado = true;
            } else {
                System.out.println("Voto corrigido. Digite de novo.");
            }
        }

        // Só agora o voto vai para o banco: o voto e o ja_votou entram na mesma transação (RN.5).
        votoDAO.gravar(urna, ultimoVoto(urna), eleitor);
        System.out.println("FIM. Voto registrado com sucesso. Obrigado, " + eleitor.getNome() + ".");
    }

    /** A urna lida do banco só tem na memória o voto que acabou de ser criado. */
    private static Voto ultimoVoto(Urna urna) {
        List<Voto> votos = urna.getVotos();
        return votos.get(votos.size() - 1);
    }

    // ---------------------------------------------------------------- encerrar e apurar (RF.5, RF.7)

    private static void encerrarUrna() throws DaoException {
        System.out.println("--- Encerrar urna (comando do mesário) ---");
        Urna urna = escolherUrna();
        if (urna.isEncerrada()) {
            System.out.println("A urna da seção " + urna.getSecao() + " já está encerrada.");
            return;
        }
        if (confirmar("Encerrar a votação da urna " + urna.getId() + " (seção " + urna.getSecao()
                + ")? Depois disso ela não aceita mais votos.")) {
            urnaDAO.encerrar(urna.getId());
            System.out.println("Urna encerrada. Use o menu 6 para apurar e o 7 para gerar o boletim.");
        } else {
            System.out.println("Operação cancelada. A urna continua aberta.");
        }
    }

    private static void apurar() throws DaoException {
        System.out.println("--- Apuração ---");
        Urna urna = escolherUrna();
        if (!urna.isEncerrada()) {
            System.out.println("A apuração só é feita com a urna encerrada. Encerre pelo menu 5.");
            return;
        }

        Map<Candidato, Integer> porCandidato = votoDAO.contarPorCandidato(urna);
        int brancos = votoDAO.contar(urna, TipoVoto.BRANCO);
        int nulos = votoDAO.contar(urna, TipoVoto.NULO);
        int total = votoDAO.contarTotal(urna);

        System.out.println("Urna " + urna.getId() + " - seção " + urna.getSecao() + " (ENCERRADA)");
        int validos = 0;
        int maior = -1;
        List<Candidato> lideres = new ArrayList<>();
        for (Map.Entry<Candidato, Integer> e : porCandidato.entrySet()) {
            Candidato c = e.getKey();
            int votos = e.getValue();
            System.out.println(String.format(PT_BR, "%s (%d) - %s: %d voto(s) - %.1f%%",
                    c.getNome(), c.getNumero(), c.getPartido(), votos, porcentagem(votos, total)));
            validos += votos;
            if (votos > maior) {
                maior = votos;
                lideres.clear();
                lideres.add(c);
            } else if (votos == maior) {
                lideres.add(c);
            }
        }
        System.out.println(String.format(PT_BR, "Brancos: %d - %.1f%%", brancos, porcentagem(brancos, total)));
        System.out.println(String.format(PT_BR, "Nulos: %d - %.1f%%", nulos, porcentagem(nulos, total)));
        System.out.println("Total de votos: " + total);
        System.out.println("Conferência: " + validos + " + " + brancos + " + " + nulos + " = "
                + (validos + brancos + nulos));

        // RF.7 (extra): a porcentagem é sobre o total, incluindo brancos e nulos.
        if (maior <= 0) {
            System.out.println("Vencedor: nenhum candidato recebeu votos.");
        } else if (lideres.size() > 1) {
            StringBuilder nomes = new StringBuilder();
            for (Candidato c : lideres) {
                nomes.append(nomes.length() == 0 ? "" : ", ").append(c.getNome());
            }
            System.out.println("Resultado: empate entre " + nomes + " com " + maior + " voto(s) cada.");
        } else {
            System.out.println("Vencedor: " + lideres.get(0).getNome() + " (" + lideres.get(0).getNumero()
                    + ") com " + maior + " voto(s).");
        }
    }

    private static double porcentagem(int parte, int total) {
        return (total == 0) ? 0.0 : (parte * 100.0) / total;
    }

    // ---------------------------------------------------------------- boletim (RF.6)

    private static void gerarBoletim() throws DaoException {
        System.out.println("--- Boletim de urna ---");
        Urna urna = escolherUrna();
        if (!urna.isEncerrada()) {
            System.out.println("O boletim só é gerado com a urna encerrada. Encerre pelo menu 5.");
            return;
        }
        Path arquivo = boletimDAO.gerar(urna);
        System.out.println("Boletim gerado em: " + arquivo);
    }

    // ---------------------------------------------------------------- apoio

    private static Urna escolherUrna() throws DaoException {
        List<Urna> urnas = urnaDAO.listar();
        if (urnas.isEmpty()) {
            throw new DaoException("Não há urna cadastrada. Use o menu 3 ou o menu 8.");
        }
        System.out.println("Urnas cadastradas:");
        for (Urna u : urnas) {
            System.out.println("  " + u.getId() + " - seção " + u.getSecao()
                    + (u.isEncerrada() ? " (encerrada)" : " (aberta)"));
        }
        int id = (urnas.size() == 1) ? urnas.get(0).getId() : lerInteiro("Número da urna: ");
        if (urnas.size() == 1) {
            System.out.println("Usando a urna " + id + ".");
        }
        return urnaDAO.buscar(id);
    }

    private static String lerTexto(String pergunta) {
        return lerTexto(pergunta, false);
    }

    private static String lerTexto(String pergunta, boolean podeVazio) {
        while (true) {
            System.out.print(pergunta);
            String texto = teclado.nextLine().trim();
            if (!texto.isEmpty() || podeVazio) {
                return texto;
            }
            System.out.println("Esse campo não pode ficar vazio.");
        }
    }

    private static int lerInteiro(String pergunta) {
        while (true) {
            System.out.print(pergunta);
            String texto = teclado.nextLine().trim();
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                System.out.println("Digite um número inteiro.");
            }
        }
    }

    private static boolean confirmar(String pergunta) {
        while (true) {
            System.out.print(pergunta + " (S/N): ");
            String resposta = teclado.nextLine().trim();
            if (resposta.equalsIgnoreCase("S")) {
                return true;
            }
            if (resposta.equalsIgnoreCase("N")) {
                return false;
            }
            System.out.println("Responda S ou N.");
        }
    }
}
