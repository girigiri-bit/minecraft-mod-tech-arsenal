# field_manual.png: 16x16 pixel-art icon for the in-game guide book item.
# Dark gunmetal-blue closed book with a small orange wrench accent on the cover.
Add-Type -AssemblyName System.Drawing

$itemDir = Join-Path $PSScriptRoot "..\src\main\resources\assets\techarsenal\textures\item"

$bmp = New-Object System.Drawing.Bitmap(16, 16)
$transparent = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)
for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) { $bmp.SetPixel($x, $y, $transparent) } }

$outline = [System.Drawing.Color]::FromArgb(255, 16, 20, 26)
$cover = [System.Drawing.Color]::FromArgb(255, 38, 48, 62)
$coverLight = [System.Drawing.Color]::FromArgb(255, 50, 64, 82)
$pageEdge = [System.Drawing.Color]::FromArgb(255, 198, 184, 148)
$wrench = [System.Drawing.Color]::FromArgb(255, 224, 128, 44)
$wrenchDark = [System.Drawing.Color]::FromArgb(255, 176, 92, 24)

# Book cover: x 2..13, y 1..14 (outline on the border, cover fill inside)
for ($y = 1; $y -le 14; $y++) {
    for ($x = 2; $x -le 13; $x++) {
        if ($x -eq 2 -or $x -eq 13 -or $y -eq 1 -or $y -eq 14) {
            $bmp.SetPixel($x, $y, $outline)
        } else {
            $bmp.SetPixel($x, $y, $cover)
        }
    }
}

# Page edge sliver along the right side (pages peeking out of the closed cover)
for ($y = 3; $y -le 12; $y++) { $bmp.SetPixel(12, $y, $pageEdge) }

# Spine highlight near the left edge for a bit of shading
for ($y = 3; $y -le 12; $y++) { $bmp.SetPixel(3, $y, $coverLight) }

# Small wrench emblem (diagonal, top-right jaw to bottom-left handle)
$bmp.SetPixel(9, 6, $wrench)
$bmp.SetPixel(10, 6, $wrench)
$bmp.SetPixel(9, 7, $wrench)
$bmp.SetPixel(8, 8, $wrench)
$bmp.SetPixel(7, 9, $wrench)
$bmp.SetPixel(6, 10, $wrenchDark)
$bmp.SetPixel(6, 11, $wrenchDark)

$p = Join-Path $itemDir "field_manual.png"
$bmp.Save($p, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Host "wrote $p"
