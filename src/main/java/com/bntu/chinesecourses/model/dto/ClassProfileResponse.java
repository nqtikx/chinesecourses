package com.bntu.chinesecourses.model.dto;

import java.util.List;

public record ClassProfileResponse(
    Long groupId,
    String groupName,
    Long semesterId,
    String semesterName,
    Long courseId,
    String courseName,
    Long teacherId,
    String teacherName,
    List<GroupScheduleRuleResponse> schedule,
    List<StudyMaterialResponse> materials,
    List<GroupNoteResponse> notes,
    List<PersonResponse> students
) {
}
