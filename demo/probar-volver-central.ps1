# Prueba el boton "Volver al inicio" del DETALLE de la Central.
. "$PSScriptRoot\uia.ps1"
App "-n com.proyecto.despachos.central/.MainActivity"
Start-Sleep -Seconds 4
$xml = Volcar-Ui
Tocar-Por-Id $xml "btnVerDespachos" | Out-Null
Start-Sleep -Seconds 3
$xml = Volcar-Ui
$m = [regex]::Match($xml, 'text="(DES-[^"]+)"')
if ($m.Success) {
    Tocar-Por-Texto $xml $m.Groups[1].Value | Out-Null
    Start-Sleep -Seconds 3
    $xml = Volcar-Ui
    Deslizar-Arriba
    Deslizar-Arriba
    $xml = Volcar-Ui
    $enDetalle = Contiene-Texto $xml "Detalle del despacho"
    if (Tocar-Por-Id $xml "btnVolverInicio") {
        Start-Sleep -Seconds 4
        $xml = Volcar-Ui
        $enMenu = (Texto-De $xml "tvEstadoConexion") -ne ""
        Write-Host ("En detalle: " + $enDetalle + " | tras boton en menu: " + $enMenu)
        if ($enDetalle -and $enMenu) {
            Write-Host "OK: boton Volver al inicio (Central detalle) funciona"
            Foto "15-boton-volver-inicio-central.png"
        } else {
            Write-Host "FALLO"
        }
    } else {
        Write-Host "no se vio el detalle tras abrirlo"
    }
} else {
    Write-Host "no hay despachos en el listado central"
}
