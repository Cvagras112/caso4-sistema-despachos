# Abre las dos apps y las deja conectadas al broker.
. "$PSScriptRoot\uia.ps1"

Write-Host "== APP 1: Central de Despacho =="
App "-n com.proyecto.despachos.central/.MainActivity"
Start-Sleep -Seconds 5
$xml = Volcar-Ui
Tocar-Por-Id $xml "btnConectar" | Out-Null
Start-Sleep -Seconds 6
$xml = Volcar-Ui
Write-Host ("Central: '" + (Texto-De $xml "tvEstadoConexion") + "'")

Write-Host "== APP 2: Repartidor =="
App "-n com.proyecto.despachos.repartidor/.ConexionActivity"
Start-Sleep -Seconds 5
$xml = Volcar-Ui
if (-not (Tocar-Por-Id $xml "spZona")) {
    Write-Host "aviso: el dump no mostro el spinner, reintento tras 3s"
    Start-Sleep -Seconds 3
    $xml = Volcar-Ui
    Tocar-Por-Id $xml "spZona" | Out-Null
}
Start-Sleep -Seconds 2
$xml = Volcar-Ui
Tocar-Por-Texto $xml "Centro" | Out-Null
Start-Sleep -Seconds 2
$xml = Volcar-Ui
Tocar-Por-Id $xml "btnConectar" | Out-Null
Start-Sleep -Seconds 10
$xml = Volcar-Ui
if (Contiene-Texto $xml "Despachos") {
    Write-Host "Repartidor: conectado y en el listado de su zona"
} else {
    $e = Texto-De $xml "tvEstadoConexion"
    Write-Host ("Repartidor: '" + $e + "' (pantalla de conexion)")
}

Write-Host "== Trayectoria final: ambas apps con procesos vivos =="
$p = cmd /c "adb shell ps -A 2>&1"
($p | Select-String "despachos.central") | Select-Object -First 1
($p | Select-String "despachos.repartidor") | Select-Object -First 1
