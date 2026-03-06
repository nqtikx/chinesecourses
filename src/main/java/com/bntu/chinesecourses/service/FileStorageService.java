package com.bntu.chinesecourses.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

  private final Path materialsDir;
  private final Path contractsDir;

  public FileStorageService(
      @Value("${app.storage.base-dir:data/storage}") String baseDir,
      @Value("${app.storage.materials-dir:materials}") String materialsSubDir,
      @Value("${app.storage.contracts-dir:contracts}") String contractsSubDir
  ) {
    Path base = Paths.get(baseDir).toAbsolutePath().normalize();
    this.materialsDir = base.resolve(materialsSubDir);
    this.contractsDir = base.resolve(contractsSubDir);
    try {
      Files.createDirectories(materialsDir);
      Files.createDirectories(contractsDir);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot initialize storage directories", e);
    }
  }

  public String saveMaterial(MultipartFile file) {
    return saveToDir(file, materialsDir);
  }

  public String saveContract(byte[] bytes, String extension) {
    String filename = LocalDate.now() + "-" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
    Path target = contractsDir.resolve(filename);
    try {
      Files.write(target, bytes);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot save contract file", e);
    }
    return target.toString();
  }

  public Resource loadAsResource(String storagePath) {
    Path path = Paths.get(storagePath).toAbsolutePath().normalize();
    if (!Files.exists(path)) {
      throw new IllegalStateException("Stored file not found: " + storagePath);
    }
    return new FileSystemResource(path);
  }

  private String saveToDir(MultipartFile file, Path dir) {
    String original = file.getOriginalFilename() == null ? "file.bin" : file.getOriginalFilename();
    String safeName = original.replaceAll("[^a-zA-Z0-9._-]", "_");
    String filename = LocalDate.now() + "-" + UUID.randomUUID().toString().substring(0, 8) + "-" + safeName;
    Path target = dir.resolve(filename);
    try {
      Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot store file", e);
    }
    return target.toString();
  }
}
