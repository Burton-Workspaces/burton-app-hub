# Development

## Tooling

- JDK **17**
- Android SDK compile/target **35**, min **26**
- Android Gradle Plugin 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01
- Hilt 2.53.1 (KSP)

Point Gradle at the SDK with `local.properties` (`sdk.dir=…`). That file is gitignored.

## Commands

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew testDebugUnitTest
```

Release assemble is blocked unless `keystore.properties` exists and `storeFile` points at a real keystore. Copy [`keystore.properties.example`](../keystore.properties.example) and keep `keystore.properties`, `*.jks`, and `*.keystore` out of git (see `.gitignore`). GitHub Actions signing is [build automation](build-automation.md).

Debug application id is `com.burton.apphub.debug` so it can sit next to a signed install.

## Layout

```
app/src/main/java/com/burton/apphub/
  MainActivity.kt              nav, bottom bar
  data/index/                  F-Droid index fetch + parse
  data/install/                PackageManager + PackageInstaller
  data/repository/             StoreRepository, DataStore, WorkManager
  domain/                      models
  ui/apps, updates, settings, components, theme
app/src/test/java/…            index parser and repo JSON codec tests
```

Parser tests cover index-v1 and index-v2 catalog JSON. Run those before changing index parsing.

## Network while debugging

The emulator can load GitHub Pages indexes. Install still needs a device (or emulator) that allows unknown apps.

If the Apps tab stays empty: confirm the repo URL returns `index-v1.json` in a browser, then tap sync.

## Versioning while developing

Do not hand-edit `CHANGELOG.md` or `version.txt` on feature branches. Those are owned by [release-please](releases.md) from Conventional Commits on `master`.

Commit subjects must follow Conventional Commits. Install the hook once:

```bash
./scripts/install-git-hooks.sh
```

See [CONTRIBUTING.md](../CONTRIBUTING.md).

After a tagged SemVer release, publish the APK into the shared Burton Workspaces catalog:

```bash
./scripts/publish-fdroid-pages.sh
```

That reads `version.txt`. Setup: [fdroid.md](fdroid.md).
