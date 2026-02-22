package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.model.dto.ArchiveRequest;
import com.bntu.chinesecourses.model.dto.PersonCreateRequest;
import com.bntu.chinesecourses.model.dto.PersonResponse;
import com.bntu.chinesecourses.model.dto.PersonUpdateRequest;
import com.bntu.chinesecourses.service.CurrentUserService;
import com.bntu.chinesecourses.service.PersonService;
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
@RequestMapping("/persons")
public class PersonController {

  private final PersonService personService;
  private final CurrentUserService currentUserService;

  public PersonController(PersonService personService, CurrentUserService currentUserService) {
    this.personService = personService;
    this.currentUserService = currentUserService;
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public PersonResponse create(@RequestBody PersonCreateRequest request) {
    return personService.create(request);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public PersonResponse get(@PathVariable Long id) {
    return personService.get(id, currentUserService.getCurrentTeacherId().orElse(null));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public PersonResponse update(@PathVariable Long id, @RequestBody PersonUpdateRequest request) {
    return personService.update(id, request);
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public List<PersonResponse> search(@RequestParam(name = "lastNamePrefix", required = false) String lastNamePrefix) {
    return personService.searchByLastNamePrefix(lastNamePrefix);
  }

  @PatchMapping("/{id}/archive")
  @PreAuthorize("hasRole('ADMIN')")
  public PersonResponse setArchived(@PathVariable Long id, @RequestBody ArchiveRequest request) {
    return personService.setArchived(id, request.archived());
  }
}
