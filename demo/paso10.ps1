# ERR-01: con el broker detenido, la Central muestra el error y permite reintentar.
. "$PSScriptRoot\uia.ps1"

# Detener el broker (proceso java del broker Moquette)
$pids = Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -match 'Broker' }
foreach ($p in $pids) {
    Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
    Write-Host ("Broker detenido (PID " + $p.ProcessId + ")")
}
Start-Sleep -Seconds 3
$cerrado = -not (Test-NetConnection -ComputerName 127.0.0.1 -Port 1883 -InformationLevel Quiet -WarningAction SilentlyContinue)
Write-Host ("Puerto 1883 cerrado: " + $cerrado)

# La Central intenta conectar -> ERR-01
App "-n com.proyecto.despachos.central/.MainActivity"
Start-Sleep -Seconds 3
$xml = Volcar-Ui
Tocar-Por-Id $xml "btnConectar" | Out-Null
Start-Sleep -Seconds 14
$xml = Volcar-Ui
$estado = Texto-De $xml "tvEstadoConexion"
$hayToast = $xml -match 'No se pudo conectar al broker'
Write-Host ("Indicador con broker apagado: '" + $estado + "'")
Write-Host ("Toast ERR-01 visible: " + $hayToast)
Foto "10-err01-broker-apagado.png"

# Reiniciar el broker para dejar el ambiente listo
$bat = Join-Path $PSScriptRoot "..\broker\build\install\broker\bin\broker.bat"
Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "`"$bat`" > broker-salida.log 2>&1" -WorkingDirectory (Join-Path $PSScriptRoot "..\broker") -WindowStyle Hidden
$encendido = $false
foreach ($i in 1..20) {
    Start-Sleep -Seconds 1
    if (Test-NetConnection -ComputerName 127.0.0.1 -Port 1883 -InformationLevel Quiet -WarningAction SilentlyContinue) {
        $encendido = $true
        break
    }
}
Write-Host ("Broker reiniciado: " + $encendido)
