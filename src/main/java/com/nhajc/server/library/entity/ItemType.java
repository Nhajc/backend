package com.nhajc.server.library.entity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
public enum ItemType {
    SONG, RELEASE, PLAYLIST, ARTIST;
    public static ItemType fromPath(String path) {
        return switch(path) {
            case "songs" -> SONG; case "releases" -> RELEASE; case "playlists" -> PLAYLIST; case "artists" -> ARTIST;
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource type not found.");
        };
    }
}

