package com.subbu.glowing_enigma.github;

public class GitHubFileNotFoundException extends RuntimeException {

    public GitHubFileNotFoundException(String path) {
        super("File not found in GitHub repo: " + path);
    }
}
