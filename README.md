# Phone Diagnostics — Hardware Testing Android App

A modern Android application built with Kotlin and Jetpack Compose to perform comprehensive hardware diagnostics (Screen, Battery, Speaker, Sensors, Haptics, Flashlight) and generate exportable device health reports.

---

## 🚀 GitHub Actions Workflows

This repository is configured with automated CI/CD workflows under `.github/workflows/`:

### 1. `android-ci.yml` (Continuous Integration)
- **Triggers:** Push or Pull Request on `main`, `master`, `develop`, or manual trigger (`workflow_dispatch`).
- **Tasks:**
  - Sets up JDK 21 (Temurin) with Gradle caching and optimized worker memory.
  - Automatically runs unit & Robolectric tests (`./gradlew testDebugUnitTest`).
  - Assembles the Debug APK (`./gradlew assembleDebug`).
  - Uploads test reports and the built `.apk` as downloadable GitHub Actions artifacts.

### 2. `android-release.yml` (Release & Distribution)
- **Triggers:** Pushing a release tag (e.g. `v1.0.0`) or manual trigger from the GitHub Actions tab.
- **Tasks:**
  - Builds the Release APK (`app-release.apk`).
  - Builds the Android App Bundle (`app-release.aab`) ready for Google Play Console upload.
  - Automatically creates a GitHub Release with the APK and AAB attached if triggered by a tag.

---

## 🔐 Configuring GitHub Secrets (Optional for Release Signing)

To sign your release builds with your own release keystore in GitHub Actions, navigate to:
**Repository Settings ➔ Secrets and variables ➔ Actions ➔ New repository secret**

| Secret Name | Description |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded release `.jks` or `.keystore` file (`base64 -w 0 my-key.jks`) |
| `STORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias name |
| `KEY_PASSWORD` | Key password |

*Note: If no custom release keystore secret is configured, the workflow automatically falls back to signing with the included repository keystore so builds succeed immediately.*

---

## 🛠️ Local Build Instructions

```bash
# Clone the repository
git clone <repo-url>
cd <repo-folder>

# Run unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Build release bundle
./gradlew bundleRelease
```

Output files will be located at:
- **Debug APK:** `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK:** `app/build/outputs/apk/release/app-release.apk`
- **App Bundle:** `app/build/outputs/bundle/release/app-release.aab`
