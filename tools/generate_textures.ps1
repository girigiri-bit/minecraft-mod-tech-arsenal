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

# --- block/monitor_front: dark screen with thin green frame ---
Write-Texture (Join-Path $blockDir "monitor_front.png") @(
    "dddddddddddddddd",
    "deeeeeeeeeeeeeed",
    "dekkkkkkkkkkkked",
    "dekkkkkkkkkkkked",
    "dekkgkkkkkkkkked",
    "dekkkkkkkkkkkked",
    "dekkkkkkkkkkkked",
    "dekkkkkkgkkkkked",
    "dekkkkkkkkkkkked",
    "dekkkkkkkkkkkked",
    "dekgkkkkkkkkkked",
    "dekkkkkkkkkkkked",
    "dekkkkkkkkkkkked",
    "deeeeeeeeeeeeeed",
    "dddddddddddddddd",
    "dddddddddddddddd"
) @{ d = @(52,54,60); e = @(40,90,60); k = @(10,14,18); g = @(30,60,45) }

# --- block/monitor_side: dark metal casing ---
Write-Texture (Join-Path $blockDir "monitor_side.png") @(
    "dddddddddddddddd",
    "dggggggggggggggd",
    "dggggggggggggggd",
    "dgssssssssssssgd",
    "dgssssssssssssgd",
    "dgssssssssssssgd",
    "dgssssssssssssgd",
    "dgssssssssssssgd",
    "dgssssssssssssgd",
    "dgssssssssssssgd",
    "dgssssssssssssgd",
    "dgssssssssssssgd",
    "dgssssssssssssgd",
    "dggggggggggggggd",
    "dggggggggggggggd",
    "dddddddddddddddd"
) @{ d = @(45,47,55); g = @(80,83,92); s = @(65,68,77) }

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

# ============ v0.3 weapons & vehicles ============

# --- item/rifle: long barrel, wooden stock ---
Write-Texture (Join-Path $itemDir "rifle.png") @(
    "................",
    "................",
    "..............k.",
    ".............kk.",
    "...........kkg..",
    "..........kgg...",
    ".........kgg....",
    "........kgg.....",
    ".......kgg......",
    "......kgg.......",
    ".....kgk........",
    "....wgkk........",
    "...wwk..........",
    "..www...........",
    ".ww.............",
    "................"
) @{ k = @(40,42,48); g = @(90,94,102); w = @(96,66,38) }

# --- item/machine_gun: heavy body, ammo box ---
Write-Texture (Join-Path $itemDir "machine_gun.png") @(
    "................",
    "..............k.",
    ".............kk.",
    "...........kkg..",
    "..........kgg...",
    ".........kgg....",
    "........kgg.....",
    ".......kggk.....",
    "......kggkk.....",
    ".....kggkyy.....",
    "....kggk.yy.....",
    "...kkgk.........",
    "..kkk...........",
    "..kk............",
    "................",
    "................"
) @{ k = @(40,42,48); g = @(70,74,82); y = @(140,120,50) }

# --- item/grenade_launcher: fat tube ---
Write-Texture (Join-Path $itemDir "grenade_launcher.png") @(
    "................",
    "................",
    "............oo..",
    "...........oooo.",
    "..........oooo..",
    ".........oooo...",
    "........oooo....",
    ".......oooo.....",
    "......oooo......",
    ".....oooo.......",
    "....koook.......",
    "...kkook........",
    "..kkkk..........",
    "..kk............",
    "................",
    "................"
) @{ o = @(80,100,60); k = @(40,44,34) }

# --- item/rocket_launcher: big shoulder tube with red tip rocket ---
Write-Texture (Join-Path $itemDir "rocket_launcher.png") @(
    "................",
    "..............rr",
    ".............rr.",
    "...........ggg..",
    "..........gsgg..",
    ".........gsgg...",
    "........gsgg....",
    ".......gsgg.....",
    "......gsgg......",
    ".....gsgg.......",
    "....gsgg........",
    "...gggg.........",
    "..kkk...........",
    "..kk............",
    "................",
    "................"
) @{ r = @(210,50,50); g = @(75,80,70); s = @(110,115,105); k = @(40,42,48) }

# --- item/laser_gun: sci-fi pistol with cyan emitter ---
Write-Texture (Join-Path $itemDir "laser_gun.png") @(
    "................",
    "................",
    "................",
    "...........cc...",
    "..........ccdd..",
    ".........ddd....",
    "........ddd.....",
    ".......ddd......",
    "......ddsd......",
    ".....ddss.......",
    "....kdk.........",
    "....kk..........",
    "...kk...........",
    "................",
    "................",
    "................"
) @{ c = @(80,230,235); d = @(55,60,75); s = @(100,108,125); k = @(35,38,48) }

# --- item/beam_saber: glowing energy blade ---
Write-Texture (Join-Path $itemDir "beam_saber.png") @(
    "..............b.",
    ".............bw.",
    "............bwb.",
    "...........bwb..",
    "..........bwb...",
    ".........bwb....",
    "........bwb.....",
    ".......bwb......",
    "......bwb.......",
    ".....bwb........",
    "....kbk.........",
    "...kkk..........",
    "..kdk...........",
    ".kdk............",
    "kk..............",
    "................"
) @{ b = @(90,180,255); w = @(220,245,255); k = @(45,48,58); d = @(80,84,95) }

# --- item/flamethrower: tank + nozzle with flame ---
Write-Texture (Join-Path $itemDir "flamethrower.png") @(
    "................",
    "................",
    ".............f..",
    "............ff..",
    "...........fgo..",
    "..........ggo...",
    ".........ggo....",
    "........ggo.....",
    ".......ggo......",
    "......ggrr......",
    ".....ggrrrr.....",
    "....kgk.rrrr....",
    "...kkk...rr.....",
    "..kk............",
    "................",
    "................"
) @{ f = @(255,170,50); o = @(200,90,30); g = @(85,90,98); r = @(160,40,35); k = @(40,42,48) }

# --- item/bullet: tiny brass round ---
Write-Texture (Join-Path $itemDir "bullet.png") @(
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    ".......y........",
    "......yby.......",
    "......yby.......",
    ".......y........",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................"
) @{ y = @(190,160,70); b = @(120,95,45) }

# --- item/grenade: green sphere ---
Write-Texture (Join-Path $itemDir "grenade.png") @(
    "................",
    "................",
    "................",
    "................",
    ".......kk.......",
    "......gggg......",
    ".....gggggg.....",
    ".....ggkggg.....",
    ".....gggggg.....",
    "......gggg......",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................"
) @{ g = @(90,120,60); k = @(45,55,35) }

# --- item/rocket: red-tipped rocket ---
Write-Texture (Join-Path $itemDir "rocket.png") @(
    "................",
    "................",
    ".......rr.......",
    "......rrrr......",
    "......gggg......",
    "......gssg......",
    "......gssg......",
    "......gggg......",
    "......gggg......",
    "......gggg......",
    ".....dgggd......",
    "....dd.ff.dd....",
    "................",
    "................",
    "................",
    "................"
) @{ r = @(210,50,50); g = @(140,144,152); s = @(110,114,122); d = @(70,72,80); f = @(255,170,60) }

# --- item/defense_turret: base + swivel gun ---
Write-Texture (Join-Path $itemDir "defense_turret.png") @(
    "................",
    "......cc........",
    "......cc........",
    ".....kkkk...kk..",
    "....kggggkkkk...",
    "....kggggk......",
    ".....kggk.......",
    "......kk........",
    "......kk........",
    ".....kkkk.......",
    "....kkkkkk......",
    "...kggggggk.....",
    "...kggggggk.....",
    "...kkkkkkkk.....",
    "................",
    "................"
) @{ c = @(80,230,235); k = @(40,44,52); g = @(95,100,110) }

# --- item/attack_helicopter: top view, rotor + tail ---
Write-Texture (Join-Path $itemDir "attack_helicopter.png") @(
    "................",
    ".......p........",
    ".......p........",
    ".......p........",
    ".......p........",
    "...pppppppppp...",
    ".......p........",
    "......ggg.......",
    "......gcg.......",
    "......ggg.......",
    ".......g........",
    ".......g........",
    ".......g........",
    "......ggg.......",
    ".......g........",
    "................"
) @{ p = @(60,64,72); g = @(85,95,80); c = @(80,180,220) }

# --- item/tank: top view, hull + turret + barrel ---
Write-Texture (Join-Path $itemDir "tank.png") @(
    "................",
    ".......g........",
    ".......g........",
    ".......g........",
    "..kggggggggggk..",
    "..kgssssssssgk..",
    "..kgsgggggssgk..",
    "..kgsggoggssgk..",
    "..kgsgggggssgk..",
    "..kgssssssssgk..",
    "..kggggggggggk..",
    "..kkkkkkkkkkkk..",
    "................",
    "................",
    "................",
    "................"
) @{ k = @(35,38,32); g = @(90,100,70); s = @(70,80,55); o = @(50,58,40) }

# --- block/landmine: dark plate with red sensor ---
Write-Texture (Join-Path $blockDir "landmine.png") @(
    "dddddddddddddddd",
    "dkkkkkkkkkkkkkkd",
    "dkggggggggggggkd",
    "dkgkkkkkkkkkkgkd",
    "dkgkggggggggkgkd",
    "dkgkgggggggghgkd",
    "dkgkggkkkkggkgkd",
    "dkgkggkrrkggkgkd",
    "dkgkggkrrkggkgkd",
    "dkgkggkkkkggkgkd",
    "dkgkggggggggkgkd",
    "dkgkggggggggkgkd",
    "dkgkkkkkkkkkkgkd",
    "dkggggggggggggkd",
    "dkkkkkkkkkkkkkkd",
    "dddddddddddddddd"
) @{ d = @(45,48,42); k = @(60,64,56); g = @(78,84,72); r = @(220,50,45); h = @(100,108,92) }

# ============ entity textures (banded camo fills for voxel models) ============
$entityDir = Join-Path $root "src\main\resources\assets\techarsenal\textures\entity"
New-Item -ItemType Directory -Force $entityDir | Out-Null

function Write-BandedTexture {
    param([string]$Path, [int]$Size, [object[]]$Colors)
    $bmp = New-Object System.Drawing.Bitmap($Size, $Size)
    $rand = New-Object System.Random(42)
    for ($y = 0; $y -lt $Size; $y++) {
        for ($x = 0; $x -lt $Size; $x++) {
            # 4x4 pixel patches picked pseudo-randomly from the palette for a camo feel
            $seed = ([int]($x / 4) * 31 + [int]($y / 4) * 17) % $Colors.Count
            $rgb = $Colors[[Math]::Abs($seed)]
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $rgb[0], $rgb[1], $rgb[2]))
        }
    }
    $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "wrote $Path"
}

Write-BandedTexture (Join-Path $entityDir "tank.png") 128 @(@(90,100,70), @(78,88,60), @(70,78,54), @(96,106,76))
Write-BandedTexture (Join-Path $entityDir "attack_helicopter.png") 128 @(@(70,76,68), @(60,66,58), @(54,58,52), @(78,84,74))
Write-BandedTexture (Join-Path $entityDir "defense_turret.png") 64 @(@(95,100,110), @(80,85,95), @(70,75,85), @(105,110,120))

Write-Host "All textures generated."
