package dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import modelo.Candidato;
import modelo.TipoVoto;
import modelo.Urna;

/** Gera o boletim de urna em TXT (RF.6), contando os votos no banco. */
public class BoletimDAO {
    private static final String LINHA = "=".repeat(50);
    private static final String TRACO = "-".repeat(50);

    private final VotoDAO votoDAO = new VotoDAO();

    /** Grava boletim_urna_<id>.txt na pasta onde o programa roda. */
    public Path gerar(Urna urna) throws DaoException {
        return gerar(urna, Path.of("."));
    }

    public Path gerar(Urna urna, Path pasta) throws DaoException {
        if (!urna.isEncerrada()) {
            throw new DaoException("O boletim só pode ser gerado com a urna encerrada.");
        }
        Map<Candidato, Integer> porCandidato = votoDAO.contarPorCandidato(urna);
        int brancos = votoDAO.contar(urna, TipoVoto.BRANCO);
        int nulos = votoDAO.contar(urna, TipoVoto.NULO);
        int total = votoDAO.contarTotal(urna);

        String data = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        StringBuilder sb = new StringBuilder();
        sb.append(LINHA).append('\n');
        sb.append("               BOLETIM DE URNA").append('\n');
        sb.append(LINHA).append('\n');
        sb.append("Urna: ").append(urna.getId()).append(" (seção ").append(urna.getSecao()).append(")\n");
        sb.append("Data: ").append(data).append('\n');
        sb.append("Situação: ENCERRADA").append('\n');
        sb.append(TRACO).append('\n');
        sb.append("Votos por candidato:").append('\n');
        for (Map.Entry<Candidato, Integer> e : porCandidato.entrySet()) {
            Candidato c = e.getKey();
            sb.append(String.format("  %d - %s (%s) - %s: %d\n",
                    c.getNumero(), c.getNome(), c.getPartido(), c.getCargo(), e.getValue()));
        }
        sb.append(TRACO).append('\n');
        sb.append("Votos em branco: ").append(brancos).append('\n');
        sb.append("Votos nulos: ").append(nulos).append('\n');
        sb.append("Total de votos: ").append(total).append('\n');
        sb.append(LINHA).append('\n');

        Path arquivo = pasta.resolve("boletim_urna_" + urna.getId() + ".txt");
        try {
            Files.writeString(arquivo, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DaoException("Não foi possível gravar o arquivo do boletim.", e);
        }
        return arquivo.toAbsolutePath().normalize();
    }
}
