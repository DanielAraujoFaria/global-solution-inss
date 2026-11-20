@echo off
rem Compila e roda os testes automatizados no Windows.
cd /d "%~dp0\.."
call scripts\compilar.bat
if exist out\test-classes rmdir /s /q out\test-classes
mkdir out\test-classes
dir /s /b src\test\java\*.java > out\fontes-teste.txt
set CP=out\classes;lib\h2-2.2.224.jar;out\test-classes;lib-teste\junit-4.13.2.jar;lib-teste\hamcrest-core-1.3.jar
javac -encoding UTF-8 --release 17 -d out\test-classes -cp "%CP%" @out\fontes-teste.txt
java -Dfile.encoding=UTF-8 -cp "%CP%" org.junit.runner.JUnitCore br.com.fiap.citizentech.inss.AssistenteServiceTest br.com.fiap.citizentech.inss.AutenticacaoSimuladaTest br.com.fiap.citizentech.inss.AvaliadorDeCasoTest br.com.fiap.citizentech.inss.BancoDeDadosTest br.com.fiap.citizentech.inss.CenariosDeValidacaoTest br.com.fiap.citizentech.inss.DaoTest br.com.fiap.citizentech.inss.DetectorDadosSensiveisTest br.com.fiap.citizentech.inss.EstatisticaTest br.com.fiap.citizentech.inss.ExportadorTest br.com.fiap.citizentech.inss.GatewayTest br.com.fiap.citizentech.inss.IndicadoresTest br.com.fiap.citizentech.inss.ModeloTest br.com.fiap.citizentech.inss.TransacaoTest
