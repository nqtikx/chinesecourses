package com.bntu.chinesecourses.repository;

import com.bntu.chinesecourses.model.entity.GroupScheduleRuleEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupScheduleRuleRepository extends JpaRepository<GroupScheduleRuleEntity, Long> {

  Optional<GroupScheduleRuleEntity> findByIdAndArchivedFalse(Long id);
  List<GroupScheduleRuleEntity> findTop50ByArchivedFalseAndGroupIdOrderByDayOfWeekAscStartTimeAsc(Long groupId);
}
