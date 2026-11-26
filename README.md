# Projeto 2: Urna Eletrônica (Entrega 3: Persistência e funcionalidades)

UFRPE/UABJ · Engenharia da Computação · Modelagem e Programação Orientada a Objetos · 2026.2

## Estrutura

```
pom.xml                  Maven (Java 17, sqlite-jdbc, JUnit 5)
src/modelo/              regras do problema (sem tela e sem SQL)
src/dao/                 SQLite por JDBC e boletim em TXT
src/app/                 Main (menu de console), DadosIniciais e Demo
test/modelo, test/dao    testes JUnit 5
urna.db                  banco, criado sozinho na primeira execução
```

## Como executar

Precisa de Java 17 ou mais novo e Maven. Na primeira vez é preciso internet (o Maven baixa o driver do SQLite).

| O que fazer | Comando (na pasta do pom.xml) |
| --- | --- |
| Menu de console | `mvn compile exec:java` |
| Testes (32 testes) | `mvn test` |
| Demo da Entrega 2 (só em memória, sem banco) | `mvn compile exec:java -Dexec.mainClass=app.Demo` |
| Recomeçar do zero | apagar o arquivo `urna.db` |

- A mensagem do SLF4J sobre logger na primeira execução é só um aviso do driver; pode ignorar.
- Se os acentos saírem errados no terminal do Windows, rode `chcp 65001` antes.

## Menu

`1` cadastrar eleitor · `2` cadastrar candidato · `3` cadastrar urna · `4` votar · `5` encerrar urna ·
`6` apurar · `7` gerar boletim (TXT) · `8` carregar dados de exemplo · `0` sair

Dados de exemplo (menu 8): candidatos Ana Souza (nº 10, título 1001), Bruno Lima (nº 20, título 1002) e
Carla Dias (nº 30, título 1003); eleitores Diego Alves (2001), Elisa Rocha (2002), Fábio Costa (2003) e
Gabriela Nunes (2004); urna 1, seção 001, com os três candidatos.

## Roteiro para as capturas de tela (para o relatório)

1. `mvn test` e mostrar `Tests run: 32, Failures: 0, Errors: 0`.
2. Menu 8, depois menu 4: título `1001`, número `20`, confirmar com `S`.
3. Menu 4: título `2002` em branco (`B` e `S`); título `2003` com o número `40` (nulo, com aviso) e `S`.
4. Menu 4 de novo com `1001` (eleitor que já votou) e com `9999` (não cadastrado).
5. Menu 6 com a urna aberta (apuração recusada), menu 5 (encerrar, `S`), menu 6 (apuração) e menu 7 (boletim). Abrir o arquivo `boletim_urna_1.txt`.
6. Menu 4 com o título `2004` (urna encerrada: erro).
7. Fechar o programa (`0`), abrir de novo e usar o menu 6: os resultados continuam lá.
8. Menus 1, 2 e 3 com dados novos (por exemplo, eleitor título `2005`, candidato número `50` e uma segunda urna).
9. Opcional: abrir `urna.db` no DB Browser for SQLite e mostrar que a tabela `voto` não tem coluna do eleitor.
