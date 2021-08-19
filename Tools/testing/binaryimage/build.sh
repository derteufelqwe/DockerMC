#
#  Working directory must be the project root!
#

source config.sh  # Import config values

VERSION="1.0"
dir1="Tools/testing/binaryimage/bungeeserver"
dir2="Tools/testing/binaryimage/minecraftserver"

###  Check if the server folders are present ###
if [ "$(ls -A $dir1)" ]; then
  echo "Found BungeeCord server."
  if [ ! -f "$dir1/waterfall.jar" ]; then
    >&2 echo "BungeeCord server is missing 'waterfall.jar' file."
#    exit 2
  fi
else
  >&2 echo "Folder 'bungeeserver' not found or empty. Create a runnable BungeeCord server in this folder."
#  exit 1
fi

if [ "$(ls -A $dir2)" ]; then
  echo "Found Minecraft server."
  if [ ! -f "$dir2/papermc.jar" ]; then
    >&2 echo "Minecraft server is missing 'papermc.jar' file."
#    exit 2
  fi
else
  >&2 echo "Folder 'minecraftserver' not found or empty. Create a runnable Minecraft server in this folder."
#  exit 1
fi



# Install the commons module
#mvn --projects Commons --also-make clean install

# Package the subprojects
#mvn --projects NodeWatcher,DockerPlugin,Server,BungeePlugin,MinecraftPlugin --also-make package

# Copy the jars / required files
mkdir "Tools/testing/binaryimage/build"
cp "BungeePlugin/target/DMCPluginBC-$VERSION.jar" "Tools/testing/binaryimage/build/DMCPluginBC.jar"
cp "MinecraftPlugin/target/DMCPluginMC-$VERSION.jar" "Tools/testing/binaryimage/build/DMCPluginMC.jar"
cp "DockerPlugin/target/DMCDockerPlugin-$VERSION.jar"  "Tools/testing/binaryimage/build/DMCDockerPlugin.jar"
cp "NodeWatcher/target/DMCNodeWatcher-$VERSION.jar" "Tools/testing/binaryimage/build"
cp "Server/target/DMCServerManager-$VERSION.jar" "Tools/testing/binaryimage/build/DMCServerManager.jar"

cp "Server/src/main/resources/dockerfiles/bungeecord.dfile" "Tools/testing/binaryimage/build"
cp "Server/src/main/resources/dockerfiles/minecraft.dfile" "Tools/testing/binaryimage/build"

# Build the image
docker "$DOCKER_ARGS" build -t "dockermctest-full" "Tools/testing/binaryimage"

read -p "pause"

# Cleanup
rm -R "Tools/testing/binaryimage/build"