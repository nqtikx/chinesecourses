import client from './client';
import type {
  AdminMeResponse,
  CourseResponse, CourseCreateRequest, CourseUpdateRequest,
  SemesterResponse, SemesterCreateRequest, SemesterUpdateRequest,
  StudyGroupResponse, StudyGroupCreateRequest, StudyGroupUpdateRequest,
  PersonResponse, PersonCreateRequest, PersonUpdateRequest,
  TeacherResponse,
  EnrollmentResponse, EnrollmentCreateRequest, EnrollmentUpdateRequest,
  LessonSessionResponse, LessonSessionCreateRequest, LessonSessionUpdateRequest,
  AttendanceResponse, AttendanceCreateRequest, AttendanceUpdateRequest,
  GroupScheduleRuleResponse, GroupScheduleRuleCreateRequest, GroupScheduleRuleUpdateRequest,
  ArchiveRequest, EnrollmentStatus, ChineseLevel,
} from '../types';

export const authApi = {
  login: (username: string, password: string) =>
    client.post<{ token: string }>('/api/auth/login', { username, password }),
  me: () => client.get<AdminMeResponse>('/api/admin/me'),
};

export const coursesApi = {
  list: () => client.get<CourseResponse[]>('/api/courses'),
  get: (id: number) => client.get<CourseResponse>(`/api/courses/${id}`),
  create: (data: CourseCreateRequest) => client.post<CourseResponse>('/api/courses', data),
  update: (id: number, data: CourseUpdateRequest) => client.put<CourseResponse>(`/api/courses/${id}`, data),
  archive: (id: number, req: ArchiveRequest) => client.patch(`/api/courses/${id}/archive`, req),
};

export const semestersApi = {
  listByCourse: (courseId: number) => client.get<SemesterResponse[]>(`/api/semesters?courseId=${courseId}`),
  get: (id: number) => client.get<SemesterResponse>(`/api/semesters/${id}`),
  create: (data: SemesterCreateRequest) => client.post<SemesterResponse>('/api/semesters', data),
  update: (id: number, data: SemesterUpdateRequest) => client.put<SemesterResponse>(`/api/semesters/${id}`, data),
  archive: (id: number, req: ArchiveRequest) => client.patch(`/api/semesters/${id}/archive`, req),
};

export const studyGroupsApi = {
  listBySemester: (semesterId: number) => client.get<StudyGroupResponse[]>(`/study-groups?semesterId=${semesterId}`),
  get: (id: number) => client.get<StudyGroupResponse>(`/study-groups/${id}`),
  create: (data: StudyGroupCreateRequest) => client.post<StudyGroupResponse>('/study-groups', data),
  update: (id: number, data: StudyGroupUpdateRequest) => client.put<StudyGroupResponse>(`/study-groups/${id}`, data),
  archive: (id: number, req: ArchiveRequest) => client.patch(`/study-groups/${id}/archive`, req),
};

export const personsApi = {
  search: (prefix: string) => client.get<PersonResponse[]>(`/persons?lastNamePrefix=${prefix}`),
  get: (id: number) => client.get<PersonResponse>(`/persons/${id}`),
  create: (data: PersonCreateRequest) => client.post<PersonResponse>('/persons', data),
  update: (id: number, data: PersonUpdateRequest) => client.put<PersonResponse>(`/persons/${id}`, data),
  archive: (id: number, req: ArchiveRequest) => client.patch(`/persons/${id}/archive`, req),
};

export const teachersApi = {
  get: (id: number) => client.get<TeacherResponse>(`/teachers/${id}`),
  create: (personId: number) => client.post<TeacherResponse>('/teachers', { personId }),
  archive: (id: number, req: ArchiveRequest) => client.patch(`/teachers/${id}/archive`, req),
};

export const enrollmentsApi = {
  listByGroup: (groupId: number) => client.get<EnrollmentResponse[]>(`/enrollments?groupId=${groupId}`),
  listBySemester: (semesterId: number, status?: EnrollmentStatus, level?: ChineseLevel) => {
    let url = `/enrollments?semesterId=${semesterId}`;
    if (status) url += `&status=${status}`;
    if (level) url += `&level=${level}`;
    return client.get<EnrollmentResponse[]>(url);
  },
  get: (id: number) => client.get<EnrollmentResponse>(`/enrollments/${id}`),
  create: (data: EnrollmentCreateRequest) => client.post<EnrollmentResponse>('/enrollments', data),
  update: (id: number, data: EnrollmentUpdateRequest) => client.put<EnrollmentResponse>(`/enrollments/${id}`, data),
  archive: (id: number, req: ArchiveRequest) => client.patch(`/enrollments/${id}/archive`, req),
};

export const lessonSessionsApi = {
  listByGroup: (groupId: number) => client.get<LessonSessionResponse[]>(`/lesson-sessions?groupId=${groupId}`),
  get: (id: number) => client.get<LessonSessionResponse>(`/lesson-sessions/${id}`),
  create: (data: LessonSessionCreateRequest) => client.post<LessonSessionResponse>('/lesson-sessions', data),
  update: (id: number, data: LessonSessionUpdateRequest) => client.put<LessonSessionResponse>(`/lesson-sessions/${id}`, data),
  archive: (id: number, req: ArchiveRequest) => client.patch(`/lesson-sessions/${id}/archive`, req),
};

export const attendanceApi = {
  listBySession: (lessonSessionId: number) => client.get<AttendanceResponse[]>(`/attendance?lessonSessionId=${lessonSessionId}`),
  listByEnrollment: (enrollmentId: number) => client.get<AttendanceResponse[]>(`/attendance?enrollmentId=${enrollmentId}`),
  get: (id: number) => client.get<AttendanceResponse>(`/attendance/${id}`),
  create: (data: AttendanceCreateRequest) => client.post<AttendanceResponse>('/attendance', data),
  update: (id: number, data: AttendanceUpdateRequest) => client.put<AttendanceResponse>(`/attendance/${id}`, data),
  archive: (id: number, req: ArchiveRequest) => client.patch(`/attendance/${id}/archive`, req),
};

export const scheduleRulesApi = {
  listByGroup: (groupId: number) => client.get<GroupScheduleRuleResponse[]>(`/group-schedule-rules?groupId=${groupId}`),
  get: (id: number) => client.get<GroupScheduleRuleResponse>(`/group-schedule-rules/${id}`),
  create: (data: GroupScheduleRuleCreateRequest) => client.post<GroupScheduleRuleResponse>('/group-schedule-rules', data),
  update: (id: number, data: GroupScheduleRuleUpdateRequest) => client.put<GroupScheduleRuleResponse>(`/group-schedule-rules/${id}`, data),
  archive: (id: number, req: ArchiveRequest) => client.patch(`/group-schedule-rules/${id}/archive`, req),
};
