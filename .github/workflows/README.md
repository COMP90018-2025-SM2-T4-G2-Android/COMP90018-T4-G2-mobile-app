# CI/CD Workflows

## Android PR Check

This workflow automatically runs on every pull request and push to `main`, `dev`, or `develop` branches.

### What it does:
- ✅ Builds the Android app (debug variant)
- ✅ Runs lint checks
- ✅ Runs unit tests
- ✅ Uploads APK artifacts (available for 7 days)
- ✅ Comments on PR with build status

### Setup Instructions:

#### 1. Configure GitHub Secrets (Optional but Recommended)

If your project uses Firebase or Google Services, you need to add your `google-services.json` as a secret:

1. Go to your repository on GitHub
2. Navigate to **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Name: `GOOGLE_SERVICES_JSON`
5. Value: Base64 encoded content of your `google-services.json` file

To encode your `google-services.json`:
```bash
base64 -i app/google-services.json | pbcopy  # macOS
base64 app/google-services.json | xclip      # Linux
```

#### 2. Add Status Badge to README (Optional)

Add this to your README.md:
```markdown
![Android Build](https://github.com/YOUR_USERNAME/COMP90018-T4-G2-mobile-app/workflows/Android%20PR%20Check/badge.svg)
```

### Workflow Triggers:
- **Pull Requests**: Automatically runs on all PRs targeting main/dev/develop
- **Push**: Runs on direct pushes to main/dev/develop branches

### Artifacts:
- **app-debug.apk**: Debug build of the app (retained for 7 days)
- **lint-results**: Lint check reports (retained for 7 days)

### Customization:

To modify the workflow:
- Edit `.github/workflows/android-pr-check.yml`
- You can change which branches trigger the workflow
- Add more test suites or checks
- Modify artifact retention period
- Add deployment steps for releases

### Troubleshooting:

**Build fails due to missing google-services.json:**
- Add the `GOOGLE_SERVICES_JSON` secret as described above
- Or commit a placeholder `google-services.json` for CI builds

**Gradle permission denied:**
- The workflow automatically grants execute permission to gradlew
- If issues persist, ensure gradlew is committed with executable permissions

**Out of memory errors:**
- Add `org.gradle.jvmargs=-Xmx2048m` to `gradle.properties`
- This is already configured in most Android projects

