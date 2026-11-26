package modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EleitorTest {
    private Candidato ana;
    private Candidato bruno;
    private Eleitor diego;
    private Urna urna;

    @BeforeEach
    void preparar() {
        ana = new Candidato("Ana Souza", "11111111111", "1001", 10, "Prefeito", "Partido X", "");
        bruno = new Candidato("Bruno Lima", "22222222222", "1002", 20, "Prefeito", "Partido Y", "");
        diego = new Eleitor("Diego Alves", "44444444444", "2001");
        urna = new Urna(1, "001", List.of(ana, bruno));
    }

    @Test
    @DisplayName("RN.1: o eleitor vota no máximo uma vez")
    void eleitorNaoVotaDuasVezes() throws Exception {
        diego.votar(urna, 20);
        assertTrue(diego.jaVotou());

        assertThrows(EleitorJaVotouException.class, () -> diego.votar(urna, 10));
        assertThrows(EleitorJaVotouException.class, () -> diego.votarBranco(urna));
        assertEquals(1, urna.getTotalVotos());
    }

    @Test
    @DisplayName("RN.3 e RN.5: se a urna recusa o voto, o jaVotou não é marcado")
    void urnaEncerradaNaoMarcaJaVotou() {
        urna.encerrar();

        assertThrows(UrnaEncerradaException.class, () -> diego.votar(urna, 20));
        assertFalse(diego.jaVotou());
        assertEquals(0, urna.getTotalVotos());
    }

    @Test
    @DisplayName("RN.6: candidato de fora da urna é recusado e o jaVotou não é marcado")
    void candidatoForaDaUrnaNaoMarcaJaVotou() {
        Candidato fora = new Candidato("Zeca Pires", "99999999999", "1009", 90, "Prefeito", "Partido W", "");

        assertThrows(CandidatoInvalidoException.class, () -> diego.votar(urna, fora));
        assertFalse(diego.jaVotou());
        assertEquals(0, urna.getTotalVotos());
    }

    @Test
    @DisplayName("Eleitor lido do banco já com jaVotou verdadeiro não consegue votar")
    void eleitorLidoDoBancoComoJaVotouNaoVota() {
        Eleitor jaVotou = new Eleitor("Elisa Rocha", "55555555555", "2002", true);

        assertTrue(jaVotou.jaVotou());
        assertThrows(EleitorJaVotouException.class, () -> jaVotou.votar(urna, 10));
        assertEquals(0, urna.getTotalVotos());
    }

    @Test
    @DisplayName("Polimorfismo: getPapel() muda conforme o objeto")
    void polimorfismoDoPapel() {
        Eleitor comoEleitor = ana; // Candidato onde se espera Eleitor
        assertEquals("Eleitor", diego.getPapel());
        assertEquals("Candidato", comoEleitor.getPapel());
    }

    @Test
    @DisplayName("Liskov: um candidato vota como qualquer eleitor")
    void candidatoTambemVota() throws Exception {
        Eleitor comoEleitor = ana;
        comoEleitor.votar(urna, 20);

        assertTrue(ana.jaVotou());
        assertEquals(1, urna.getTotalVotos());
    }

    @Test
    @DisplayName("Voto em branco é contado como branco")
    void votoEmBranco() throws Exception {
        diego.votarBranco(urna);

        assertTrue(diego.jaVotou());
        assertEquals(1, urna.contarBrancos());
    }

    @Test
    @DisplayName("Dados obrigatórios vazios são recusados")
    void dadosVaziosSaoRecusados() {
        assertThrows(IllegalArgumentException.class, () -> new Eleitor("", "123", "1"));
        assertThrows(IllegalArgumentException.class, () -> new Eleitor("Fulano", "", "1"));
        assertThrows(IllegalArgumentException.class, () -> new Eleitor("Fulano", "123", " "));
        assertThrows(IllegalArgumentException.class,
                () -> new Candidato("Fulano", "123", "1", 0, "Prefeito", "X", ""));
    }
}
