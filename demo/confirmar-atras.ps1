# Confirmación limpia: Repartidor Atrás desde el listado -> menú principal.
. "$PSScriptRoot\uia.ps1"
App "-n com.proyecto.despachos.repartidor/.ConexionActivity"
Start-Sleep -Seconds 4
$xml = Volcar-Ui
if (Contiene-Texto $xml "Conectar y ver despachos") {
    Tocar-Por-Id $xml "btnConectar" | Out-Null
    Start-Sleep -Seconds 10
    $xml = Volcar-Ui
}
$enLista = Contiene-Texto $xml "Despachos"
Write-Host ("Pantalla actual es el listado: " + $enLista)
cmd /c "adb shell input keyevent 4 >nul 2>&1"
Start-Sleep -Seconds 3
$xml = Volcar-Ui
$enMenu = Contiene-Texto $xml "Zona de trabajo"
Write-Host ("Tras Atras esta en el menu principal: " + $enMenu)
if ($enLista -and $enMenu) { Write-Host "OK: Repartidor Atras listado -> menu" } else { Write-Host "FALLO" }
