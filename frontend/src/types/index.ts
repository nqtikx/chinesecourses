export type AdminRole = 'ROLE_ADMIN' | 'ROLE_TEACHER' | 'ROLE_USER';
export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED';
export type EnrollmentStatus = 'APPLICANT' | 'ACTIVE' | 'COMPLETED';
export type ChineseLevel = 'HSK1' | 'HSK2' | 'HSK3' | 'HSK4' | 'HSK5' | 'HSK6';

export interface AdminMeResponse {
  id: number;
  username: string;
  role: AdminRole;
  teacherId: number | null;
  createdAt: string;
}

export interface CourseResponse {
  id: number;
  name: string;
  description: string | null;
  archived: boolean;
  createdAt: string;
}

export interface SemesterResponse {
  id: number;
  courseId: number;
  name: string;
  startDate: string;
  endDate: string;
  archived: boolean;
  createdAt: string;
}

export interface StudyGroupResponse {
  id: number;
  semesterId: number;
  teacherId: number | null;
  name: string;
  scheduleNotes: string | null;
  archived: boolean;
  createdAt: string;
}

export interface PersonResponse {
  id: number;
  lastName: string;
  firstName: string;
  middleName: string | null;
  birthDate: string | null;
  phone: string | null;
  email: string | null;
  archived: boolean;
  createdAt: string;
}

export interface TeacherResponse {
  id: number;
  personId: number;
  archived: boolean;
  createdAt: string;
}

export interface EnrollmentResponse {
  id: number;
  studentId: number;
  payerId: number | null;
  semesterId: number;
  groupId: number | null;
  status: EnrollmentStatus;
  level: ChineseLevel;
  archived: boolean;
  startDate: string | null;
  endDate: string | null;
  contractNumber: string | null;
  createdAt: string;
}

export interface ProfileCourseItemResponse {
  enrollmentId: number;
  courseId: number | null;
  courseName: string | null;
  groupId: number | null;
  groupName: string | null;
  groupTeacherName: string | null;
  startDate: string | null;
  endDate: string | null;
  status: string;
}

export interface UserProfileResponse {
  userId: number;
  username: string;
  role: AdminRole;
  personId: number | null;
  fullName: string | null;
  email: string | null;
  phone: string | null;
  currentCourse: ProfileCourseItemResponse | null;
  completedCourses: ProfileCourseItemResponse[];
}

export interface AdminUserListItemResponse {
  id: number;
  username: string;
  role: AdminRole;
  personId: number | null;
  fullName: string | null;
}

export interface StudyMaterialResponse {
  id: number;
  groupId: number;
  fileName: string;
  fileType: string | null;
  uploaderUserId: number;
  createdAt: string;
}

export interface GroupNoteResponse {
  id: number;
  groupId: number;
  authorUserId: number;
  text: string;
  createdAt: string;
}

export interface ClassProfileGroupItemResponse {
  groupId: number;
  groupName: string;
  courseName: string;
  semesterName: string;
  teacherName: string;
  studentsCount: number;
}

export interface ClassProfileResponse {
  groupId: number;
  groupName: string;
  semesterId: number;
  semesterName: string;
  courseId: number;
  courseName: string;
  teacherId: number | null;
  teacherName: string;
  schedule: GroupScheduleRuleResponse[];
  materials: StudyMaterialResponse[];
  notes: GroupNoteResponse[];
  students: PersonResponse[];
}

export interface ScheduleCellResponse {
  dayOfWeek: number;
  dayLabel: string;
  startTime: string;
  endTime: string;
  room: string;
}

export interface GroupScheduleTableResponse {
  groupId: number;
  groupName: string;
  courseName: string;
  semesterName: string;
  teacherName: string;
  rows: ScheduleCellResponse[];
}

export interface ContractDocumentResponse {
  id: number;
  userId: number;
  courseId: number;
  groupId: number | null;
  contractNumber: string;
  fileName: string;
  basePrice: number;
  discountPercent: number;
  finalPrice: number;
  createdAt: string;
}

export interface LessonSessionResponse {
  id: number;
  groupId: number;
  teacherId: number | null;
  startsAt: string;
  endsAt: string;
  topic: string | null;
  room: string | null;
  canceled: boolean;
  archived: boolean;
  createdAt: string;
}

export interface AttendanceResponse {
  id: number;
  lessonSessionId: number;
  enrollmentId: number;
  status: AttendanceStatus;
  comment: string | null;
  markedAt: string;
  archived: boolean;
  createdAt: string;
}

export interface GroupScheduleRuleResponse {
  id: number;
  groupId: number;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  room: string | null;
  active: boolean;
  archived: boolean;
  createdAt: string;
}

export interface CourseCreateRequest {
  name: string;
  description?: string;
}

export interface CourseUpdateRequest {
  name: string;
  description?: string;
  archived: boolean;
}

export interface SemesterCreateRequest {
  courseId: number;
  name: string;
  startDate: string;
  endDate: string;
}

export interface SemesterUpdateRequest {
  courseId: number;
  name: string;
  startDate: string;
  endDate: string;
  archived: boolean;
}

export interface StudyGroupCreateRequest {
  semesterId: number;
  teacherId?: number;
  name: string;
  scheduleNotes?: string;
}

export interface StudyGroupUpdateRequest {
  semesterId: number;
  teacherId?: number;
  name: string;
  scheduleNotes?: string;
  archived: boolean;
}

export interface PersonCreateRequest {
  lastName: string;
  firstName: string;
  middleName?: string;
  birthDate?: string;
  phone?: string;
  email?: string;
}

export interface PersonUpdateRequest {
  lastName: string;
  firstName: string;
  middleName?: string;
  birthDate?: string;
  phone?: string;
  email?: string;
  archived: boolean;
}

export interface EnrollmentCreateRequest {
  studentId: number;
  payerId?: number;
  semesterId: number;
  groupId?: number;
  status: EnrollmentStatus;
  level: ChineseLevel;
  startDate?: string;
}

export interface EnrollmentUpdateRequest {
  payerId?: number;
  semesterId: number;
  groupId?: number;
  status: EnrollmentStatus;
  level: ChineseLevel;
  startDate?: string;
  endDate?: string;
  archived: boolean;
}

export interface LessonSessionCreateRequest {
  groupId: number;
  teacherId?: number;
  startsAt: string;
  endsAt: string;
  topic?: string;
  room?: string;
}

export interface LessonSessionUpdateRequest {
  groupId: number;
  teacherId?: number;
  startsAt: string;
  endsAt: string;
  topic?: string;
  room?: string;
  canceled: boolean;
  archived: boolean;
}

export interface AttendanceCreateRequest {
  lessonSessionId: number;
  enrollmentId: number;
  status: AttendanceStatus;
  comment?: string;
}

export interface AttendanceUpdateRequest {
  status: AttendanceStatus;
  comment?: string;
  archived: boolean;
}

export interface GroupScheduleRuleCreateRequest {
  groupId: number;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  room?: string;
  active: boolean;
}

export interface GroupScheduleRuleUpdateRequest {
  groupId: number;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  room?: string;
  active: boolean;
  archived: boolean;
}

export interface ArchiveRequest {
  archived: boolean;
}
