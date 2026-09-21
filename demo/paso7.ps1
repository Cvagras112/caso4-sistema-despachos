# Paso 7 alternativo: desliza y pulsa Confirmar entrega.
. "$PSScriptRoot\uia.ps1"
Deslizar-Arriba
$xml = Volcar-Ui
if (Tocar-Por-Id $xml "btnConfirmarEntrega") {
    Start-Sleep -Seconds 3
    $xml = Volcar-Ui
    $estado = Texto-De $xml "tvDetalleEstado"
    Write-Host ("Estado en detalle: '" + $estado + "'")
    Foto "07-despacho-entregado.png"
    if ($estado -eq "Entregado") { Write-Host "RESULTADO PASO 7: OK" } else { Write-Host "RESULTADO PASO 7: FALLO" }
} else {
    Deslizar-Arriba
    $xml = Volcar-Ui
    if (Tocar-Por-Id $xml "btnConfirmarEntrega") {
        Start-Sleep -Seconds 3
        $xml = Volcar-Ui
        $estado = Texto-De $xml "tvDetalleEstado"
        Write-Host ("Estado en detalle: '" + $estado + "'")
        Foto "07-despacho-entregado.png"
        if ($estado -eq "Entregado") { Write-Host "RESULTADO PASO 7: OK" } else { Write-Host "RESULTADO PASO 7: FALLO" }
    } else {
        Write-Host "RESULTADO PASO 7: FALLO (boton no visible incluso tras deslizar)"
    }
}
