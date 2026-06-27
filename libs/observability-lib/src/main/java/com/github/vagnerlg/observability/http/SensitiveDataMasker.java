package com.github.vagnerlg.observability.http;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class SensitiveDataMasker {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "senha", "secret", "token", "accessToken", "refreshToken",
            "authorization", "credit_card", "cvv", "cpf", "ssn", "pin"
    );

    private static final Pattern PATTERN = buildPattern();

    private SensitiveDataMasker() {}

    static String mask(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }
        return PATTERN.matcher(body).replaceAll("$1: \"***\"");
    }

    private static Pattern buildPattern() {
        String alternatives = SENSITIVE_KEYS.stream()
                .map(k -> "\"" + k + "\"")
                .collect(Collectors.joining("|"));
        // Matches: "key": "value" (with optional whitespace around colon)
        return Pattern.compile("(" + alternatives + ")\\s*:\\s*\"[^\"]*\"", Pattern.CASE_INSENSITIVE);
    }
}
