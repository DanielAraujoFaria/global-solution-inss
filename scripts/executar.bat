@echo off
rem Executa o assistente no Windows. Exemplos:  executar.bat   |   executar.bat --demo   |   executar.bat --ajuda
cd /d "%~dp0\.."
chcp 65001 > nul
if not exist out\classes call scripts\compilar.bat
java -Dfile.encoding=UTF-8 -cp "out\classes;lib\h2-2.2.224.jar" br.com.fiap.citizentech.inss.App %*
