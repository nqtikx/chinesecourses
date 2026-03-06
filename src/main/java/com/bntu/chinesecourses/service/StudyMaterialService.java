package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.exception.NotFoundException;
import com.bntu.chinesecourses.model.dto.StudyMaterialResponse;
import com.bntu.chinesecourses.model.entity.StudyMaterialEntity;
import com.bntu.chinesecourses.model.entity.StudyGroupEntity;
import com.bntu.chinesecourses.repository.StudyMaterialRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StudyMaterialService {
  private final StudyMaterialRepository studyMaterialRepository;
  private final GroupAccessService groupAccessService;
  private final FileStorageService fileStorageService;
  private final CurrentUserService currentUserService;

  public StudyMaterialService(
      StudyMaterialRepository studyMaterialRepository,
      GroupAccessService groupAccessService,
      FileStorageService fileStorageService,
      CurrentUserService currentUserService
  ) {
    this.studyMaterialRepository = studyMaterialRepository;
    this.groupAccessService = groupAccessService;
    this.fileStorageService = fileStorageService;
    this.currentUserService = currentUserService;
  }

  @Transactional
  public StudyMaterialResponse upload(Long groupId, MultipartFile file) {
    StudyGroupEntity group = groupAccessService.requireManageableGroup(groupId);
    Long uploaderId = currentUserService.getCurrentUserId()
        .orElseThrow(() -> new IllegalStateException("Current user not found"));
    String path = fileStorageService.saveMaterial(file);
    StudyMaterialEntity entity = new StudyMaterialEntity(
        null,
        group,
        file.getOriginalFilename() == null ? "file" : file.getOriginalFilename(),
        file.getContentType(),
        path,
        uploaderId,
        false,
        Instant.now()
    );
    return toResponse(studyMaterialRepository.save(entity));
  }

  @Transactional(readOnly = true)
  public List<StudyMaterialResponse> listByGroup(Long groupId) {
    groupAccessService.requireVisibleGroup(groupId);
    return studyMaterialRepository.findTop200ByArchivedFalseAndGroup_IdOrderByCreatedAtDesc(groupId)
        .stream()
        .map(StudyMaterialService::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public Resource download(Long id) {
    StudyMaterialEntity material = studyMaterialRepository.findByIdAndArchivedFalse(id)
        .orElseThrow(() -> new NotFoundException("Material not found"));
    groupAccessService.requireVisibleGroup(material.getGroup().getId());
    return fileStorageService.loadAsResource(material.getStoragePath());
  }

  @Transactional(readOnly = true)
  public String resolveFilename(Long id) {
    return studyMaterialRepository.findByIdAndArchivedFalse(id)
        .map(StudyMaterialEntity::getFileName)
        .orElse("material.bin");
  }

  @Transactional(readOnly = true)
  public String resolveFileType(Long id) {
    return studyMaterialRepository.findByIdAndArchivedFalse(id)
        .map(StudyMaterialEntity::getFileType)
        .filter(type -> type != null && !type.isBlank())
        .orElse("application/octet-stream");
  }

  private static StudyMaterialResponse toResponse(StudyMaterialEntity e) {
    return new StudyMaterialResponse(
        e.getId(),
        e.getGroup().getId(),
        e.getFileName(),
        e.getFileType(),
        e.getUploaderUserId(),
        e.getCreatedAt()
    );
  }
}
