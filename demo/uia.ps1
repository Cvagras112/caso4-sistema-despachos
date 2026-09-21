# Ayudante para automatizar la demo del Caso 4 en el emulador.
# Uso: . .\demo\uia.ps1   (desde la raíz del proyecto)
# Nota: adb se invoca vía cmd /c y las redirecciones se hacen dentro de cmd (>nul),
#       nunca con pipes de PowerShell (provocan bloqueos en shells anidados).

$ErrorActionPreference = 'SilentlyContinue'
New-Item -ItemType Directory -Force -Path "docs\evidencia" | Out-Null

function Volcar-Ui {
    cmd /c "adb shell uiautomator dump /sdcard/ui.xml >nul 2>&1"
    cmd /c "adb pull /sdcard/ui.xml %TEMP%\ui_demo.xml >nul 2>&1"
    if (Test-Path "$env:TEMP\ui_demo.xml") {
        $xml = Get-Content "$env:TEMP\ui_demo.xml" -Raw
        return ($xml -replace '',' ')
    }
    return ""
}

function Tocar-Por-Id($xml, $id) {
    $m = [regex]::Match($xml, 'resource-id="[^"]*' + [regex]::Escape($id) + '"[^>]*bounds="\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]"')
    if (-not $m.Success) {
        Write-Host "NO ENCONTRADO id=$id"
        return $false
    }
    $x = [int](([int]$m.Groups[1].Value + [int]$m.Groups[3].Value) / 2)
    $y = [int](([int]$m.Groups[2].Value + [int]$m.Groups[4].Value) / 2)
    cmd /c "adb shell input tap $x $y >nul 2>&1"
    Write-Host "TAP id=$id en ($x,$y)"
    return $true
}

function Tocar-Por-Texto($xml, $texto) {
    $m = [regex]::Match($xml, 'text="' + [regex]::Escape($texto) + '"[^>]*bounds="\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]"')
    if (-not $m.Success) {
        Write-Host "NO ENCONTRADO texto=$texto"
        return $false
    }
    $x = [int](([int]$m.Groups[1].Value + [int]$m.Groups[3].Value) / 2)
    $y = [int](([int]$m.Groups[2].Value + [int]$m.Groups[4].Value) / 2)
    cmd /c "adb shell input tap $x $y >nul 2>&1"
    Write-Host "TAP texto='$texto' en ($x,$y)"
    return $true
}

function Texto-De($xml, $id) {
    $m = [regex]::Match($xml, 'text="([^"]*)"[^>]*resource-id="[^"]*' + [regex]::Escape($id) + '"')
    return $m.Groups[1].Value
}

function Contiene-Texto($xml, $texto) {
    $xml -match 'text="' + [regex]::Escape($texto) + '"'
}

function Esperar-Texto($texto, $segundos = 12) {
    foreach ($i in 1..$segundos) {
        $xml = Volcar-Ui
        if (Contiene-Texto $xml $texto) {
            Write-Host "OK: encontrado '$texto' (intento $i)"
            return $true
        }
        Start-Sleep -Seconds 1
    }
    Write-Host "FALLO: no aparecio '$texto' en $segundos s"
    return $false
}

function Escribir-Campo($xml, $id, $texto) {
    # $texto usa %s como espacio (así lo exige adb input text).
    # Limpia el campo con DELs, escribe y oculta el teclado para que el
    # siguiente tap use coordenadas de un layout sin desplazamiento.
    if (Tocar-Por-Id $xml $id) {
        Start-Sleep -Milliseconds 600
        cmd /c "adb shell input keyevent 123 >nul 2>&1"
        foreach ($i in 1..45) {
            cmd /c "adb shell input keyevent 67 >nul 2>&1"
        }
        cmd /c "adb shell input text $texto >nul 2>&1"
        Start-Sleep -Milliseconds 500
        cmd /c "adb shell input keyevent 4 >nul 2>&1"
        Start-Sleep -Milliseconds 700
    }
}

function Deslizar-Arriba {
    cmd /c "adb shell input swipe 160 500 160 120 500 >nul 2>&1"
    Start-Sleep -Milliseconds 900
}

function Foto($nombre) {
    cmd /c "adb shell screencap -p /sdcard/foto.png >nul 2>&1"
    cmd /c "adb pull /sdcard/foto.png docs\evidencia\$nombre >nul 2>&1"
    Write-Host "Foto: docs\evidencia\$nombre"
}

function App($comando) {
    cmd /c "adb shell am start $comando >nul 2>&1"
}
