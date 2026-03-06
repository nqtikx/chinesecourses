package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.model.dto.AdminContractCreateRequest;
import com.bntu.chinesecourses.model.dto.ContractDocumentResponse;
import com.bntu.chinesecourses.service.ContractPdfService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/contracts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminContractsController {

  private final ContractPdfService contractPdfService;

  public AdminContractsController(ContractPdfService contractPdfService) {
    this.contractPdfService = contractPdfService;
  }

  @GetMapping
  public List<ContractDocumentResponse> listAll() {
    return contractPdfService.listAllContracts();
  }

  @PostMapping
  public ResponseEntity<byte[]> createContract(@Valid @RequestBody AdminContractCreateRequest request) {
    ContractPdfService.GeneratedContract contract = contractPdfService.generate(request);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    headers.setContentDisposition(ContentDisposition.attachment()
        .filename(contract.fileName())
        .build());
    return ResponseEntity.ok()
        .headers(headers)
        .body(contract.content());
  }
}
