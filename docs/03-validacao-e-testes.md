# 3. Como o protótipo foi testado e validado, e como os resultados são medidos

Há **quatro níveis** de verificação. Os três primeiros foram executados e têm resultado real; o quarto (pessoas de verdade) é o protocolo que a equipe deve aplicar.

| Nível | O que responde | Status |
|---|---|---|
| 1. Testes automatizados (JUnit) | O código faz o que o programador pensou? | ✅ 72 de 72 passam |
| 2. Cenários de validação (19) | O assistente se comporta como a regra de negócio manda, inclusive nas situações de risco? | ✅ 19 de 19 |
| 3. Teste de mutação | Os testes realmente pegam erros? | ✅ 8 de 8 defeitos injetados foram detectados |
| 4. Teste de usabilidade com pessoas | O cidadão entende, confia e sabe o próximo passo? | ⏳ a equipe aplica (seção 4) |

## 3.1 Testes automatizados (`./scripts/testar.sh`)
72 testes em 13 classes: estatística (valores calculados à mão), detector de dados sensíveis, regras de prazo e urgência,
`AvaliadorDeCaso` (ordem de prioridade), autenticação (expiração, escopo), banco (criação, script, restaurar pedidos),
DAOs (insert/select/update), **transação com rollback**, gateway, serviço de conversa, indicadores e exportação CSV.
Os testes usam banco em memória e relógio controlável (`RelogioMutavel`), então são repetíveis e rápidos (~1,5 s).

## 3.2 Os 19 cenários (`./scripts/executar.sh --simular`)
Cada cenário é uma conversa roteirizada com um **resultado esperado** (oráculo). Além disso, **toda** conversa passa por invariantes de segurança:
nunca consultar sem consentimento e autenticação; nunca enviar sem confirmação; nenhuma senha gravada.

| Cenário | Situação | Esperado |
|---|---|---|
| C01 | Fluxo feliz (Maria) | Concluída digital, protocolo simulado |
| C02 | Prazo curto, falta documento (Carlos) | Pendente + lembrete consentido |
| C03 | Documento ilegível | Encaminha com resumo |
| C04 | Prazo vencido (Ana) | Agência |
| C05 | Representante não habilitada | Bloqueia e nada de terceiro é mostrado |
| C06 | Representante habilitada | Conclui para o representado |
| C07 | Indeferido | Orienta, não decide nem recorre |
| C08 | Dados divergentes | Agência |
| C09 | Caso complexo (rural) | Agência |
| C10 | Sem exigência | Orienta confirmar com o INSS |
| C11 | Não autoriza consulta | Nada consultado |
| C12 | Login cancelado, prefere Meu INSS | Orientada no Meu INSS |
| C13 | Integração indisponível | Não finge consulta; encaminha |
| C14 | Envio indisponível | Checklist pronta, conclusão no Meu INSS |
| C15 | Senha digitada no chat | Bloqueada e nunca gravada |
| C16 | Não confirma envio | Nada enviado |
| C17 | Pede uma pessoa no início | Central 135 |
| C18 | Para de responder | Abandonada |
| C19 | Só lê orientações e sai | Encerra |

**Resultado:** 19/19 corretos. Desfechos: 11 com encaminhamento (destes, 7 vão para uma pessoa — Central 135 ou agência — conforme o relatório de indicadores), 3 concluídas digitais, 1 orientada no Meu INSS, 2 pendentes, 2 abandonadas.

## 3.3 Teste de mutação (os testes são confiáveis?)
Injetamos 8 defeitos deliberados e conferimos se a suíte quebrava: M1 prazo calculado errado, M2 envio sem confirmação, M3 CPF não mascarado,
M4 remoção do rollback, M5 titularidade ignorada, M6 consentimento não auditado, M7 mascaramento desligado, M8 lembrete fora da regra.
**Todos os 8 foram detectados.** (Prova de que os testes não passam "à toa".)

## 3.4 O que a validação automática NÃO prova
- Não prova que o cidadão **entende** (a clareza das frases só se mede com pessoas).
- Não mede tempo humano nem satisfação (as respostas do roteiro são nossas) — por isso esses indicadores **não** são calculados para dados simulados.
- Não mede "documentos aceitos pelo INSS sem nova solicitação": exigiria integração real.
- Não foi testado com leitor de tela.

## 4. Protocolo de teste com pessoas (para a equipe executar)
**Amostra:** mínimo 5 pessoas (idealmente 8–10), com variedade: pelo menos 1 pessoa 60+ e 1 com pouca prática digital. Voluntários, sem coletar nome.
**Termo oral:** "É um protótipo com dados fictícios; você pode parar quando quiser; não guardamos seu nome."
**Tarefa:** "Você recebeu uma exigência do INSS e quer saber o que fazer." Cada participante usa um perfil fictício (ex.: Maria ou Carlos).
**Execução:**
```bash
./scripts/executar.sh --interativo --teste P01 --sql    # P02, P03... para os próximos
./scripts/executar.sh --relatorio                       # ao final
./scripts/executar.sh --exportar resultados             # CSV para planilha
```
Cada sessão grava automaticamente: desfecho, duração, checklist, e as 3 perguntas finais (clareza 1–5, satisfação 1–5, "sabe o próximo passo?").
**Observação (papel):** o observador anota onde a pessoa hesitou, qual palavra não entendeu, e se pediu ajuda humana.
**Critérios de sucesso sugeridos (definidos antes, para não "ajustar a meta ao resultado"):**
- ≥ 80% dizem saber qual o próximo passo (reportar com intervalo de Wilson).
- Média de clareza ≥ 4,0 e de satisfação ≥ 4,0.
- 0 casos de dado sensível gravado sem máscara.
- Nenhum caso em que o assistente prosseguiu sem consentimento.

**Modelo de resultados (preencher):**
| Participante | Perfil | Duração (s) | Clareza | Satisfação | Sabe o próximo passo? | Observação |
|---|---|---|---|---|---|---|
| P01 | | | | | | |
| P02 | | | | | | |
| P03 | | | | | | |
| P04 | | | | | | |
| P05 | | | | | | |

Com n < 5 o relatório mostra um aviso; com n pequeno, **intervalos largos são esperados** e devem ser reportados, não escondidos.

## 3.5 Como os resultados são medidos
Ver `04-indicadores-e-estatistica.md`. Resumo: taxa de resolução sem atendimento adicional, taxa de encaminhamento adequado, checklists completas,
dias entre notificação e envio, duração, clareza, satisfação, "sabe o próximo passo", e métricas de privacidade — todos separados por origem
(DEMONSTRACAO, SIMULACAO, TESTE_USUARIO) para que dado de roteiro nunca se misture com dado de pessoa.
