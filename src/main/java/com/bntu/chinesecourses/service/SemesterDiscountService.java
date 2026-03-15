package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.model.entity.EnrollmentEntity;
import com.bntu.chinesecourses.model.entity.EnrollmentStatus;
import com.bntu.chinesecourses.model.entity.SemesterEntity;
import com.bntu.chinesecourses.repository.EnrollmentRepository;
import com.bntu.chinesecourses.repository.SemesterRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SemesterDiscountService {

  private static final List<EnrollmentStatus> DISCOUNT_ELIGIBLE_STATUSES =
      List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED);

  private final EnrollmentRepository enrollmentRepository;
  private final SemesterRepository semesterRepository;
  private final List<Integer> discountGrowthPercentages;

  public SemesterDiscountService(
      EnrollmentRepository enrollmentRepository,
      SemesterRepository semesterRepository,
      @Value("${app.contracts.discount-growth-percentages:0,5,10,15,20}") String discountGrowthPercentages) {
    this.enrollmentRepository = enrollmentRepository;
    this.semesterRepository = semesterRepository;
    this.discountGrowthPercentages = parseGrowthPercentages(discountGrowthPercentages);
  }

  @Transactional(readOnly = true)
  public DiscountEvaluation evaluate(Long studentId, Long currentSemesterId) {
    List<EnrollmentEntity> completed = enrollmentRepository.findByArchivedFalseAndStudentIdAndStatusOrderByCreatedAtDesc(
        studentId, EnrollmentStatus.COMPLETED);
    int completedCoursesCount = completed.size();

    if (currentSemesterId == null) {
      return new DiscountEvaluation(completedCoursesCount, 0, 0, false);
    }

    List<SemesterEntity> semesters = semesterRepository.findByArchivedFalseOrderByStartDateAscIdAsc();
    if (semesters.isEmpty()) {
      return new DiscountEvaluation(completedCoursesCount, 0, 0, false);
    }

    Map<Long, Integer> semesterIndexById = new HashMap<>();
    for (int i = 0; i < semesters.size(); i++) {
      semesterIndexById.put(semesters.get(i).getId(), i);
    }

    Integer currentIndex = semesterIndexById.get(currentSemesterId);
    if (currentIndex == null) {
      return new DiscountEvaluation(completedCoursesCount, 0, 0, false);
    }

    List<EnrollmentEntity> eligible = enrollmentRepository.findByArchivedFalseAndStudentIdAndStatusIn(
        studentId, DISCOUNT_ELIGIBLE_STATUSES);
    Set<Long> enrolledSemesterIds = new HashSet<>();
    for (EnrollmentEntity enrollment : eligible) {
      enrolledSemesterIds.add(enrollment.getSemesterId());
    }

    if (!enrolledSemesterIds.contains(currentSemesterId)) {
      return new DiscountEvaluation(completedCoursesCount, 0, 0, false);
    }

    int streak = 0;
    for (int i = currentIndex; i >= 0; i--) {
      Long semesterId = semesters.get(i).getId();
      if (enrolledSemesterIds.contains(semesterId)) {
        streak++;
      } else {
        break;
      }
    }

    int streakStartIndex = currentIndex - streak + 1;
    boolean hasEnrollmentBeforeStreak = false;
    for (Long semesterId : enrolledSemesterIds) {
      Integer idx = semesterIndexById.get(semesterId);
      if (idx != null && idx < streakStartIndex) {
        hasEnrollmentBeforeStreak = true;
        break;
      }
    }

    int discount = resolveDiscountByStreak(streak);
    return new DiscountEvaluation(completedCoursesCount, streak, discount, hasEnrollmentBeforeStreak);
  }

  private int resolveDiscountByStreak(int streak) {
    if (streak <= 1) {
      return 0;
    }
    int level = streak - 1;
    int index = Math.min(level, discountGrowthPercentages.size() - 1);
    return discountGrowthPercentages.get(index);
  }

  private static List<Integer> parseGrowthPercentages(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of(0, 5, 10, 15, 20);
    }
    String[] parts = raw.split(",");
    List<Integer> values = new ArrayList<>();
    for (String part : parts) {
      String trimmed = part.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      try {
        int parsed = Integer.parseInt(trimmed);
        values.add(Math.max(0, parsed));
      } catch (NumberFormatException ignored) {
        // Skip invalid values and fall back to defaults if list becomes empty.
      }
    }
    if (values.isEmpty()) {
      return List.of(0, 5, 10, 15, 20);
    }
    if (values.getFirst() != 0) {
      values.addFirst(0);
    }
    return List.copyOf(values);
  }

  public record DiscountEvaluation(
      int completedCoursesCount,
      int consecutiveSemesterStreak,
      int nextDiscountPercent,
      boolean discountResetByGap
  ) {
  }
}
