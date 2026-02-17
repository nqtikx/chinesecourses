package com.bntu.chinesecourses.service.impl;

import com.bntu.chinesecourses.exception.BadRequestException;
import com.bntu.chinesecourses.exception.ConflictException;
import com.bntu.chinesecourses.exception.NotFoundException;
import com.bntu.chinesecourses.model.dto.GroupScheduleRuleCreateRequest;
import com.bntu.chinesecourses.model.dto.GroupScheduleRuleResponse;
import com.bntu.chinesecourses.model.dto.GroupScheduleRuleUpdateRequest;
import com.bntu.chinesecourses.model.entity.GroupScheduleRuleEntity;
import com.bntu.chinesecourses.model.entity.StudyGroupEntity;
import com.bntu.chinesecourses.repository.GroupScheduleRuleRepository;
import com.bntu.chinesecourses.repository.StudyGroupRepository;
import com.bntu.chinesecourses.service.GroupScheduleRuleService;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupScheduleRuleServiceImpl implements GroupScheduleRuleService {

  private final GroupScheduleRuleRepository ruleRepository;
  private final StudyGroupRepository studyGroupRepository;

  public GroupScheduleRuleServiceImpl(
      GroupScheduleRuleRepository ruleRepository,
      StudyGroupRepository studyGroupRepository
  ) {
    this.ruleRepository = ruleRepository;
    this.studyGroupRepository = studyGroupRepository;
  }

  @Override
  @Transactional
  public GroupScheduleRuleResponse create(GroupScheduleRuleCreateRequest request) {
    validateTimes(request.startTime(), request.endTime());

    StudyGroupEntity group = studyGroupRepository.findByIdAndArchivedFalse(request.groupId())
        .orElseThrow(() -> new NotFoundException("Study group not found: id=" + request.groupId()));

    GroupScheduleRuleEntity entity = new GroupScheduleRuleEntity(
        null,
        group,
        request.dayOfWeek(),
        request.startTime(),
        request.endTime(),
        normalizeRoom(request.room()),
        request.active(),
        false,
        Instant.now()
    );

    GroupScheduleRuleEntity saved = ruleRepository.save(entity);
    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public GroupScheduleRuleResponse get(Long id) {
    GroupScheduleRuleEntity entity = ruleRepository.findByIdAndArchivedFalse(id)
        .orElseThrow(() -> new NotFoundException("Group schedule rule not found: id=" + id));
    return toResponse(entity);
  }

  @Override
  @Transactional
  public GroupScheduleRuleResponse update(Long id, GroupScheduleRuleUpdateRequest request) {
    validateTimes(request.startTime(), request.endTime());

    GroupScheduleRuleEntity entity = ruleRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Group schedule rule not found: id=" + id));

    StudyGroupEntity group = studyGroupRepository.findByIdAndArchivedFalse(request.groupId())
        .orElseThrow(() -> new NotFoundException("Study group not found: id=" + request.groupId()));

    if (group.isArchived()) {
      throw new ConflictException("Cannot assign rule to archived group: id=" + group.getId());
    }

    entity.setGroup(group);
    entity.setDayOfWeek(request.dayOfWeek());
    entity.setStartTime(request.startTime());
    entity.setEndTime(request.endTime());
    entity.setRoom(normalizeRoom(request.room()));
    entity.setActive(request.active());
    entity.setArchived(request.archived());

    return toResponse(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public List<GroupScheduleRuleResponse> findTop50ByGroup(Long groupId) {
    return ruleRepository.findTop50ByArchivedFalseAndGroupIdOrderByDayOfWeekAscStartTimeAsc(groupId).stream()
        .map(GroupScheduleRuleServiceImpl::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public GroupScheduleRuleResponse setArchived(Long id, boolean archived) {
    GroupScheduleRuleEntity entity = ruleRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Group schedule rule not found: id=" + id));
    entity.setArchived(archived);
    return toResponse(entity);
  }

  private static void validateTimes(java.time.LocalTime start, java.time.LocalTime end) {
    if (!start.isBefore(end)) {
      throw new BadRequestException("startTime must be before endTime");
    }
  }

  private static String normalizeRoom(String room) {
    if (room == null) {
      return null;
    }
    String trimmed = room.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static GroupScheduleRuleResponse toResponse(GroupScheduleRuleEntity entity) {
    return new GroupScheduleRuleResponse(
        entity.getId(),
        entity.getGroup().getId(),
        entity.getDayOfWeek(),
        entity.getStartTime(),
        entity.getEndTime(),
        entity.getRoom(),
        entity.isActive(),
        entity.isArchived(),
        entity.getCreatedAt()
    );
  }
}
