# Evidencia final del botón Atrás: el menú principal del repartidor visible.
. "$PSScriptRoot\uia.ps1"
$x = Volcar-Ui
Write-Host ("Pantalla actual: " + (([regex]::Matches($x, 'text="[^"]{2,40}"') | ForEach-Object { $_.Value }) -join " | "))
Foto "12-atras-menu-principal-repartidor.png"

# Cambiar a la Central y capturar su menu principal
App "-n com.proyecto.despachos.central/.MainActivity"
Start-Sleep -Seconds 4
Foto "12-navegacion-atras-central-menu.png"
