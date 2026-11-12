package modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Urna {
    private int id;
    private String secao;
    private boolean encerrada;
    private List<Candidato> candidatos;   // agregacao: chega pelo construtor
    private List<Voto> votos;             // composicao: nasce dentro da urna

    public Urna(int id, String secao, List<Candidato> candidatos) {
        this.id = id;
        this.secao = secao;
        this.encerrada = false;
        this.candidatos = new ArrayList<>(candidatos);
        this.votos = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public String getSecao() {
        return secao;
    }

    public boolean isEncerrada() {
        return encerrada;
    }

    public List<Candidato> getCandidatos() {
        return Collections.unmodifiableList(candidatos);
    }

    /** Voto direto no candidato. */
    public void votar(Candidato candidato)
            throws UrnaEncerradaException, CandidatoInvalidoException {
        verificarAberta();
        if (!candidatos.contains(candidato)) {
            throw new CandidatoInvalidoException();
        }
        votos.add(new Voto(TipoVoto.VALIDO, candidato));
    }

    /** Voto pelo numero. Numero que nao existe vira voto nulo. */
    public TipoVoto votar(int numero) throws UrnaEncerradaException {
        verificarAberta();
        for (Candidato c : candidatos) {
            if (c.getNumero() == numero) {
                votos.add(new Voto(TipoVoto.VALIDO, c));
                return TipoVoto.VALIDO;
            }
        }
        votos.add(new Voto(TipoVoto.NULO, null));
        return TipoVoto.NULO;
    }

    public void votarBranco() throws UrnaEncerradaException {
        verificarAberta();
        votos.add(new Voto(TipoVoto.BRANCO, null));
    }

    public void encerrar() {
        encerrada = true;
    }

    public Map<Candidato, Integer> contabilizarVotos() {
        Map<Candidato, Integer> resultado = new LinkedHashMap<>();
        for (Candidato c : candidatos) {
            resultado.put(c, 0);
        }
        for (Voto v : votos) {
            if (v.getTipo() == TipoVoto.VALIDO) {
                resultado.merge(v.getCandidato(), 1, Integer::sum);
            }
        }
        return resultado;
    }

    public int contarBrancos() {
        return contar(TipoVoto.BRANCO);
    }

    public int contarNulos() {
        return contar(TipoVoto.NULO);
    }

    public int getTotalVotos() {
        return votos.size();
    }

    /** Lista nao modificavel: ninguem altera os votos de fora. */
    public List<Voto> getVotos() {
        return Collections.unmodifiableList(votos);
    }

    private int contar(TipoVoto tipo) {
        int total = 0;
        for (Voto v : votos) {
            if (v.getTipo() == tipo) {
                total++;
            }
        }
        return total;
    }

    private void verificarAberta() throws UrnaEncerradaException {
        if (encerrada) {
            throw new UrnaEncerradaException(secao);
        }
    }
}
