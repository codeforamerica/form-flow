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
brew install --cask temurin@17
brew install jenv gradle postgresql@14

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

# Check if jenv can find java 17
if ! jenv versions | grep -q 17; then
  jenv add /Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
fi

# If the postgres service isn't running in brew, start it
if ! brew services list | grep postgresql@14 | grep started; then
  brew services start postgresql@14
fi

# Create form-flow-test databases and users in postgres, if they don't exist
if ! psql -lqt | cut -d \| -f 1 | tr -d ' ' | grep -qx form-flow-test; then
  createdb form-flow-test
  createuser -s form-flow-test
fi

# Build the jar and run tests
./gradlew clean webJar jar test

echo '--- FormFlow Setup Script Complete ---'
