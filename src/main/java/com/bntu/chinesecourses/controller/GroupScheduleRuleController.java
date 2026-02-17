package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.model.dto.ArchiveRequest;
import com.bntu.chinesecourses.model.dto.GroupScheduleRuleCreateRequest;
import com.bntu.chinesecourses.model.dto.GroupScheduleRuleResponse;
import com.bntu.chinesecourses.model.dto.GroupScheduleRuleUpdateRequest;
import com.bntu.chinesecourses.service.GroupScheduleRuleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/group-schedule-rules")
public class GroupScheduleRuleController {

  private final GroupScheduleRuleService groupScheduleRuleService;

  public GroupScheduleRuleController(GroupScheduleRuleService groupScheduleRuleService) {
    this.groupScheduleRuleService = groupScheduleRuleService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public GroupScheduleRuleResponse create(@Valid @RequestBody GroupScheduleRuleCreateRequest request) {
    return groupScheduleRuleService.create(request);
  }

  @GetMapping("/{id}")
  public GroupScheduleRuleResponse get(@PathVariable Long id) {
    return groupScheduleRuleService.get(id);
  }

  @PutMapping("/{id}")
  public GroupScheduleRuleResponse update(@PathVariable Long id, @Valid @RequestBody GroupScheduleRuleUpdateRequest request) {
    return groupScheduleRuleService.update(id, request);
  }

  @GetMapping
  public List<GroupScheduleRuleResponse> findTop50(@RequestParam Long groupId) {
    return groupScheduleRuleService.findTop50ByGroup(groupId);
  }

  @PatchMapping("/{id}/archive")
  public GroupScheduleRuleResponse setArchived(@PathVariable Long id, @RequestBody ArchiveRequest request) {
    return groupScheduleRuleService.setArchived(id, request.archived());
  }
}
