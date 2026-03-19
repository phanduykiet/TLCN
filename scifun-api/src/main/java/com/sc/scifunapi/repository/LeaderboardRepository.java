package com.sc.scifunapi.repository;

import com.sc.scifunapi.entity.Leaderboard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface LeaderboardRepository extends MongoRepository<Leaderboard, String> {

    List<Leaderboard> findBySubjectIdAndPeriodOrderByRankAsc(String subjectId, String period);

    List<Leaderboard> findByUserIdAndSubjectId(String userId, String subjectId);

    Leaderboard findByUserIdAndSubjectIdAndPeriod(String userId, String subjectId, String period);

    void deleteBySubjectIdAndPeriod(String subjectId, String period);

    void deleteByUserId(String userId);

    void deleteBySubjectIdAndPeriodAndUserIdNotIn(String subjectId, String period, Collection<String> userIds);

    // Lấy leaderboard theo subject + period, có phân trang
    Page<Leaderboard> findBySubjectIdAndPeriod(String subjectId, String period, Pageable pageable);
}
