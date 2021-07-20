#
#  Working directory must be the project root!
#

VERSION="1.0"

# Install the commons module
#mvn --projects Commons --also-make clean install

# Package the subprojects
#mvn --projects NodeWatcher,DockerPlugin,Server,BungeePlugin,MinecraftPlugin --also-make package

# Copy the jars / required files
mkdir "Tools/testing/fullimage/build"
cp "BungeePlugin/target/DMCPluginBC-$VERSION.jar" "Tools/testing/fullimage/build"
cp "DockerPlugin/target/DMCDockerPlugin-$VERSION.jar"  "Tools/testing/fullimage/build/DMCDockerPlugin.jar"
cp "MinecraftPlugin/target/DMCPluginMC-$VERSION.jar" "Tools/testing/fullimage/build"
cp "NodeWatcher/target/DMCNodeWatcher-$VERSION.jar" "Tools/testing/fullimage/build"
cp "Server/target/DMCServerManager-$VERSION.jar" "Tools/testing/fullimage/build/DMCServerManager.jar"

# Build the image
docker -H ubuntu1:2375 build -t "dockermctest-full" "Tools/testing/fullimage"

read -p "pause"

# Cleanup
rm -R "Tools/testing/fullimage/build"