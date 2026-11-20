# 1. Arquitetura, tecnologias e desafios técnicos

## 1.1 O que o protótipo faz
Conduz o cidadão por: **entender a exigência → autorizar a consulta (LGPD) → entrar via gov.br (simulado) → checklist de
documentos → confirmar → protocolo (simulado)**. Quando a regra manda, **encaminha para uma pessoa** (Meu INSS, Central 135 ou agência)
com o motivo e um resumo sem dados pessoais. Tudo fica gravado em banco (inserir, consultar, atualizar).

Diagramas: `diagramas/arquitetura.png`, `diagramas/fluxo.png`, `diagramas/modelo_er.png`, `diagramas/classes.png`.

## 1.2 Arquitetura em camadas
| Camada | Pacote | Responsabilidade |
|---|---|---|
| Interface | `ui` | `Canal` (interface). `CanalConsole` (teclado) e `CanalRoteirizado` (testes/demonstração). Trocar por WhatsApp Business API = nova implementação de `Canal`. |
| Regras e conversa | `servico` | `AssistenteService` (máquina de estados), `AvaliadorDeCaso`, `DetectorDadosSensiveis`, `AutenticacaoSimulada`, `PoliticaDeLembrete`, `ExplicadorExigencia`, `InssGateway`. |
| Persistência | `dao`, `db` | DAOs com JDBC e `PreparedStatement`; `Transacao` com commit/rollback. |
| Modelo | `modelo` | Entidades, records, enums com comportamento, exceções próprias. |
| Medição | `analise`, `validacao` | Indicadores, estatística, CSV, 19 cenários. |

## 1.3 Tecnologias e por quê
| Escolha | Justificativa |
|---|---|
| Java 17 | Exigido pela disciplina; records, enums e switch modernos deixam as regras legíveis. |
| JDBC puro + `PreparedStatement` | Conteúdo da disciplina (conexão com BD); impede injeção de SQL; sem framework escondendo o que acontece. |
| H2 embarcado | O professor roda com um comando, sem instalar servidor. O código não depende dele: a URL é configurável (PostgreSQL/Oracle em produção). |
| JUnit 4 | Testes repetíveis; 72 testes. |
| Assistente por **regras** (não IA generativa) | Em serviço público o texto oficial não pode ser "reescrito" com risco de erro. A interface `ExplicadorExigencia` permite, no futuro, um LLM **com revisão humana**, sem mudar o resto. |
| Console no lugar do WhatsApp | A API oficial exige conta empresarial e aprovação; o foco do protótipo é a **lógica e os dados**. O canal é uma interface justamente para isso. |

## 1.4 Conceitos da disciplina aplicados
- **POO**: classe abstrata `Entidade` (herdada por Cidadao, Pedido, Exigencia, ItemChecklist); `DaoBase` (herdada pelos 9 DAOs); interfaces `Canal`, `InssGateway`, `ExplicadorExigencia` com implementações trocáveis (polimorfismo); encapsulamento; exceções próprias (`AcessoNegadoException`, `ErroDeBancoException`, `FimDeEntradaException`).
- **Estruturas de dados**: `Map`/`EnumMap`/`TreeMap` (contagem por desfecho/motivo), `ArrayDeque` (entradas roteirizadas), `List` (checklist, mensagens).
- **Modelagem**: 12 tabelas, chaves estrangeiras, CHECK (notas 1–5, status), UNIQUE (uma avaliação por conversa), índices.
- **Conexão com BD**: `Conexao`, `Transacao`, `BancoDeDados` (cria/popula/reinicia), script SQL separado do código.

## 1.5 Desafios técnicos (e alternativas)

### Desafio 1 — Enviar o protocolo sem deixar o banco inconsistente
**Problema.** Confirmar o envio altera 3 tabelas (`envio_protocolo` INSERT, `item_checklist` UPDATE, `exigencia` UPDATE). Se falhar no meio, o cidadão poderia ver "enviado" sem protocolo, ou o contrário — grave num serviço de prazo.
**Alternativas.** (a) Três comandos soltos com auto-commit — simples, mas inconsistente em falha. (b) Uma tabela só com tudo — perde o modelo. (c) **Transação atômica** — escolhida.
**Solução.** `Transacao.executar(...)` desliga o auto-commit, faz os 3 passos e dá `rollback` em qualquer exceção. O teste `TransacaoTest` força a falha no 3º passo e confere que nada ficou gravado; o teste de mutação (retirar o rollback) é detectado.

### Desafio 2 — O cidadão digita senha, CPF ou código no chat
**Problema.** Pessoas costumam digitar dados sensíveis no WhatsApp, e golpistas se aproveitam disso. Gravar esse texto criaria um banco de dados perigoso.
**Alternativas.** (a) Confiar no aviso inicial — não basta. (b) Criptografar tudo — o dado continuaria sendo guardado sem necessidade. (c) **Detectar e descartar antes de gravar** — escolhida.
**Solução.** `DetectorDadosSensiveis` (expressões regulares): senha/credencial → a mensagem inteira é descartada; CPF, cartão e código de 6 dígitos → mascarados. Só o texto mascarado é gravado, o evento vai para `auditoria` e o cidadão é avisado. Limite honesto: regex não pega tudo (ex.: CPF escrito por extenso); por isso há também a política de retenção e a auto-auditoria (`--simular` item 10).

## 1.6 Decisões de projeto que protegem o cidadão
- Nunca consulta sem **consentimento + autenticação**; nunca envia sem **confirmação** (invariantes verificadas em todos os 19 cenários).
- Representante só vê o pedido do representado se for **habilitado**; senão é encaminhado.
- Prazo vencido, indeferimento, dados divergentes e caso complexo **vão para uma pessoa** — o assistente não "tenta resolver".
- O texto oficial da exigência é sempre mostrado **sem alteração**, ao lado da explicação simples.

## 1.7 Limitações
Banco com conexão única (produção usaria pool); login gov.br e INSS simulados; console não testado com leitor de tela; `pom.xml` não executado no ambiente de desenvolvimento.
