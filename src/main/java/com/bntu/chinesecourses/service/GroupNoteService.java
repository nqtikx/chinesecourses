package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.model.dto.GroupNoteCreateRequest;
import com.bntu.chinesecourses.model.dto.GroupNoteResponse;
import com.bntu.chinesecourses.model.entity.GroupNoteEntity;
import com.bntu.chinesecourses.model.entity.StudyGroupEntity;
import com.bntu.chinesecourses.repository.GroupNoteRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupNoteService {
  private final GroupNoteRepository groupNoteRepository;
  private final GroupAccessService groupAccessService;
  private final CurrentUserService currentUserService;

  public GroupNoteService(
      GroupNoteRepository groupNoteRepository,
      GroupAccessService groupAccessService,
      CurrentUserService currentUserService
  ) {
    this.groupNoteRepository = groupNoteRepository;
    this.groupAccessService = groupAccessService;
    this.currentUserService = currentUserService;
  }

  @Transactional
  public GroupNoteResponse create(Long groupId, GroupNoteCreateRequest request) {
    StudyGroupEntity group = groupAccessService.requireManageableGroup(groupId);
    Long authorUserId = currentUserService.getCurrentUserId().orElseThrow();
    GroupNoteEntity entity = new GroupNoteEntity(
        null,
        group,
        authorUserId,
        request.text().trim(),
        false,
        Instant.now()
    );
    return toResponse(groupNoteRepository.save(entity));
  }

  @Transactional(readOnly = true)
  public List<GroupNoteResponse> listByGroup(Long groupId) {
    groupAccessService.requireVisibleGroup(groupId);
    return groupNoteRepository.findTop200ByArchivedFalseAndGroup_IdOrderByCreatedAtDesc(groupId)
        .stream()
        .map(GroupNoteService::toResponse)
        .toList();
  }

  private static GroupNoteResponse toResponse(GroupNoteEntity e) {
    return new GroupNoteResponse(
        e.getId(),
        e.getGroup().getId(),
        e.getAuthorUserId(),
        e.getText(),
        e.getCreatedAt()
    );
  }
}
