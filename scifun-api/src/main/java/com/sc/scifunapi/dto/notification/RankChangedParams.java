package com.sc.scifunapi.dto.notification;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RankChangedParams {
    private String userId;
    private String subjectId;
    private String subjectName;
    private String period;
    private int oldRank;
    private int newRank;

    @Builder.Default
    private boolean persist = true;

    @Builder.Default
    private boolean email = true;
}
