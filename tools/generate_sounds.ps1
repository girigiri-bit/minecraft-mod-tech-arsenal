# Synthesizes the mod's custom sounds with ffmpeg (OGG Vorbis, 44.1kHz mono):
#   machine_gun_fire - short punchy "da" burst (rapid fire = dadadada)
#   rifle_fire       - single sharper crack
#   tank_cannon      - deep boom
#   saber_swing      - "fwon" whoosh (downward chirp + noise swell)
#   saber_special    - "vwoon" heavy modulated sweep for the spin attack
$ff = Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages" -Recurse -Filter ffmpeg.exe -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
if (-not $ff) { throw "ffmpeg not found" }

$soundDir = Join-Path $PSScriptRoot "..\src\main\resources\assets\techarsenal\sounds"
New-Item -ItemType Directory -Force $soundDir | Out-Null
$common = @("-y", "-hide_banner", "-loglevel", "error")
$enc = @("-ar", "44100", "-ac", "1", "-c:a", "libvorbis", "-qscale:a", "4")

# Machine gun: white-noise snap + 140Hz thump, very fast decay
& $ff @common `
    -f lavfi -i "anoisesrc=d=0.12:c=white:a=1.0" `
    -f lavfi -i "aevalsrc='0.9*sin(2*PI*140*t)*exp(-28*t)':d=0.12" `
    -filter_complex "[0:a]highpass=f=250,lowpass=f=4500,volume='exp(-38*t)':eval=frame[n];[n][1:a]amix=inputs=2:duration=longest,volume=2.4" `
    @enc (Join-Path $soundDir "machine_gun_fire.ogg")

# Rifle: longer crack with more low end
& $ff @common `
    -f lavfi -i "anoisesrc=d=0.25:c=white:a=1.0" `
    -f lavfi -i "aevalsrc='1.0*sin(2*PI*100*t)*exp(-16*t)':d=0.25" `
    -filter_complex "[0:a]highpass=f=180,lowpass=f=3500,volume='exp(-22*t)':eval=frame[n];[n][1:a]amix=inputs=2:duration=longest,volume=2.6" `
    @enc (Join-Path $soundDir "rifle_fire.ogg")

# Tank cannon: deep rumbling boom
& $ff @common `
    -f lavfi -i "anoisesrc=d=0.8:c=pink:a=1.0" `
    -f lavfi -i "aevalsrc='1.0*sin(2*PI*55*t)*exp(-5*t)+0.4*sin(2*PI*110*t)*exp(-8*t)':d=0.8" `
    -filter_complex "[0:a]lowpass=f=900,volume='exp(-7*t)':eval=frame[n];[n][1:a]amix=inputs=2:duration=longest,volume=2.6" `
    @enc (Join-Path $soundDir "tank_cannon.ogg")

# Saber swing: "fwon" - instant-attack falling chirp + fast whoosh
# (25ms attack so it lands exactly on the swing, no perceived delay)
& $ff @common `
    -f lavfi -i "aevalsrc='0.9*sin(2*PI*(340-450*t)*t)*exp(-9*t)':d=0.3" `
    -f lavfi -i "anoisesrc=d=0.3:c=white:a=0.9" `
    -filter_complex "[1:a]bandpass=f=700:w=500,volume='min(t/0.025\,1)*exp(-11*t)':eval=frame[w];[0:a][w]amix=inputs=2:duration=longest,volume=2.4" `
    @enc (Join-Path $soundDir "saber_swing.ogg")

# Saber special: "vwoon" - instant-attack vibrato hum that rings out
& $ff @common `
    -f lavfi -i "aevalsrc='(0.9*sin(2*PI*(115+55*sin(2*PI*1.8*t))*t)+0.35*sin(2*PI*(230+110*sin(2*PI*1.8*t))*t))*min(t/0.03\,1)*exp(-3.2*t)':d=0.7" `
    -f lavfi -i "anoisesrc=d=0.7:c=white:a=0.5" `
    -filter_complex "[1:a]bandpass=f=420:w=300,volume='min(t/0.03\,1)*exp(-5*t)*0.7':eval=frame[w];[0:a][w]amix=inputs=2:duration=longest,volume=2.4" `
    @enc (Join-Path $soundDir "saber_special.ogg")

Get-ChildItem $soundDir -Name
