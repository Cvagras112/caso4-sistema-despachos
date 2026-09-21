# ERR-01 + FA-02: broker se apaga, la Central muestra el error; al volver, se reconecta.
. "$PSScriptRoot\uia.ps1"

# 1) Esperar a que el broker (lanzamiento paralelo) esté arriba
$enc = $false
foreach ($i in 1..30) {
    if (Test-NetConnection -ComputerName 127.0.0.1 -Port 1883 -InformationLevel Quiet -WarningAction SilentlyContinue) {
        $enc = $true
        break
    }
    Start-Sleep -Seconds 1
}
Write-Host ("Broker arriba: " + $enc)

# 2) Apagarlo con el marcador STOP (el broker lo vigila cada 500 ms)
New-Item -ItemType File -Path "STOP" -Force | Out-Null
$baj = $false
foreach ($i in 1..15) {
    Start-Sleep -Seconds 1
    if (-not (Test-NetConnection -ComputerName 127.0.0.1 -Port 1883 -InformationLevel Quiet -WarningAction SilentlyContinue)) {
        $baj = $true
        break
    }
}
Write-Host ("Broker apagado: " + $baj)

# 3) La Central (que ya perdió la conexión) muestra el indicador y permite reintentar
App "-n com.proyecto.despachos.central/.MainActivity"
Start-Sleep -Seconds 3
$xml = Volcar-Ui
if (Contiene-Texto $xml "Nuevo despacho") {
    cmd /c "adb shell input keyevent 4 >nul 2>&1"
    Start-Sleep -Seconds 2
    $xml = Volcar-Ui
}
$estado = Texto-De $xml "tvEstadoConexion"
Write-Host ("Indicador sin broker: '" + $estado + "'")
Foto "10-err01-broker-apagado.png"
Write-Host ("Toast/indicador ERR-04 visible: " + (($estado -match 'ERR-04') -or ($estado -match 'Reintentando')))

# 4) Reiniciar el broker para la prueba de reconexión
Remove-Item "STOP" -Force -ErrorAction SilentlyContinue
Write-Host "STOP retirado; esperar reconexion automatica de Paho (RNF-07)..."
$re = $false
foreach ($i in 1..60) {
    Start-Sleep -Seconds 2
    $xml = Volcar-Ui
    $estado = Texto-De $xml "tvEstadoConexion"
    if ($estado -match 'Conectado al broker|Reconectado') {
        $re = $true
        Write-Host ("Indicador tras reintento: '" + $estado + "' (intento " + $i + ")")
        break
    }
    if ($i % 10 -eq 0) { Write-Host ("Esperando reconexion... " + $i + " (" + $estado + ")") }
}
if ($re) { Write-Host "RESULTADO: ERR-01 y reconexión automática demostrados (FA-02 / RNF-07)" } else { Write-Host "RESULTADO: reconexión no visible" }
Foto "10-err01-reconexion.png"
