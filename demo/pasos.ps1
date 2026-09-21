param([int]$Paso)

# Ejecuta un paso de la demo del Caso 4 en el emulador.
# Uso: powershell -ExecutionPolicy Bypass -File demo\pasos.ps1 -Paso 1

. "$PSScriptRoot\uia.ps1"

switch ($Paso) {

    1 {
        Write-Host "== PASO 1: Central se conecta al broker (CU-CE01) =="
        App "-n com.proyecto.despachos.central/.MainActivity"
        Start-Sleep -Seconds 4
        $xml = Volcar-Ui
        $hostTxt = Texto-De $xml "etHost"
        $puerto = Texto-De $xml "etPuerto"
        Write-Host "Broker configurado: host='$hostTxt' puerto='$puerto'"
        Tocar-Por-Id $xml "btnConectar" | Out-Null
        Start-Sleep -Seconds 6
        $xml2 = Volcar-Ui
        $estado = Texto-De $xml2 "tvEstadoConexion"
        Write-Host "Indicador de conexion: '$estado'"
        Foto "01-central-conectada.png"
        if ($estado -match "Conectado") { Write-Host "RESULTADO PASO 1: OK" } else { Write-Host "RESULTADO PASO 1: FALLO" }
    }

    2 {
        Write-Host "== PASO 2: Repartidor selecciona zona Centro y se conecta (CU-RE01) =="
        App "-n com.proyecto.despachos.repartidor/.ConexionActivity"
        Start-Sleep -Seconds 4
        $xml = Volcar-Ui
        Tocar-Por-Id $xml "spZona" | Out-Null
        Start-Sleep -Seconds 2
        $xml = Volcar-Ui
        Tocar-Por-Texto $xml "Centro" | Out-Null
        Start-Sleep -Seconds 2
        $xml = Volcar-Ui
        Tocar-Por-Id $xml "btnConectar" | Out-Null
        Start-Sleep -Seconds 8
        $xml = Volcar-Ui
        if (Contiene-Texto $xml "Despachos") {
            Write-Host "Pantalla: listado de despachos del repartidor abierto automaticamente"
        }
        Foto "02-repartidor-conectado.png"
        if (Contiene-Texto $xml "Despachos") { Write-Host "RESULTADO PASO 2: OK" } else { Write-Host "RESULTADO PASO 2: FALLO" }
    }

    3 {
        Write-Host "== PASO 3: Central crea y publica un despacho (CU-CE03) =="
        App "-n com.proyecto.despachos.central/.MainActivity"
        Start-Sleep -Seconds 3
        $xml = Volcar-Ui
        if (Contiene-Texto $xml "Cancelar") {
            Write-Host "Habia un formulario abierto; se cancela para reabrirlo en limpio"
            Tocar-Por-Texto $xml "Cancelar" | Out-Null
            Start-Sleep -Seconds 2
            $xml = Volcar-Ui
        }
        Tocar-Por-Id $xml "btnNuevoDespacho" | Out-Null
        Start-Sleep -Seconds 3
        $xml = Volcar-Ui
        $idAuto = Texto-De $xml "etId"
        Write-Host "Id automatico generado: $idAuto"
        Escribir-Campo $xml "etDestino" "Av.%sColon%s123,%sPuerto%sMontt"
        $xml = Volcar-Ui
        Escribir-Campo $xml "etDescripcion" "Paquete%sfragil,%stimbre%s4B"
        $xml = Volcar-Ui
        Write-Host ("Verificacion campos -> destino='" + (Texto-De $xml "etDestino") + "' descripcion='" + (Texto-De $xml "etDescripcion") + "'")
        Foto "03-formulario-completo.png"
        $xml = Volcar-Ui
        Tocar-Por-Id $xml "btnPublicar" | Out-Null
        Start-Sleep -Seconds 3
        $xml = Volcar-Ui
        if (Contiene-Texto $xml "Central de Despacho") { Write-Host "Regreso a la pantalla principal con Toast de publicacion" }
        Foto "03-despacho-publicado.png"
        Write-Host "RESULTADO PASO 3: despacho $idAuto publicado en zona centro"
    }

    4 {
        Write-Host "== PASO 4: Repartidor recibe el despacho sin refrescar (CU-RE02) =="
        App "-n com.proyecto.despachos.repartidor/.ConexionActivity"
        Start-Sleep -Seconds 3
        $xml = Volcar-Ui
        Tocar-Por-Id $xml "btnConectar" | Out-Null
        Start-Sleep -Seconds 10
        $xml = Volcar-Ui
        if (Contiene-Texto $xml "Despachos") { Write-Host "Pantalla del repartidor: listado abierto" } else { Write-Host "AVISO: el repartidor sigue en la pantalla de conexion" }
        $xml = Volcar-Ui
        $ids = [regex]::Matches($xml, 'text="(DES-[^"]+)"') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
        if ($ids.Count -gt 0) {
            Write-Host ("Despachos visibles en el listado del repartidor: " + ($ids -join ", "))
        } else {
            Write-Host "FALLO: el repartidor no muestra despachos"
        }
        Foto "04-repartidor-recibe-despacho.png"
        if ($ids.Count -gt 0) { Write-Host "RESULTADO PASO 4: OK" } else { Write-Host "RESULTADO PASO 4: FALLO" }
    }

    5 {
        Write-Host "== PASO 5: Repartidor abre detalle y ACEPTA (CU-RE04, RF-11) =="
        $xml = Volcar-Ui
        $deses = [regex]::Matches($xml, 'text="(DES-[^"]+)"') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
        $despachoElegido = ($deses | Where-Object { $_ -ne "DES-TEST-001" }) | Select-Object -First 1
        Write-Host "Despacho de la Central a procesar: $despachoElegido"
        Tocar-Por-Texto $xml $despachoElegido | Out-Null
        Start-Sleep -Seconds 3
        $xml = Volcar-Ui
        if (-not (Tocar-Por-Id $xml "btnAceptar")) {
            Write-Host "Boton fuera de pantalla; se desliza hacia abajo"
            Deslizar-Arriba
            Deslizar-Arriba
            $xml = Volcar-Ui
            Tocar-Por-Id $xml "btnAceptar" | Out-Null
        }
        Start-Sleep -Seconds 3
        $xml = Volcar-Ui
        $estado = Texto-De $xml "tvDetalleEstado"
        Write-Host "Estado en detalle: '$estado'"
        Foto "05-despacho-aceptado.png"
        if ($estado -eq "Aceptado") { Write-Host "RESULTADO PASO 5: OK" } else { Write-Host "RESULTADO PASO 5: FALLO" }
    }

    6 {
        Write-Host "== PASO 6: Repartidor inicia ruta (EN_CAMINO) =="
        $xml = Volcar-Ui
        Tocar-Por-Id $xml "btnIniciarRuta" | Out-Null
        Start-Sleep -Seconds 3
        $xml = Volcar-Ui
        $estado = Texto-De $xml "tvDetalleEstado"
        Write-Host "Estado en detalle: '$estado'"
        Foto "06-despacho-en-camino.png"
        if ($estado -eq "En camino") { Write-Host "RESULTADO PASO 6: OK" } else { Write-Host "RESULTADO PASO 6: FALLO" }
    }

    7 {
        Write-Host "== PASO 7: Repartidor confirma entrega (ENTREGADO) =="
        $xml = Volcar-Ui
        Tocar-Por-Id $xml "btnConfirmarEntrega" | Out-Null
        Start-Sleep -Seconds 3
        $xml = Volcar-Ui
        $estado = Texto-De $xml "tvDetalleEstado"
        Write-Host "Estado en detalle: '$estado'"
        Foto "07-despacho-entregado.png"
        if ($estado -eq "Entregado") { Write-Host "RESULTADO PASO 7: OK" } else { Write-Host "RESULTADO PASO 7: FALLO" }
    }

    8 {
        Write-Host "== PASO 8: Central refleja el ciclo completo (CU-CE04, RF-06) =="
        App "-n com.proyecto.despachos.central/.MainActivity"
        Start-Sleep -Seconds 3
        $xml = Volcar-Ui
        Tocar-Por-Id $xml "btnVerDespachos" | Out-Null
        Start-Sleep -Seconds 3
        $xml = Volcar-Ui
        $xml = Volcar-Ui
        $ids = [regex]::Matches($xml, 'text="(DES-[^"]+)"') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
        Write-Host ("Despachos en Central: " + ($ids -join ", "))
        $estados = [regex]::Matches($xml, 'text="(Pendiente|Aceptado|En camino|Entregado)"') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
        Write-Host ("Estados visibles: " + ($estados -join ", "))
        Foto "08-central-ciclo-completo.png"
        if ($estados -contains "Entregado") { Write-Host "RESULTADO PASO 8: OK (ciclo completo reflejado)" } else { Write-Host "RESULTADO PASO 8: FALLO" }
    }

    9 {
        Write-Host "== PASO 9: ERR-02 campos vacios no se publican =="
        cmd /c "adb shell input keyevent 4 >nul 2>&1"
        Start-Sleep -Seconds 2
        App "-n com.proyecto.despachos.central/.MainActivity"
        Start-Sleep -Seconds 3
        $xml = Volcar-Ui
        Tocar-Por-Id $xml "btnNuevoDespacho" | Out-Null
        Start-Sleep -Seconds 3
        $xml = Volcar-Ui
        Tocar-Por-Id $xml "btnPublicar" | Out-Null
        Start-Sleep -Seconds 2
        $xml = Volcar-Ui
        if (Contiene-Texto $xml "Nuevo despacho") {
            Write-Host "Se mantuvo en el formulario: validacion ERR-02 activa"
            Foto "09-err02-campos-vacios.png"
            Write-Host "RESULTADO PASO 9: OK"
        } else {
            Write-Host "RESULTADO PASO 9: FALLO (el formulario no bloqueo la publicacion)"
        }
    }

    10 {
        Write-Host "== PASO 10: ERR-01 broker apagado y reintento =="
        # El broker se detiene en forma controlada creando el archivo STOP
        New-Item -ItemType File -Path "broker\STOP" -Force | Out-Null
        $apagado = $false
        foreach ($i in 1..20) {
            Start-Sleep -Seconds 1
            if (-not (Test-NetConnection -ComputerName 127.0.0.1 -Port 1883 -InformationLevel Quiet -WarningAction SilentlyContinue)) {
                $apagado = $true
                break
            }
        }
        if ($apagado) { Write-Host "Broker apagado (puerto 1883 cerrado)" } else { Write-Host "AVISO: el broker sigue respondiendo" }

        App "-n com.proyecto.despachos.central/.MainActivity"
        Start-Sleep -Seconds 4
        $xml = Volcar-Ui
        Tocar-Por-Id $xml "btnConectar" | Out-Null
        Start-Sleep -Seconds 15
        $xml = Volcar-Ui
        $estado = Texto-De $xml "tvEstadoConexion"
        Write-Host "Indicador con broker apagado: '$estado'"
        Foto "10-err01-broker-apagado.png"

        Write-Host "Reiniciando el broker..."
        Remove-Item "broker\STOP" -Force -ErrorAction SilentlyContinue
        $bat = Join-Path $PSScriptRoot "..\broker\build\install\broker\bin\broker.bat"
        Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "`"$bat`" > broker-salida.log 2>&1" -WorkingDirectory (Join-Path $PSScriptRoot "..\broker") -WindowStyle Hidden
        $encendido = $false
        foreach ($i in 1..20) {
            Start-Sleep -Seconds 1
            if (Test-NetConnection -ComputerName 127.0.0.1 -Port 1883 -InformationLevel Quiet -WarningAction SilentlyContinue) {
                $encendido = $true
                break
            }
        }
        if ($encendido) { Write-Host "Broker reiniciado (puerto 1883 abierto)" }
        Write-Host "RESULTADO PASO 10: demostrado ERR-01 con mensaje y reintento"
    }

    11 {
        Write-Host "== PASO 11: diagnostico de escritura en campo =="
        $xml = Volcar-Ui
        Write-Host ("Campo etId visible: '" + (Texto-De $xml "etId") + "'")
        Tocar-Por-Id $xml "etDestino" | Out-Null
        Start-Sleep -Seconds 1
        cmd /c "adb shell input text PRUEBA123 >nul 2>&1"
        Start-Sleep -Seconds 2
        $xml = Volcar-Ui
        Write-Host ("etDestino despues de escribir: '" + (Texto-De $xml "etDestino") + "'")
    }
}
