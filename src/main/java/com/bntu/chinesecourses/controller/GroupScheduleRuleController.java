package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.model.dto.ArchiveRequest;
import com.bntu.chinesecourses.model.dto.GroupScheduleRuleCreateRequest;
import com.bntu.chinesecourses.model.dto.GroupScheduleRuleResponse;
import com.bntu.chinesecourses.model.dto.GroupScheduleRuleUpdateRequest;
import com.bntu.chinesecourses.service.GroupAccessService;
import com.bntu.chinesecourses.service.GroupScheduleRuleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/group-schedule-rules")
public class GroupScheduleRuleController {

  private final GroupScheduleRuleService groupScheduleRuleService;
  private final GroupAccessService groupAccessService;

  public GroupScheduleRuleController(GroupScheduleRuleService groupScheduleRuleService, GroupAccessService groupAccessService) {
    this.groupScheduleRuleService = groupScheduleRuleService;
    this.groupAccessService = groupAccessService;
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  @ResponseStatus(HttpStatus.CREATED)
  public GroupScheduleRuleResponse create(@Valid @RequestBody GroupScheduleRuleCreateRequest request) {
    groupAccessService.requireManageableGroup(request.groupId());
    return groupScheduleRuleService.create(request);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER','USER')")
  public GroupScheduleRuleResponse get(@PathVariable Long id) {
    GroupScheduleRuleResponse response = groupScheduleRuleService.get(id);
    groupAccessService.requireVisibleGroup(response.groupId());
    return response;
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public GroupScheduleRuleResponse update(@PathVariable Long id, @Valid @RequestBody GroupScheduleRuleUpdateRequest request) {
    groupAccessService.requireManageableGroup(request.groupId());
    return groupScheduleRuleService.update(id, request);
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER','USER')")
  public List<GroupScheduleRuleResponse> findTop50(@RequestParam Long groupId) {
    groupAccessService.requireVisibleGroup(groupId);
    return groupScheduleRuleService.findTop50ByGroup(groupId);
  }

  @PatchMapping("/{id}/archive")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public GroupScheduleRuleResponse setArchived(@PathVariable Long id, @RequestBody ArchiveRequest request) {
    GroupScheduleRuleResponse existing = groupScheduleRuleService.get(id);
    groupAccessService.requireManageableGroup(existing.groupId());
    return groupScheduleRuleService.setArchived(id, request.archived());
  }
}
