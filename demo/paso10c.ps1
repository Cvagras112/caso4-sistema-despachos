# Reintento de conexion tras ERR-01: el broker vuelve y la Central se reconecta.
. "$PSScriptRoot\uia.ps1"

$enc = $false
foreach ($i in 1..40) {
    if (Test-NetConnection -ComputerName 127.0.0.1 -Port 1883 -InformationLevel Quiet -WarningAction SilentlyContinue) {
        $enc = $true
        break
    }
    Start-Sleep -Seconds 1
}
Write-Host ("Broker arriba: " + $enc)

App "-n com.proyecto.despachos.central/.MainActivity"
Start-Sleep -Seconds 3
$xml = Volcar-Ui
Tocar-Por-Id $xml "btnConectar" | Out-Null
Start-Sleep -Seconds 6
$xml = Volcar-Ui
$estado = Texto-De $xml "tvEstadoConexion"
Write-Host ("Indicador tras reintento: '" + $estado + "'")
Foto "10-reintento-exitoso.png"
if ($estado -match "Conectado") {
    Write-Host "RESULTADO: reintento exitoso tras ERR-01 (FA-02 / RNF-07)"
} else {
    Write-Host "RESULTADO FALLO"
}
