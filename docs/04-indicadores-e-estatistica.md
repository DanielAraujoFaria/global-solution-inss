# 4. Indicadores e análise estatística

## 4.1 Indicadores
| # | Indicador | Fórmula | Origem válida | Meta sugerida |
|---|---|---|---|---|
| 1 | Desfechos e motivos de encaminhamento | contagem por categoria | todas | — |
| 2 | Resolução sem atendimento adicional | conversas CONCLUIDA_DIGITAL ou ORIENTADA_MEU_INSS ÷ total | simulação / teste | acompanhar (não é "quanto maior melhor") |
| 3 | Encaminhamento humano | conversas encaminhadas à Central 135 ou à agência ÷ total (encaminhamentos ao Meu INSS contam como orientação digital) | simulação / teste | adequação = 100% (ver cenários) |
| 4 | Checklist completa | checklists com todos os obrigatórios prontos ÷ checklists iniciadas | simulação / teste | ≥ 70% |
| 5 | Dias entre notificação e envio | data do envio − data de emissão da exigência | simulação / teste | menor que o prazo da exigência |
| 6 | Duração da conversa | fim − início (s) | **só teste com pessoas** | — |
| 7 | Clareza (1–5) | média | **só teste com pessoas** | ≥ 4,0 |
| 8 | Satisfação (1–5) | média | **só teste com pessoas** | ≥ 4,0 |
| 9 | Sabe o próximo passo | sim ÷ avaliações | **só teste com pessoas** | ≥ 80% |
| 10 | Privacidade | tentativas bloqueadas; mensagens com dado sensível sem máscara | toda a base | 0 sem máscara |

Indicador **não medido** (e por quê): *documentos aceitos pelo INSS sem nova solicitação* e *tempo de análise do INSS* — dependem de integração e de dados reais do INSS.

## 4.2 Estatística (implementada à mão em `Estatistica.java`, testada)
- **Descritiva:** média, mediana, desvio padrão amostral, mínimo e máximo.
- **Proporções:** intervalo de confiança de 95% de **Wilson** (melhor que a aproximação normal com n pequeno).
- **Médias:** IC 95% pela **t de Student** (tabela para 1–30 graus de liberdade; acima disso, 1,96).
- **Por que intervalos?** Com amostra pequena, um percentual sozinho engana. Mostramos sempre o intervalo.

## 4.3 Resultado da simulação (19 cenários — roteirizados)
| Indicador | Resultado | IC 95% |
|---|---|---|
| Resolução sem atendimento adicional | 4 de 19 = **21,1%** | 8,5% a 43,3% |
| Encaminhamento humano | 7 de 19 = **36,8%** | 19,1% a 59,0% |
| Checklist completa | 5 de 7 = **71,4%** | 35,9% a 91,8% |
| Dias entre notificação e envio (n=3) | média 8,33; mediana 10,00; DP 2,89; mín 5; máx 10 | 1,16 a 15,51 (t) |
| Mensagens com dado sensível sem máscara | **0** (esperado 0) | — |
| Tentativas de dado sensível bloqueadas | 1 | — |

**Como ler (sem exagerar):**
- Os 19 cenários foram **escolhidos por nós para cobrir situações**, não sorteados da população. Portanto 21,1% **não** é "a taxa de resolução do assistente no mundo real": é a composição do nosso conjunto de testes. Serve para ver que o conjunto cobre tanto casos automatizáveis quanto os que **devem** ir a uma pessoa.
- Encaminhar muito **não é falha**: prazo vencido, indeferimento, dados divergentes e caso complexo precisam de pessoa. O que se mede é se o encaminhamento foi **adequado** (19/19).
- Intervalos largos refletem n pequeno e devem ser mantidos assim no relatório.

## 4.4 Como comparar com o cenário atual (para o relatório do grupo)
O impacto de tempo (ex.: "reduzir os 30 dias de análise") **não** é medido pelo protótipo. Hipótese a testar em campo: o assistente reduz idas e vindas por documento errado/incompleto, encurtando o intervalo *notificação → cumprimento*. Proposta de medição: comparar, em piloto real, a mediana de dias entre notificação e cumprimento com e sem o assistente, e a proporção de exigências reabertas por documento inadequado.

## 4.5 Alerta sobre números citados pelo grupo
A frase "hoje essa análise demora 30 dias" **precisa de fonte oficial**. Fontes secundárias consultadas indicam: prazo para cumprir exigência de 30 dias corridos contados da ciência (prorrogável por mais 30); prazo legal de análise de 45 dias; e médias de concessão na imprensa em 2026 por volta de 50–66 dias. Recomenda-se citar a fonte oficial (INSS/Meu INSS/painel de dados) antes de usar o número no vídeo ou no relatório.
