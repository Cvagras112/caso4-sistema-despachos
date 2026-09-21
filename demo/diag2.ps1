# Diagnóstico 2: busca botones en la pantalla actual.
. "$PSScriptRoot\uia.ps1"
$x = Volcar-Ui
Write-Host "btnAceptar presente: " + ($x -match 'btnAceptar')
$ids = [regex]::Matches($x, 'resource-id="[^"]*btn[^"]*"') | ForEach-Object { $_.Value }
$ids | Select-Object -First 8
$m = [regex]::Match($x, 'resource-id="[^"]*btnAceptar"[^>]*bounds="(\[[^\]]+\]\[[^\]]+\])"')
if ($m.Success) {
    Write-Host ("bounds btnAceptar: " + $m.Groups[1].Value)
} else {
    Write-Host "btnAceptar sin bounds"
}
$visibles = [regex]::Matches($x, 'text="[^"]{1,70}"') | ForEach-Object { $_.Value }
$visibles | Select-Object -First 20
