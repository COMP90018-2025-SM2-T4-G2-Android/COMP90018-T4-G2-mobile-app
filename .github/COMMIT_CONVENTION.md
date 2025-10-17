# Commit Message Convention - Quick Reference

## Format
```
<type>: <subject>

<body>

<footer>
```

## Types

| Type | Description | Example |
|------|-------------|---------|
| `feat` | New feature | `feat: Add card scanning with ML Kit` |
| `fix` | Bug fix | `fix: Resolve payment dialog crash` |
| `docs` | Documentation only | `docs: Update README with setup instructions` |
| `style` | Code style (formatting, semicolons) | `style: Format payment fragment code` |
| `refactor` | Code refactoring | `refactor: Simplify card validation logic` |
| `perf` | Performance improvement | `perf: Optimize database queries` |
| `test` | Adding/updating tests | `test: Add unit tests for card validator` |
| `build` | Build system/dependencies | `build: Update Gradle to 8.2` |
| `ci` | CI configuration | `ci: Add PR build check workflow` |
| `chore` | Maintenance tasks | `chore: Update dependencies` |
| `revert` | Revert previous commit | `revert: Revert "Add card scanning"` |

## Rules

### ✅ DO
- Use imperative mood: "Add feature" not "Added feature"
- Capitalize first letter: "Add" not "add"
- Keep subject under 50 characters
- Separate subject from body with blank line
- Wrap body at 72 characters
- Explain *what* and *why*, not *how*
- Reference issues: `Closes #42`, `Fixes #58`, `Related to #33`

### ❌ DON'T
- End subject with period
- Use past tense: "Added" ❌
- Be vague: "Update files" ❌
- Write novels in subject line ❌

## Examples

### ✅ Good Examples

```
feat: Add payment card validation

Implement comprehensive validation for credit card inputs:
- Luhn algorithm for card number validation
- Expiry date format checking (MM/YY)
- CVV length validation (3-4 digits)

Closes #42
```

```
fix: Resolve crash when dismissing payment dialog

Fixed NullPointerException in AddCardDialogFragment by checking
for null context before showing toast message.

Fixes #58
```

```
refactor: Extract card validation to separate class

Move validation logic from AddCardDialogFragment to dedicated
CardValidator class for better testability and reusability.

Related to #33
```

```
docs: Add CI/CD setup instructions

Add documentation for GitHub Actions workflow including:
- How to configure google-services.json secret
- Build status badge setup
- Troubleshooting common issues
```

### ❌ Bad Examples

```
update files  ❌ (Too vague, no context)
```

```
fixed the bug  ❌ (Which bug? No details)
```

```
WIP  ❌ (Not descriptive, use draft PR instead)
```

```
Added new feature for payment processing and updated the UI to match the new design and also fixed some bugs.  ❌ (Too long, run-on sentence)
```

## Using the Template

The commit template is already configured in this repository. When you commit:

```bash
git add .
git commit
```

Your editor will open with the template. Simply fill it in:

```
feat: Add your feature description here

Explain what changes you made and why. Keep lines under 72 characters.

Closes #42
```

## Tips

1. **One logical change per commit**: Don't mix features, fixes, and refactors
2. **Commit often**: Small, focused commits are easier to review and revert
3. **Write for others**: Future you (or teammates) should understand the commit without seeing the code
4. **Reference issues**: Always link to related tickets or issues

## Questions?

See [CONTRIBUTING.md](CONTRIBUTING.md) for full contribution guidelines.

