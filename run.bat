@echo off
rem Configura o terminal para UTF-8 (acentos corretos) e inicia o programa
chcp 65001 >nul
java -cp out br.com.gsanches.financeiro.Main
pause
