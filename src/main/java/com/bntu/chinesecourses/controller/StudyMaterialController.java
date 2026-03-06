package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.model.dto.StudyMaterialResponse;
import com.bntu.chinesecourses.service.StudyMaterialService;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/materials")
@PreAuthorize("hasAnyRole('ADMIN','TEACHER','USER','GROUP')")
public class StudyMaterialController {

  private final StudyMaterialService studyMaterialService;

  public StudyMaterialController(StudyMaterialService studyMaterialService) {
    this.studyMaterialService = studyMaterialService;
  }

  @PostMapping("/upload")
  public StudyMaterialResponse upload(@RequestParam Long groupId, @RequestParam("file") MultipartFile file) {
    return studyMaterialService.upload(groupId, file);
  }

  @GetMapping
  public List<StudyMaterialResponse> list(@RequestParam Long groupId) {
    return studyMaterialService.listByGroup(groupId);
  }

  @GetMapping("/{id}/download")
  public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long id) {
    String filename = studyMaterialService.resolveFilename(id);
    String fileType = studyMaterialService.resolveFileType(id);
    var resource = studyMaterialService.download(id);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
    headers.setContentType(MediaType.parseMediaType(Objects.requireNonNull(fileType)));
    return ResponseEntity.ok().headers(headers).body(resource);
  }

  @GetMapping("/{id}/view")
  public ResponseEntity<org.springframework.core.io.Resource> view(@PathVariable Long id) {
    String fileType = studyMaterialService.resolveFileType(id);
    var resource = studyMaterialService.download(id);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(Objects.requireNonNull(fileType)));
    return ResponseEntity.ok().headers(headers).body(resource);
  }
}
