package modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Urna {
    private final int id;
    private final String secao;
    private boolean encerrada;
    private final List<Candidato> candidatos;              // agregação: chega pelo construtor
    private final List<Voto> votos = new ArrayList<>();    // composição: nasce dentro da urna

    public Urna(int id, String secao, List<Candidato> candidatos) {
        this(id, secao, candidatos, false);
    }

    /** Usado ao ler do banco: devolve a urna com o estado (encerrada) que estava gravado. */
    public Urna(int id, String secao, List<Candidato> candidatos, boolean encerrada) {
        this.id = id;
        this.secao = secao;
        this.candidatos = new ArrayList<>(candidatos);
        this.encerrada = encerrada;
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

    /** Lista não modificável: ninguém mexe nos votos de fora. */
    public List<Voto> getVotos() {
        return Collections.unmodifiableList(votos);
    }

    /** Comando do mesário. Depois disso a urna não aceita mais voto (RN.3). */
    public void encerrar() {
        encerrada = true;
    }

    /** Devolve o candidato do número (ou null) sem votar; a interface usa para mostrar e pedir confirmação. */
    public Candidato buscarCandidato(int numero) {
        for (Candidato c : candidatos) {
            if (c.getNumero() == numero) {
                return c;
            }
        }
        return null;
    }

    /** Voto direto num candidato; ele precisa pertencer à urna (RN.6). */
    public void votar(Candidato candidato) throws UrnaEncerradaException, CandidatoInvalidoException {
        verificarAberta();
        if (candidato == null || !candidatos.contains(candidato)) {
            throw new CandidatoInvalidoException();
        }
        votos.add(new Voto(TipoVoto.VALIDO, candidato));
    }

    /** Voto pelo número digitado: número que não existe vira voto NULO. */
    public TipoVoto votar(int numero) throws UrnaEncerradaException {
        verificarAberta();
        Candidato candidato = buscarCandidato(numero);
        if (candidato == null) {
            votos.add(new Voto(TipoVoto.NULO, null));
            return TipoVoto.NULO;
        }
        votos.add(new Voto(TipoVoto.VALIDO, candidato));
        return TipoVoto.VALIDO;
    }

    public void votarBranco() throws UrnaEncerradaException {
        verificarAberta();
        votos.add(new Voto(TipoVoto.BRANCO, null));
    }

    /** Conta os votos válidos de cada candidato (candidato sem voto aparece com 0). */
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

    private int contar(TipoVoto tipo) {
        int total = 0;
        for (Voto v : votos) {
            if (v.getTipo() == tipo) {
                total++;
            }
        }
        return total;
    }

    /** Concentra a checagem da RN.3. */
    private void verificarAberta() throws UrnaEncerradaException {
        if (encerrada) {
            throw new UrnaEncerradaException(secao);
        }
    }
}
