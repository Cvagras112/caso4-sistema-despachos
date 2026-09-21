# Inicia únicamente la App de administración (Central) y la deja conectada.
. "$PSScriptRoot\uia.ps1"
App "-n com.proyecto.despachos.central/.MainActivity"
Start-Sleep -Seconds 5
$xml = Volcar-Ui
if (Tocar-Por-Id $xml "btnConectar") {
    Start-Sleep -Seconds 6
    $xml = Volcar-Ui
    Write-Host ("Central: '" + (Texto-De $xml "tvEstadoConexion") + "'")
    Foto "13-app-administracion-iniciada.png"
} else {
    Write-Host "aviso: reintento de dump en 4s"
    Start-Sleep -Seconds 4
    $xml = Volcar-Ui
    if (Tocar-Por-Id $xml "btnConectar") {
        Start-Sleep -Seconds 6
        $xml = Volcar-Ui
        Write-Host ("Central: '" + (Texto-De $xml "tvEstadoConexion") + "'")
        Foto "13-app-administracion-iniciada.png"
    } else {
        Write-Host "FALLO: la pantalla de la Central no aparece"
    }
}
