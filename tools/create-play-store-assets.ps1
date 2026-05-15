Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing
try { Add-Type -AssemblyName System.Speech } catch {}
try { Add-Type -AssemblyName System.Runtime.WindowsRuntime } catch {}

$Root = Resolve-Path (Join-Path $PSScriptRoot "..")
$Out = Join-Path $Root "play-store"
$Listing = Join-Path $Out "listing"
$Graphics = Join-Path $Out "graphics"
$Screens = Join-Path $Out "screenshots"
$Phone = Join-Path $Screens "phone"
$Tablet7 = Join-Path $Screens "7-inch-tablet"
$Tablet10 = Join-Path $Screens "10-inch-tablet"
$Promo = Join-Path $Out "promo-video"
$Frames = Join-Path $Promo "frames"
$script:PromoAudio = $null

@($Listing, $Graphics, $Phone, $Tablet7, $Tablet10, $Promo, $Frames) | ForEach-Object {
    New-Item -ItemType Directory -Force -Path $_ | Out-Null
}

function ColorFromHex([string]$hex, [int]$alpha = 255) {
    $h = $hex.TrimStart("#")
    return [System.Drawing.Color]::FromArgb(
        $alpha,
        [Convert]::ToInt32($h.Substring(0, 2), 16),
        [Convert]::ToInt32($h.Substring(2, 2), 16),
        [Convert]::ToInt32($h.Substring(4, 2), 16)
    )
}

$Dark = ColorFromHex "0F0F1A"
$Surface = ColorFromHex "1A1A2E"
$Surface2 = ColorFromHex "252542"
$Rose = ColorFromHex "E91E63"
$RoseLight = ColorFromHex "FF5C8D"
$Gold = ColorFromHex "FFD54F"
$White = [System.Drawing.Color]::White
$Muted = ColorFromHex "BFC0D6"
$Success = ColorFromHex "4CAF50"

function New-Canvas([int]$w, [int]$h, [bool]$alpha = $false) {
    $format = if ($alpha) {
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
    } else {
        [System.Drawing.Imaging.PixelFormat]::Format24bppRgb
    }
    $bmp = New-Object System.Drawing.Bitmap($w, $h, $format)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    return @($bmp, $g)
}

function FontOf([float]$size, [int]$style = 0) {
    try { return New-Object System.Drawing.Font("Segoe UI", $size, $style, [System.Drawing.GraphicsUnit]::Pixel) }
    catch { return New-Object System.Drawing.Font([System.Drawing.FontFamily]::GenericSansSerif, $size, $style, [System.Drawing.GraphicsUnit]::Pixel) }
}

function Save-Png($bmp, [string]$path) {
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
}

function RoundedRectPath([System.Drawing.RectangleF]$rect, [float]$radius) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $radius * 2
    $path.AddArc($rect.X, $rect.Y, $d, $d, 180, 90)
    $path.AddArc($rect.Right - $d, $rect.Y, $d, $d, 270, 90)
    $path.AddArc($rect.Right - $d, $rect.Bottom - $d, $d, $d, 0, 90)
    $path.AddArc($rect.X, $rect.Bottom - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    return $path
}

function FillRounded($g, [float]$x, [float]$y, [float]$w, [float]$h, [float]$r, $color) {
    if ($r -le 0) {
        $brush = New-Object System.Drawing.SolidBrush($color)
        $g.FillRectangle($brush, $x, $y, $w, $h)
        $brush.Dispose()
        return
    }
    $path = RoundedRectPath ([System.Drawing.RectangleF]::new($x, $y, $w, $h)) $r
    $brush = New-Object System.Drawing.SolidBrush($color)
    $g.FillPath($brush, $path)
    $brush.Dispose()
    $path.Dispose()
}

function StrokeRounded($g, [float]$x, [float]$y, [float]$w, [float]$h, [float]$r, $color, [float]$width = 2) {
    if ($r -le 0) {
        $pen = New-Object System.Drawing.Pen($color, $width)
        $g.DrawRectangle($pen, $x, $y, $w, $h)
        $pen.Dispose()
        return
    }
    $path = RoundedRectPath ([System.Drawing.RectangleF]::new($x, $y, $w, $h)) $r
    $pen = New-Object System.Drawing.Pen($color, $width)
    $g.DrawPath($pen, $path)
    $pen.Dispose()
    $path.Dispose()
}

function DrawText($g, [string]$text, [float]$x, [float]$y, [float]$size, $color, [int]$style = 0, [float]$w = 0, [float]$h = 0, [string]$align = "Near") {
    $font = FontOf $size $style
    $brush = New-Object System.Drawing.SolidBrush($color)
    if ($w -gt 0 -and $h -gt 0) {
        $fmt = New-Object System.Drawing.StringFormat
        $fmt.Alignment = [System.Drawing.StringAlignment]::$align
        $fmt.LineAlignment = [System.Drawing.StringAlignment]::Near
        $g.DrawString($text, $font, $brush, [System.Drawing.RectangleF]::new($x, $y, $w, $h), $fmt)
        $fmt.Dispose()
    } else {
        $g.DrawString($text, $font, $brush, $x, $y)
    }
    $brush.Dispose()
    $font.Dispose()
}

function DrawCameraIcon($g, [float]$cx, [float]$cy, [float]$scale) {
    FillRounded $g ($cx - 78*$scale) ($cy - 34*$scale) (156*$scale) (82*$scale) (16*$scale) $White
    FillRounded $g ($cx - 40*$scale) ($cy - 62*$scale) (80*$scale) (36*$scale) (10*$scale) $White
    $lensOuter = New-Object System.Drawing.SolidBrush($Dark)
    $lensInner = New-Object System.Drawing.SolidBrush($White)
    $g.FillEllipse($lensOuter, $cx - 35*$scale, $cy - 19*$scale, 70*$scale, 70*$scale)
    $g.FillEllipse($lensInner, $cx - 20*$scale, $cy - 4*$scale, 40*$scale, 40*$scale)
    $flash = New-Object System.Drawing.SolidBrush($Gold)
    $g.FillEllipse($flash, $cx + 52*$scale, $cy - 22*$scale, 20*$scale, 20*$scale)
    $flash.Dispose()
    $lensOuter.Dispose()
    $lensInner.Dispose()
}

function DrawBackground($g, [int]$w, [int]$h) {
    $g.Clear($Dark)
    $brush1 = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        [System.Drawing.Rectangle]::new(0, 0, $w, $h),
        (ColorFromHex "211337"),
        $Dark,
        35
    )
    $g.FillRectangle($brush1, 0, 0, $w, $h)
    $brush1.Dispose()
    $roseGlow = New-Object System.Drawing.SolidBrush((ColorFromHex "E91E63" 40))
    $goldGlow = New-Object System.Drawing.SolidBrush((ColorFromHex "FFD54F" 28))
    $g.FillEllipse($roseGlow, $w - 560, -180, 660, 520)
    $g.FillEllipse($goldGlow, -180, $h - 300, 520, 420)
    $roseGlow.Dispose()
    $goldGlow.Dispose()
}

function DrawButton($g, [float]$x, [float]$y, [float]$w, [float]$h, [string]$text, $bg, $fg = $White) {
    FillRounded $g $x $y $w $h 28 $bg
    DrawText $g $text ($x + 42) ($y + 18) 30 $fg ([System.Drawing.FontStyle]::Bold)
}

function DrawTopBar($g, [string]$title, [int]$photoCount = -1, [int]$y = 58) {
    FillRounded $g 70 $y 1780 86 0 $Surface
    FillRounded $g 94 ($y + 18) 50 50 25 $Surface2
    DrawText $g "x" 111 ($y + 20) 31 $White
    DrawText $g $title 172 ($y + 19) 36 $White ([System.Drawing.FontStyle]::Bold)
    if ($photoCount -ge 0) {
        DrawText $g "$photoCount photos" 1640 ($y + 28) 27 $Muted 0 150 42 "Far"
    }
}

function DrawHomeUi($g, [string]$headline, [string]$subhead) {
    DrawBackground $g 1920 1080
    DrawText $g $headline 100 65 52 $White ([System.Drawing.FontStyle]::Bold)
    DrawText $g $subhead 102 128 28 $Muted
    FillRounded $g 100 205 760 700 44 (ColorFromHex "171728" 230)
    $grad = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        [System.Drawing.Rectangle]::new(360, 290, 180, 180), $Rose, $RoseLight, 40
    )
    $g.FillEllipse($grad, 360, 285, 190, 190)
    $grad.Dispose()
    DrawCameraIcon $g 455 375 0.78
    DrawText $g "PHOTOBOOTH" 195 540 70 $White ([System.Drawing.FontStyle]::Bold) 570 90 "Center"
    DrawText $g "Capture the moment" 250 630 34 $Muted 0 470 52 "Center"
    FillRounded $g 1010 240 650 580 40 (ColorFromHex "171728" 230)
    DrawButton $g 1100 310 470 85 "Start Photobooth" $Rose
    DrawButton $g 1100 425 470 78 "View Gallery" $Surface2
    DrawButton $g 1100 525 470 78 "Settings" $Surface2
    DrawButton $g 1100 625 470 78 "Tutorial" $Surface2
    FillRounded $g 1100 725 470 68 22 (ColorFromHex "FFD54F" 42)
    DrawText $g "Get More Photos" 1190 742 25 $Gold ([System.Drawing.FontStyle]::Bold)
}

function DrawCaptureUi($g, [string]$headline, [string]$subhead) {
    DrawBackground $g 1920 1080
    DrawText $g $headline 100 65 52 $White ([System.Drawing.FontStyle]::Bold)
    DrawText $g $subhead 102 128 28 $Muted
    FillRounded $g 95 195 1730 740 44 (ColorFromHex "10101C")
    $preview = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        [System.Drawing.Rectangle]::new(95, 195, 1730, 740), (ColorFromHex "394963"), (ColorFromHex "1A1A2E"), 20
    )
    $g.FillRectangle($preview, 95, 195, 1730, 740)
    $preview.Dispose()
    FillRounded $g 135 230 54 54 27 (ColorFromHex "0F0F1A" 150)
    DrawText $g "x" 153 232 31 $White
    FillRounded $g 1245 233 210 46 23 (ColorFromHex "0F0F1A" 150)
    DrawText $g "15 photos left" 1270 244 22 $White
    FillRounded $g 1480 233 120 46 23 (ColorFromHex "0F0F1A" 150)
    DrawText $g "Ready" 1512 244 22 $White
    $ring = New-Object System.Drawing.SolidBrush((ColorFromHex "FFD54F" 180))
    $g.FillEllipse($ring, 785, 365, 350, 350)
    $ring.Dispose()
    StrokeRounded $g 785 365 350 350 175 $Gold 8
    DrawText $g "3" 890 405 190 $White ([System.Drawing.FontStyle]::Bold)
    DrawText $g "Get ready" 860 720 42 $White ([System.Drawing.FontStyle]::Bold)
    $cap = New-Object System.Drawing.SolidBrush($Rose)
    $g.FillEllipse($cap, 870, 845, 120, 120)
    $cap.Dispose()
    DrawCameraIcon $g 930 900 0.35
    DrawText $g "Tap to capture" 830 980 25 (ColorFromHex "FFFFFF" 180)
}

function DrawGalleryUi($g, [string]$headline, [string]$subhead, [bool]$detail = $false) {
    DrawBackground $g 1920 1080
    DrawText $g $headline 100 65 52 $White ([System.Drawing.FontStyle]::Bold)
    DrawText $g $subhead 102 128 28 $Muted
    DrawTopBar $g "Gallery" 24 185
    $colors = @("E91E63","FFD54F","42A5F5","66BB6A","FF5C8D","7E57C2","26C6DA","FFA726")
    for ($i = 0; $i -lt 8; $i++) {
        $col = $i % 4
        $row = [Math]::Floor($i / 4)
        $x = 115 + $col * 315
        $y = 315 + $row * 300
        FillRounded $g $x $y 280 250 22 $Surface
        $photoBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
            [System.Drawing.Rectangle]::new($x, $y, 280, 178),
            (ColorFromHex $colors[$i]),
            (ColorFromHex "1A1A2E"),
            45
        )
        $g.FillRectangle($photoBrush, $x, $y, 280, 178)
        $photoBrush.Dispose()
        DrawText $g "My Event" ($x + 18) ($y + 198) 22 $White ([System.Drawing.FontStyle]::Bold)
        if ($i % 3 -eq 0) { DrawText $g "uploaded" ($x + 185) ($y + 198) 16 $Gold }
    }
    if ($detail) {
        FillRounded $g 1115 300 700 670 34 (ColorFromHex "1A1A2E" 245)
        FillRounded $g 1145 340 310 390 20 $White
        $sample = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
            [System.Drawing.Rectangle]::new(1160, 355, 280, 360), $Rose, $Gold, 35
        )
        $g.FillRectangle($sample, 1160, 355, 280, 360)
        $sample.Dispose()
        DrawText $g "My Event" 1490 340 40 $White ([System.Drawing.FontStyle]::Bold)
        FillRounded $g 1490 420 240 140 14 $White
        DrawText $g "QR" 1572 456 48 $Dark ([System.Drawing.FontStyle]::Bold)
        DrawButton $g 1490 600 255 60 "Upload" $Rose
        DrawButton $g 1490 675 255 60 "Print" $Gold $Dark
        DrawButton $g 1490 750 255 60 "Share to Apps" $Surface2
        DrawText $g "Email and SMS fields are ready when your services are configured." 1490 835 22 $Muted 0 280 110 "Near"
    }
}

function DrawSettingsUi($g, [string]$headline, [string]$subhead) {
    DrawBackground $g 1920 1080
    DrawText $g $headline 100 65 52 $White ([System.Drawing.FontStyle]::Bold)
    DrawText $g $subhead 102 128 28 $Muted
    DrawTopBar $g "Settings"
    $sections = @(
        @("Event", "Event name, filename pattern"),
        @("Camera", "Front camera and camera selection"),
        @("Capture Mode and Filters", "Booth mode, GIFs, filters, layouts"),
        @("Frame Overlay", "Transparent PNG event frames"),
        @("Watermark / Logo", "Brand new captures"),
        @("Upload", "Anonymous host or Immich"),
        @("SMS Gateway", "Send guest links by text"),
        @("SMTP / Email", "Send photos by email")
    )
    for ($i = 0; $i -lt $sections.Count; $i++) {
        $col = $i % 2
        $row = [Math]::Floor($i / 2)
        $x = 110 + $col * 860
        $y = 185 + $row * 190
        FillRounded $g $x $y 805 150 24 $Surface
        FillRounded $g ($x + 26) ($y + 30) 58 58 16 (ColorFromHex "E91E63" 35)
        DrawText $g $sections[$i][0] ($x + 110) ($y + 28) 31 $White ([System.Drawing.FontStyle]::Bold)
        DrawText $g $sections[$i][1] ($x + 110) ($y + 73) 24 $Muted
        FillRounded $g ($x + 680) ($y + 52) 74 36 18 $(if ($i -in 2,3,4) { $Rose } else { $Surface2 })
    }
}

function DrawFrameDesignerUi($g, [string]$headline, [string]$subhead) {
    DrawBackground $g 1920 1080
    DrawText $g $headline 100 65 52 $White ([System.Drawing.FontStyle]::Bold)
    DrawText $g $subhead 102 128 28 $Muted
    DrawTopBar $g "Manage Frames"
    FillRounded $g 130 210 760 650 26 $Surface
    DrawText $g "Upload New Frame" 180 255 34 $White ([System.Drawing.FontStyle]::Bold)
    FillRounded $g 180 330 620 72 16 $Surface2
    DrawText $g "Frame name" 210 350 24 $Muted
    DrawButton $g 180 440 260 64 "Choose PNG" $Surface2
    FillRounded $g 180 535 390 250 18 (ColorFromHex "FFFFFF" 18)
    StrokeRounded $g 215 570 320 180 24 $Rose 6
    DrawText $g "transparent event overlay" 235 640 24 $Muted
    DrawButton $g 620 710 210 64 "Save Frame" $Rose
    FillRounded $g 1010 210 760 650 26 $Surface
    DrawText $g "Existing Frames" 1060 255 34 $White ([System.Drawing.FontStyle]::Bold)
    @("Wedding Gold", "Birthday Pop", "Company Step-and-Repeat") | ForEach-Object -Begin { $i = 0 } -Process {
        $y = 330 + $i * 140
        FillRounded $g 1060 $y 630 96 18 $Surface2
        FillRounded $g 1085 ($y + 18) 62 62 10 (ColorFromHex "FFFFFF" 25)
        DrawText $g $_ 1180 ($y + 28) 27 $White
        DrawText $g "Delete" 1575 ($y + 30) 22 $Rose
        $i++
    }
}

function DrawIcon() {
    $pair = New-Canvas 512 512 $true
    $bmp = $pair[0]; $g = $pair[1]
    $g.Clear([System.Drawing.Color]::Transparent)
    $bg = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        [System.Drawing.Rectangle]::new(20, 20, 472, 472), $Rose, (ColorFromHex "AD1457"), 45
    )
    $g.FillEllipse($bg, 20, 20, 472, 472)
    $bg.Dispose()
    $ring = New-Object System.Drawing.Pen((ColorFromHex "FFD54F" 170), 14)
    $g.DrawEllipse($ring, 42, 42, 428, 428)
    $ring.Dispose()
    DrawCameraIcon $g 256 255 1.55
    $spark = New-Object System.Drawing.SolidBrush($Gold)
    $g.FillEllipse($spark, 355, 142, 44, 44)
    $spark.Dispose()
    Save-Png $bmp (Join-Path $Graphics "icon-512.png")
    $g.Dispose(); $bmp.Dispose()
}

function DrawFeatureGraphic() {
    $pair = New-Canvas 1024 500
    $bmp = $pair[0]; $g = $pair[1]
    DrawBackground $g 1024 500
    DrawText $g "Your Android" 58 82 48 $White ([System.Drawing.FontStyle]::Bold)
    DrawText $g "Event Photo Booth" 58 142 52 $White ([System.Drawing.FontStyle]::Bold)
    DrawText $g "Capture, share, print, and keep the moments flowing." 62 226 23 $Muted
    FillRounded $g 62 315 250 52 24 (ColorFromHex "FFD54F" 45)
    DrawText $g "Frames  GIFs  QR" 96 327 22 $Gold ([System.Drawing.FontStyle]::Bold)
    FillRounded $g 610 72 315 350 34 $Surface
    DrawCameraIcon $g 768 206 0.95
    DrawText $g "PHOTOBOOTH" 637 325 38 $White ([System.Drawing.FontStyle]::Bold) 262 58 "Center"
    DrawText $g "Capture the moment" 655 374 22 $Muted 0 230 34 "Center"
    Save-Png $bmp (Join-Path $Graphics "feature-graphic-1024x500.png")
    $g.Dispose(); $bmp.Dispose()
}

function SaveScreen([string]$path, [scriptblock]$draw) {
    $pair = New-Canvas 1920 1080
    $bmp = $pair[0]; $g = $pair[1]
    & $draw $g
    Save-Png $bmp $path
    $g.Dispose(); $bmp.Dispose()
}

function GenerateScreenshots([string]$folder, [bool]$phoneSet) {
    SaveScreen (Join-Path $folder "01-home.png") { param($g) DrawHomeUi $g "Ready for every event" "Start a self-serve photo booth in seconds." }
    SaveScreen (Join-Path $folder "02-capture-countdown.png") { param($g) DrawCaptureUi $g "A clear countdown guests understand" "Smile prompt, screen flash, and full-screen capture." }
    SaveScreen (Join-Path $folder "03-gallery.png") { param($g) DrawGalleryUi $g "Every capture in one gallery" "Photos and GIFs are ready for guests." $false }
    if ($phoneSet) {
        SaveScreen (Join-Path $folder "04-sharing-qr-print.png") { param($g) DrawGalleryUi $g "Share by QR, email, SMS, or print" "Fast delivery options for event hosts and guests." $true }
        SaveScreen (Join-Path $folder "05-settings-capture.png") { param($g) DrawSettingsUi $g "Customize the booth experience" "Frames, filters, GIFs, layouts, upload, and sharing." }
        SaveScreen (Join-Path $folder "06-frame-designer.png") { param($g) DrawFrameDesignerUi $g "Add custom event frames" "Import transparent PNG overlays for branded captures." }
    } else {
        SaveScreen (Join-Path $folder "04-settings.png") { param($g) DrawSettingsUi $g "Set up your event your way" "Templates, sharing, uploads, logos, and capture options." }
    }
}

function WriteListingFiles() {
    $title = "Photobooth Event Camera"
    $short = "Turn any Android device into a fun event photo booth with sharing and prints"
    $full = @"
Make every event easier to remember with Photobooth Event Camera.

Turn your Android phone or tablet into a self-serve photo booth for weddings, parties, school events, fundraisers, company events, reunions, pop-ups, and family celebrations. Set it up once, start the booth, and let guests capture moments with a clean full-screen camera experience.

Photobooth is made for real events: quick capture, simple sharing, custom frames, GIFs, QR codes, email, SMS, and wireless printing from one Android device.

What you can do with Photobooth:

• Create a self-serve event photo station
• Capture photos with a clear three-second countdown
• Use booth mode for multi-photo sessions
• Make animated GIFs from booth captures
• Add event frames and transparent PNG overlays
• Apply photo filters like sepia, vintage, warm, cool, vivid, black and white, and grayscale
• Choose event layouts including single photo, 2x2 grid, and vertical photo strip
• Add a watermark or logo for branded events
• Save every capture in the local gallery
• Upload photos and show QR codes for quick guest access
• Send photos by email with SMTP setup
• Send photos by SMS with your gateway setup
• Share through Android apps already on the device
• Print wirelessly with Android print support
• Configure event name, filenames, camera, filters, templates, frames, sharing, and upload settings

Perfect for:

• Weddings and receptions
• Birthday parties
• Graduation events
• School dances and fundraisers
• Corporate events
• Church and community events
• Pop-up booths
• Family reunions
• DIY party photo stations
• Branded event activations

Photobooth keeps the experience simple for guests and flexible for hosts. Guests tap to capture, smile for the countdown, and the finished photo or GIF is ready in the gallery for sharing, uploading, QR code access, or printing.

Privacy matters at events. Photos are saved locally on your device unless you choose to upload, share, email, text, or print them. Optional service credentials are configured on your device for the services you choose to use.

Bring a photo booth feel to your next event with the Android device you already have.
"@
    Set-Content -Path (Join-Path $Listing "title.txt") -Value $title -Encoding UTF8
    Set-Content -Path (Join-Path $Listing "short-description.txt") -Value $short -Encoding UTF8
    Set-Content -Path (Join-Path $Listing "full-description.txt") -Value $full -Encoding UTF8
    Set-Content -Path (Join-Path $Listing "listing-copy.md") -Encoding UTF8 -Value @"
# Photobooth Event Camera

## Title
$title

## Short Description
$short

## Full Description
$full
"@
    Set-Content -Path (Join-Path $Listing "asset-alt-text.md") -Encoding UTF8 -Value @"
# Google Play Alt Text

## Icon
Camera mark on a rose and gold photobooth badge.

## Feature Graphic
Photobooth Event Camera branding beside a stylized app home screen.

## Phone Screenshots
1. Home screen with Start Photobooth, Gallery, Settings, and Tutorial.
2. Capture screen showing the three-second countdown.
3. Gallery grid with saved event photos.
4. Gallery detail with QR sharing, upload, print, and share actions.
5. Settings screen with capture, frame, upload, SMS, and email options.
6. Frame manager for importing transparent PNG event overlays.

## Tablet Screenshots
1. Landscape home screen for launching the booth.
2. Landscape capture countdown screen.
3. Landscape gallery grid for event photos.
4. Landscape settings screen for event configuration.
"@
    Set-Content -Path (Join-Path $Out "README.md") -Encoding UTF8 -Value @"
# Google Play Store Listing Package

This folder contains ready-to-upload Google Play listing materials for Photobooth Event Camera.

## App Details

- Title: `listing/title.txt`
- Short description: `listing/short-description.txt`
- Full description: `listing/full-description.txt`
- Combined copy: `listing/listing-copy.md`
- Alt text: `listing/asset-alt-text.md`

## Graphics

- App icon: `graphics/icon-512.png` — 512x512 PNG with alpha.
- Feature graphic: `graphics/feature-graphic-1024x500.png` — 1024x500 PNG.

## Screenshots

Upload the contents of each folder to the matching Play Console device section:

- Phone: `screenshots/phone/`
- 7-inch tablet: `screenshots/7-inch-tablet/`
- 10-inch tablet: `screenshots/10-inch-tablet/`

## Promo Video

- Final MP4: `promo-video/photobooth-promo.mp4`
- Voiceover script: `promo-video/voiceover-script.txt`
- Voiceover audio: `promo-video/voiceover-final.mp3`
- Captions: `promo-video/voiceover-captions.srt`
- YouTube notes: `promo-video/youtube-upload-notes.md`

Google Play's preview video field requires a YouTube URL. Upload `photobooth-promo.mp4` to YouTube as public or unlisted, disable ads/monetization, make it embeddable, then paste the YouTube video URL into Play Console.
"@
}

function GenerateVoiceover() {
    $script = @"
Turn your Android device into a self-serve photo booth for any event.

With Photobooth Event Camera, guests get a simple countdown, fun frames, filters, booth sessions, and animated GIFs.

Every capture is saved to the gallery, ready to share by QR code, email, SMS, Android apps, or wireless printing.

Set up your event, start the booth, and keep the moments flowing.
"@
    Set-Content -Path (Join-Path $Promo "voiceover-script.txt") -Value $script -Encoding UTF8
    Set-Content -Path (Join-Path $Promo "youtube-upload-notes.md") -Encoding UTF8 -Value @"
# YouTube Upload Notes For Google Play

Google Play accepts a preview video by YouTube URL, not by direct MP4 upload.

Upload `photobooth-promo.mp4` to YouTube and use these settings:

- Visibility: Public or Unlisted
- Embedding: Allowed
- Age restriction: Off
- Monetization/ads: Off
- URL: Use the clean video URL only, with no playlist or timestamp parameters

After upload, paste the YouTube URL into Play Console's preview video field.
"@
    $edgeAudio = Join-Path $Promo "voiceover-final.mp3"
    $edgeCaptions = Join-Path $Promo "voiceover-captions.srt"
    try {
        & python -m edge_tts -f (Join-Path $Promo "voiceover-script.txt") -v en-US-GuyNeural --rate=-12% --write-media $edgeAudio --write-subtitles $edgeCaptions | Out-Null
        if ((Test-Path $edgeAudio) -and (Get-Item $edgeAudio).Length -gt 1024) {
            $script:PromoAudio = $edgeAudio
            return
        }
    } catch {
        Write-Warning "edge-tts voiceover generation failed. $($_.Exception.Message)"
    }
    $wav = Join-Path $Promo "voiceover-play.wav"
    if (Test-Path $wav) {
        try { Remove-Item -LiteralPath $wav -Force } catch {}
    }
    $generated = $false
    try {
        [Windows.Media.SpeechSynthesis.SpeechSynthesizer, Windows.Media.SpeechSynthesis, ContentType = WindowsRuntime] | Out-Null
        $winSynth = New-Object Windows.Media.SpeechSynthesis.SpeechSynthesizer
        $operation = $winSynth.SynthesizeTextToStreamAsync($script)
        $method = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
            $_.Name -eq "AsTask" -and
            $_.GetParameters().Count -eq 1 -and
            $_.GetParameters()[0].ParameterType.Name -eq "IAsyncOperation`1"
        } | Select-Object -First 1)
        if ($null -eq $method) { throw "WinRT AsTask helper was not found." }
        $task = $method.MakeGenericMethod([Windows.Media.SpeechSynthesis.SpeechSynthesisStream]).Invoke($null, @($operation))
        $speechStream = $task.GetAwaiter().GetResult()
        $netStream = [System.WindowsRuntimeSystemExtensions]::AsStreamForRead($speechStream)
        $fileStream = [System.IO.File]::Open($wav, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write)
        try {
            $netStream.CopyTo($fileStream)
        } finally {
            $fileStream.Dispose()
            $netStream.Dispose()
            $speechStream.Dispose()
            $winSynth.Dispose()
        }
        if ((Get-Item $wav).Length -lt 1024) {
            throw "WinRT generated an unexpectedly small audio file."
        }
        $generated = $true
        $script:PromoAudio = $wav
    } catch {
        Write-Warning "WinRT voiceover generation failed. $($_.Exception.Message)"
        if (Test-Path $wav) {
            try { Remove-Item -LiteralPath $wav -Force } catch {}
        }
    }
    if ($generated) { return }
    $fallback = Join-Path $Promo "voiceover-silent-placeholder.wav"
    if (Test-Path $fallback) {
        $script:PromoAudio = $fallback
        return
    }
    Write-Warning "No local text-to-speech voice is available; creating a silent placeholder audio track."
    & ffmpeg -y -f lavfi -i anullsrc=channel_layout=stereo:sample_rate=44100 -t 28 $fallback | Out-Null
    Set-Content -Path (Join-Path $Promo "voiceover-generation-note.txt") -Encoding UTF8 -Value @"
The script generated a silent placeholder audio track because this Windows environment has no usable text-to-speech voice installed.

The narration script is complete in voiceover-script.txt. To finish the voiceover audio, install a Windows speech voice and rerun tools/create-play-store-assets.ps1, or record the script and replace voiceover-silent-placeholder.wav before re-encoding the video.
"@
    $script:PromoAudio = $fallback
}

function GeneratePromoFrames() {
    SaveScreen (Join-Path $Frames "frame-001.png") { param($g) DrawHomeUi $g "Your Android Event Photo Booth" "Set up once. Let guests capture all night." }
    SaveScreen (Join-Path $Frames "frame-002.png") { param($g) DrawCaptureUi $g "Countdown capture guests understand" "A clean camera flow built for events." }
    SaveScreen (Join-Path $Frames "frame-003.png") { param($g) DrawSettingsUi $g "Frames, filters, GIFs, and layouts" "Customize the booth for weddings, parties, and events." }
    SaveScreen (Join-Path $Frames "frame-004.png") { param($g) DrawGalleryUi $g "Every memory saved in the gallery" "Photos and GIFs stay ready after capture." $false }
    SaveScreen (Join-Path $Frames "frame-005.png") { param($g) DrawGalleryUi $g "Share by QR, email, SMS, apps, or print" "Fast handoff for hosts and guests." $true }
    SaveScreen (Join-Path $Frames "frame-006.png") { param($g) DrawHomeUi $g "Start the booth. Keep moments flowing." "Photobooth Event Camera for Android." }
}

function GeneratePromoVideo() {
    GenerateVoiceover
    GeneratePromoFrames
    $listPath = Join-Path $Promo "frames.txt"
    @"
file 'frames/frame-001.png'
duration 4.5
file 'frames/frame-002.png'
duration 4.5
file 'frames/frame-003.png'
duration 4.5
file 'frames/frame-004.png'
duration 4.5
file 'frames/frame-005.png'
duration 5
file 'frames/frame-006.png'
duration 5
file 'frames/frame-006.png'
"@ | Set-Content -Path $listPath -Encoding ASCII
    $wav = $script:PromoAudio
    if ([string]::IsNullOrWhiteSpace($wav) -or -not (Test-Path $wav)) {
        throw "Promo audio was not generated"
    }
    $mp4 = Join-Path $Promo "photobooth-promo.mp4"
    & ffmpeg -y -f concat -safe 0 -i $listPath -i $wav -f lavfi -i "anullsrc=channel_layout=stereo:sample_rate=44100" -filter_complex "[1:a][2:a]amix=inputs=2:duration=first:weights=1 0.08[a]" -map 0:v -map "[a]" -vf "format=yuv420p,fps=30" -c:v libx264 -preset medium -crf 18 -c:a aac -shortest $mp4 | Out-Null
}

function ValidateImage([string]$path, [int]$w, [int]$h, [bool]$requireAlpha) {
    $bmp = [System.Drawing.Bitmap]::FromFile($path)
    try {
        if ($bmp.Width -ne $w -or $bmp.Height -ne $h) {
            throw "$path is $($bmp.Width)x$($bmp.Height), expected ${w}x${h}"
        }
        $hasAlpha = [System.Drawing.Image]::IsAlphaPixelFormat($bmp.PixelFormat)
        if ($requireAlpha -and -not $hasAlpha) { throw "$path does not have alpha" }
        if (-not $requireAlpha -and $hasAlpha) { throw "$path has alpha but should not" }
    } finally {
        $bmp.Dispose()
    }
}

function ValidateOutputs() {
    ValidateImage (Join-Path $Graphics "icon-512.png") 512 512 $true
    ValidateImage (Join-Path $Graphics "feature-graphic-1024x500.png") 1024 500 $false
    Get-ChildItem $Screens -Recurse -Filter *.png | ForEach-Object {
        ValidateImage $_.FullName 1920 1080 $false
    }
    $titleLen = (Get-Content (Join-Path $Listing "title.txt") -Raw).Trim().Length
    $shortLen = (Get-Content (Join-Path $Listing "short-description.txt") -Raw).Trim().Length
    $fullLen = (Get-Content (Join-Path $Listing "full-description.txt") -Raw).Trim().Length
    if ($titleLen -gt 30) { throw "Title is $titleLen characters" }
    if ($shortLen -gt 80) { throw "Short description is $shortLen characters" }
    if ($fullLen -gt 4000) { throw "Full description is $fullLen characters" }
    $mp4 = Join-Path $Promo "photobooth-promo.mp4"
    if (-not (Test-Path $mp4)) { throw "Promo video was not created" }
    & ffprobe -v error -select_streams v:0 -show_entries stream=width,height -of csv=p=0 $mp4 | Out-Null
}

WriteListingFiles
DrawIcon
DrawFeatureGraphic
GenerateScreenshots $Phone $true
GenerateScreenshots $Tablet7 $false
GenerateScreenshots $Tablet10 $false
GeneratePromoVideo
ValidateOutputs

Write-Host "Play Store package generated at $Out"
