# Verifica el estado del despacho en la Central y completa el ciclo si falta.
. "$PSScriptRoot\uia.ps1"
App "-n com.proyecto.despachos.central/.MainActivity"
Start-Sleep -Seconds 4
$xml = Volcar-Ui
$estados = [regex]::Matches($xml, 'text="(Pendiente|Aceptado|En camino|Entregado)"') | ForEach-Object { $_.Groups[1].Value }
Write-Host ("Central ve: " + ($estados -join ", "))
