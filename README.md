# Burton App Hub

An F-Droid-style client for Burton Workspaces apps. The phone reads F-Droid repository indexes, lists packages, and installs APKs through the system package installer.

Signed APKs are published on [GitHub Releases](https://github.com/Burton-Workspaces/burton-app-hub/releases). Droidify / F-Droid: [burton-app-dist](https://github.com/Burton-Workspaces/burton-app-dist) (`https://burton-workspaces.github.io/burton-app-dist/fdroid/repo`). Add more repositories from Settings.

## What it does

- **Apps** — every package from enabled repositories, with search by name, summary, or application id
- **Updates** — installed apps that have a newer `versionCode` in a repository; update one or all
- **Settings** — about (app name and version), extra repositories, auto-update checks
- **App detail** — versions, install / update / open / uninstall

First launch fetches the default Burton Workspaces index. Installs need the “unknown apps” permission for this package.

## Requirements

- Android 8.0+ (API 26)
- Network access to the repository hosts (HTTPS)
- Permission to install unknown apps (prompted on first install)

## Docs

| Doc | Contents |
| --- | --- |
| [Using the app](docs/using.md) | Screens, repositories, installs, and auto-update |
| [Architecture](docs/architecture.md) | Packages, F-Droid indexes, install pipeline |
| [Development](docs/development.md) | Build, run, test, project layout |
| [Build automation](docs/build-automation.md) | GitHub Actions, workflow permissions, signing secrets |
| [Releases](docs/releases.md) | SemVer 2.0, local build + publish walkthrough, GitHub Releases |
| [F-Droid / Droidify](docs/fdroid.md) | Same catalog as Burton Sonos, Fingerprint, one-command Pages publish |
| [Contributing](CONTRIBUTING.md) | Conventional Commits (required) |

## Quick start (debug)

```bash
./gradlew :app:installDebug
```

Debug builds use application id `com.burton.apphub.debug`. Release builds need a keystore; see [docs/releases.md](docs/releases.md).

```bash
./gradlew testDebugUnitTest
```
