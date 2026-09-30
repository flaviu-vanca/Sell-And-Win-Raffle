#!/usr/bin/env bash
# Builds a self-contained package of Sell & Win Raffle with jpackage: the app, the JavaFX modules and a trimmed Java
# runtime in one folder or installer, so the person who runs it does not need Java installed.
#
#   packaging/package.sh [app-image|exe|msi|deb|rpm|dmg|pkg]...     (default: app-image)
#
# Results go to target/dist. Installer types only work on their own operating system (exe/msi need the WiX Toolset 3
# on Windows). The version comes from $APP_VERSION, else from pom.xml; it must be numeric (1, 1.2 or 1.2.3).
# Needs a full JDK 21 (jpackage) and Maven on the PATH.
set -euo pipefail

cd "$(dirname "$0")/.."

NAME="Sell and Win Raffle"   # no "&": it breaks the installer file names
MODULE="raffle.sellandwinraffle"
MAIN_CLASS="raffle.main.EntryPoint"

types=("$@")
[ ${#types[@]} -gt 0 ] || types=(app-image)

version="${APP_VERSION:-$(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' pom.xml | head -1)}"
version="${version#v}"
if ! [[ "$version" =~ ^[0-9]+(\.[0-9]+){0,2}$ ]]; then
   echo "Version '$version' is not numeric (1, 1.2 or 1.2.3): set APP_VERSION" >&2
   exit 1
fi

# The icon format depends on the operating system
case "$(uname -s)" in
   MINGW* | MSYS* | CYGWIN*) windows=1; icon="packaging/icon.ico" ;;
   Darwin)                   windows=0; icon="" ;;
   *)                        windows=0; icon="src/main/resources/icons/app-icon.png" ;;
esac

echo "== Building the application jar and collecting the runtime modules"
mvn -B -q clean package -DskipTests
rm -rf target/modules target/dist
mvn -B -q dependency:copy-dependencies -DoutputDirectory=target/modules -DincludeScope=runtime
cp target/Sell-And-Win-Raffle-*.jar target/modules/

# Every JavaFX library comes twice: a platform jar (javafx-base-21-win.jar) and an empty one without the platform
# name. Both on the module path would be the same module twice, so keep only the platform ones.
for jar in target/modules/javafx-*.jar; do
   if [[ "$(basename "$jar")" =~ ^javafx-[a-z]+-[0-9][0-9.]*\.jar$ ]]; then
      rm "$jar"
   fi
done

for type in "${types[@]}"; do
   echo "== jpackage --type $type ($version)"
   args=(--type "$type" --name "$NAME" --app-version "$version"
         --vendor "Flaviu Vanca" --description "Raffle management and live draw"
         --module-path target/modules --module "$MODULE/$MAIN_CLASS"
         --dest target/dist)
   [ -z "$icon" ] || args+=(--icon "$icon")
   if [ "$windows" = 1 ] && [ "$type" != "app-image" ]; then
      # per-user install: no administrator rights needed; the fixed upgrade id makes a new version replace the old one
      args+=(--win-per-user-install --win-menu --win-shortcut --win-dir-chooser
             --win-upgrade-uuid "8d2f0c5e-6f43-4f0e-9b59-2b1a7c6d4e01")
   fi
   jpackage "${args[@]}"
done

echo "== Done"
ls -la target/dist
