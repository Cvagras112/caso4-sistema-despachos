# Diagnóstico: muestra los textos visibles en pantalla.
. "$PSScriptRoot\uia.ps1"
$x = Volcar-Ui
Write-Host "Longitud del dump: $($x.Length)"
$extraidos = [regex]::Matches($x, 'text="[^"]{1,80}"') | ForEach-Object { $_.Value }
$extraidos | Select-Object -First 25
