# Assistente de Serviços do INSS pelo WhatsApp — Protótipo Java

FIAP · Global Solution 2026 · CitizenTech · turma 2SIOR

Protótipo funcional (console) do assistente que ajuda o cidadão a **entender e cumprir uma exigência do INSS**
(lista de documentos, prazo, envio) por um canal mais simples e seguro. **A decisão sobre o benefício continua sendo do INSS.**

> Todos os dados são **fictícios**. O login gov.br e o INSS são **simulados**. Nenhum número de telefone, CPF ou senha é guardado.

## Requisitos
- JDK 17 ou superior (testado com JDK 21). Não precisa instalar banco: o **H2** vai em `lib/`.

## Como rodar (Linux/macOS)
```bash
./scripts/compilar.sh                 # compila para out/classes
./scripts/executar.sh                 # conversa interativa (você escolhe o perfil fictício)
./scripts/executar.sh --demo          # conversa roteirizada completa, mostrando o que vai para o banco ([BD])
./scripts/executar.sh --consultar     # lê o banco em OUTRO processo (prova a persistência)
./scripts/executar.sh --simular       # roda 19 cenários de validação e imprime os indicadores
./scripts/executar.sh --relatorio     # indicadores do que está gravado no banco
./scripts/executar.sh --exportar saida   # CSV sem dados pessoais para planilha
./scripts/testar.sh                   # 72 testes automatizados (JUnit 4)
./scripts/executar.sh --ajuda         # todos os modos
```
No Windows use `scripts\compilar.bat`, `scripts\executar.bat` e `scripts\testar.bat`.

## Sessão de teste com pessoas (para a validação)
```bash
./scripts/executar.sh --interativo --teste P01 --sql   # P01 = código do participante (sem nome)
./scripts/executar.sh --relatorio                      # depois de ≥5 participantes
```
Cada participante começa com os pedidos no mesmo estado. Veja `docs/03-validacao-e-testes.md`, seção 4.

## Banco de dados
Padrão: H2 em arquivo (`dados/assistente_inss`), criado e populado sozinho na primeira execução.
Para outro banco (PostgreSQL, Oracle…) basta trocar a URL, sem mexer no código:
`-Dinss.db.url=... -Dinss.db.user=... -Dinss.db.password=...` ou as variáveis `INSS_DB_URL`, `INSS_DB_USER`, `INSS_DB_PASSWORD`
(coloque o driver JDBC no classpath).
`pom.xml` incluso para quem preferir Maven (**não foi executado** no ambiente de desenvolvimento; o caminho oficial é `scripts/`).

## Documentação
| Arquivo | Conteúdo |
|---|---|
| `docs/01-arquitetura-e-tecnologias.md` | arquitetura, tecnologias e justificativas, conceitos da disciplina, 2 desafios técnicos |
| `docs/02-dados-coletados-e-lgpd.md` | dados coletados, finalidade, tratamento LGPD, segurança e acessibilidade |
| `docs/03-validacao-e-testes.md` | como o protótipo foi testado/validado e como medir resultados |
| `docs/04-indicadores-e-estatistica.md` | indicadores, fórmulas, análise estatística, limites |
| `docs/05-roteiro-video-tecnico.md` | roteiro de 2 minutos |
| `docs/06-conversa-exemplo.md` | conversa real gerada pelo protótipo (substitui o wireframe) |
| `docs/diagramas/` | arquitetura, fluxo, modelo de dados (ER) e classes (.dot + .png) |

## Estrutura do código (`src/main/java/br/com/fiap/citizentech/inss`)
`modelo` (entidades, enums, exceções) · `dao` (JDBC) · `db` (conexão, transação, script) · `servico` (regras e conversa) ·
`ui` (canal) · `analise` (indicadores/estatística/CSV) · `validacao` (19 cenários).
