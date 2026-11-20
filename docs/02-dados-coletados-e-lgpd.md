# 2. Dados coletados, finalidade, LGPD, segurança e acessibilidade

## 2.1 Dados que o protótipo coleta ou usa
| Dado | Onde | Para quê (finalidade) | Observação |
|---|---|---|---|
| Perfil do cidadão (nome **fictício**, faixa etária, tipo de acesso) | `cidadao` | Saber se pode usar a automação (titular/representante habilitado) e adaptar a linguagem | Seed fictício; em produção viria do gov.br com escopo mínimo |
| Pedido e exigência (protocolo, texto oficial, prazo, status) | `pedido`, `exigencia` | Explicar a pendência e avisar do prazo | Só após consentimento + login |
| Checklist de documentos | `item_checklist` | Mostrar o que falta e registrar o que já está pronto | Guarda **nome** do documento, nunca o arquivo |
| Conversa (desfecho, encaminhamento, motivo, horários, hash da sessão) | `conversa` | Medir se ajudou e encaminhar corretamente | Telefone **não** é guardado; sessão = hash SHA-256 |
| Mensagens | `mensagem` | Depurar e auditar a conversa | **Já mascaradas**; podem ser apagadas por retenção |
| Envio simulado | `envio_protocolo` | Provar que houve confirmação do cidadão | Protocolo fictício |
| Avaliação (clareza 1–5, satisfação 1–5, sabe o próximo passo) | `avaliacao` | Medir a qualidade do serviço | Sem nome; só por `conversa_id` |
| Auditoria (consentimento, bloqueios, tentativas de dado sensível) | `auditoria` | Prestação de contas | Sem conteúdo sensível |

**Não coletamos**: número de telefone, senha, código de verificação, CPF completo, cartão, endereço, arquivos de documentos.

## 2.2 Tratamento conforme a LGPD (Lei 13.709/2018)
| Princípio / direito | Como o protótipo atende |
|---|---|
| Finalidade e necessidade | Cada dado acima tem finalidade explícita; o que não é necessário não é coletado. |
| Consentimento claro | Antes de consultar o pedido, o assistente lista exatamente **o que vai consultar** e pergunta "Você autoriza?". Recusar leva a atendimento humano, sem punição. Evento registrado em `auditoria`. |
| Minimização | CSV exportado só tem desfechos e notas, sem mensagens nem dados pessoais. Encaminhamento leva um resumo sem dados pessoais. |
| Segurança | `PreparedStatement` em 100% das consultas; sessão com validade de 10 min e escopos; só o hash fica no banco; senha/credencial nunca gravadas. |
| Retenção | `MensagemDao.purgarAnterioresA(data)` apaga mensagens antigas (política sugerida: 30 dias; **a definir com o encarregado/DPO**). |
| Transparência | Aviso inicial: "protótipo, dados fictícios; nunca peço senha, código ou pagamento". |
| Prevenção | Detector de dados sensíveis; auto-auditoria que procura mensagens não mascaradas (esperado 0). |
| Titular com representante | Representante só atua com habilitação; caso contrário bloqueado e encaminhado. |

> Observação: para uso real seriam necessários RIPD (relatório de impacto), base legal formalizada com o INSS e definição do encarregado. Fora do escopo do protótipo.

## 2.3 Segurança (resumo)
1. Dados fictícios; sem integração real.
2. Nunca pede senha/código/pagamento; login só no navegador oficial do gov.br (simulado).
3. Regras de bloqueio: consentimento, autenticação (3 tentativas), escopo, titularidade.
4. Injeção de SQL: impossível por `PreparedStatement`.
5. Falha de integração → encaminhamento seguro (`INTEGRACAO_INDISPONIVEL`).

## 2.4 Acessibilidade (o que existe e o que falta)
**Existe:** linguagem simples (explicação ao lado do texto oficial); menus numerados (sem digitação livre obrigatória); marcação textual `[obrigatório]/[opcional]` em vez de cor/emoji; frases curtas; opção de falar com pessoa em qualquer ponto; aviso de prazo com "faltam N dias"; atenção a 60+ (perfil Maria no teste).
**Falta (honesto):** teste com leitor de tela e com pessoas com deficiência; áudio/voz; versão em fonte maior. No WhatsApp real, áudio e mensagens de voz seriam prioridade para baixa escolaridade digital.
