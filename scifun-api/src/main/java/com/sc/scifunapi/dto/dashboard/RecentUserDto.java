package com.sc.scifunapi.dto.dashboard;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecentUserDto {

    private String id;
    private String fullname;
    private String role;
}