echo "Make sure you have built the driverimage."

id=$(docker create driverimage)
docker export "$id" | tar -x -C rootfs
docker rm -vf "$id"
echo "Build plugin files."

docker plugin rm testplugin
docker plugin create testplugin .
echo "Created plugin testplugin"
