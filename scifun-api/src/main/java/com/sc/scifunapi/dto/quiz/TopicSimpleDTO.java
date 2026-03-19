package com.sc.scifunapi.dto.quiz;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicSimpleDTO {
    private String _id;
    private String name;
    private String description;
    private String level;
    private String subject;  // Chỉ trả về subjectId giống Express
}
