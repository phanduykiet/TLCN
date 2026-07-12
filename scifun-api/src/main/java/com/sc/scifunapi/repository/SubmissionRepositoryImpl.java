package com.sc.scifunapi.repository;

import com.sc.scifunapi.dto.submission.SubmissionStatProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;

@RequiredArgsConstructor
public class SubmissionRepositoryImpl implements SubmissionRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<SubmissionStatProjection> findStatProjectionsByUserId(String userId) {
        MatchOperation match = Aggregation.match(Criteria.where("userId").is(userId));

        ProjectionOperation project = Aggregation.project()
                .and("_id").as("id")
                .and("quiz.$id").as("quizId")   // lấy ObjectId trong DBRef, KHÔNG resolve Quiz thật
                .and("score").as("score")
                .and("createdAt").as("createdAt");

        Aggregation aggregation = Aggregation.newAggregation(match, project);

        return mongoTemplate.aggregate(aggregation, "submissions", SubmissionStatProjection.class)
                .getMappedResults();
    }
}