@echo off
chcp 65001 >nul

rem Garante que o MySQL esta rodando
tasklist /FI "IMAGENAME eq mysqld.exe" 2>nul | find /I "mysqld.exe" >nul
if errorlevel 1 (
    echo Iniciando o MySQL...
    start "MySQL" /min "C:\Users\redgs\mysql\bin\mysqld.exe" --defaults-file="C:\Users\redgs\mysql\my.ini" --console
    timeout /t 6 >nul
)

rem Sobe o servidor web e abre o navegador
start "Controle Financeiro (servidor)" /min cmd /c java -cp "out;lib/*" br.com.gsanches.financeiro.web.MainWeb
timeout /t 3 >nul
start http://localhost:8080
echo.
echo  O Controle Financeiro esta rodando em http://localhost:8080
echo  (o servidor roda em uma janela minimizada - feche-a para parar)
echo.
pause
