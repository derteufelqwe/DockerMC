# General
- For docker swarm to work both machines must be linux. Windows just doesnt work

# Create the VM
- Create a Hyper-V VM with a linux distro of your choice
- In the Options set Secure boot to "UEFI"

# Create the Network
- If possible create an external network switch in the hyper-v manager
- Otherwise create an internal one and share your internet connection with that switch
- The Linux vms need fixed IPs, their standard gateway must be the IP of the switch on your host
- You must set a DNS server or the DNS wont work

# Share a folder between Host and Guest
- Create a folder on the Host system and enable network access
- On linux add the following to `/etc/fstab`
  ```
  //<path_on_host>   <path_on_guest>  cifs  uid=1000,gid=1000,rw,dir_mode=0777,file_mode=0777,nounix,credentials=/etc/cifspasswd  0  0
  ```
  Example:
  ```
  //192.168.137.1/Desktop/ServerManager   /home/arne/ServerManager  cifs  uid=1000,gid=1000,rw,dir_mode=0777,file_mode=0777,nounix,guest,user=arne  0  0
  ```
  The Folder `path_on_guest` must already exist
- Write your windows username and password to `/etc/cifspasswd` like this
  ```
  username=user
  password=password
  ```
  
- If you have no password on windows and don't care about security, you can do the following:
- Disable "Password protected sharing" (see source)
- Right-Click the Folder to share -> Go to "Freigabe" -> Click "Freigabe" -> In the empty drop-down menue select "Guest" and click okay
- In Linux replace the credentials=... part with "guest,user=<username>"
- Source: https://www.raspberrypi.org/forums/viewtopic.php?t=147990

# Enable insecure Docker API access:
Add ` -H tcp://0.0.0.0:2375` to `/lib/systemd/system/docker.service` `ExecStart`
Reload docker with `sudo systemctl daemon-reload` followed by `sudo systemctl restart docker`

# Create Minecraft Images
- Build a Minecraft docker image and make sure to push it to the local registry
