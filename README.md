# Cable Meter - Next Gen Taxi Meter

Android taxi meter application for Hong Kong taxi operators with hardware integration.

## 📦 Release Process

### Quick Release

Use the automated release script:

```bash
./release.sh
```

### What the Script Does

1. Shows current version (from `app/gradle.properties`)
2. Prompts for new version name and code
3. Updates `app/gradle.properties`
4. Commits changes with message: `release: vX.X.X (BUILD_CODE)`
5. Creates git tag: `vX.X.X`
6. Optionally pushes to remote
7. Triggers GitHub Actions build automatically

### Example

```bash
$ ./release.sh

Current Version:
  Version Name: 8.2.4
  Version Code: 2025

Enter new version name (e.g., 8.2.5):
Version Name [8.2.4]: 8.2.5

Enter new version code:
Version Code [2026]: 2026

Summary:
  Version Name: 8.2.4 → 8.2.5
  Version Code: 2025 → 2026
  Branch: develop

Proceed with release? (y/N): y

🚀 Starting release process...
[1/5] Updating app/gradle.properties...
[2/5] Staging changes...
[3/5] Committing...
[4/5] Creating tag: v8.2.5
[5/5] Push to remote? (y/N): y

✓ Pushed to remote

🎉 Release 8.2.5 created successfully!
GitHub Actions will now build the release!
```

---

## 🔄 What Happens After Release

When you push a tag (e.g., `v8.2.5`), **GitHub Actions automatically**:

1. **Builds 4 APK variants:**
   - `cm-dev-Debug.apk`
   - `cm-dev2-Debug.apk` (Development environment)
   - `cm-qa-Debug.apk` (QA/Testing)
   - `cm-prd-Debug.apk` (Production)

2. **Uploads to OneDrive:**
   - Path: `/apps_archive/app-cable-meter/YYYY-MM-DD/`
   - Latest: `/apps_archive/app-cable-meter/latest/`

3. **Sends Teams Notification:**
   - Pipeline ID and version
   - Last 7 commits
   - Download links for all APKs

---

## 🎯 GitHub Actions Triggers

| Trigger | When | Builds |
|---------|------|--------|
| **Tag Push** | `git push origin v*` | ✅ All 4 variants |
| **Pull Request** | PR to `main`/`develop` | ✅ All 4 variants |
| **Manual** | Actions → "Run workflow" | ✅ All 4 variants |

---

## 📝 Version Naming

### Version Name
```
MAJOR.MINOR.PATCH
```
- **Patch** (8.2.4 → 8.2.5): Bug fixes
- **Minor** (8.2.5 → 8.3.0): New features
- **Major** (8.3.0 → 9.0.0): Breaking changes

### Version Code
- Sequential integer
- Must always increase
- Never reuse old codes

---

## 🛠️ Manual Release (Without Script)

If needed, you can release manually:

```bash
# 1. Edit version
vim app/gradle.properties
# Change VERSION_NAME and VERSION_CODE

# 2. Commit
git add app/gradle.properties
git commit -m "release: v8.2.5 (2026)"

# 3. Tag
git tag -a v8.2.5 -m "Release 8.2.5 (build 2026)"

# 4. Push
git push origin develop
git push origin v8.2.5
```

---

## 🏗️ Development

### Prerequisites
- JDK 17
- Android SDK (API 34)
- Android Studio

### Build
```bash
# Debug build (dev2 environment)
./gradlew assembleDev2Debug

# All variants
./gradlew assembleDebug

# Clean build
./gradlew clean assembleDebug
```

### Install
```bash
# Install to connected device
./gradlew installDev2Debug

# Or use adb
adb install -r app/build/outputs/apk/dev2/debug/cm-dev2-Debug.apk
```

---

## 📱 Project Structure

```
app-cable-meter/
├── app/                        # Main application
│   ├── gradle.properties      # VERSION_NAME & VERSION_CODE
│   └── src/
├── NxGnFirebaseModule/        # Firebase integration
├── measure-board-module/      # Hardware communication
├── release.sh                 # Release automation script
└── .github/workflows/         # CI/CD configuration
```

---

## 🔍 Checking Version

### In gradle.properties
```bash
grep VERSION app/gradle.properties
```

### In APK
```bash
aapt dump badging app.apk | grep version
```

### In Git
```bash
git tag -l
git describe --tags
```

---

## ⚙️ Environment Variants

The app has 4 build flavors:

- **dev**: Legacy environment
- **dev2**: **Development environment** (active)
- **qa**: QA/Testing environment
- **prd**: Production environment

Each variant can be built in Debug or Release mode.

---

## 📚 Documentation

- **App Documentation**: See [APP_DOCUMENTATION.md](APP_DOCUMENTATION.md) for detailed technical documentation
- **Architecture**: MVVM with Repository Pattern
- **Language**: Kotlin
- **UI**: Jetpack Compose

---

## 🔐 Signing

The app uses a keystore for signing. Keystore properties are stored in:
- Local: `keystore.properties`
- CI/CD: GitHub Secrets

---

## 📞 Support

For issues or questions:
- Check GitHub Actions logs for build failures
- Review APP_DOCUMENTATION.md for technical details
- Contact the development team

---

**Current Version:** 8.2.4 (Build 2025)
**Last Updated:** November 2025
