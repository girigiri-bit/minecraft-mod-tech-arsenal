# Generates the v0.4 item textures (16x16 pixel art) with System.Drawing.
# Pixel maps use single characters mapped to RGBA colors; '.' = transparent.
# NOTE: PowerShell hashtable keys are case-insensitive, so every palette
# character must be unique ignoring case.
Add-Type -AssemblyName System.Drawing

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

# Laser designator: rangefinder body with red emitter lens (P = bright lens core)
Write-Texture (Join-Path $itemDir "laser_designator.png") @(
    "................",
    "................",
    "................",
    "....DDDDDDDD....",
    "...DGGGGGGGGD...",
    "...DGDDDDDDGD...",
    "..DDDDDDDDDDDD..",
    ".DLLDGGGGGGDRRD.",
    ".DLLDGGGGGGDRPD.",
    ".DDDDDDDDDDDRRD.",
    "..DDDDDDDDDDDD..",
    "...DGDDDDDDGD...",
    "...DGGGGGGGGD...",
    "....DDDDDDDD....",
    "................",
    "................"
) @{ D = 45,48,55,255; G = 95,100,110,255; L = 120,200,230,255; R = 180,20,20,255; P = 255,90,80,255 }

# Drone attack module: circuit chip with red power core (P = core highlight)
Write-Texture (Join-Path $itemDir "drone_upgrade_damage.png") @(
    "................",
    "..G..G..G..G....",
    "..G..G..G..G....",
    ".DDDDDDDDDDDDD..",
    ".DTTTTTTTTTTDD..",
    ".DTRRTTTTRRTDD..",
    ".DTRPRTTRPRTDD..",
    ".DTRRTTTTRRTDD..",
    ".DTTTTRRTTTTDD..",
    ".DTTTTRPRTTTDD..",
    ".DTTTTRRTTTTDD..",
    ".DTTTTTTTTTTDD..",
    ".DDDDDDDDDDDDD..",
    "..G..G..G..G....",
    "..G..G..G..G....",
    "................"
) @{ D = 40,44,52,255; T = 70,76,88,255; R = 170,30,30,255; P = 255,120,90,255; G = 200,180,60,255 }

# Drone armor module: chip with blue plating (E = light plate)
Write-Texture (Join-Path $itemDir "drone_upgrade_armor.png") @(
    "................",
    "..G..G..G..G....",
    "..G..G..G..G....",
    ".DDDDDDDDDDDDD..",
    ".DTTTTTTTTTTDD..",
    ".DTBBBBBBBBTDD..",
    ".DTBEEEEEEBTDD..",
    ".DTBEBBBBEBTDD..",
    ".DTBEBBBBEBTDD..",
    ".DTBEEEEEEBTDD..",
    ".DTBBBBBBBBTDD..",
    ".DTTTTTTTTTTDD..",
    ".DDDDDDDDDDDDD..",
    "..G..G..G..G....",
    "..G..G..G..G....",
    "................"
) @{ D = 40,44,52,255; T = 70,76,88,255; B = 70,120,200,255; E = 140,180,240,255; G = 200,180,60,255 }

# Tank shell: brass casing with steel tip (W = tip highlight, E = brass highlight)
Write-Texture (Join-Path $itemDir "shell.png") @(
    "................",
    "................",
    ".......SS.......",
    "......SWWS......",
    "......SWWS......",
    ".....BEEEEB.....",
    ".....BEEEEB.....",
    ".....BEEEEB.....",
    ".....BEEEEB.....",
    ".....BEEEEB.....",
    ".....BEEEEB.....",
    ".....BEEEEB.....",
    ".....RRRRRR.....",
    ".....RRRRRR.....",
    "................",
    "................"
) @{ S = 120,126,138,255; W = 190,196,205,255; B = 150,110,40,255; E = 210,170,80,255; R = 120,40,30,255 }

# Beam blade: uniform white (the renderer tints it)
$bmp = New-Object System.Drawing.Bitmap(16, 16)
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, 255, 255, 255))
    }
}
$bladePath = Join-Path $itemDir "beam_blade.png"
$bmp.Save($bladePath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Host "wrote $bladePath"
