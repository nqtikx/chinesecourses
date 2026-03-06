package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.model.dto.ClassProfileResponse;
import com.bntu.chinesecourses.model.dto.ClassProfileGroupItemResponse;
import com.bntu.chinesecourses.model.dto.GroupNoteCreateRequest;
import com.bntu.chinesecourses.model.dto.GroupNoteResponse;
import com.bntu.chinesecourses.service.ClassProfileService;
import com.bntu.chinesecourses.service.GroupNoteService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/class-profiles")
@PreAuthorize("hasAnyRole('ADMIN','TEACHER','USER')")
public class ClassProfileController {

  private final ClassProfileService classProfileService;
  private final GroupNoteService groupNoteService;

  public ClassProfileController(ClassProfileService classProfileService, GroupNoteService groupNoteService) {
    this.classProfileService = classProfileService;
    this.groupNoteService = groupNoteService;
  }

  @GetMapping("/me")
  public ClassProfileResponse myClassProfile() {
    return classProfileService.getMyClassProfile();
  }

  @GetMapping("/{groupId:\\d+}")
  public ClassProfileResponse byGroup(@PathVariable Long groupId) {
    return classProfileService.getByGroupId(groupId);
  }

  @GetMapping("/groups")
  public List<ClassProfileGroupItemResponse> groups() {
    return classProfileService.listAvailableGroups();
  }

  @GetMapping("/{groupId:\\d+}/notes")
  public List<GroupNoteResponse> notes(@PathVariable Long groupId) {
    return groupNoteService.listByGroup(groupId);
  }

  @PostMapping("/{groupId:\\d+}/notes")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public GroupNoteResponse addNote(@PathVariable Long groupId, @Valid @RequestBody GroupNoteCreateRequest request) {
    return groupNoteService.create(groupId, request);
  }
}
