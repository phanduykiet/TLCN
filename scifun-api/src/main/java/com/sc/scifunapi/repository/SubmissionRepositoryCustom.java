package com.sc.scifunapi.repository;

import com.sc.scifunapi.dto.submission.SubmissionStatProjection;

import java.util.List;

public interface SubmissionRepositoryCustom {
    List<SubmissionStatProjection> findStatProjectionsByUserId(String userId);
}