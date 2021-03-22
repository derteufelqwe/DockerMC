# Container labels
All containers which belong to DockerMC have the tag `Owner=DockerMC`

## Basic container types
Basic container types have the key `Type` and the following values:
- REGISTRY
- DNS
- SYSTEM_CERTS_GEN
- BUNGEE
- MINECRAFT


# ToDo
- Verschiedene Maps für ein Image | Just use multiple services
- Server signs für Server
- Config system überarbeiten
- Configs validieren
- Commands
- Metadata and tags in consul
- Minecraft commands

- Spigot Plugin mit api, um einen Shutdown des Servers zu verarbeiten
- Testen ob Service wirklich gestartet ist
- Player join order in der lobby
- persistente Server (evtl an einen Host binden)
- Healthchecks überarbeiten (2. Check für server, evtl. kein curl, höherer startup delay)
- Support für Redis und mysql datenbank

- Webseite zur Verwaltung
- Telegram Bot
- Discord Bot



# Tags
## Everything
Owner = DockerMC
## 


# Important notes
1. If your service has a node container limit, and their replications are larger than the containers that fit the constraint
   (service with 3 instances, limited to 1 container per node, 2 nodes available -> 1 container can't be placed) a normal
   service update command doesn't work since docker tries to restart the one container it can't place and thus won't do anything.
   To solve this set ``bungeePoolParallelUpdates`` or ``lobbyPoolParallelUpdates`` to a value **larger** than the amount of
   containers that can't be placed. (2 or more for the example)
 


