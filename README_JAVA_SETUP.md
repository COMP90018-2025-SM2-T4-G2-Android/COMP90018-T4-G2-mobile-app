# Java Setup Instructions

## Installing Java for Android Development

Your project requires Java 11 or 17. Here's how to install it:

### Option 1: Using Homebrew (Recommended)

1. Open Terminal
2. Run: `brew install --cask temurin@17`
3. Enter your password when prompted
4. Verify: `java -version`

### Option 2: Manual Installation

1. Download JDK 17 from: https://adoptium.net/temurin/releases/?version=17
2. Download the macOS `.pkg` installer
3. Run the installer and follow the prompts
4. Verify: `java -version`

### Setting JAVA_HOME (if needed)

After installation, you may need to set JAVA_HOME. Add this to your `~/.zshrc`:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH=$JAVA_HOME/bin:$PATH
```

Then reload: `source ~/.zshrc`

### Verify Installation

Run:
```bash
java -version
which java
```

You should see Java 17 output.

### Build the Project

Once Java is installed, rebuild:
```bash
cd /Users/johnkim/.cursor/worktrees/COMP90018-T4-G2-mobile-app/xBCgr
./gradlew clean build
```

