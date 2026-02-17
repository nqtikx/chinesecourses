package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.model.dto.GroupScheduleRuleCreateRequest;
import com.bntu.chinesecourses.model.dto.GroupScheduleRuleResponse;
import com.bntu.chinesecourses.model.dto.GroupScheduleRuleUpdateRequest;
import java.util.List;

public interface GroupScheduleRuleService {

  GroupScheduleRuleResponse create(GroupScheduleRuleCreateRequest request);

  GroupScheduleRuleResponse get(Long id);
  GroupScheduleRuleResponse update(Long id, GroupScheduleRuleUpdateRequest request);
  List<GroupScheduleRuleResponse> findTop50ByGroup(Long groupId);
  GroupScheduleRuleResponse setArchived(Long id, boolean archived);
}
