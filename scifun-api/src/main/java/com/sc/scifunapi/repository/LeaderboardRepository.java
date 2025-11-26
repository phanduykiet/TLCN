package com.sc.scifunapi.repository;

import com.sc.scifunapi.entity.Leaderboard;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaderboardRepository extends MongoRepository<Leaderboard, String> {

    List<Leaderboard> findBySubjectIdAndPeriodOrderByRankAsc(String subjectId, String period);

    List<Leaderboard> findByUserId(String userId);

    Leaderboard findByUserIdAndSubjectIdAndPeriod(String userId, String subjectId, String period);
}
