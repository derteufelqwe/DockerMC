echo "Building the driverimage..."
docker build -t driverimage .

echo "Exporting image filesystem..."
id=$(docker create driverimage)
docker export "$id" | tar -x -C rootfs
docker rm -vf "$id"

echo "Building plugin..."
docker plugin rm testplugin
docker plugin create testplugin .

echo "Created plugin testplugin"
