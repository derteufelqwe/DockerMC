#
#  Working directory must be the project root!
#

error_trap() {
  echo "Error raised on line $1. Examine log output for more information"
  read -p "pause"
  rm -R "$THISFOLDER/build"
  exit
}
trap 'error_trap $LINENO' ERR


# Change the working directory to the project root
cd "$(dirname $0)/../../.."

THISFOLDER="Tools/testing/binaryimage"

# shellcheck disable=SC1090
source "$THISFOLDER/config.sh"  # Import config values

VERSION="1.0"
dir1="$THISFOLDER//bungeeserver"
dir2="$THISFOLDER//minecraftserver"


###  Check if the server folders are present ###
if [ "$(ls -A $dir1)" ]; then
  echo "Found BungeeCord server."
  if [ ! -f "$dir1/waterfall.jar" ]; then
    >&2 echo "BungeeCord server is missing 'waterfall.jar' file."
    exit 2
  fi
else
  >&2 echo "Folder 'bungeeserver' not found or empty. Create a runnable BungeeCord server in this folder."
  exit 1
fi

if [ "$(ls -A $dir2)" ]; then
  echo "Found Minecraft server."
  if [ ! -f "$dir2/papermc.jar" ]; then
    >&2 echo "Minecraft server is missing 'papermc.jar' file."
    exit 2
  fi
else
  >&2 echo "Folder 'minecraftserver' not found or empty. Create a runnable Minecraft server in this folder."
  exit 1
fi



# Install the commons module
mvn --projects Commons --also-make clean install

# Package the subprojects
#mvn --projects NodeWatcher,DockerPlugin,Server,BungeePlugin,MinecraftPlugin --also-make package
mvn --projects Server --also-make package

# Copy the jars / required files
mkdir "Tools/testing/binaryimage/build"
cp "BungeePlugin/target/DMCPluginBC-$VERSION.jar"     "$THISFOLDER/build/DMCPluginBC.jar"
cp "MinecraftPlugin/target/DMCPluginMC-$VERSION.jar"  "$THISFOLDER/build/DMCPluginMC.jar"
cp "DockerPlugin/target/DMCDockerPlugin-$VERSION.jar" "$THISFOLDER/build/DMCDockerPlugin.jar"
cp "NodeWatcher/target/DMCNodeWatcher-$VERSION.jar"   "$THISFOLDER/build"
cp "Server/target/DMCServerManager-$VERSION.jar"      "$THISFOLDER/build/DMCServerManager.jar"

cp "Server/src/main/resources/dockerfiles/bungeecord.dfile" "$THISFOLDER/build"
cp "Server/src/main/resources/dockerfiles/minecraft.dfile"  "$THISFOLDER/build"

# Build the image
docker "$DOCKER_ARGS" build -t "dockermctest-full" "$THISFOLDER/"

# Cleanup
rm -R "$THISFOLDER/build"
