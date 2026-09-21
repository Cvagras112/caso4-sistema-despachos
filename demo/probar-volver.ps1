# Prueba el boton "Volver al inicio" del listado del Repartidor.
. "$PSScriptRoot\uia.ps1"
App "-n com.proyecto.despachos.repartidor/.ConexionActivity"
Start-Sleep -Seconds 4
$xml = Volcar-Ui
if (Contiene-Texto $xml "Conectar y ver despachos") {
    Tocar-Por-Id $xml "btnConectar" | Out-Null
    Start-Sleep -Seconds 10
    $xml = Volcar-Ui
}
# Si quedamos en el detalle, volvemos al listado primero
if (Contiene-Texto $xml "Detalle del despacho") {
    cmd /c "adb shell input keyevent 4 >nul 2>&1"
    Start-Sleep -Seconds 3
    $xml = Volcar-Ui
}
$enLista = Contiene-Texto $xml "Despachos"
Write-Host ("En listado: " + $enLista)
if (Tocar-Por-Id $xml "btnVolverInicio") {
    Start-Sleep -Seconds 4
    $xml = Volcar-Ui
    $enMenu = Contiene-Texto $xml "Zona de trabajo"
    Write-Host ("Tras el boton, en menu principal: " + $enMenu)
    if ($enLista -and $enMenu) {
        Write-Host "OK: boton Volver al inicio funciona"
        Foto "14-boton-volver-inicio.png"
    } else {
        Write-Host "FALLO: el boton no regreso al menu"
    }
} else {
    Write-Host "boton Volver al inicio no visible en el listado"
}
