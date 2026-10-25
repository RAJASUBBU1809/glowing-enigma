package com.subbu.glowing_enigma.github;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads files from the photo repo using the GitHub Contents API.
 */
@Component
public class GitHubClient {

    private static final MediaType RAW = MediaType.parseMediaType("application/vnd.github.raw");
    private static final MediaType JSON = MediaType.parseMediaType("application/vnd.github+json");
    private static final ParameterizedTypeReference
            <List<GitHubContent>> CONTENT_LIST = new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final GitHubProperties props;

    public GitHubClient(RestClient.Builder builder, GitHubProperties props) {
        this.props = props;
        this.restClient = builder
                .baseUrl(props.apiUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.token())
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    /**
     * Downloads the raw bytes of a file, e.g. {@code "meta/index.json"}. Works for files up to 100MB.
     *
     * @throws GitHubFileNotFoundException if the path does not exist on the configured branch
     */
    public byte[] fetchFile(String path) {
        return restClient.get()
                .uri(uri -> contentsUri(uri, path))
                .accept(RAW)
                .retrieve()
                .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (request, response) -> {
                    throw new GitHubFileNotFoundException(path);
                })
                .body(byte[].class);
    }

    /**
     * Lists the files and folders in a directory ({@code ""} for the repo root).
     * GitHub returns at most 1,000 entries per directory.
     *
     * @throws GitHubFileNotFoundException if the directory does not exist on the configured branch
     */
    public List<GitHubContent> listDirectory(String path) {
        return restClient.get()
                .uri(uri -> contentsUri(uri, path))
                .accept(JSON)
                .retrieve()
                .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (request, response) -> {
                    throw new GitHubFileNotFoundException(path);
                })
                .body(CONTENT_LIST);
    }

    /**
     * Builds {@code /repos/{owner}/{repo}/contents/{path}?ref={branch}}. Each path segment is passed as a
     * URI variable so that spaces and special characters in file names are encoded while the slashes are kept.
     */
    private URI contentsUri(UriBuilder uri, String path) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("owner", props.owner());
        variables.put("repo", props.repo());
        uri.path("/repos/{owner}/{repo}/contents");

        String[] segments = path.split("/");
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].isEmpty()) {
                continue;
            }
            uri.pathSegment("{s" + i + "}");
            variables.put("s" + i, segments[i]);
        }

        variables.put("ref", props.branch());
        return uri.queryParam("ref", "{ref}").build(variables);
    }
}
