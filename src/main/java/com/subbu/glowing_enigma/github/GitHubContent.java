package com.subbu.glowing_enigma.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One entry of a directory listing from the GitHub Contents API.
 *
 * @param type {@code "file"} or {@code "dir"}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubContent(String name, String path, String sha, long size, String type) {

    public boolean isFile() {
        return "file".equals(type);
    }

    public boolean isDirectory() {
        return "dir".equals(type);
    }
}
