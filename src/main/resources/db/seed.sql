-- =====================================================================
-- Dados FICTÍCIOS para o protótipo acadêmico. Nenhum dado real de cidadão.
-- Datas são relativas a CURRENT_DATE para que os cenários sejam sempre
-- reproduzíveis (ex.: "prazo vence em 4 dias").
-- A ORDEM dos cidadãos define o número do perfil no login simulado (1..11).
-- =====================================================================

-- ---------- Catálogo de tipos de exigência ----------
INSERT INTO tipo_exigencia (codigo, titulo, texto_oficial_exemplo, explicacao_simples, prazo_padrao_dias, exige_analise_humana) VALUES
('COMP_RESIDENCIA', 'Comprovante de endereço',
 'Apresentar comprovante de residência atualizado em nome do requerente.',
 'O INSS quer confirmar onde você mora. Envie uma conta ou documento recente com o seu nome e o seu endereço.',
 30, FALSE),
('DOC_IDENTIDADE', 'Documento de identificação',
 'Apresentar documento oficial de identificação com foto e número de CPF.',
 'O INSS precisa conferir quem você é. Envie um documento oficial com foto e o número do CPF.',
 30, FALSE),
('VINCULO_TRABALHO', 'Comprovação de vínculo de trabalho',
 'Apresentar documentos que comprovem o vínculo empregatício ou a atividade exercida no período informado.',
 'O INSS quer confirmar que você trabalhou no período informado. Envie os documentos que mostram onde e quando você trabalhou.',
 30, FALSE),
('LAUDO_MEDICO', 'Laudo ou relatório médico',
 'Apresentar laudo ou relatório médico atualizado, legível e assinado pelo profissional assistente.',
 'O INSS precisa de um documento do seu médico, recente, legível e assinado, para continuar a análise.',
 30, FALSE),
('PERIODO_RURAL', 'Esclarecimento de atividade rural',
 'Esclarecer e comprovar os períodos de atividade rural declarados, mediante documentos e autodeclaração.',
 'O INSS pede explicações sobre o período em que você trabalhou na área rural. Esse tipo de pedido precisa ser conferido por uma pessoa do INSS.',
 30, TRUE);

-- ---------- Documentos-modelo (viram a checklist) ----------
INSERT INTO documento_modelo (tipo_exigencia_id, nome, orientacao, obrigatorio, ordem) VALUES
((SELECT id FROM tipo_exigencia WHERE codigo = 'COMP_RESIDENCIA'), 'Conta recente (água, luz, gás ou telefone) em seu nome',
 'Pode ser a fatura em papel ou a segunda via no site da empresa. O nome e o endereço precisam aparecer inteiros.', TRUE, 1),
((SELECT id FROM tipo_exigencia WHERE codigo = 'COMP_RESIDENCIA'), 'Declaração de residência',
 'Só é preciso se a conta estiver no nome de outra pessoa. Pergunte ao 135 qual modelo de declaração é aceito.', FALSE, 2),
((SELECT id FROM tipo_exigencia WHERE codigo = 'DOC_IDENTIDADE'), 'RG ou CNH (frente e verso)',
 'Fotografe com boa luz, sem cortar as bordas, mostrando frente e verso.', TRUE, 1),
((SELECT id FROM tipo_exigencia WHERE codigo = 'DOC_IDENTIDADE'), 'Comprovante do CPF (se o número não constar no documento)',
 'O número do CPF pode ser consultado no app ou no site da Receita Federal.', FALSE, 2),
((SELECT id FROM tipo_exigencia WHERE codigo = 'VINCULO_TRABALHO'), 'Carteira de Trabalho (páginas de identificação e de contratos)',
 'Fotografe as páginas com foto, qualificação e os registros de contrato, bem legíveis.', TRUE, 1),
((SELECT id FROM tipo_exigencia WHERE codigo = 'VINCULO_TRABALHO'), 'Contrato ou declaração do empregador (se houver)',
 'Peça ao empregador um documento com datas de início e fim, cargo e assinatura.', FALSE, 2),
((SELECT id FROM tipo_exigencia WHERE codigo = 'VINCULO_TRABALHO'), 'Holerites ou comprovantes de pagamento do período',
 'Procure os holerites no RH da empresa ou nos aplicativos de folha de pagamento.', FALSE, 3),
((SELECT id FROM tipo_exigencia WHERE codigo = 'LAUDO_MEDICO'), 'Laudo ou relatório médico recente, legível e assinado',
 'Peça ao seu médico um relatório atualizado, com assinatura e número do CRM, e confira se todas as páginas estão legíveis.', TRUE, 1),
((SELECT id FROM tipo_exigencia WHERE codigo = 'LAUDO_MEDICO'), 'Exames que acompanham o laudo (se houver)',
 'Junte os exames que o médico citou no relatório.', FALSE, 2),
((SELECT id FROM tipo_exigencia WHERE codigo = 'PERIODO_RURAL'), 'Autodeclaração de atividade rural',
 'O INSS orienta o modelo. Peça na agência ou no 135.', TRUE, 1),
((SELECT id FROM tipo_exigencia WHERE codigo = 'PERIODO_RURAL'), 'Documentos de terra ou de produção (se houver)',
 'Notas de produtor, contratos de arrendamento, declarações do sindicato rural.', FALSE, 2);

-- ---------- Cidadãos fictícios (ordem = número do perfil) ----------
INSERT INTO cidadao (nome_ficticio, faixa_etaria, tipo_acesso, representa_id) VALUES
('Maria das Dores Silva',   '60+',   'TITULAR',                      NULL),  -- 1  fluxo principal
('Carlos Eduardo Lima',     '40-59', 'TITULAR',                      NULL),  -- 2  prazo curto, laudo
('Ana Paula Souza',         '18-39', 'TITULAR',                      NULL),  -- 3  prazo vencido
('Rita de Cássia Alves',    '40-59', 'REPRESENTANTE_NAO_HABILITADO', NULL),  -- 4  representa o 10 sem habilitação
('Marta Ferreira',          '40-59', 'REPRESENTANTE_HABILITADO',     NULL),  -- 5  representa o 11 com habilitação
('Raimundo Nonato Pereira', '60+',   'TITULAR',                      NULL),  -- 6  pedido indeferido
('Lúcia Helena Barbosa',    '40-59', 'TITULAR',                      NULL),  -- 7  dados divergentes
('José Antunes de Oliveira','60+',   'TITULAR',                      NULL),  -- 8  atividade rural (caso complexo)
('Beatriz Moreira',         '18-39', 'TITULAR',                      NULL),  -- 9  sem exigência pendente
('Antônio Alves',           '60+',   'TITULAR',                      NULL),  -- 10 representado da Rita
('Josefa Rodrigues',        '60+',   'TITULAR',                      NULL);  -- 11 representada da Marta

UPDATE cidadao SET representa_id = (SELECT id FROM cidadao WHERE nome_ficticio = 'Antônio Alves')
 WHERE nome_ficticio = 'Rita de Cássia Alves';
UPDATE cidadao SET representa_id = (SELECT id FROM cidadao WHERE nome_ficticio = 'Josefa Rodrigues')
 WHERE nome_ficticio = 'Marta Ferreira';

-- ---------- Pedidos fictícios ----------
INSERT INTO pedido (cidadao_id, protocolo, beneficio, status, dados_divergentes, data_pedido) VALUES
((SELECT id FROM cidadao WHERE nome_ficticio = 'Maria das Dores Silva'),    'FICT-0001', 'Aposentadoria por idade',                    'COM_EXIGENCIA', FALSE, DATEADD('DAY', -20, CURRENT_DATE)),
((SELECT id FROM cidadao WHERE nome_ficticio = 'Carlos Eduardo Lima'),      'FICT-0002', 'Auxílio por incapacidade temporária',         'COM_EXIGENCIA', FALSE, DATEADD('DAY', -40, CURRENT_DATE)),
((SELECT id FROM cidadao WHERE nome_ficticio = 'Ana Paula Souza'),          'FICT-0003', 'Salário-maternidade',                         'COM_EXIGENCIA', FALSE, DATEADD('DAY', -70, CURRENT_DATE)),
((SELECT id FROM cidadao WHERE nome_ficticio = 'Antônio Alves'),            'FICT-0010', 'Aposentadoria por idade',                    'COM_EXIGENCIA', FALSE, DATEADD('DAY', -25, CURRENT_DATE)),
((SELECT id FROM cidadao WHERE nome_ficticio = 'Josefa Rodrigues'),         'FICT-0011', 'Benefício assistencial (BPC)',               'COM_EXIGENCIA', FALSE, DATEADD('DAY', -22, CURRENT_DATE)),
((SELECT id FROM cidadao WHERE nome_ficticio = 'Raimundo Nonato Pereira'),  'FICT-0006', 'Aposentadoria por tempo de contribuição',     'INDEFERIDO',    FALSE, DATEADD('DAY', -90, CURRENT_DATE)),
((SELECT id FROM cidadao WHERE nome_ficticio = 'Lúcia Helena Barbosa'),     'FICT-0007', 'Pensão por morte',                           'COM_EXIGENCIA', TRUE,  DATEADD('DAY', -30, CURRENT_DATE)),
((SELECT id FROM cidadao WHERE nome_ficticio = 'José Antunes de Oliveira'), 'FICT-0008', 'Aposentadoria por idade (trabalhador rural)', 'COM_EXIGENCIA', FALSE, DATEADD('DAY', -35, CURRENT_DATE)),
((SELECT id FROM cidadao WHERE nome_ficticio = 'Beatriz Moreira'),          'FICT-0009', 'Salário-maternidade',                         'EM_ANALISE',    FALSE, DATEADD('DAY', -12, CURRENT_DATE));

-- ---------- Exigências fictícias (prazo padrão = 30 dias corridos) ----------
INSERT INTO exigencia (pedido_id, tipo_exigencia_id, texto_oficial, data_emissao, data_prazo, status) VALUES
((SELECT id FROM pedido WHERE protocolo = 'FICT-0001'), (SELECT id FROM tipo_exigencia WHERE codigo = 'COMP_RESIDENCIA'),
 'Apresentar comprovante de residência atualizado em nome do requerente.',
 DATEADD('DAY', -10, CURRENT_DATE), DATEADD('DAY', 20, CURRENT_DATE), 'ABERTA'),
((SELECT id FROM pedido WHERE protocolo = 'FICT-0002'), (SELECT id FROM tipo_exigencia WHERE codigo = 'LAUDO_MEDICO'),
 'Apresentar laudo ou relatório médico atualizado, legível e assinado pelo profissional assistente.',
 DATEADD('DAY', -26, CURRENT_DATE), DATEADD('DAY', 4, CURRENT_DATE), 'ABERTA'),
((SELECT id FROM pedido WHERE protocolo = 'FICT-0003'), (SELECT id FROM tipo_exigencia WHERE codigo = 'VINCULO_TRABALHO'),
 'Apresentar documentos que comprovem o vínculo empregatício no período informado.',
 DATEADD('DAY', -45, CURRENT_DATE), DATEADD('DAY', -15, CURRENT_DATE), 'ABERTA'),
((SELECT id FROM pedido WHERE protocolo = 'FICT-0010'), (SELECT id FROM tipo_exigencia WHERE codigo = 'COMP_RESIDENCIA'),
 'Apresentar comprovante de residência atualizado em nome do requerente.',
 DATEADD('DAY', -8, CURRENT_DATE), DATEADD('DAY', 22, CURRENT_DATE), 'ABERTA'),
((SELECT id FROM pedido WHERE protocolo = 'FICT-0011'), (SELECT id FROM tipo_exigencia WHERE codigo = 'DOC_IDENTIDADE'),
 'Apresentar documento oficial de identificação com foto e número de CPF.',
 DATEADD('DAY', -5, CURRENT_DATE), DATEADD('DAY', 25, CURRENT_DATE), 'ABERTA'),
((SELECT id FROM pedido WHERE protocolo = 'FICT-0007'), (SELECT id FROM tipo_exigencia WHERE codigo = 'DOC_IDENTIDADE'),
 'Apresentar documento oficial de identificação com foto e número de CPF.',
 DATEADD('DAY', -7, CURRENT_DATE), DATEADD('DAY', 23, CURRENT_DATE), 'ABERTA'),
((SELECT id FROM pedido WHERE protocolo = 'FICT-0008'), (SELECT id FROM tipo_exigencia WHERE codigo = 'PERIODO_RURAL'),
 'Esclarecer e comprovar os períodos de atividade rural declarados.',
 DATEADD('DAY', -9, CURRENT_DATE), DATEADD('DAY', 21, CURRENT_DATE), 'ABERTA');
