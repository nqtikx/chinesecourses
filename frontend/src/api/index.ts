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
  ArchiveRequest, EnrollmentStatus, ChineseLevel, UserProfileResponse, AdminUserListItemResponse,
} from '../types';

export const authApi = {
  login: (username: string, password: string) =>
    client.post<{ token: string }>('/api/auth/login', { username, password }),
  me: () => client.get<AdminMeResponse>('/api/admin/me'),
};

export const profileApi = {
  me: () => client.get<UserProfileResponse>('/api/profile/me'),
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
  listBySemester: (semesterId: number) => client.get<StudyGroupResponse[]>(`/api/study-groups?semesterId=${semesterId}`),
  get: (id: number) => client.get<StudyGroupResponse>(`/api/study-groups/${id}`),
  create: (data: StudyGroupCreateRequest) => client.post<StudyGroupResponse>('/api/study-groups', data),
  update: (id: number, data: StudyGroupUpdateRequest) => client.put<StudyGroupResponse>(`/api/study-groups/${id}`, data),
  archive: (id: number, req: ArchiveRequest) => client.patch(`/api/study-groups/${id}/archive`, req),
};

export const personsApi = {
  search: (prefix: string) => client.get<PersonResponse[]>(`/api/persons?lastNamePrefix=${prefix}`),
  searchByLastName: (prefix: string) => client.get<PersonResponse[]>(`/api/persons?lastNamePrefix=${prefix}`),
  get: (id: number) => client.get<PersonResponse>(`/api/persons/${id}`),
  create: (data: PersonCreateRequest) => client.post<PersonResponse>('/api/persons', data),
  update: (id: number, data: PersonUpdateRequest) => client.put<PersonResponse>(`/api/persons/${id}`, data),
  archive: (id: number, req: ArchiveRequest) => client.patch(`/api/persons/${id}/archive`, req),
};

export const teachersApi = {
  list: () => client.get<TeacherResponse[]>('/api/teachers'),
  get: (id: number) => client.get<TeacherResponse>(`/api/teachers/${id}`),
  create: (personId: number) => client.post<TeacherResponse>('/api/teachers', { personId }),
  archive: (id: number, req: ArchiveRequest) => client.patch(`/api/teachers/${id}/archive`, req),
};

export const enrollmentsApi = {
  listByGroup: (groupId: number) => client.get<EnrollmentResponse[]>(`/api/enrollments?groupId=${groupId}`),
  listBySemester: (semesterId: number, status?: EnrollmentStatus, level?: ChineseLevel) => {
    let url = `/api/enrollments?semesterId=${semesterId}`;
    if (status) url += `&status=${status}`;
    if (level) url += `&level=${level}`;
    return client.get<EnrollmentResponse[]>(url);
  },
  get: (id: number) => client.get<EnrollmentResponse>(`/api/enrollments/${id}`),
  create: (data: EnrollmentCreateRequest) => client.post<EnrollmentResponse>('/api/enrollments', data),
  update: (id: number, data: EnrollmentUpdateRequest) => client.put<EnrollmentResponse>(`/api/enrollments/${id}`, data),
  archive: (id: number, req: ArchiveRequest) => client.patch(`/api/enrollments/${id}/archive`, req),
};

export const lessonSessionsApi = {
  listByGroup: (groupId: number) => client.get<LessonSessionResponse[]>(`/api/lesson-sessions?groupId=${groupId}`),
  get: (id: number) => client.get<LessonSessionResponse>(`/api/lesson-sessions/${id}`),
  create: (data: LessonSessionCreateRequest) => client.post<LessonSessionResponse>('/api/lesson-sessions', data),
  update: (id: number, data: LessonSessionUpdateRequest) => client.put<LessonSessionResponse>(`/api/lesson-sessions/${id}`, data),
  archive: (id: number, req: ArchiveRequest) => client.patch(`/api/lesson-sessions/${id}/archive`, req),
};

export const attendanceApi = {
  listBySession: (lessonSessionId: number) => client.get<AttendanceResponse[]>(`/api/attendance?lessonSessionId=${lessonSessionId}`),
  listByEnrollment: (enrollmentId: number) => client.get<AttendanceResponse[]>(`/api/attendance?enrollmentId=${enrollmentId}`),
  get: (id: number) => client.get<AttendanceResponse>(`/api/attendance/${id}`),
  create: (data: AttendanceCreateRequest) => client.post<AttendanceResponse>('/api/attendance', data),
  update: (id: number, data: AttendanceUpdateRequest) => client.put<AttendanceResponse>(`/api/attendance/${id}`, data),
  archive: (id: number, req: ArchiveRequest) => client.patch(`/api/attendance/${id}/archive`, req),
};

export const scheduleRulesApi = {
  listByGroup: (groupId: number) => client.get<GroupScheduleRuleResponse[]>(`/api/group-schedule-rules?groupId=${groupId}`),
  get: (id: number) => client.get<GroupScheduleRuleResponse>(`/api/group-schedule-rules/${id}`),
  create: (data: GroupScheduleRuleCreateRequest) => client.post<GroupScheduleRuleResponse>('/api/group-schedule-rules', data),
  update: (id: number, data: GroupScheduleRuleUpdateRequest) => client.put<GroupScheduleRuleResponse>(`/api/group-schedule-rules/${id}`, data),
  archive: (id: number, req: ArchiveRequest) => client.patch(`/api/group-schedule-rules/${id}/archive`, req),
};

export const adminUsersApi = {
  list: () => client.get<AdminUserListItemResponse[]>('/api/admin/users'),
  profile: (id: number) => client.get<UserProfileResponse>(`/api/admin/users/${id}/profile`),
};

export const adminEnrollmentsApi = {
  create: (data: { userId: number; courseId: number; groupId?: number; startDate?: string }) =>
    client.post('/api/admin/enrollments', data),
  complete: (id: number, data?: { status: 'COMPLETED'; endDate?: string }) =>
    client.patch(`/api/admin/enrollments/${id}`, data ?? { status: 'COMPLETED' }),
};

export const contractsApi = {
  generate: (data: { userId: number; courseId: number; groupId?: number }) =>
    client.post('/api/admin/contracts', data, { responseType: 'blob' }),
};
