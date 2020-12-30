package de.derteufelqwe.bungeeplugin.redis;

/**
 * The types of messages that can be sent through redis
 */
public enum MessageType {
    PLAYER_JOIN,                    // A player joined the proxy
    PLAYER_LEAVE,                   // A player left the proxy
    PLAYER_SERVER_CHANGE,           // A player connects to a (new) minecraft server
    REQUEST_PLAYER_SERVER_CHANGE    // Requests a player to be sent to a different server
    ;
}
