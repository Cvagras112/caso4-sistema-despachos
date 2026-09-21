# Verifica que el botón Atrás lleve al menú principal en ambas apps.
. "$PSScriptRoot\uia.ps1"

function Marca($ok, $nombre) {
    if ($ok) { Write-Host "OK: $nombre" } else { Write-Host "FALLO: $nombre" }
}

# --- 1) Central: Atrás desde el LISTADO -> menú principal ---
Write-Host "== 1) Central: Atras desde el listado =="
App "-n com.proyecto.despachos.central/.MainActivity"
Start-Sleep -Seconds 3
$xml = Volcar-Ui
Tocar-Por-Id $xml "btnVerDespachos" | Out-Null
Start-Sleep -Seconds 3
$xml = Volcar-Ui
$enLista = Contiene-Texto $xml "Despachos creados"
cmd /c "adb shell input keyevent 4 >nul 2>&1"
Start-Sleep -Seconds 3
$xml = Volcar-Ui
$enMenu = (Texto-De $xml "tvEstadoConexion") -ne "" -and (Contiene-Texto $xml "Conexión al broker MQTT")
Write-Host ("En listado: " + $enLista + " | tras Atras en menu: " + $enMenu)
Marca ($enLista -and $enMenu) "Central Atras listado -> menu"

# --- 2) Central: Atrás desde el DETALLE -> menú principal ---
Write-Host "== 2) Central: Atras desde el detalle =="
App "-n com.proyecto.despachos.central/.MainActivity"
Start-Sleep -Seconds 3
$xml = Volcar-Ui
Tocar-Por-Id $xml "btnVerDespachos" | Out-Null
Start-Sleep -Seconds 3
$xml = Volcar-Ui
$m = [regex]::Match($xml, 'text="(DES-[^"]+)"')
if ($m.Success) {
    Tocar-Por-Texto $xml $m.Groups[1].Value | Out-Null
    Start-Sleep -Seconds 3
    $xml = Volcar-Ui
    $enDetalle = Contiene-Texto $xml "Detalle del despacho"
    cmd /c "adb shell input keyevent 4 >nul 2>&1"
    Start-Sleep -Seconds 3
    $xml = Volcar-Ui
    $enMenu2 = Contiene-Texto $xml "Conexión al broker MQTT"
    Write-Host ("En detalle: " + $enDetalle + " | tras Atras en menu: " + $enMenu2)
    Marca ($enDetalle -and $enMenu2) "Central Atras detalle -> menu"
} else {
    Write-Host "No hay despachos en el listado; salto esta prueba"
}

# --- 3) Repartidor: Atrás desde el LISTADO -> menú principal ---
Write-Host "== 3) Repartidor: Atras desde el listado =="
App "-n com.proyecto.despachos.repartidor/.ConexionActivity"
Start-Sleep -Seconds 4
$xml = Volcar-Ui
Tocar-Por-Id $xml "btnConectar" | Out-Null
Start-Sleep -Seconds 10
$xml = Volcar-Ui
$enLista2 = Contiene-Texto $xml "Despachos"
cmd /c "adb shell input keyevent 4 >nul 2>&1"
Start-Sleep -Seconds 3
$xml = Volcar-Ui
$enMenu3 = Contiene-Texto $xml "Zona de trabajo"
Write-Host ("En listado: " + $enLista2 + " | tras Atras en menu: " + $enMenu3)
Marca ($enLista2 -and $enMenu3) "Repartidor Atras listado -> menu"

# --- 4) Repartidor: Atrás desde el DETALLE -> menú principal ---
Write-Host "== 4) Repartidor: Atras desde el detalle =="
$xml = Volcar-Ui
if (Contiene-Texto $xml "Conectar y ver despachos") {
    Tocar-Por-Id $xml "btnConectar" | Out-Null
    Start-Sleep -Seconds 10
    $xml = Volcar-Ui
}
$m2 = [regex]::Match($xml, 'text="(DES-[^"]+)"')
if ($m2.Success) {
    Tocar-Por-Texto $xml $m2.Groups[1].Value | Out-Null
    Start-Sleep -Seconds 3
    $xml = Volcar-Ui
    $enDetalle2 = Contiene-Texto $xml "Detalle del despacho"
    cmd /c "adb shell input keyevent 4 >nul 2>&1"
    Start-Sleep -Seconds 3
    $xml = Volcar-Ui
    $enMenu4 = Contiene-Texto $xml "Zona de trabajo"
    Write-Host ("En detalle: " + $enDetalle2 + " | tras Atras en menu: " + $enMenu4)
    Marca ($enDetalle2 -and $enMenu4) "Repartidor Atras detalle -> menu"
} else {
    Write-Host "No hay despachos visibles en el repartidor; salto esta prueba"
}
