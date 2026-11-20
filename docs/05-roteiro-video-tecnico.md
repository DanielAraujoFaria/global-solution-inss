# 5. Roteiro do vídeo técnico (2 minutos)

Gravar a tela do terminal + falar. Antes: `./scripts/executar.sh --reiniciar` e fonte grande no terminal.

| Tempo | Tela | Fala (sugestão) |
|---|---|---|
| 0:00–0:15 | Título + README | "Este é o protótipo do Assistente de Serviços do INSS pelo WhatsApp. Ele ajuda o cidadão a cumprir uma exigência; a decisão continua sendo do INSS. Feito em Java com JDBC e banco H2." |
| 0:15–0:30 | Diagrama de arquitetura | "Camadas: interface, serviço, DAOs e banco. O canal é uma interface; trocar o console pelo WhatsApp é criar uma nova implementação." |
| 0:30–1:15 | `./scripts/executar.sh --demo` | "Rodando a conversa. O cidadão digita uma senha: o sistema bloqueia, apaga e avisa. Depois autoriza a consulta, entra no gov.br simulado e recebe a explicação com o texto oficial ao lado. As linhas [BD] mostram os INSERTs e UPDATEs no banco." |
| 1:15–1:30 | `--consultar` em novo processo | "Em outro processo, os dados continuam lá: persistência real com consulta." |
| 1:30–1:50 | Trecho do código `Transacao` + teste | "Desafio 1: o envio altera três tabelas. Usei transação com rollback; o teste força a falha e prova que nada fica pela metade. Desafio 2: dado sensível digitado no chat é mascarado antes de gravar." |
| 1:50–2:00 | `./scripts/testar.sh` e `--simular` | "72 testes e 19 cenários passam. Os indicadores usam intervalo de confiança e deixam claro o que ainda precisa de teste com pessoas." |

Dicas: não mostrar telefone/CPF reais; todos os dados são fictícios — dizer isso. Se estourar 2 min, corte o trecho do `--consultar`.
