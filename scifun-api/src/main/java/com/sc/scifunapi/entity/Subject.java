package com.sc.scifunapi.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "subjects")
public class Subject {

    @Id
    private String id;

    private String name;

    @Builder.Default
    private String description = null;

    @Builder.Default
    private Integer maxTopics = 20;

    @Builder.Default
    private String image =
            "https://res.cloudinary.com/dglm2f7sr/image/upload/v1761400287/default_gdfbhs.png";
}
