# Contributing to CashPal

Thank you for considering contributing to CashPal! This document provides guidelines and instructions for contributing.

## Table of Contents
- [Getting Started](#getting-started)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Pull Request Process](#pull-request-process)
- [Code Style](#code-style)
- [Development Workflow](#development-workflow)

## Getting Started

### Prerequisites
- Android Studio (latest stable version)
- JDK 17
- Git
- Android SDK with minimum SDK 24 and target SDK 34

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/YOUR_ORG/COMP90018-T4-G2-mobile-app.git
   cd COMP90018-T4-G2-mobile-app
   ```

2. **Configure commit message template**
   ```bash
   git config commit.template .gitmessage
   ```
   
   This will use the project's commit message template every time you commit.

3. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned repository
   - Wait for Gradle sync to complete

4. **Build the project**
   ```bash
   ./gradlew build
   ```

## Commit Message Guidelines

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification for commit messages.

### Format
```
<type>: <subject>

<body>

<footer>
```

### Types
- **feat**: A new feature
- **fix**: A bug fix
- **docs**: Documentation changes
- **style**: Code style changes (formatting, missing semicolons, etc.)
- **refactor**: Code refactoring without changing functionality
- **perf**: Performance improvements
- **test**: Adding or updating tests
- **build**: Changes to build system or dependencies
- **ci**: Changes to CI configuration
- **chore**: Maintenance tasks

### Examples

**Good commit messages:**
```
feat: Add card scanning functionality

Implement ML Kit integration for credit card scanning using the device camera.
Users can now scan their cards instead of manually entering details.

Closes #42
```

```
fix: Resolve crash on payment dialog dismiss

Fixed NullPointerException when dismissing AddCardDialogFragment after
successful card addition by checking for null context before showing toast.

Fixes #58
```

**Bad commit messages:**
```
update files
fixed bug
wip
```

### Rules
1. Use the imperative mood ("Add feature" not "Added feature")
2. Capitalize the first letter
3. No period at the end of the subject line
4. Keep subject line under 50 characters
5. Wrap body at 72 characters
6. Separate subject from body with a blank line
7. Use the body to explain *what* and *why*, not *how*

## Pull Request Process

1. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/your-bug-fix
   ```

2. **Make your changes**
   - Write clean, readable code
   - Follow the project's code style
   - Add comments for complex logic
   - Update documentation as needed

3. **Test your changes**
   - Run unit tests: `./gradlew test`
   - Run lint checks: `./gradlew lint`
   - Build the app: `./gradlew assembleDebug`
   - Test manually on device/emulator

4. **Commit your changes**
   ```bash
   git add .
   git commit
   # The commit template will open in your editor
   ```

5. **Push to your fork**
   ```bash
   git push origin feature/your-feature-name
   ```

6. **Create a Pull Request**
   - Go to the repository on GitHub
   - Click "New Pull Request"
   - Fill out the PR template completely
   - Request review from team members

7. **Address review feedback**
   - Make requested changes
   - Push additional commits
   - Re-request review when ready

8. **Merge**
   - Once approved, a maintainer will merge your PR
   - The PR branch will be deleted automatically

## Code Style

### Kotlin Style Guide
We follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) with these additions:

- Use 4 spaces for indentation
- Maximum line length: 120 characters
- Use meaningful variable names
- Prefer `val` over `var` when possible
- Use named parameters for functions with multiple parameters
- Add KDoc comments for public APIs

### XML Style Guide
- Use 4 spaces for indentation
- Group related attributes together
- Order attributes: id, layout, then alphabetically
- Use `@dimen`, `@color`, `@string` resources instead of hardcoded values
- Follow Material 3 design guidelines

### Example
```kotlin
/**
 * Validates credit card number using Luhn algorithm.
 * 
 * @param cardNumber The credit card number to validate
 * @return true if valid, false otherwise
 */
fun validateCardNumber(cardNumber: String): Boolean {
    val digits = cardNumber.filter { it.isDigit() }
    if (digits.length < 13 || digits.length > 19) {
        return false
    }
    
    return performLuhnCheck(digits)
}
```

## Development Workflow

### Branch Strategy
- `main`: Production-ready code
- `dev`: Development branch for integration
- `feature/*`: New features
- `fix/*`: Bug fixes
- `refactor/*`: Code refactoring
- `docs/*`: Documentation updates

### Before Submitting
- [ ] Code builds successfully
- [ ] All tests pass
- [ ] Lint checks pass
- [ ] Manual testing completed
- [ ] Documentation updated
- [ ] Commit messages follow guidelines
- [ ] PR template filled out completely

### CI/CD
All PRs automatically run:
- Build verification
- Lint checks
- Unit tests
- APK generation

PRs cannot be merged if CI checks fail.

## Questions?

If you have questions or need help:
- Open an issue with the "question" label
- Ask in the project's discussion forum
- Contact the maintainers

## Code of Conduct

- Be respectful and inclusive
- Provide constructive feedback
- Focus on the code, not the person
- Help others learn and grow

---

Thank you for contributing to CashPal! 🎉

