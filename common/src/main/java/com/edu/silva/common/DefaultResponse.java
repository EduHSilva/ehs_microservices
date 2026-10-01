package com.edu.silva.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
@Schema(description = "Standard envelope returned by every endpoint")
public class DefaultResponse<T> {

    @Schema(description = "Human readable message describing the outcome", example = "User successfully saved")
    private String message;

    @Schema(description = "Payload of the response")
    private T data;

    @Schema(description = "HATEOAS style links (self, edit, delete or pagination)")
    private Map<String, String> links;

    public DefaultResponse(String message) {
        this.message = message;
    }

    public DefaultResponse(String message, T data) {
        this.message = message;
        this.data = data;
    }

    public DefaultResponse(String message, T data, String baseUrl, UUID id) {
        this.message = message;
        this.data = data;
        this.links = buildEntityLinks(baseUrl, id);
    }

    public static <T> DefaultResponse<PageResponse<T>> paged(String message, Page<T> page, String baseUrl) {
        DefaultResponse<PageResponse<T>> response = new DefaultResponse<>(message, new PageResponse<>(page));
        response.setLinks(buildPaginationLinks(baseUrl, page));
        return response;
    }

    private static Map<String, String> buildEntityLinks(String baseUrl, UUID id) {
        Map<String, String> links = new HashMap<>();

        String url = String.format("%s/%s", baseUrl, id);

        links.put("self", url);
        links.put("edit", url);
        links.put("delete", url);

        return links;
    }

    private static Map<String, String> buildPaginationLinks(String baseUrl, Page<?> page) {
        Map<String, String> links = new HashMap<>();

        int pageNumber = page.getNumber();
        int size = page.getSize();

        links.put("self", String.format("%s?page=%d&size=%d", baseUrl, pageNumber, size));

        if (page.hasNext()) {
            links.put("next", String.format("%s?page=%d&size=%d", baseUrl, pageNumber + 1, size));
        }

        if (page.hasPrevious()) {
            links.put("prev", String.format("%s?page=%d&size=%d", baseUrl, pageNumber - 1, size));
        }

        return links;
    }
}