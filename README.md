# Projeto 2: Urna eletrônica

Simulador de eleição com urna eletrônica, feito em Java para a disciplina **Modelagem e Programação Orientada a Objetos** (UFRPE – UABJ, Engenharia da Computação, semestre 2026.2), com o professor Michael Oliveira da Cruz.

Os eleitores votam digitando o número do candidato e confirmando, ou votam em branco. Cada eleitor vota uma única vez e o voto é secreto: depois de votar, ninguém consegue saber em quem a pessoa votou. Quando a votação termina, a urna é encerrada, conta os votos de cada candidato, os brancos e os nulos, e gera um boletim de urna em arquivo TXT.

O sistema é voltado a quem organiza e simula a eleição (mesário ou administrador) e aos eleitores cadastrados.

## Equipe

| Nome | Matrícula | E-mail |
|------|-----------|--------|
| Julia Gabriela de Andrade Barboza | 20250029167 | julia.barboza@ufrpe.br |
| Raiane Mayra Freitas Cintra | 20250031835 | raiane.cintra@ufrpe.br |
| Maria Luisa Marcelina de Meirelles | 20250029238 | luisameirelles7@gmail.com |
| Maria Helena da Silva Martins | 20250023619 | mariahelenahellena124@gmail.com |

## Funcionalidades

| ID | Funcionalidade | Prioridade |
|----|----------------|------------|
| RF2.1 | Cadastrar eleitores e candidatos | Alta |
| RF2.2 | Cadastrar urnas e dizer quais candidatos concorrem em cada uma | Alta |
| RF2.3 | Votar digitando o número do candidato e confirmando | Alta |
| RF2.4 | Aceitar voto em branco; número que não existe vira voto nulo, com aviso | Alta |
| RF2.5 | Encerrar a urna e contar os votos, os brancos e os nulos | Alta |
| RF2.6 | Gerar o boletim de urna em TXT | Média |
| RF2.7 | (extra) Mostrar a porcentagem de cada candidato e o vencedor | Baixa |

## Regras de negócio

| ID | Regra |
|----|-------|
| RN2.1 | Cada eleitor vota no máximo uma vez; quem controla é o `jaVotou` do eleitor |
| RN2.2 | Voto secreto: nenhuma ligação entre voto e eleitor |
| RN2.3 | Urna encerrada não aceita voto |
| RN2.4 | O voto é criado dentro da urna, por `votar()` |
| RN2.5 | Marcar `jaVotou` e gravar o voto acontecem juntos |
| RN2.6 | Só eleitor cadastrado vota; só candidato ligado à urna aparece nela |

## Modelo de classes

| Classe | Atributos | Métodos principais |
|--------|-----------|--------------------|
| `Pessoa` (abstrata) | - nome: String<br>- cpf: String | |
| `Eleitor` (herda de Pessoa) | - tituloEleitor: String<br>- jaVotou: boolean | + registrarVoto() |
| `Candidato` (herda de Eleitor) | - numero: int<br>- cargo: String<br>- partido: String<br>- foto: String (caminho da imagem) | |
| `Urna` | - id: int<br>- secao: String<br>- encerrada: boolean<br>- candidatos: List\<Candidato\><br>- votos: List\<Voto\> | + votar(Candidato)<br>+ votar(int numero)<br>+ votarBranco()<br>+ contabilizarVotos()<br>+ encerrar() |
| `Voto` | - tipo: TipoVoto<br>- candidato: Candidato (vazio se branco ou nulo) | |
| `TipoVoto` (enum) | VALIDO<br>BRANCO<br>NULO | |

Legenda: `-` é privado e `+` é público.

O `Voto` não tem ligação com o `Eleitor`: só o campo `jaVotou` diz se a pessoa votou.

### Relacionamentos

| Classes | Tipo | Por quê |
|---------|------|---------|
| Pessoa e Eleitor | Herança | Todo eleitor é uma pessoa. Eleitor reaproveita nome e CPF e acrescenta título de eleitor e `jaVotou`. |
| Eleitor e Candidato | Herança | Todo candidato também é eleitor. A herança tem três níveis (Pessoa, Eleitor e Candidato), com `super(...)` preenchendo os dados da classe de cima. |
| Urna e Candidato | Agregação (0..\*) | O candidato existe sem a urna e pode concorrer em várias urnas. A lista chega pronta pelo construtor da urna. |
| Urna e Voto | Composição (0..\*) | O voto só existe dentro da urna. A urna cria cada `Voto` nos métodos `votar()`. Se a urna for apagada, os votos vão junto. |
| Voto e Candidato | Associação (0..1) | O voto válido aponta para o candidato escolhido; branco e nulo não apontam para ninguém. |
| Voto e TipoVoto | Uso de enum | O tipo (VALIDO, BRANCO ou NULO) é um conjunto fixo de valores. |
| Eleitor e Urna | Dependência | O eleitor vota na urna, mas a urna não guarda o eleitor, para o voto ser secreto (RN2.2). |

## Banco de dados

SQLite acessado por JDBC, num único arquivo. As tabelas são criadas sozinhas na primeira execução.

| Tabela | Colunas |
|--------|---------|
| eleitor | id (PK), nome, cpf, titulo, ja_votou |
| candidato | id (PK), eleitor_id (FK), numero, cargo, partido, foto |
| urna | id (PK), secao, encerrada |
| urna_candidato | urna_id (FK), candidato_id (FK) |
| voto | id (PK), urna_id (FK), tipo (VALIDO, BRANCO ou NULO), candidato_id (FK, vazio se for branco ou nulo) |

- A tabela `voto` não tem nenhuma coluna ligada ao eleitor, então não dá para saber em quem cada pessoa votou (RN2.2).
- Marcar `ja_votou` e gravar o voto acontecem na mesma transação JDBC: commit se tudo der certo, rollback se algo falhar (RN2.5).
- Há uma classe DAO para cada tabela. O DAO captura a `SQLException` e lança uma exceção do próprio sistema, assim a tela não sabe nada de SQL.

## Boletim de urna

Ao encerrar a urna, o botão GERAR BOLETIM grava um arquivo TXT com a urna, a data, o total de cada candidato, os brancos, os nulos e o total geral. O botão só fica disponível com a urna encerrada.

## Exceções

| Exceção | Quando é lançada |
|---------|------------------|
| `EleitorJaVotouException` | O eleitor tentou votar de novo |
| `UrnaEncerradaException` | Tentou votar numa urna encerrada |
| `EleitorNaoEncontradoException` | Título ou CPF não cadastrado |
| `CandidatoInvalidoException` | O candidato não pertence à urna |

Os erros aparecem em caixa de mensagem (`JOptionPane`) e o programa continua funcionando.

## Telas

1. **Identificação:** o eleitor digita o título de eleitor ou CPF e clica em ENTRAR. Se não estiver cadastrado ou já tiver votado, aparece uma mensagem de erro com o botão OK.
2. **Votação:** o eleitor digita o número do candidato e a tela mostra a foto, o nome e o partido. CONFIRMA registra o voto, CORRIGE limpa o número e BRANCO vota em branco. Número que não existe gera aviso e voto nulo. Depois de CONFIRMA, aparece a mensagem "FIM" e o sistema volta para a identificação.
3. **Apuração:** aparece quando a urna é encerrada. Mostra candidato, votos e porcentagem, além de brancos, nulos e total, e o vencedor (extra). Tem o botão GERAR BOLETIM (TXT).

## Tecnologias

| Item | Detalhes |
|------|----------|
| Linguagem | Java 17 |
| Build | Maven (baixa o driver `org.xerial:sqlite-jdbc`) |
| IDE | VS Code |
| Interface | Console (demonstração do domínio) e Swing (telas finais) |
| Banco de dados | SQLite via JDBC |
| Arquivos | TXT (boletim de urna) |
| Testes | JUnit 5 |
| Controle de versão | Git, com uma tag por entrega |

## Organização do código

```
.
├── pom.xml
├── README.md
└── src
    ├── main/java
    │   ├── modelo/    Pessoa, Eleitor, Candidato, Urna, Voto, TipoVoto e exceções
    │   ├── dao/       acesso ao SQLite e gravação do boletim TXT
    │   └── gui/       telas Swing e menu de console
    └── test/java      testes JUnit 5
```

O pacote `modelo` nunca usa nada de tela.

## Como compilar e executar

Em breve. Esta seção será preenchida quando o projeto estiver executável, com a versão do Java, os comandos e as dependências.

## Andamento

| Etapa | Conteúdo | Tag no Git | Situação |
|-------|----------|------------|----------|
| 1 | Modelagem | `entrega1` | Concluída |
| 2 | Classes do modelo e Main de console | `entrega2` | Pendente |
| 3 | Banco de dados SQLite e funcionalidades | `entrega3` | Pendente |
| 4 | Telas Swing e versão final | `entrega4` | Pendente |

## Commits

Cada commit começa com o código do requisito ou da regra que ele atende, por exemplo: `RF2.3: votar por número com confirmação`.
