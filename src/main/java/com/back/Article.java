package com.back;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@RequiredArgsConstructor
public class Article {
    private long id;
    private String title;
    private String body;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;

    public boolean isBlind() {
        return false;
    }
}
