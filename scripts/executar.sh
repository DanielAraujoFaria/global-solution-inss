#!/usr/bin/env bash
# Executa o assistente. Exemplos:  ./scripts/executar.sh   |   ./scripts/executar.sh --demo   |   ./scripts/executar.sh --ajuda
cd "$(dirname "$0")/.."
[ -d out/classes ] || ./scripts/compilar.sh
exec java -Dfile.encoding=UTF-8 -cp "out/classes:lib/h2-2.2.224.jar" br.com.fiap.citizentech.inss.App "$@"
