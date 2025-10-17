# Team Setup Guide

## 🎯 One-Time Setup for Team Members

When you first clone or pull these changes, run this command:

```bash
git config commit.template .gitmessage
```

This enables the commit message template for your local repository.

## 📝 What Was Added

### 1. CI/CD Pipeline (`.github/workflows/android-pr-check.yml`)
- ✅ Automatically builds and tests all PRs
- ✅ Runs on push to main/dev/develop branches
- ✅ Uploads APK artifacts
- ✅ Comments on PRs with build status

**Action Required**: 
- Add `GOOGLE_SERVICES_JSON` secret in GitHub repository settings (if not already done)
- See `.github/workflows/README.md` for instructions

### 2. Commit Message Template (`.gitmessage`)
- 📋 Standardized commit format
- 📋 Inline examples and guidelines
- 📋 Automatically opens when you run `git commit`

**Action Required**: 
- Run: `git config commit.template .gitmessage`
- This is a one-time setup per developer

### 3. Pull Request Template (`.github/pull_request_template.md`)
- 📋 Automatically appears when creating PRs
- 📋 Ensures all required information is provided
- 📋 Includes checklists for reviewers

**No action required** - works automatically!

### 4. Contributing Guidelines (`.github/CONTRIBUTING.md`)
- 📚 Complete guide for contributing to the project
- 📚 Code style guidelines
- 📚 Development workflow
- 📚 Branch strategy

**Action Required**: 
- Read before making your first contribution

### 5. Commit Convention Guide (`.github/COMMIT_CONVENTION.md`)
- 📖 Quick reference for commit message format
- 📖 Examples of good and bad commits
- 📖 Type definitions and usage

**Action Required**: 
- Bookmark for quick reference

## 🚀 Quick Start

After pulling these changes:

```bash
# 1. Configure commit template (one-time)
git config commit.template .gitmessage

# 2. Test it out - make a commit
git add .
git commit
# Your editor will open with the template!

# 3. Read the contributing guide
cat .github/CONTRIBUTING.md
# or open it in your IDE
```

## 📋 Daily Workflow

### Creating a Commit
```bash
git add <files>
git commit
# Template opens in your editor:
# feat: Your feature description
#
# Detailed explanation of what and why
#
# Closes #42
```

### Creating a Pull Request
1. Push your branch: `git push origin feature/your-feature`
2. Go to GitHub and click "New Pull Request"
3. The PR template will auto-populate
4. Fill in all sections
5. Submit for review

### Before Submitting PR
- [ ] Code builds: `./gradlew assembleDebug`
- [ ] Tests pass: `./gradlew test`
- [ ] Lint clean: `./gradlew lint`
- [ ] Commit messages follow convention
- [ ] PR template filled out

## 🎨 Commit Message Examples

### Feature
```
feat: Add card validation using Luhn algorithm

Implement comprehensive credit card number validation including:
- Luhn checksum verification
- Length validation (13-19 digits)
- Format checking

Closes #42
```

### Bug Fix
```
fix: Resolve crash on dialog dismiss

Fixed NullPointerException when dismissing AddCardDialogFragment
by adding null check for context before showing toast.

Fixes #58
```

### Refactoring
```
refactor: Extract validation logic to CardValidator class

Moved card validation from fragment to dedicated class for:
- Better testability
- Code reusability
- Separation of concerns

Related to #33
```

## 🔍 Reviewing PRs

When reviewing a PR:
1. Check CI status - must be ✅ green
2. Review the code for:
   - Code quality
   - Test coverage
   - Material 3 compliance
   - Performance impact
3. Run locally if needed
4. Approve or request changes

## ❓ Questions?

- **Commit template not working?** 
  - Make sure you ran: `git config commit.template .gitmessage`
  - Check it's set: `git config commit.template`

- **PR template not showing?** 
  - It only appears when creating PRs on GitHub, not locally
  - Make sure you're creating a PR through the GitHub UI

- **CI failing?**
  - Check the logs in the "Actions" tab
  - Common issues: missing dependencies, lint errors, test failures
  - See `.github/workflows/README.md` for troubleshooting

## 🎉 That's It!

You're all set! Start contributing with confidence knowing that:
- Your commits will be well-formatted
- Your PRs will have all necessary information  
- The CI will catch build issues automatically
- The team has consistent standards

Happy coding! 🚀

