local taskName = ARGV[1];
local serverName = ARGV[2];

local online = redis.call("SCARD", "minecraft#players#LobbyServer-1")

return online