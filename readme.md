# DockerMC ServerManager
DockerMC is a docker based minecraft server management system. It's currently under development.
More infos follow with its release.


# Important notes
1. If your service has a node container limit, and their replications are larger than the containers that fit the constraint
   (service with 3 instances, limited to 1 container per node, 2 nodes available -> 1 container can't be placed) a normal
   service update command doesn't work since docker tries to restart the one container it can't place and thus won't do anything.
   To solve this set ``bungeePoolParallelUpdates`` or ``lobbyPoolParallelUpdates`` to a value **larger** than the amount of
   containers that can't be placed. (2 or more for the example)
 


# Running the tests
1. Install sysbox (https://github.com/nestybox/sysbox/blob/master/docs/user-guide/install-package.md)
Uninstalling: https://github.com/nestybox/sysbox/blob/master/docs/developers-guide/build.md
   