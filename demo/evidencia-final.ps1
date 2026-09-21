# Evidencias finales de los botones "Volver al inicio".
. "$PSScriptRoot\uia.ps1"
Foto "15-boton-volver-inicio-central.png"
App "-n com.proyecto.despachos.repartidor/.ConexionActivity"
Start-Sleep -Seconds 4
Foto "14-boton-volver-inicio-repartidor.png"
Write-Host "Estado del broker Mosquitto:"
Get-CimInstance Win32_Process -Filter "Name='mosquitto.exe'" | ForEach-Object { "PID $($_.ProcessId) activo" }
Test-NetConnection -ComputerName 127.0.0.1 -Port 1883 -InformationLevel Quiet -WarningAction SilentlyContinue
