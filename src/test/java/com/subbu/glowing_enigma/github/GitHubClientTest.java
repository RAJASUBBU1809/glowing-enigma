package com.subbu.glowing_enigma.github;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(GitHubClient.class)
@EnableConfigurationProperties(GitHubProperties.class)
@TestPropertySource(properties = {
        "github.token=test-token",
        "github.owner=me",
        "github.repo=photos"
})
class GitHubClientTest {

    @Autowired
    private GitHubClient client;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void fetchFileRequestsRawContentWithToken() {
        server.expect(requestTo("https://api.github.com/repos/me/photos/contents/originals/2024/IMG%201.jpg?ref=main"))
                .andExpect(method(GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/vnd.github.raw"))
                .andRespond(withSuccess(new byte[]{1, 2, 3}, MediaType.APPLICATION_OCTET_STREAM));

        assertThat(client.fetchFile("originals/2024/IMG 1.jpg")).containsExactly(1, 2, 3);
        server.verify();
    }

    @Test
    void fetchFileThrowsWhenMissing() {
        server.expect(requestTo("https://api.github.com/repos/me/photos/contents/missing.jpg?ref=main"))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.fetchFile("missing.jpg"))
                .isInstanceOf(GitHubFileNotFoundException.class)
                .hasMessageContaining("missing.jpg");
    }

    @Test
    void listDirectoryParsesEntries() {
        String json = """
                [
                  {"name": "index.json", "path": "meta/index.json", "sha": "abc", "size": 120, "type": "file", "url": "ignored"},
                  {"name": "old", "path": "meta/old", "sha": "def", "size": 0, "type": "dir"}
                ]
                """;
        server.expect(requestTo("https://api.github.com/repos/me/photos/contents/meta?ref=main"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/vnd.github+json"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<GitHubContent> entries = client.listDirectory("meta");

        assertThat(entries).extracting(GitHubContent::name).containsExactly("index.json", "old");
        assertThat(entries.get(0).isFile()).isTrue();
        assertThat(entries.get(1).isDirectory()).isTrue();
    }

    @Test
    void listDirectoryOfRootUsesContentsEndpoint() {
        server.expect(requestTo("https://api.github.com/repos/me/photos/contents?ref=main"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.listDirectory("")).isEmpty();
    }
}
