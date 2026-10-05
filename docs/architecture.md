# Architecture

The app is a single Gradle module (`:app`), Kotlin, Jetpack Compose, Hilt, and OkHttp. UI never talks HTTP directly; screens collect `StoreRepository` state.

```
ui/          Compose screens and ViewModels (Hilt)
domain/      Repo, CatalogApp, ApkVersion, StoreSnapshot
data/
  index      FdroidIndexClient + FdroidIndexParser (v1 JSON, v2 JSON, v1 jar)
  install    InstalledApps, ApkInstaller, InstallResultReceiver
  repository StoreRepository, LocalPrefs (DataStore), CatalogRefreshWorker
di/          OkHttp client (WAN timeouts)
```

## Repositories

Each source is an F-Droid repo URL. Fetch order:

1. `{address}/index-v1.json`
2. `{address}/index-v2.json`
3. `{address}/index-v1.jar` (zip; first `index-v1.json` / `index-v2.json` entry)

Default seed is Burton Workspaces (`https://burton-workspaces.github.io/burton-app-dist/fdroid/repo`). Extra repos and the auto-update flag live in DataStore (`burton_app_hub`).

Packages that appear in more than one enabled repo are merged; the highest `versionCode` wins.

## Install pipeline

`ApkInstaller` streams the APK with OkHttp, checks SHA-256, then opens a `PackageInstaller` session. `InstallResultReceiver` handles `STATUS_PENDING_USER_ACTION` (system confirm UI) and success/failure. The store then refreshes installed `versionCode`s.

Silent background install is not used: a normal app cannot skip the system installer prompt.

## Auto-update

When the Settings switch is on, `CatalogRefreshWorker` runs about every six hours, refreshes indexes, and posts a notification if any installed app has an update. Tapping the notification opens the Updates tab.

## UI shell

`MainActivity` hosts a `NavHost` and a persistent bottom bar. Tab order is Apps → Updates → Settings. App detail is `app/{packageName}` and keeps the Apps tab selected.
