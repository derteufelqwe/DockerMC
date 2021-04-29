echo "Building Docker Plugin"

if [ -d "./rootfs" ]; then
  echo "Filesystem already existing"

else
  echo "Creating filesystem"
  mkdir rootfs
  cp src/main/resources/config.json config.json

  echo "Building the driverimage..."
  docker build -t driverimage .

  echo "Exporting image filesystem..."
  id=$(docker create driverimage)
  docker export "$id" | tar -x -C rootfs
  docker rm -vf "$id"
fi


echo "Building and pushing plugin (1/2)..."
docker plugin create "derteufelqwe/dockermc-log-driver:latest" .
docker plugin push "derteufelqwe/dockermc-log-driver:latest"
docker plugin rm "docker plugin push derteufelqwe/dockermc-log-driver:latest"

echo "Building and pushing plugin (2/2)..."
docker plugin create "derteufelqwe/dockermc-log-driver:$VERSION" .
docker plugin push "derteufelqwe/dockermc-log-driver:$VERSION"

echo "Created docker plugin."