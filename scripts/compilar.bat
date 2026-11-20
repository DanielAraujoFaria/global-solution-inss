@echo off
rem Compila o projeto no Windows (exige JDK 17 ou superior no PATH).
cd /d "%~dp0\.."
if exist out\classes rmdir /s /q out\classes
mkdir out\classes
dir /s /b src\main\java\*.java > out\fontes.txt
javac -encoding UTF-8 --release 17 -d out\classes -cp lib\h2-2.2.224.jar @out\fontes.txt
xcopy /s /y /q src\main\resources\* out\classes\ > nul
echo Compilado em out\classes
