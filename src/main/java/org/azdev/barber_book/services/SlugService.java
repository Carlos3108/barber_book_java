package org.azdev.barber_book.services;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SlugService {
    public String generateSlug(String name){
        String normalized = StringUtils.stripAccents(name)
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", "")
                .replaceAll("\\s+", "-");
        String suffix = UUID.randomUUID().toString().substring(0, 4);

        return normalized + "-" + suffix;
    }
}
