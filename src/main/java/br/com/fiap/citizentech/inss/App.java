package br.com.fiap.citizentech.inss;

import br.com.fiap.citizentech.inss.analise.Exportador;
import br.com.fiap.citizentech.inss.analise.RelatorioIndicadores;
import br.com.fiap.citizentech.inss.analise.ResumoDoBanco;
import br.com.fiap.citizentech.inss.dao.ConversaDao;
import br.com.fiap.citizentech.inss.db.BancoDeDados;
import br.com.fiap.citizentech.inss.db.BdLog;
import br.com.fiap.citizentech.inss.db.Conexao;
import br.com.fiap.citizentech.inss.modelo.Origem;
import br.com.fiap.citizentech.inss.servico.AssistenteFactory;
import br.com.fiap.citizentech.inss.servico.AssistenteService;
import br.com.fiap.citizentech.inss.ui.CanalConsole;
import br.com.fiap.citizentech.inss.ui.CanalRoteirizado;
import br.com.fiap.citizentech.inss.validacao.CenariosDeValidacao;
import br.com.fiap.citizentech.inss.validacao.CenariosDeValidacao.ResultadoCenario;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Clock;
import java.util.List;

/**
 * Assistente de Serviços do INSS pelo WhatsApp (protótipo acadêmico, dados fictícios).
 * Veja os modos de execução com --ajuda.
 */
public final class App {
    private static final String AJUDA = """
            Assistente de Serviços do INSS pelo WhatsApp - protótipo acadêmico (dados fictícios)

            Uso: java -cp "out/classes:lib/h2-2.2.224.jar" br.com.fiap.citizentech.inss.App [modo]

              (sem modo) | --interativo   Conversa no terminal. Opções: --sql (mostra as operações no banco)
                                           e --teste P01 (marca a sessão como teste com pessoa real)
              --demo                       Conversa roteirizada (para o vídeo), mostrando o banco em ação
              --consultar                  Mostra o que está gravado no banco
              --simular                    Roda os cenários de validação e imprime o relatório de indicadores
              --relatorio                  Relatório de indicadores a partir do banco em arquivo
              --exportar <pasta>           Exporta CSV (conversas e avaliações) para Excel/Power BI
              --restaurar-pedidos          Volta pedidos e checklists ao início, SEM apagar os dados coletados
              --reiniciar                  Apaga e recria o banco em arquivo com os dados fictícios
              --ajuda                      Esta mensagem
            """;

    private App() {
    }

    public static void main(String[] args) throws Exception {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        List<String> a = List.of(args);
        String modo = a.isEmpty() ? "--interativo" : a.get(0);
        switch (modo) {
            case "--interativo" -> interativo(out, a);
            case "--demo" -> demo(out);
            case "--consultar" -> consultar(out);
            case "--simular" -> System.exit(simular(out));
            case "--relatorio" -> relatorio(out);
            case "--exportar" -> exportar(out, a.size() > 1 ? a.get(1) : "exportacao");
            case "--restaurar-pedidos" -> restaurarPedidos(out);
            case "--reiniciar" -> reiniciar(out);
            default -> out.print(AJUDA);
        }
    }

    // ------------------------------------------------------------------ modos

    private static void interativo(PrintStream out, List<String> args) throws Exception {
        String rotulo = args.contains("--teste") && args.indexOf("--teste") + 1 < args.size()
                ? args.get(args.indexOf("--teste") + 1) : null;
        Origem origem = rotulo != null ? Origem.TESTE_USUARIO : Origem.DEMONSTRACAO;
        try (Connection con = Conexao.abrir()) {
            BancoDeDados.prepararSeNecessario(con);
            if (args.contains("--sql")) {
                BdLog.ativar(out);
            }
            AssistenteService servico = AssistenteFactory.criar(con);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            boolean outro = true;
            while (outro) {
                if (rotulo != null) {
                    // em teste com pessoas, todo participante começa do mesmo estado dos pedidos
                    BancoDeDados.restaurarPedidos(con);
                }
                AssistenteService.Resultado r = servico.atender(new CanalConsole(entrada, out), origem, rotulo);
                out.println("== Atendimento encerrado. Desfecho: " + r.desfecho()
                        + (r.motivoEncaminhamento() == null ? "" : " (" + r.motivoEncaminhamento() + ")") + " ==");
                out.print("Iniciar outro atendimento? (s/n) > ");
                String resposta = entrada.readLine();
                outro = resposta != null && resposta.trim().equalsIgnoreCase("s");
            }
        }
    }

    private static void demo(PrintStream out) throws Exception {
        try (Connection con = Conexao.abrir()) {
            BancoDeDados.reiniciar(con);
            out.println(">>> DEMONSTRAÇÃO: banco reiniciado com dados fictícios. Linhas [BD] mostram o que é gravado.\n");
            BdLog.ativar(out);
            AssistenteService servico = AssistenteFactory.criar(con);
            CanalRoteirizado canal = new CanalRoteirizado(out,
                    "minha senha é 123456",                          // tentativa de enviar senha: é bloqueada
                    "1", "1", "0", "1", "1", "1",                    // pendência, motivo, sem relato, autoriza, perfil Maria, autoriza
                    "1", "2",                                        // conta recente: tenho; declaração: não tenho
                    "1",                                             // confirma o envio
                    "5", "5", "1");                                  // avaliação
            servico.atender(canal, Origem.DEMONSTRACAO, "demo");
            BdLog.desativar();
            out.println(">>> DADOS GRAVADOS NO BANCO (continuam lá depois que o programa fecha):\n");
            ResumoDoBanco.imprimir(con, out);
        }
    }

    private static void consultar(PrintStream out) throws Exception {
        try (Connection con = Conexao.abrir()) {
            BancoDeDados.prepararSeNecessario(con);
            ResumoDoBanco.imprimir(con, out);
        }
    }

    /** @return código de saída: 0 se todos os cenários passaram, 1 caso contrário */
    private static int simular(PrintStream out) throws Exception {
        try (Connection con = Conexao.emMemoria("simulacao")) {
            List<ResultadoCenario> resultados = CenariosDeValidacao.executarTodos(con, Clock.systemDefaultZone());
            out.println("=== VALIDAÇÃO POR CENÁRIOS ===");
            out.printf("%-4s %-7s %-22s %-34s %s%n", "ID", "Status", "Desfecho obtido", "Motivo do encaminhamento", "Cenário");
            int ok = 0;
            for (ResultadoCenario r : resultados) {
                if (r.ok()) {
                    ok++;
                }
                out.printf("%-4s %-7s %-22s %-34s %s%n", r.cenario().id(), r.ok() ? "OK" : "FALHA",
                        r.obtido().desfecho(),
                        r.obtido().motivoEncaminhamento() == null ? "-" : r.obtido().motivoEncaminhamento(),
                        r.cenario().descricao());
                for (String falha : r.falhas()) {
                    out.println("       -> " + falha);
                }
            }
            out.printf("%nCenários aprovados: %d de %d%n%n", ok, resultados.size());
            out.println(RelatorioIndicadores.gerar(con, Origem.SIMULACAO));
            return ok == resultados.size() ? 0 : 1;
        }
    }

    private static void relatorio(PrintStream out) throws Exception {
        try (Connection con = Conexao.abrir()) {
            BancoDeDados.prepararSeNecessario(con);
            ConversaDao conversas = new ConversaDao(con);
            boolean algum = false;
            for (Origem origem : Origem.values()) {
                if (!conversas.listar(origem).isEmpty()) {
                    out.println(RelatorioIndicadores.gerar(con, origem));
                    algum = true;
                }
            }
            if (!algum) {
                out.println("Ainda não há conversas gravadas. Use o modo interativo ou --demo.");
            }
        }
    }

    private static void exportar(PrintStream out, String pasta) throws Exception {
        try (Connection con = Conexao.abrir()) {
            BancoDeDados.prepararSeNecessario(con);
            for (Path p : Exportador.exportar(con, Path.of(pasta))) {
                out.println("Gerado: " + p.toAbsolutePath());
            }
        }
    }

    private static void restaurarPedidos(PrintStream out) throws Exception {
        try (Connection con = Conexao.abrir()) {
            BancoDeDados.prepararSeNecessario(con);
            BancoDeDados.restaurarPedidos(con);
            out.println("Pedidos e checklists voltaram ao estado inicial. Conversas e avaliações foram mantidas.");
        }
    }

    private static void reiniciar(PrintStream out) throws Exception {
        try (Connection con = Conexao.abrir()) {
            BancoDeDados.reiniciar(con);
            out.println("Banco recriado com os dados fictícios.");
        }
    }
}
