. "$PSScriptRoot\uia.ps1"
$x = Volcar-Ui
$m = [regex]::Match($x, '<node[^>]*btnIniciarRuta[^>]*>')
if ($m.Success) {
    Write-Host ("NODO: " + $m.Value)
} else {
    Write-Host "NODO btnIniciarRuta no esta en el dump"
}
