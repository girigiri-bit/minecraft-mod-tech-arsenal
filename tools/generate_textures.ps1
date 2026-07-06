# Generates the 16x16 pixel-art textures for Tech Arsenal.
# Run from the repo root:  powershell -ExecutionPolicy Bypass -File tools\generate_textures.ps1
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$itemDir = Join-Path $root "src\main\resources\assets\techarsenal\textures\item"
$blockDir = Join-Path $root "src\main\resources\assets\techarsenal\textures\block"
New-Item -ItemType Directory -Force $itemDir | Out-Null
New-Item -ItemType Directory -Force $blockDir | Out-Null

function Write-Texture {
    param([string]$Path, [string[]]$Rows, [hashtable]$Palette)
    $bmp = New-Object System.Drawing.Bitmap(16, 16)
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $ch = $Rows[$y][$x]
            if ($ch -eq '.') {
                $color = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)
            } else {
                $rgb = $Palette[[string]$ch]
                $color = [System.Drawing.Color]::FromArgb(255, $rgb[0], $rgb[1], $rgb[2])
            }
            $bmp.SetPixel($x, $y, $color)
        }
    }
    $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "wrote $Path"
}

# --- block/security_camera: metal panel, dark lens area bottom-right (UV 10-14) ---
Write-Texture (Join-Path $blockDir "security_camera.png") @(
    "ddddddddddddddgg",
    "dsssssssssssssdg",
    "dsgggggggggggsdg",
    "dsgggggggggggsdg",
    "dsggsssssssggsdg",
    "dsggsdddddsggsdg",
    "dsggsdddddsggsdg",
    "dsggsdddddsggsdg",
    "dsggsdddddsggsdg",
    "dsggsssssssggsdg",
    "dsggggggggkkkkkg",
    "dsggggggggkbbbkg",
    "dsggggggggkbwbkg",
    "dsggggggggkbbbkg",
    "dssssssssskkkkkg",
    "dddddddddddddddg"
) @{ d = @(70,70,78); s = @(120,122,130); g = @(154,156,164); k = @(20,20,26); b = @(30,60,110); w = @(210,230,255) }

# --- item/camera_monitor: tablet with green scanline screen ---
Write-Texture (Join-Path $itemDir "camera_monitor.png") @(
    "................",
    ".dddddddddddddd.",
    ".dggggggggggggd.",
    ".dgkkkkkkkkkkgd.",
    ".dgkeekkkkkekgd.",
    ".dgkkkkkkkkkkgd.",
    ".dgkeeeekkkkkgd.",
    ".dgkkkkkkkkekgd.",
    ".dgkkkeekkkkkgd.",
    ".dgkkkkkkkkkkgd.",
    ".dgkekkkkeekkgd.",
    ".dgkkkkkkkkkkgd.",
    ".dggggggggggggd.",
    ".dggggrggggggdd.",
    ".dddddddddddddd.",
    "................"
) @{ d = @(52,54,60); g = @(110,112,120); k = @(8,22,14); e = @(60,220,110); r = @(220,60,60) }

# --- item/satellite_remote: gray remote, antenna, red button ---
Write-Texture (Join-Path $itemDir "satellite_remote.png") @(
    "..........aa....",
    ".........aa.....",
    "........aa......",
    ".......aa.......",
    "......ba........",
    ".....dddddd.....",
    ".....dggggd.....",
    ".....dgrrgd.....",
    ".....dgrrgd.....",
    ".....dggggd.....",
    ".....dgkkgd.....",
    ".....dgkkgd.....",
    ".....dggggd.....",
    ".....dggggd.....",
    ".....dddddd.....",
    "................"
) @{ a = @(200,204,212); b = @(90,94,102); d = @(60,62,70); g = @(130,134,142); r = @(225,55,55); k = @(35,120,60) }

# --- item/missile_launcher: diagonal olive tube with grip ---
Write-Texture (Join-Path $itemDir "missile_launcher.png") @(
    "............kk..",
    "...........kook.",
    "..........koook.",
    ".........koook..",
    "........koook...",
    ".......koook....",
    "......koook.....",
    ".....koook......",
    "....koook.......",
    "...koook........",
    "..koook.........",
    ".koook..........",
    "koook...........",
    "kook............",
    "kdk.............",
    ".d.............."
) @{ k = @(40,44,34); o = @(110,120,80); d = @(70,52,36) }

# --- item/guided_missile: vertical missile, red tip, fins ---
Write-Texture (Join-Path $itemDir "guided_missile.png") @(
    "................",
    ".......rr.......",
    "......rrrr......",
    "......rrrr......",
    "......gggg......",
    "......gssg......",
    "......gssg......",
    "......gggg......",
    "......gssg......",
    "......gssg......",
    "......gggg......",
    "......gggg......",
    "....ddggggdd....",
    "...dd.gggg.dd...",
    "..dd...ff...dd..",
    "................"
) @{ r = @(210,50,50); g = @(150,154,162); s = @(120,124,132); d = @(70,72,80); f = @(255,170,60) }

# --- item/drone: quadcopter top view ---
Write-Texture (Join-Path $itemDir "drone.png") @(
    "pp..........pp..",
    "pxp........pxp..",
    ".pap......pap...",
    "..aa......aa....",
    "...a......a.....",
    "...adddddda.....",
    "....dggggd......",
    "....dgrrgd......",
    "....dgrrgd......",
    "....dggggd......",
    "...adddddda.....",
    "...a......a.....",
    "..aa......aa....",
    ".pap......pap...",
    "pxp........pxp..",
    "pp..........pp.."
) @{ p = @(90,95,105); x = @(50,52,60); a = @(60,62,70); d = @(45,47,55); g = @(120,124,132); r = @(220,60,60) }

# --- item/drone_bolt: small yellow energy bolt ---
Write-Texture (Join-Path $itemDir "drone_bolt.png") @(
    "................",
    "................",
    "................",
    "................",
    "................",
    "......y.........",
    ".....ywy........",
    "....ywwwy.......",
    ".....ywwwy......",
    "......ywy.......",
    ".......y........",
    "................",
    "................",
    "................",
    "................",
    "................"
) @{ y = @(230,180,40); w = @(255,240,150) }

Write-Host "All textures generated."
