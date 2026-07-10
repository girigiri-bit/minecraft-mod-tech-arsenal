# Generates 3D item models (JSON elements) for the six guns, textured from
# techarsenal:item/gun_palette color swatches. Guns are built along -Z
# (barrel to the north) in 16-unit model space.
$modelDir = Join-Path $PSScriptRoot "..\src\main\resources\assets\techarsenal\models\item"

# swatch name -> [u, v] of a 4x4 color cell in gun_palette.png
$SW = @{
    dark   = @(0, 0);  metal = @(4, 0);  light = @(8, 0);   black = @(12, 0)
    wood   = @(0, 4);  olive = @(4, 4);  red   = @(8, 4);   cyan  = @(12, 4)
    orange = @(0, 8);  brass = @(4, 8);  dwood = @(8, 8);   white = @(12, 8)
    mdark  = @(0, 12); steel = @(4, 12)
}

function New-Element {
    param([double[]]$From, [double[]]$To, [string]$Swatch)
    $uv = $SW[$Swatch]
    $u0 = $uv[0]; $v0 = $uv[1]; $u1 = $u0 + 4; $v1 = $v0 + 4
    $faces = @("north","south","east","west","up","down") | ForEach-Object {
        "`"$_`": { `"uv`": [$u0, $v0, $u1, $v1], `"texture`": `"#0`" }"
    }
    "    {`n      `"from`": [$($From -join ', ')],`n      `"to`": [$($To -join ', ')],`n      `"faces`": { $($faces -join ', ') }`n    }"
}

function Write-GunModel {
    param([string]$Name, [object[]]$Boxes)
    $elements = ($Boxes | ForEach-Object { New-Element $_.f $_.t $_.s }) -join ",`n"
    $json = @"
{
  "credit": "Tech Arsenal generated gun model",
  "textures": {
    "0": "techarsenal:item/gun_palette",
    "particle": "techarsenal:item/gun_palette"
  },
  "elements": [
$elements
  ],
  "display": {
    "thirdperson_righthand": { "rotation": [65, 0, 0], "translation": [0, 2.5, 1], "scale": [0.55, 0.55, 0.55] },
    "thirdperson_lefthand": { "rotation": [65, 0, 0], "translation": [0, 2.5, 1], "scale": [0.55, 0.55, 0.55] },
    "firstperson_righthand": { "rotation": [0, 5, 0], "translation": [1.5, 1.0, 0], "scale": [0.6, 0.6, 0.6] },
    "firstperson_lefthand": { "rotation": [0, -5, 0], "translation": [-1.5, 1.0, 0], "scale": [0.6, 0.6, 0.6] },
    "gui": { "rotation": [25, -135, 0], "translation": [0, 0, 0], "scale": [0.6, 0.6, 0.6] },
    "ground": { "rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.35, 0.35, 0.35] },
    "fixed": { "rotation": [0, 90, 0], "translation": [0, 0, 0], "scale": [0.7, 0.7, 0.7] }
  }
}
"@
    $path = Join-Path $modelDir "$Name.json"
    [System.IO.File]::WriteAllText($path, $json)
    Write-Host "wrote $path"
}

# --- Assault rifle ---
Write-GunModel "rifle" @(
    @{ f = @(7.25, 8.75, -5);  t = @(8.75, 10.25, 3);   s = "dark" },   # barrel
    @{ f = @(7, 8.5, -6.5);    t = @(9, 10.5, -5);      s = "black" },  # muzzle
    @{ f = @(7.6, 10.25, -4);  t = @(8.4, 11.25, -3);   s = "black" },  # front sight
    @{ f = @(6.75, 7.5, 3);    t = @(9.25, 11, 9);      s = "metal" },  # receiver
    @{ f = @(7.5, 11, 4);      t = @(8.5, 11.8, 8);     s = "black" },  # top rail
    @{ f = @(7, 8, 9);         t = @(9, 10.75, 14.5);   s = "wood" },   # stock
    @{ f = @(7.4, 4.5, 6.5);   t = @(8.6, 7.5, 8.5);    s = "dwood" },  # grip
    @{ f = @(7.4, 4.5, 3.5);   t = @(8.6, 7.5, 6);      s = "mdark" }   # magazine
)

# --- Machine gun ---
Write-GunModel "machine_gun" @(
    @{ f = @(7, 8.5, -7);      t = @(9, 10.5, 2);       s = "dark" },   # barrel
    @{ f = @(6.75, 8.25, -4);  t = @(9.25, 10.75, 0);   s = "light" },  # barrel shroud
    @{ f = @(6.75, 8.25, -8);  t = @(9.25, 10.75, -7);  s = "black" },  # muzzle
    @{ f = @(6.25, 7, 2);      t = @(9.75, 11.5, 10);   s = "mdark" },  # receiver
    @{ f = @(6.75, 3.5, 4);    t = @(9.25, 7, 7.5);     s = "steel" },  # box magazine
    @{ f = @(7.4, 4.5, 8);     t = @(8.6, 7, 10);       s = "dwood" },  # grip
    @{ f = @(7, 8, 10);        t = @(9, 11, 14);        s = "metal" },  # stock
    @{ f = @(7.6, 11.5, 4);    t = @(8.4, 12.5, 7);     s = "black" }   # carry handle
)

# --- Grenade launcher ---
Write-GunModel "grenade_launcher" @(
    @{ f = @(6.5, 7.5, -6);    t = @(9.5, 10.5, 1);     s = "olive" },  # fat barrel
    @{ f = @(6.25, 7.25, -7);  t = @(9.75, 10.75, -6);  s = "black" },  # muzzle ring
    @{ f = @(6, 6, 1);         t = @(10, 11, 5);        s = "mdark" },  # drum
    @{ f = @(6.75, 7, 5);      t = @(9.25, 10.5, 9);    s = "metal" },  # receiver
    @{ f = @(7.4, 4, 6.5);     t = @(8.6, 7, 8.5);      s = "dwood" },  # grip
    @{ f = @(7, 7.5, 9);       t = @(9, 10.5, 14);      s = "olive" },  # stock
    @{ f = @(7.6, 10.5, -2);   t = @(8.4, 11.5, -1);    s = "black" }   # sight
)

# --- Rocket launcher ---
Write-GunModel "rocket_launcher" @(
    @{ f = @(6, 6.5, -8);      t = @(10, 10.5, 10);     s = "olive" },  # main tube
    @{ f = @(5.5, 6, -10);     t = @(10.5, 11, -8);     s = "black" },  # front bell
    @{ f = @(5.5, 6, 10);      t = @(10.5, 11, 12);     s = "black" },  # rear bell
    @{ f = @(7.4, 3.5, 2);     t = @(8.6, 6.5, 4);      s = "dwood" },  # rear grip
    @{ f = @(7.4, 3.5, -2);    t = @(8.6, 6.5, 0);      s = "dwood" },  # front grip
    @{ f = @(7.5, 10.5, -1);   t = @(8.5, 12, 1);       s = "metal" },  # sight block
    @{ f = @(6.5, 10.5, 4);    t = @(9.5, 11.2, 8);     s = "red" }     # warning stripe
)

# --- Laser gun ---
Write-GunModel "laser_gun" @(
    @{ f = @(6.9, 7.5, -2);    t = @(9.1, 10.5, 7);     s = "light" },  # sleek body
    @{ f = @(7.4, 8.5, -6.5);  t = @(8.6, 9.75, -2);    s = "dark" },   # emitter barrel
    @{ f = @(7.2, 8.25, -7.5); t = @(8.8, 10, -6.5);    s = "cyan" },   # emitter tip
    @{ f = @(7.1, 10.5, 1);    t = @(8.9, 11.5, 4);     s = "cyan" },   # energy cell
    @{ f = @(7.7, 10.5, -2);   t = @(8.3, 11.6, 0);     s = "black" },  # top fin
    @{ f = @(7.4, 4.5, 5);     t = @(8.6, 7.5, 7);      s = "black" },  # grip
    @{ f = @(7.2, 7.75, 7);    t = @(8.8, 10, 10);      s = "metal" }   # short stock
)

# --- Flamethrower ---
Write-GunModel "flamethrower" @(
    @{ f = @(6.75, 7.5, 0);    t = @(9.25, 10.5, 8);    s = "metal" },  # body
    @{ f = @(6.9, 4.5, 2);     t = @(9.1, 7.5, 7);      s = "orange" }, # fuel tank
    @{ f = @(7.3, 8.5, -7);    t = @(8.7, 9.75, 0);     s = "dark" },   # nozzle barrel
    @{ f = @(7, 8.25, -8);     t = @(9, 10, -7);        s = "brass" },  # nozzle tip
    @{ f = @(7.4, 4.5, 8);     t = @(8.6, 7.5, 10);     s = "dwood" },  # grip
    @{ f = @(7, 10.5, 3);      t = @(9, 11.3, 6);       s = "dark" }    # rear handle
)
