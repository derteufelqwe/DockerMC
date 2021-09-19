# Server preparation
- Setup as many Linux Servers as you want. They need to be able to communicate with eachother
- The servers need fixed IPs
- Openssh cli installed

# DNS Setup
- Add each host to each machine to `/etc/hosts` like this `<server_ip>  <hostname>`
- Add the DNS record `registry.swarm` pointing to the ip of the swarm manager

# Install the certificates
- Run the ServerManager once to generate the certificates
- Install them on the hosts

## Linux
Copy `ca.crt` to `/etc/docker/certs.d/<hostname:port>`.
If the port is 443 you can ignore the it in the file name.

## Windows
Copy `ca.crt` to your windows machine. Rightclick and install the certificate for the whole computer. Restart docker.
