package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.model.dto.ArchiveRequest;
import com.bntu.chinesecourses.model.dto.StudyGroupCreateRequest;
import com.bntu.chinesecourses.model.dto.StudyGroupResponse;
import com.bntu.chinesecourses.model.dto.StudyGroupUpdateRequest;
import com.bntu.chinesecourses.service.StudyGroupService;
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
@RequestMapping("/study-groups")
public class StudyGroupController {

  private final StudyGroupService studyGroupService;

  public StudyGroupController(StudyGroupService studyGroupService) {
    this.studyGroupService = studyGroupService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public StudyGroupResponse create(@Valid @RequestBody StudyGroupCreateRequest request) {
    return studyGroupService.create(request);
  }

  @GetMapping("/{id}")
  public StudyGroupResponse get(@PathVariable Long id) {
    return studyGroupService.get(id);
  }

  @PutMapping("/{id}")
  public StudyGroupResponse update(@PathVariable Long id, @Valid @RequestBody StudyGroupUpdateRequest request) {
    return studyGroupService.update(id, request);
  }

  @GetMapping
  public List<StudyGroupResponse> findTop50(@RequestParam Long semesterId) {
    return studyGroupService.findTop50BySemester(semesterId);
  }

  @PatchMapping("/{id}/archive")
  public StudyGroupResponse setArchived(@PathVariable Long id, @RequestBody ArchiveRequest request) {
    return studyGroupService.setArchived(id, request.archived());
  }
}
