# v0.6 textures: detailed 128x128 camo skins for tank / attack helicopter and
# a 16x16 color palette used by the 3D gun item models.
Add-Type -AssemblyName System.Drawing

$entityDir = Join-Path $PSScriptRoot "..\src\main\resources\assets\techarsenal\textures\entity"
$itemDir = Join-Path $PSScriptRoot "..\src\main\resources\assets\techarsenal\textures\item"
$rand = New-Object System.Random(42)

function New-Bitmap128 { New-Object System.Drawing.Bitmap(128, 128) }

function Fill-Camo {
    param($Bmp, [int]$X0, [int]$Y0, [int]$X1, [int]$Y1)
    # Olive camo: base + blobs of dark green and brown + subtle noise
    $base = [System.Drawing.Color]::FromArgb(255, 92, 98, 72)
    for ($y = $Y0; $y -lt $Y1; $y++) { for ($x = $X0; $x -lt $X1; $x++) { $Bmp.SetPixel($x, $y, $base) } }
    foreach ($blob in 1..60) {
        $cx = $rand.Next($X0, $X1); $cy = $rand.Next($Y0, $Y1); $r = $rand.Next(3, 9)
        $c = if ($rand.Next(2) -eq 0) { [System.Drawing.Color]::FromArgb(255, 70, 78, 56) } else { [System.Drawing.Color]::FromArgb(255, 105, 92, 66) }
        for ($y = [Math]::Max($Y0, $cy - $r); $y -lt [Math]::Min($Y1, $cy + $r); $y++) {
            for ($x = [Math]::Max($X0, $cx - $r); $x -lt [Math]::Min($X1, $cx + $r); $x++) {
                if ((($x - $cx) * ($x - $cx) + ($y - $cy) * ($y - $cy)) -le $r * $r) { $Bmp.SetPixel($x, $y, $c) }
            }
        }
    }
    # Panel line noise
    foreach ($i in 1..25) {
        $x = $rand.Next($X0, $X1); $y = $rand.Next($Y0, $Y1)
        $Bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, 60, 66, 48))
    }
}

function Fill-Steel {
    param($Bmp, [int]$X0, [int]$Y0, [int]$X1, [int]$Y1, [bool]$Treads)
    $base = [System.Drawing.Color]::FromArgb(255, 52, 55, 62)
    $dark = [System.Drawing.Color]::FromArgb(255, 38, 40, 46)
    $lite = [System.Drawing.Color]::FromArgb(255, 78, 82, 92)
    for ($y = $Y0; $y -lt $Y1; $y++) {
        for ($x = $X0; $x -lt $X1; $x++) {
            $c = $base
            if ($Treads -and (($x % 4) -eq 0)) { $c = $dark }          # tread ridges
            elseif ((($x * 7 + $y * 13) % 23) -eq 0) { $c = $lite }    # metallic speckle
            $Bmp.SetPixel($x, $y, $c)
        }
    }
}

function Fill-Glass {
    param($Bmp, [int]$X0, [int]$Y0, [int]$X1, [int]$Y1)
    for ($y = $Y0; $y -lt $Y1; $y++) {
        for ($x = $X0; $x -lt $X1; $x++) {
            $c = if ((($x + $y) % 7) -lt 2) { [System.Drawing.Color]::FromArgb(255, 150, 205, 235) }
                 else { [System.Drawing.Color]::FromArgb(255, 62, 110, 160) }
            $Bmp.SetPixel($x, $y, $c)
        }
    }
}

# --- tank.png: v0..51 dark tracks (treads), v52..59 mid steel, v60..127 camo ---
$bmp = New-Bitmap128
Fill-Steel $bmp 0 0 128 52 $true
Fill-Steel $bmp 0 52 128 60 $false
Fill-Camo $bmp 0 60 128 128
$p = Join-Path $entityDir "tank.png"; $bmp.Save($p, [System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose(); Write-Host "wrote $p"

# --- attack_helicopter.png: v0..49 camo (glass patch u96..128 v0..15), v50..127 dark steel ---
$bmp = New-Bitmap128
Fill-Camo $bmp 0 0 128 50
Fill-Glass $bmp 96 0 128 16
Fill-Steel $bmp 0 50 128 128 $false
$p = Join-Path $entityDir "attack_helicopter.png"; $bmp.Save($p, [System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose(); Write-Host "wrote $p"

# --- gun_palette.png: 4x4 swatches used by the 3D gun models ---
$bmp = New-Object System.Drawing.Bitmap(16, 16)
$swatches = @(
    @(0, 0, 45, 48, 55),    # dark gunmetal
    @(4, 0, 85, 90, 100),   # mid metal
    @(8, 0, 125, 130, 140), # light metal
    @(12, 0, 22, 24, 28),   # black
    @(0, 4, 110, 78, 48),   # wood
    @(4, 4, 92, 98, 72),    # olive
    @(8, 4, 180, 45, 40),   # red accent
    @(12, 4, 80, 200, 255), # cyan energy
    @(0, 8, 235, 150, 50),  # orange
    @(4, 8, 205, 175, 95),  # brass
    @(8, 8, 80, 55, 35),    # dark wood
    @(12, 8, 230, 232, 235),# white
    @(0, 12, 60, 64, 74),   # gunmetal mid-dark
    @(4, 12, 35, 38, 44),   # near-black steel
    @(8, 12, 140, 60, 40),  # rust red
    @(12, 12, 100, 160, 110)# green accent
)
foreach ($s in $swatches) {
    for ($y = $s[1]; $y -lt $s[1] + 4; $y++) {
        for ($x = $s[0]; $x -lt $s[0] + 4; $x++) {
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $s[2], $s[3], $s[4]))
        }
    }
}
$p = Join-Path $itemDir "gun_palette.png"; $bmp.Save($p, [System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose(); Write-Host "wrote $p"
