package com.sc.scifunapi.service;

import com.sc.scifunapi.entity.Leaderboard;
import com.sc.scifunapi.entity.Subject;
import com.sc.scifunapi.entity.User;
import com.sc.scifunapi.entity.UserProgress;
import com.sc.scifunapi.repository.LeaderboardRepository;
import com.sc.scifunapi.repository.SubjectRepository;
import com.sc.scifunapi.repository.UserProgressRepository;
import com.sc.scifunapi.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final LeaderboardRepository leaderboardRepository;
    private final UserProgressRepository userProgressRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;

    /**
     * Làm mới bảng xếp hạng cho một môn học (alltime / daily / weekly / monthly)
     * Logic tương đương rebuildSubjectLeaderboardSv bên Express.
     */
    @Transactional
    public Map<String, Object> rebuildSubjectLeaderboard(String subjectId, String period) {

        if (subjectId == null || subjectId.isBlank()) {
            throw new RuntimeException("subjectId is required");
        }
        if (period == null || period.isBlank()) {
            period = "alltime";
        }

        // Lấy subject (để lấy tên môn học)
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Môn học không tồn tại"));

        // Lấy rank cũ để gán previousRank
        List<Leaderboard> oldRows =
                leaderboardRepository.findBySubjectIdAndPeriodOrderByRankAsc(subjectId, period);

        Map<String, Integer> prevRank = new HashMap<>();
        for (Leaderboard r : oldRows) {
            if (r.getUserId() != null) {
                prevRank.put(r.getUserId(), r.getRank());
            }
        }

        // Lấy toàn bộ UserProgress của subject này
        List<UserProgress> progresses = userProgressRepository.findBySubjectId(subjectId);

        if (progresses.isEmpty()) {
            // Không còn ai có progress -> xoá hết leaderboard của subject+period
            leaderboardRepository.deleteBySubjectIdAndPeriod(subjectId, period);
            return Map.of(
                    "subjectId", subjectId,
                    "period", period,
                    "updated", 0,
                    "notified", 0
            );
        }

        // Lấy thông tin user (fullname, avatar) để fill leaderboard
        Set<String> userIds = progresses.stream()
                .map(UserProgress::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userRepository.findAllById(userIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
        }

        final double MULTIPLIER = 1e12;

        // Tạm thời gói dữ liệu cần để sort
        class RowTmp {
            UserProgress progress;
            double totalScore;
            double sortKey;
        }

        List<RowTmp> rows = new ArrayList<>();

        for (UserProgress p : progresses) {
            double progressVal = p.getProgress();
            double avgScoreVal = p.getAverageScore();
            double totalScore = progressVal + avgScoreVal;

            // createdAt trong UserProgress: nếu bạn có field createdAt thì dùng, không thì fallback lastUpdatedAt
            Date ts = null;
            try {
                // nếu entity có getCreatedAt()
                ts = (Date) p.getClass().getMethod("getCreatedAt").invoke(p);
            } catch (Exception ignored) {
            }
            if (ts == null) {
                ts = p.getLastUpdatedAt();
            }
            long createdMs = (ts != null) ? ts.getTime() : 0L;

            double sortKey = totalScore * MULTIPLIER - createdMs;

            RowTmp r = new RowTmp();
            r.progress = p;
            r.totalScore = totalScore;
            r.sortKey = sortKey;

            rows.add(r);
        }

        // Sort: sortKey desc (tương đương totalScore desc, cùng điểm thì createdAt asc)
        rows.sort((a, b) -> Double.compare(b.sortKey, a.sortKey));

        // Tính rank giống $rank: cùng sortKey -> cùng rank
        List<Leaderboard> snapshots = new ArrayList<>();
        double lastSortKey = Double.NaN;
        int lastRank = 0;
        int index = 0;

        for (RowTmp r : rows) {
            index++;
            double sk = r.sortKey;

            int rank;
            if (Double.isNaN(lastSortKey) || Double.compare(sk, lastSortKey) != 0) {
                rank = index;
                lastRank = rank;
                lastSortKey = sk;
            } else {
                // cùng sortKey => cùng rank
                rank = lastRank;
            }

            UserProgress up = r.progress;
            String userId = up.getUserId();
            if (userId == null) continue;

            User user = userMap.get(userId);

            // Tìm row cũ để update, nếu không có thì tạo mới
            Leaderboard lb = leaderboardRepository
                    .findByUserIdAndSubjectIdAndPeriod(userId, subjectId, period);
            if (lb == null) {
                lb = new Leaderboard();
                lb.setUserId(userId);
                lb.setSubjectId(subjectId);
                lb.setPeriod(period);
            }

            lb.setUserName(user != null ? user.getFullname() : "Người dùng");
            lb.setUserAvatar(user != null ? user.getAvatar() : null);
            lb.setSubjectName(subject.getName());

            lb.setProgress(up.getProgress());
            lb.setAverageScore(up.getAverageScore());
            lb.setTotalScore(r.totalScore);
            lb.setCompletedQuizzes(up.getCompletedQuizzes()); // int
            lb.setCompletedTopics(up.getCompletedTopics());   // int
            lb.setRank(rank);
            lb.setPreviousRank(prevRank.getOrDefault(userId, null));
            lb.setProgressCreatedAt(up.getLastUpdatedAt() != null ? up.getLastUpdatedAt() : new Date());
            lb.setUpdatedAt(new Date());

            snapshots.add(lb);
        }

        // Xoá những leaderboard rows không còn trong snapshot
        Set<String> keepUserIds = snapshots.stream()
                .map(Leaderboard::getUserId)
                .collect(Collectors.toSet());

        if (!keepUserIds.isEmpty()) {
            leaderboardRepository.deleteBySubjectIdAndPeriodAndUserIdNotIn(subjectId, period, keepUserIds);
        } else {
            leaderboardRepository.deleteBySubjectIdAndPeriod(subjectId, period);
        }

        // Lưu lại snapshot mới
        leaderboardRepository.saveAll(snapshots);

        // Phần notifyRankChanged bên Node mình để notified = 0 (nếu sau này bạn có NotificationService thì thêm vào đây)
        int notified = 0;

        return Map.of(
                "subjectId", subjectId,
                "period", period,
                "updated", snapshots.size(),
                "notified", notified
        );
    }

    // Lấy bảng xếp hạng cho một môn học
    public Map<String, Object> getSubjectLeaderboardSv(
            String subjectId,
            Integer page,
            Integer limit,
            String period
    ) {
        if (subjectId == null || subjectId.isBlank()) {
            throw new RuntimeException("subjectId is required");
        }

        int pageNumber = (page == null || page < 1) ? 1 : page;
        int pageSize   = (limit == null || limit < 1) ? 20 : limit;
        String periodVal = (period == null || period.isBlank()) ? "alltime" : period;

        Pageable pageable = PageRequest.of(
                pageNumber - 1,
                pageSize,
                Sort.by(Sort.Direction.ASC, "rank")
        );

        Page<Leaderboard> lbPage =
                leaderboardRepository.findBySubjectIdAndPeriod(subjectId, periodVal, pageable);

        long total = lbPage.getTotalElements();

        List<Map<String, Object>> data = lbPage.getContent().stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("rank", r.getRank());
            m.put("previousRank", r.getPreviousRank());
            m.put("userId", r.getUserId());
            m.put("userName", r.getUserName());
            m.put("userAvatar", r.getUserAvatar());
            m.put("subjectId", r.getSubjectId());
            m.put("subjectName", r.getSubjectName());
            m.put("progress", r.getProgress());
            m.put("averageScore", r.getAverageScore());
            m.put("totalScore", r.getTotalScore());
            m.put("completedQuizzes", r.getCompletedQuizzes());
            m.put("completedTopics", r.getCompletedTopics());
            m.put("createdAt", r.getProgressCreatedAt());
            return m;
        }).toList();

        Map<String, Object> res = new HashMap<>();
        res.put("total", total);
        res.put("page", pageNumber);
        res.put("limit", pageSize);
        res.put("data", data);
        return res;
    }
}
