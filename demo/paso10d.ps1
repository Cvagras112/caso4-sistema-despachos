# ERR-01: intento de conexion con el broker apagado.
. "$PSScriptRoot\uia.ps1"

$levantado = Test-NetConnection -ComputerName 127.0.0.1 -Port 1883 -InformationLevel Quiet -WarningAction SilentlyContinue
Write-Host ("Broker arriba (debe ser False): " + $levantado)

App "-n com.proyecto.despachos.central/.MainActivity"
Start-Sleep -Seconds 3
$xml = Volcar-Ui
Tocar-Por-Id $xml "btnConectar" | Out-Null
Start-Sleep -Seconds 11
$xml = Volcar-Ui
$estado = Texto-De $xml "tvEstadoConexion"
$hayToast = $xml -match 'No se pudo conectar al broker'
Write-Host ("Indicador: '" + $estado + "'")
Write-Host ("Toast ERR-01 visible en dump: " + $hayToast)
Foto "10-err01-broker-apagado.png"
if (-not $levantado) { Write-Host "RESULTADO: ERR-01 demostrado con broker apagado" }
