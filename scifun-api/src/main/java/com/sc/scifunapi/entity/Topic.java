package com.sc.scifunapi.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "topics")
public class Topic {

    @Id
    private String id;

    private String name;

    private String description;

    // Liên kết với Subject
    @DBRef(lazy = true)
    private Subject subject;
}
