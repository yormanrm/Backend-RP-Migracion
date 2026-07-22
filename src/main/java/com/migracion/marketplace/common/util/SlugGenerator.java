package com.migracion.marketplace.common.util;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class SlugGenerator {

    private static final String SUFFIX_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SUFFIX_LENGTH = 4;
    private final SecureRandom random = new SecureRandom();

    public String toSlug(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
    }

    public String randomSuffix() {
        StringBuilder sb = new StringBuilder(SUFFIX_LENGTH);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            sb.append(SUFFIX_CHARS.charAt(random.nextInt(SUFFIX_CHARS.length())));
        }
        return sb.toString();
    }
}
