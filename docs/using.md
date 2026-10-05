# Using Burton App Hub

Burton App Hub is a client for F-Droid repositories. It does not host APKs itself. Put the phone on a network that can reach the repository URLs (GitHub Pages for the default Burton Workspaces repo).

## Permissions

| Android | Permission | Why |
| --- | --- | --- |
| All | Internet | Fetch `index-v1.json` / `index-v2.json` and download APKs |
| All | Install unknown apps | `PackageInstaller` for packages from repositories |
| 13+ | Notifications | Optional; used when auto-update finds newer packages |
| All | Query all packages | Compare repository `versionCode` to what is installed |

On first **Install** / **Update**, Android may send you to Settings to allow this app to install unknown apps.

## Screens

Bottom navigation, left to right: **Apps**, **Updates**, **Settings**.

### Apps

Lists every package from enabled repositories. Search filters by name, summary, and application id as you type. The sync control on the title row refetches indexes.

Tap a row for detail: versions, repository, license, and install / update / open / uninstall.

### Updates

Only installed apps whose repository `versionCode` is higher than the installed one. **Update all** queues a download + system install prompt for each.

### Settings

- **About** — app name and `versionName` from `version.txt`. Long-press files an issue.
- **Auto update** — a 6-hour WorkManager check; notifies when updates exist (does not silently install — Android still shows the system installer)
- **Repositories** — enable, disable, or remove a source; **+** adds another F-Droid repo URL (optional SHA-256 fingerprint, stored for display)

### File an issue

Shake the phone, or long-press **About** in Settings. Burton Issues opens on New issue with this app already selected. Nothing is posted until you submit; Back cancels.

Default repository:

```
https://burton-workspaces.github.io/burton-app-dist/fdroid/repo
```

Fingerprint: `D5 17 D0 45 B3 E2 FB 29 7C 0E C0 BB A1 7A FF 03 48 8A 4B B4 EF 43 13 31 A3 A1 C3 FB 46 A5 EF B6`

## Installs

Downloads go to the app cache. SHA-256 from the index is verified before `PackageInstaller` opens the system confirmation. Failed checksums abort the install.

Debug builds use application id `com.burton.apphub.debug` and can sit next to a signed install.
