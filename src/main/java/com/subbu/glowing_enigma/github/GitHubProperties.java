package com.subbu.glowing_enigma.github;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "github")
public record GitHubProperties(
        @NotBlank String token,
        @NotBlank String owner,
        @NotBlank String repo,
        @DefaultValue("main") String branch,
        @DefaultValue("https://api.github.com") String apiUrl
) {
}
