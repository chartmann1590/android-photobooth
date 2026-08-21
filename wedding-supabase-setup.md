# Wedding Supabase Deployment

This branch is configured for a private wedding build. The live gallery is deployed into the existing DriveVault Supabase project:

```text
drivevault-dashcam: szmkkkeaxkwkppxlrfzf
https://szmkkkeaxkwkppxlrfzf.supabase.co/functions/v1/wedding-gallery
```

The Android app expects these ignored `local.properties` values:

```properties
wedding.gallery.url=https://szmkkkeaxkwkppxlrfzf.supabase.co/functions/v1/wedding-gallery
wedding.gallery.password=Hartmann0822!
wedding.upload.token=<random-upload-token>
```

To redeploy the DriveVault-hosted wedding gallery:

```powershell
.\tools\setup-wedding-supabase.ps1 `
  -ProjectRef "szmkkkeaxkwkppxlrfzf" `
  -GalleryPassword "Hartmann0822!" `
  -UploadToken "<random-upload-token>" `
  -SessionSecret "<random-session-secret>" `
  -ServiceRoleKey "<service-role-key>"
```

After deployment, rebuild:

```powershell
.\gradlew.bat assembleWeddingDebug
```
