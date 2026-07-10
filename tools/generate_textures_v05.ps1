# Generates the v0.5 security textures (16x16 pixel art) with System.Drawing.
# Palette characters must be unique ignoring case (PS hashtables are case-insensitive).
Add-Type -AssemblyName System.Drawing

$blockDir = Join-Path $PSScriptRoot "..\src\main\resources\assets\techarsenal\textures\block"
$itemDir = Join-Path $PSScriptRoot "..\src\main\resources\assets\techarsenal\textures\item"

function Write-Texture {
    param([string]$Path, [string[]]$Rows, [hashtable]$Palette)
    $bmp = New-Object System.Drawing.Bitmap(16, 16)
    for ($y = 0; $y -lt 16; $y++) {
        $row = $Rows[$y]
        for ($x = 0; $x -lt 16; $x++) {
            $ch = $row[$x]
            if ($ch -ne '.') {
                $c = $Palette[[string]$ch]
                $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($c[3], $c[0], $c[1], $c[2]))
            }
        }
    }
    $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "wrote $Path"
}

# Face scanner body: pole strip (0..3,0..7), head plate (4..13,0..8), trims (0,8..)
Write-Texture (Join-Path $blockDir "face_scanner.png") @(
    "DDDDGGGGGGGGGG..",
    "DAADGDDDDDDDDG..",
    "DAADGDDDDDDDDG..",
    "DDDDGDDDDDDDDG..",
    "DAADGDDDDDDDDG..",
    "DAADGDDDDDDDDG..",
    "DDDDGDDDDDDDDG..",
    "DAADGGGGGGGGGG..",
    "GGGGGGGG........",
    "GDDDDDDG........",
    "RP..............",
    "................",
    "................",
    "................",
    "................",
    "................"
) @{ D = 45,48,55,255; A = 62,66,75,255; G = 100,105,115,255; R = 160,30,30,255; P = 255,110,90,255 }

# Face scanner screen: blue scan display with a face silhouette + scan line
Write-Texture (Join-Path $blockDir "face_scanner_screen.png") @(
    "DDDDDDDDDDDDDDDD",
    "DBBBBBBBBBBBBBBD",
    "DBBBBBFFFFBBBBBD",
    "DBBBBFFFFFFBBBBD",
    "DBBBBFFFFFFBBBBD",
    "DLLLLLLLLLLLLLLD",
    "DBBBBFFFFFFBBBBD",
    "DBBBBBFFFFBBBBBD",
    "DBBBBBBFFBBBBBBD",
    "DBBBBFFFFFFBBBBD",
    "DBBBFFFFFFFFBBBD",
    "DBBFFFFFFFFFFBBD",
    "DBBBBBBBBBBBBBBD",
    "DBBBBBBBBBBBBBBD",
    "DBBBBBBBBBBBBBBD",
    "DDDDDDDDDDDDDDDD"
) @{ D = 25,28,35,255; B = 20,45,90,255; F = 90,160,220,255; L = 140,230,255,255 }

# Auth monitor: idle (blue face icon)
Write-Texture (Join-Path $blockDir "auth_monitor_idle.png") @(
    "DDDDDDDDDDDDDDDD",
    "DBBBBBBBBBBBBBBD",
    "DBBBBBBBBBBBBBBD",
    "DBBBBBFFFFBBBBBD",
    "DBBBBFFFFFFBBBBD",
    "DBBBBFFFFFFBBBBD",
    "DBBBBFFFFFFBBBBD",
    "DBBBBBFFFFBBBBBD",
    "DBBBBBBFFBBBBBBD",
    "DBBBBFFFFFFBBBBD",
    "DBBBFFFFFFFFBBBD",
    "DBBFFFFFFFFFFBBD",
    "DBBFFFFFFFFFFBBD",
    "DBBBBBBBBBBBBBBD",
    "DBBBBBBBBBBBBBBD",
    "DDDDDDDDDDDDDDDD"
) @{ D = 25,28,35,255; B = 20,45,90,255; F = 90,160,220,255 }

# Auth monitor: granted (green check)
Write-Texture (Join-Path $blockDir "auth_monitor_granted.png") @(
    "DDDDDDDDDDDDDDDD",
    "DGGGGGGGGGGGGGGD",
    "DGGGGGGGGGGGGGGD",
    "DGGGGGGGGGGGWGGD",
    "DGGGGGGGGGGWWGGD",
    "DGGGGGGGGGWWGGGD",
    "DGGGGGGGGWWGGGGD",
    "DGGWGGGGWWGGGGGD",
    "DGGWWGGWWGGGGGGD",
    "DGGGWWWWGGGGGGGD",
    "DGGGGWWGGGGGGGGD",
    "DGGGGGGGGGGGGGGD",
    "DGGGGGGGGGGGGGGD",
    "DGGGGGGGGGGGGGGD",
    "DGGGGGGGGGGGGGGD",
    "DDDDDDDDDDDDDDDD"
) @{ D = 25,28,35,255; G = 20,110,40,255; W = 130,255,150,255 }

# Auth monitor: denied (red cross)
Write-Texture (Join-Path $blockDir "auth_monitor_denied.png") @(
    "DDDDDDDDDDDDDDDD",
    "DRRRRRRRRRRRRRRD",
    "DRRRRRRRRRRRRRRD",
    "DRRWWRRRRRRWWRRD",
    "DRRRWWRRRRWWRRRD",
    "DRRRRWWRRWWRRRRD",
    "DRRRRRWWWWRRRRRD",
    "DRRRRRRWWRRRRRRD",
    "DRRRRRWWWWRRRRRD",
    "DRRRRWWRRWWRRRRD",
    "DRRRWWRRRRWWRRRD",
    "DRRWWRRRRRRWWRRD",
    "DRRRRRRRRRRRRRRD",
    "DRRRRRRRRRRRRRRD",
    "DRRRRRRRRRRRRRRD",
    "DDDDDDDDDDDDDDDD"
) @{ D = 25,28,35,255; R = 140,25,25,255; W = 255,150,140,255 }

# Security door: top half (metal, small window)
Write-Texture (Join-Path $blockDir "security_door_top.png") @(
    "GDDDDDDDDDDDDDDG",
    "GDMMMMMMMMMMMMDG",
    "GDMDDDDDDDDDDMDG",
    "GDMDWWWWWWWWDMDG",
    "GDMDWLLLLLLWDMDG",
    "GDMDWLLLLLLWDMDG",
    "GDMDWWWWWWWWDMDG",
    "GDMDDDDDDDDDDMDG",
    "GDMMMMMMMMMMMMDG",
    "GDDDDDDDDDDDDDDG",
    "GDMMMMMMMMMMMMDG",
    "GDMDDDDDDDDDDMDG",
    "GDMDDDDDDDDDDMDG",
    "GDMDDDDDDDDDDMDG",
    "GDMMMMMMMMMMMMDG",
    "GDDDDDDDDDDDDDDG"
) @{ G = 120,126,138,255; D = 62,66,75,255; M = 90,95,105,255; W = 30,32,40,255; L = 120,190,230,255 }

# Security door: bottom half (metal, hazard stripes + keypad)
Write-Texture (Join-Path $blockDir "security_door_bottom.png") @(
    "GDDDDDDDDDDDDDDG",
    "GDMMMMMMMMMMMMDG",
    "GDMDDDDDDDDKKMDG",
    "GDMDDDDDDDDKPMDG",
    "GDMDDDDDDDDKKMDG",
    "GDMMMMMMMMMMMMDG",
    "GDYYBBYYBBYYBBDG",
    "GDBBYYBBYYBBYYDG",
    "GDYYBBYYBBYYBBDG",
    "GDMMMMMMMMMMMMDG",
    "GDMDDDDDDDDDDMDG",
    "GDMDDDDDDDDDDMDG",
    "GDMDDDDDDDDDDMDG",
    "GDMMMMMMMMMMMMDG",
    "GDDDDDDDDDDDDDDG",
    "GDDDDDDDDDDDDDDG"
) @{ G = 120,126,138,255; D = 62,66,75,255; M = 90,95,105,255; Y = 200,170,40,255; B = 30,32,38,255; K = 40,44,52,255; P = 200,60,50,255 }

# Item: security door icon (whole door shrunk)
Write-Texture (Join-Path $itemDir "security_door.png") @(
    "....GGGGGGGG....",
    "....GDDDDDDG....",
    "....GDWWWWDG....",
    "....GDWLLWDG....",
    "....GDWWWWDG....",
    "....GDDDDDDG....",
    "....GDMMMMDG....",
    "....GDDDDDDG....",
    "....GDYBYBDG....",
    "....GDBYBYDG....",
    "....GDDDDKDG....",
    "....GDDDDDDG....",
    "....GDMMMMDG....",
    "....GDDDDDDG....",
    "....GGGGGGGG....",
    "................"
) @{ G = 120,126,138,255; D = 62,66,75,255; M = 90,95,105,255; W = 30,32,40,255; L = 120,190,230,255; Y = 200,170,40,255; B = 30,32,38,255; K = 200,60,50,255 }

# Item: door key
Write-Texture (Join-Path $itemDir "door_key.png") @(
    "................",
    "................",
    "....GGG.........",
    "...G...G........",
    "...G...G........",
    "...G...G........",
    "....GGG.........",
    ".....G..........",
    ".....G..........",
    ".....GG.........",
    ".....G..........",
    ".....GG.........",
    ".....G..........",
    "................",
    "................",
    "................"
) @{ G = 210,180,90,255 }
