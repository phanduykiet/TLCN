package com.sc.scifunapi.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "videolessons") // Mongoose "VideoLesson" -> "videolessons"
public class VideoLesson {

    @Id
    private String id;

    private String title;

    private String url;

    // duration in seconds (có thể null)
    private Integer duration;

    // ref tới Topic giống Mongoose: topic: { type: ObjectId, ref: "Topic" }
    @DBRef
    private Topic topic;
}
