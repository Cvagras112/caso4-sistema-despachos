# Vuelve al inicio de la Central y la conecta.
. "$PSScriptRoot\uia.ps1"
cmd /c "adb shell input keyevent 4 >nul 2>&1"
Start-Sleep -Seconds 3
$xml = Volcar-Ui
if (Tocar-Por-Id $xml "btnConectar") {
    Start-Sleep -Seconds 6
    $xml = Volcar-Ui
    Write-Host ("Central: '" + (Texto-De $xml "tvEstadoConexion") + "'")
} else {
    Write-Host "Aun no se ve MainActivity; pantalla actual:"
    [regex]::Matches($xml, 'text="[^"]{2,50}"') | ForEach-Object { $_.Value } | Select-Object -First 8
}
