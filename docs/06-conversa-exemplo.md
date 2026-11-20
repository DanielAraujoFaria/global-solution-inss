# Conversa de exemplo gerada pelo protótipo (modo --demo, dados fictícios)

Substitui o wireframe: é a saída real do programa. Linhas [BD] mostram o que é gravado.

```
>>> DEMONSTRAÇÃO: banco reiniciado com dados fictícios. Linhas [BD] mostram o que é gravado.

      [BD] INSERT conversa         id=1
[Assistente INSS]
  Olá! Eu sou o assistente de serviços do INSS no WhatsApp oficial.
  (Protótipo acadêmico da FIAP: todos os dados são fictícios.)
  
  Combinado de segurança: eu nunca peço senha, código de verificação nem pagamento por aqui.
  A decisão sobre o seu benefício é sempre do INSS; eu ajudo você a entender e a organizar.

[Assistente INSS]
  Como posso ajudar?
    1) Entender e resolver uma pendência (exigência) do meu pedido
    2) Falar com uma pessoa do INSS
    3) Ver orientações gerais sobre exigências
    4) Sair

[Você] > minha senha é 123456

      [BD] INSERT auditoria        id=1
[Assistente INSS]
  Por segurança, não envie senha, código de verificação, CPF ou número de cartão por aqui. Eu nunca peço isso no chat. Apaguei essa informação do registro.

[Você] > 1

[Assistente INSS]
  O que está mais difícil para você?
    1) Não entendi o que o INSS está pedindo
    2) Não sei quais documentos mandar
    3) Tenho medo de perder o prazo
    4) Não consigo anexar no Meu INSS

[Você] > 1

      [BD] UPDATE conversa         1 linha(s)
[Assistente INSS]
  Vou mostrar o texto oficial e explicar com palavras simples.

[Assistente INSS]
  Se quiser, escreva em uma frase o que está acontecendo (não escreva senha, código nem CPF). Para pular, envie 0.

[Você] > 0

[Assistente INSS]
  Enquanto isso, o que vale para qualquer exigência: em regra, o prazo para cumprir é de 30 dias contados da ciência (o prazo do seu caso aparece na própria exigência). Se ela não for cumprida, o pedido pode ser indeferido ou encerrado.

[Assistente INSS]
  Para olhar o SEU pedido eu preciso da sua autorização. Vou consultar só: situação do pedido, texto da exigência, prazo da exigência, número do protocolo.
  O login é feito no navegador seguro do gov.br, fora do WhatsApp. Eu não vejo nem guardo a sua senha.

[Assistente INSS]
  Você autoriza?
    1) Autorizo e quero entrar com gov.br
    2) Não autorizo

[Você] > 1

      [BD] INSERT auditoria        id=2
[Assistente INSS]
  Vou abrir o link oficial do gov.br (simulado): https://sso.acesso.gov.br/simulado
  Entre por lá, no navegador, e volte aqui. Nunca digite a senha do gov.br no WhatsApp.

      [BD] SELECT cidadao          11 linha(s)
[Navegador seguro gov.br - SIMULADO]
  Login gov.br (SIMULADO). Escolha o perfil fictício para entrar:
    1) Maria das Dores Silva [titular]
    2) Carlos Eduardo Lima [titular]
    3) Ana Paula Souza [titular]
    4) Rita de Cássia Alves [representante NÃO habilitado]
    5) Marta Ferreira [representante habilitado]
    6) Raimundo Nonato Pereira [titular]
    7) Lúcia Helena Barbosa [titular]
    8) José Antunes de Oliveira [titular]
    9) Beatriz Moreira [titular]
    10) Antônio Alves [titular]
    11) Josefa Rodrigues [titular]
    0) Cancelar o login

[Você] > 1

[Navegador seguro gov.br - SIMULADO]
  O Assistente INSS pede autorização para consultar: situação do pedido, texto da exigência, prazo e protocolo.
    1) Autorizar
    2) Negar

[Você] > 1

      [BD] UPDATE conversa         1 linha(s)
      [BD] INSERT auditoria        id=3
[Navegador seguro gov.br - SIMULADO]
  Login concluído. Pode voltar para a conversa no WhatsApp.

[Assistente INSS]
  Oi, Maria! Login confirmado. Vou consultar só o que você autorizou.

      [BD] SELECT cidadao          1 linha(s)
      [BD] SELECT pedido           1 linha(s)
      [BD] SELECT exigencia        1 linha(s)
      [BD] INSERT auditoria        id=4
[Assistente INSS]
  Pedido FICT-0001 - Aposentadoria por idade.

      [BD] UPDATE conversa         1 linha(s)
[Assistente INSS]
  O que o INSS escreveu (texto oficial, sem alterações):
    "Apresentar comprovante de residência atualizado em nome do requerente."
  
  Em palavras simples:
    O INSS quer confirmar onde você mora. Envie uma conta ou documento recente com o seu nome e o seu endereço.
  
  Prazo registrado: 08/12/2026 (faltam 20 dias).

      [BD] SELECT item_checklist   0 linha(s)
      [BD] INSERT item_checklist   2 linha(s)
      [BD] SELECT item_checklist   2 linha(s)
      [BD] UPDATE exigencia        1 linha(s)
[Assistente INSS]
  Vamos montar a sua lista de documentos. Para cada item, diga como você está.

[Assistente INSS]
  Item 1 de 2: Conta recente (água, luz, gás ou telefone) em seu nome  [obrigatório]
  Dica: Pode ser a fatura em papel ou a segunda via no site da empresa. O nome e o endereço precisam aparecer inteiros.

[Assistente INSS]
    1) Tenho e está legível
    2) Não tenho
    3) Não sei se serve
    4) Tenho, mas está ilegível ou incompleto

[Você] > 1

      [BD] UPDATE item_checklist   1 linha(s)
[Assistente INSS]
  Anotado.

[Assistente INSS]
  Item 2 de 2: Declaração de residência  [opcional]
  Dica: Só é preciso se a conta estiver no nome de outra pessoa. Pergunte ao 135 qual modelo de declaração é aceito.

[Assistente INSS]
    1) Tenho e está legível
    2) Não tenho
    3) Não sei se serve
    4) Tenho, mas está ilegível ou incompleto

[Você] > 2

      [BD] UPDATE item_checklist   1 linha(s)
[Assistente INSS]
  Sem problema. Como conseguir: Só é preciso se a conta estiver no nome de outra pessoa. Pergunte ao 135 qual modelo de declaração é aceito.

      [BD] UPDATE conversa         1 linha(s)
[Assistente INSS]
  Tudo o que é obrigatório está pronto. Isto é o que seria enviado ao INSS:
    - Conta recente (água, luz, gás ou telefone) em seu nome
  
  (Neste protótipo nenhum arquivo é transmitido de verdade. Em produção você anexaria cada documento aqui e eles seguiriam direto para o INSS.)

[Assistente INSS]
  Você confirma o envio?
    1) Confirmo o envio
    2) Não, quero revisar antes

[Você] > 1

      [BD] INSERT auditoria        id=5
      [BD] SELECT cidadao          1 linha(s)
      [BD] SELECT exigencia        1 linha(s)
      [BD] SELECT pedido           1 linha(s)
      [BD] SELECT envio_protocolo  1 linha(s)
      [BD] INSERT envio_protocolo  id=1
      [BD] UPDATE item_checklist   1 linha(s)
      [BD] UPDATE exigencia        1 linha(s)
      [BD] INSERT auditoria        id=6
[Assistente INSS]
  Pronto! Envio registrado. Protocolo (simulado): SIM-2026-000001.
  Para acompanhar o andamento: Meu INSS ou telefone 135. A decisão sobre o pedido continua sendo do INSS.

      [BD] UPDATE conversa         1 linha(s)
      [BD] INSERT auditoria        id=7
[Assistente INSS]
  Última coisa, bem rápida (envie 0 para pular).
  De 1 a 5, o quanto ficou claro o que o INSS pede de você? (1 = nada claro, 5 = muito claro)

[Você] > 5

[Assistente INSS]
  De 1 a 5, o quanto você ficou satisfeito(a) com este atendimento?

[Você] > 5

[Assistente INSS]
  Você sabe qual é o seu próximo passo?
    1) Sim
    2) Ainda não

[Você] > 1

      [BD] INSERT avaliacao        id=1
[Assistente INSS]
  Obrigado! Sua resposta ajuda a melhorar o serviço.

>>> DADOS GRAVADOS NO BANCO (continuam lá depois que o programa fecha):

--- Conversas (mais recentes) ---
  id  origem        desfecho           encaminhamento  motivo  autenticada
  1   DEMONSTRACAO  CONCLUIDA_DIGITAL  -               -       true

--- Exigências ---
  id  protocolo  tipo              status    data_prazo  data_cumprimento
  1   FICT-0001  COMP_RESIDENCIA   CUMPRIDA  2026-12-08  2026-11-18
  2   FICT-0002  LAUDO_MEDICO      ABERTA    2026-11-22  -
  3   FICT-0003  VINCULO_TRABALHO  ABERTA    2026-11-03  -
  4   FICT-0010  COMP_RESIDENCIA   ABERTA    2026-12-10  -
  5   FICT-0011  DOC_IDENTIDADE    ABERTA    2026-12-13  -
  6   FICT-0007  DOC_IDENTIDADE    ABERTA    2026-12-11  -
  7   FICT-0008  PERIODO_RURAL     ABERTA    2026-12-09  -

--- Checklist (itens já respondidos) ---
  id  exig  nome                                obrig  status
  1   1     Conta recente (água, luz, gás ou …  true   ENVIADO
  2   1     Declaração de residência            false  PENDENTE

--- Envios (protocolos simulados) ---
  id  exig  conversa  protocolo        enviado_em
  1   1     1         SIM-2026-000001  2026-11-18 20:48:15.46946

--- Auditoria da última conversa ---
  evento                   detalhe
  DADO_SENSIVEL_DETECTADO  CREDENCIAL
  CONSENTIMENTO_CONCEDIDO  escopos=situação do pedido, texto…
  AUTENTICACAO_CONCLUIDA   sessão de 10 min
  CONSULTA_REALIZADA       pedidos=1
  CONFIRMACAO_ENVIO        exigencia=1
  ENVIO_REGISTRADO         SIM-2026-000001
  CONVERSA_FINALIZADA      CONCLUIDA_DIGITAL

```
