package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.model.dto.ContractDocumentResponse;
import com.bntu.chinesecourses.service.ContractPdfService;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contracts")
@PreAuthorize("hasAnyRole('ADMIN','TEACHER','USER')")
public class ContractsController {

  private final ContractPdfService contractPdfService;

  public ContractsController(ContractPdfService contractPdfService) {
    this.contractPdfService = contractPdfService;
  }

  @GetMapping("/my")
  public List<ContractDocumentResponse> myContracts() {
    return contractPdfService.listMyContracts();
  }

  @GetMapping
  public List<ContractDocumentResponse> byGroup(@RequestParam(required = false) Long groupId) {
    if (groupId == null) {
      return contractPdfService.listMyContracts();
    }
    return contractPdfService.listByGroup(groupId);
  }

  @GetMapping("/{id}/download")
  public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long id) {
    ContractPdfService.StoredContractFile file = contractPdfService.download(id);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    headers.setContentDisposition(ContentDisposition.attachment().filename(file.fileName()).build());
    return ResponseEntity.ok().headers(headers).body(file.resource());
  }
}
