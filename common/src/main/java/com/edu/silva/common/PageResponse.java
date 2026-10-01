package com.edu.silva.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private long total;
    private Integer page;
    private Integer size;
    private List<T> items;

    public PageResponse(Page<T> page) {
        this.total = page.getTotalElements();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.items = page.getContent();
    }
}