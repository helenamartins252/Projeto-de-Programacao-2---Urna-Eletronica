package modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UrnaTest {
    private Candidato ana;
    private Candidato bruno;
    private Urna urna;

    @BeforeEach
    void preparar() {
        ana = new Candidato("Ana Souza", "11111111111", "1001", 10, "Prefeito", "Partido X", "");
        bruno = new Candidato("Bruno Lima", "22222222222", "1002", 20, "Prefeito", "Partido Y", "");
        urna = new Urna(1, "001", List.of(ana, bruno));
    }

    @Test
    @DisplayName("RN.3: urna encerrada não aceita voto de nenhum tipo")
    void urnaEncerradaRecusaVoto() {
        urna.encerrar();

        assertTrue(urna.isEncerrada());
        assertThrows(UrnaEncerradaException.class, () -> urna.votar(10));
        assertThrows(UrnaEncerradaException.class, () -> urna.votar(ana));
        assertThrows(UrnaEncerradaException.class, () -> urna.votarBranco());
        assertEquals(0, urna.getTotalVotos());
    }

    @Test
    @DisplayName("RF.4: número que não existe vira voto nulo")
    void numeroInexistenteViraNulo() throws Exception {
        assertEquals(TipoVoto.NULO, urna.votar(99));
        assertEquals(TipoVoto.VALIDO, urna.votar(10));

        assertEquals(1, urna.contarNulos());
        assertEquals(2, urna.getTotalVotos());
    }

    @Test
    @DisplayName("RN.6: só candidato ligado à urna recebe voto")
    void candidatoForaDaUrnaEhRecusado() {
        Candidato fora = new Candidato("Zeca Pires", "99999999999", "1009", 90, "Prefeito", "Partido W", "");

        assertThrows(CandidatoInvalidoException.class, () -> urna.votar(fora));
        assertThrows(CandidatoInvalidoException.class, () -> urna.votar((Candidato) null));
        assertEquals(0, urna.getTotalVotos());
    }

    @Test
    @DisplayName("RF.5: votos, brancos e nulos somam o total")
    void contagemFechaComOTotal() throws Exception {
        urna.votar(10);
        urna.votar(20);
        urna.votar(20);
        urna.votarBranco();
        urna.votar(77);
        urna.encerrar();

        Map<Candidato, Integer> porCandidato = urna.contabilizarVotos();
        assertEquals(1, porCandidato.get(ana));
        assertEquals(2, porCandidato.get(bruno));
        assertEquals(1, urna.contarBrancos());
        assertEquals(1, urna.contarNulos());

        int validos = porCandidato.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(urna.getTotalVotos(), validos + urna.contarBrancos() + urna.contarNulos());
    }

    @Test
    @DisplayName("buscarCandidato devolve o candidato (ou null) sem votar")
    void buscarCandidatoNaoVota() {
        assertEquals(ana, urna.buscarCandidato(10));
        assertNull(urna.buscarCandidato(55));
        assertEquals(0, urna.getTotalVotos());
    }

    @Test
    @DisplayName("Encapsulamento: a lista de votos não pode ser alterada de fora")
    void listaDeVotosNaoModificavel() throws Exception {
        urna.votar(10);
        assertThrows(UnsupportedOperationException.class, () -> urna.getVotos().clear());
    }

    @Test
    @DisplayName("Urna lida do banco já encerrada continua recusando voto")
    void urnaCriadaJaEncerrada() {
        Urna lida = new Urna(2, "002", List.of(ana), true);
        assertTrue(lida.isEncerrada());
        assertThrows(UrnaEncerradaException.class, () -> lida.votar(10));
    }

    @Test
    @DisplayName("RN.2: o Voto não tem nenhuma ligação com o eleitor")
    void votoNaoTemLigacaoComEleitor() {
        // O voto só pode guardar o tipo e o candidato escolhido (e o candidato, só se for válido).
        Set<Class<?>> permitidos = Set.of(TipoVoto.class, Candidato.class);
        for (Field campo : Voto.class.getDeclaredFields()) {
            assertTrue(permitidos.contains(campo.getType()),
                    "O campo " + campo.getName() + " não deveria existir no Voto");
        }
    }

    @Test
    @DisplayName("RN.4: o construtor do Voto não é público (só a urna cria votos)")
    void construtorDoVotoNaoEhPublico() {
        for (Constructor<?> construtor : Voto.class.getDeclaredConstructors()) {
            assertFalse(Modifier.isPublic(construtor.getModifiers()));
        }
    }
}
