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
- Certs generation
- Certs ohne bindmounts (für Swarmfunktionalität)
- Certs in Image kopieren
- ggf. Certs über Webservice verbreiten
- Commands zum Erneuern der Zertifikate
- Testen ob Service wirklich gestartet ist
- Player join order in der lobby
- RAM / CPU limitieren
- BungeeCord Plugin multithreaden

# Plan
- Docker-API certs für das Plugin
- Verteilung der Certs ohne Bind-Mount

 


