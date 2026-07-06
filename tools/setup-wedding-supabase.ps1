param(
    [Parameter(Mandatory = $true)]
    [string]$ProjectRef,

    [Parameter(Mandatory = $true)]
    [string]$GalleryPassword,

    [Parameter(Mandatory = $true)]
    [string]$UploadToken,

    [Parameter(Mandatory = $true)]
    [string]$SessionSecret,

    [Parameter(Mandatory = $true)]
    [string]$ServiceRoleKey
)

$ErrorActionPreference = "Stop"

function Invoke-Checked([scriptblock]$Command) {
    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code $LASTEXITCODE"
    }
}

function ConvertTo-Sha256Hex([string]$Value) {
    $sha = [Security.Cryptography.SHA256]::Create()
    $bytes = [Text.Encoding]::UTF8.GetBytes($Value)
    return [BitConverter]::ToString($sha.ComputeHash($bytes)).Replace("-", "").ToLowerInvariant()
}

$saltBytes = New-Object byte[] 24
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($saltBytes)
$salt = [Convert]::ToBase64String($saltBytes)
$passwordHash = ConvertTo-Sha256Hex "${salt}:${GalleryPassword}"

Invoke-Checked { supabase link --project-ref $ProjectRef }
Invoke-Checked { supabase db query --linked --file "supabase/migrations/20260706000100_wedding_gallery.sql" }

$escapedSalt = $salt.Replace("'", "''")
$escapedHash = $passwordHash.Replace("'", "''")
$sql = "insert into public.wedding_gallery_config (key, value) values ('password_salt', '$escapedSalt'), ('password_hash', '$escapedHash') on conflict (key) do update set value = excluded.value, updated_at = now();"
Invoke-Checked { supabase db query --linked $sql }

Invoke-Checked { supabase secrets set `
    "WEDDING_UPLOAD_TOKEN=$UploadToken" `
    "WEDDING_SESSION_SECRET=$SessionSecret" `
    "WEDDING_SERVICE_ROLE_KEY=$ServiceRoleKey" }

Invoke-Checked { supabase functions deploy wedding-gallery --no-verify-jwt }

Write-Host "Wedding gallery deployed: https://$ProjectRef.supabase.co/functions/v1/wedding-gallery"
