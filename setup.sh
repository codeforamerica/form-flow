set -e

install_jenv() {
    if ! grep -q jenv "$1"; then
      echo "export PATH=""$HOME"/.jenv/bin:"$PATH""" >> "$1"
      echo "eval "$(jenv init -)"" >> "$1"
      $("$2 $1")
    fi
}

echo '--- FormFlow Setup Script ---'

if ! brew --version; then
  curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh
else
  brew update
fi

echo 'Installing brew packages'
brew install --cask temurin@21
brew install jenv gradle postgresql@16

# Install jenv in either the .bashrc or zshrc, whichever is present
if [ -f ~/.bashrc ]; then
  install_jenv "$HOME/.bashrc" "sh"
elif [ -f ~/.zshrc ]; then
  install_jenv "$HOME/.zshrc" "zsh"
else
  echo 'No shell config file found, cant install jenv'
  exit 1
fi

# M1 Mac install stuff
if [[ $(uname -m) == 'arm64' ]]; then
  export PATH="$HOME/.jenv/bin:$PATH"
  export JENV_ROOT="/opt/homebrew/Cellar/jenv/"
  eval "$(/opt/homebrew/bin/brew shellenv)"
  eval "$(jenv init -)"
fi

# Check if jenv can find java 21
if ! jenv versions | grep -q 21; then
  jenv add /Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
fi

# If the postgres service isn't running in brew, start it
if ! brew services list | grep postgresql@16 | grep started; then
  brew services start postgresql@16
fi

# Create form-flow-test databases and users in postgres, if they don't exist
if ! psql -lqt | cut -d \| -f 1 | tr -d ' ' | grep -qx form-flow-test; then
  createdb form-flow-test
  createuser -s form-flow-test
fi

# Increase PostgreSQL max_connections to handle multiple test contexts
# This is needed for Flyway 11.17.0+ which may use more connections during migrations
echo 'Configuring PostgreSQL max_connections...'
PG_VERSION=16
PG_DATA_DIR=$(brew --prefix postgresql@${PG_VERSION})/var/postgresql@${PG_VERSION}
PG_CONF_FILE="${PG_DATA_DIR}/postgresql.conf"

# Try to find postgresql.conf in common locations
if [ -f "$PG_CONF_FILE" ]; then
  # Backup original config
  if ! grep -q "# max_connections increased for Flyway 11.17.0+" "$PG_CONF_FILE"; then
    # Check current max_connections value
    CURRENT_MAX=$(grep "^max_connections" "$PG_CONF_FILE" | head -1 | awk '{print $3}' | tr -d '#')
    if [ -z "$CURRENT_MAX" ] || [ "$CURRENT_MAX" -lt 200 ]; then
      echo "Setting max_connections to 200 in $PG_CONF_FILE"
      # Comment out existing max_connections line if present
      sed -i.bak 's/^max_connections/# max_connections (original)/' "$PG_CONF_FILE" 2>/dev/null || true
      # Add new max_connections setting
      echo "" >> "$PG_CONF_FILE"
      echo "# max_connections increased for Flyway 11.17.0+ test compatibility" >> "$PG_CONF_FILE"
      echo "max_connections = 200" >> "$PG_CONF_FILE"
      echo "PostgreSQL configuration updated. Restarting PostgreSQL service..."
      brew services restart postgresql@${PG_VERSION} || true
    else
      echo "max_connections is already set to $CURRENT_MAX (>= 200), skipping update"
    fi
  else
    echo "max_connections already configured for Flyway 11.17.0+"
  fi
else
  # Alternative: try using ALTER SYSTEM (requires superuser, works without restart in some cases)
  echo "Attempting to set max_connections via ALTER SYSTEM..."
  psql -U postgres -d postgres -c "ALTER SYSTEM SET max_connections = 200;" 2>/dev/null || \
  psql -U form-flow-test -d postgres -c "ALTER SYSTEM SET max_connections = 200;" 2>/dev/null || \
  echo "Warning: Could not automatically set max_connections. You may need to manually set it in postgresql.conf"
  echo "  To set manually: edit postgresql.conf and set max_connections = 200, then restart PostgreSQL"
fi

# Build the jar and run tests
./gradlew clean webJar jar test

echo '--- FormFlow Setup Script Complete ---'
