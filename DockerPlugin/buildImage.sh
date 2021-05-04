set -e  # Fail on error

echo "Building Docker Plugin"

BASEIMAGE="openjdk:8-jre-alpine"
IMAGE="derteufelqwe/dockermc-drivers"

if [ -d "./rootfs" ]; then
  echo "Filesystem already existing"

else
  echo "Creating filesystem"
  mkdir rootfs
  cp src/main/resources/config.json config.json

  echo "Exporting image filesystem..."
  id=$(docker create "$BASEIMAGE")
  docker export "$id" | tar -x -C rootfs
  docker rm -vf "$id"
  mkdir "rootfs/plugin"
fi

cp "target/DMCDockerPlugin-$VERSION.jar" "rootfs/plugin/DMCDockerPlugin.jar"

echo "Building and pushing plugin (1/2)..."
docker plugin create "$IMAGE:latest" .
docker plugin push "$IMAGE:latest"
docker plugin rm "$IMAGE:latest"

echo "Building and pushing plugin (2/2)..."
docker plugin create "$IMAGE:$VERSION" .
docker plugin push "$IMAGE:$VERSION"

echo "Created docker plugin."
