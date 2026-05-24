import { useEffect, useState } from 'react';
import { attendanceApi, classProfilesApi, lessonSessionsApi, studyGroupsApi, semestersApi, coursesApi, enrollmentsApi, personsApi } from '../api';
import type { AttendanceJournalResponse, AttendanceResponse, AttendanceStatus, LessonSessionResponse, StudyGroupResponse, SemesterResponse, CourseResponse, EnrollmentResponse } from '../types';
import { useAuth } from '../context/AuthContext';
import Badge from '../components/ui/Badge';
import EmptyState from '../components/ui/EmptyState';
import { toast } from '../components/ui/Toast';
import { CheckSquare, UserCheck, Clock, Check, X, AlertTriangle, FileText } from 'lucide-react';

const STATUS_LABELS: Record<AttendanceStatus, string> = { PRESENT: 'Присутствует', ABSENT: 'Отсутствует', LATE: 'Опоздал', EXCUSED: 'Уваж. причина' };
const STATUS_ICONS: Record<AttendanceStatus, typeof Check> = { PRESENT: Check, ABSENT: X, LATE: AlertTriangle, EXCUSED: FileText };
const STATUS_STYLES: Record<AttendanceStatus, string> = {
  PRESENT: 'bg-emerald-100 text-emerald-700 border-emerald-300 hover:bg-emerald-200',
  ABSENT: 'bg-red-100 text-red-700 border-red-300 hover:bg-red-200',
  LATE: 'bg-amber-100 text-amber-700 border-amber-300 hover:bg-amber-200',
  EXCUSED: 'bg-blue-100 text-blue-700 border-blue-300 hover:bg-blue-200',
};
const STATUSES: AttendanceStatus[] = ['PRESENT', 'ABSENT', 'LATE', 'EXCUSED'];

interface StudentRow {
  enrollment: EnrollmentResponse;
  studentName: string;
  attendance: AttendanceResponse | null;
}

export default function AttendancePage() {
  const { isAdmin, isTeacher, isGroup } = useAuth();
  const canEdit = isAdmin || isTeacher;

  const [courses, setCourses] = useState<CourseResponse[]>([]);
  const [semesters, setSemesters] = useState<SemesterResponse[]>([]);
  const [groups, setGroups] = useState<StudyGroupResponse[]>([]);
  const [sessions, setSessions] = useState<LessonSessionResponse[]>([]);

  const [selectedCourse, setSelectedCourse] = useState<number | null>(null);
  const [selectedSemester, setSelectedSemester] = useState<number | null>(null);
  const [selectedGroup, setSelectedGroup] = useState<number | null>(null);
  const [selectedSession, setSelectedSession] = useState<number | null>(null);

  const [students, setStudents] = useState<StudentRow[]>([]);
  const [journal, setJournal] = useState<AttendanceJournalResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState<number | null>(null);

  useEffect(() => {
    if (isGroup) {
      classProfilesApi.me().then(({ data }) => {
        setCourses([{ id: data.courseId, name: data.courseName, description: null, archived: false, createdAt: new Date().toISOString() }]);
        setSemesters([{ id: data.semesterId, courseId: data.courseId, name: data.semesterName, startDate: new Date().toISOString().slice(0, 10), endDate: new Date().toISOString().slice(0, 10), archived: false, createdAt: new Date().toISOString() }]);
        setGroups([{ id: data.groupId, semesterId: data.semesterId, teacherId: data.teacherId, name: data.groupName, scheduleNotes: null, archived: false, createdAt: new Date().toISOString() }]);
        setSelectedCourse(data.courseId);
        setSelectedSemester(data.semesterId);
        setSelectedGroup(data.groupId);
      }).catch(() => {});
      return;
    }
    coursesApi.list().then(({ data }) => { setCourses(data); if (data.length) setSelectedCourse(data[0].id); });
  }, [isGroup]);

  useEffect(() => {
    if (isGroup) return;
    if (selectedCourse) {
      semestersApi.listByCourse(selectedCourse).then(({ data }) => {
        setSemesters(data);
        setSelectedSemester(data.length ? data[0].id : null);
      });
    }
  }, [selectedCourse, isGroup]);

  useEffect(() => {
    if (isGroup) return;
    if (selectedSemester) {
      studyGroupsApi.listBySemester(selectedSemester).then(({ data }) => {
        setGroups(data);
        setSelectedGroup(data.length ? data[0].id : null);
      });
    } else { setGroups([]); setSelectedGroup(null); }
  }, [selectedSemester, isGroup]);

  useEffect(() => {
    if (selectedGroup) {
      lessonSessionsApi.listByGroup(selectedGroup).then(({ data }) => {
        const sorted = data.filter(s => !s.archived).sort((a, b) => new Date(b.startsAt).getTime() - new Date(a.startsAt).getTime());
        setSessions(sorted);
        setSelectedSession(sorted.length ? sorted[0].id : null);
      });
    } else { setSessions([]); setSelectedSession(null); }
  }, [selectedGroup]);

  useEffect(() => {
    if (selectedSession && selectedGroup) {
      loadAttendance();
    } else { setStudents([]); }
  }, [selectedSession]);

  const loadAttendance = async () => {
    if (!selectedSession || !selectedGroup) return;
    setLoading(true);
    try {
      let groupStudentsMap = new Map<number, string>();
      try {
        const { data: classProfile } = await classProfilesApi.byGroup(selectedGroup);
        groupStudentsMap = new Map(
          classProfile.students.map((s) => [s.id, [s.lastName, s.firstName, s.middleName].filter(Boolean).join(' ')])
        );
      } catch {
        groupStudentsMap = new Map();
      }

      const [enrollRes, attRes] = await Promise.all([
        enrollmentsApi.listByGroup(selectedGroup),
        attendanceApi.listBySession(selectedSession),
      ]);
      const activeEnrollments = enrollRes.data.filter(e => !e.archived && e.status === 'ACTIVE');
      const rows: StudentRow[] = await Promise.all(activeEnrollments.map(async (e) => {
        let studentName = groupStudentsMap.get(e.studentId) || 'Неизвестный слушатель';
        if (!groupStudentsMap.has(e.studentId) && !isGroup) {
          try {
            const { data: p } = await personsApi.get(e.studentId);
            studentName = [p.lastName, p.firstName, p.middleName].filter(Boolean).join(' ');
          } catch {}
        }
        const att = attRes.data.find(a => a.enrollmentId === e.id && !a.archived) || null;
        return { enrollment: e, studentName, attendance: att };
      }));
      rows.sort((a, b) => a.studentName.localeCompare(b.studentName));
      setStudents(rows);

      const session = sessions.find((s) => s.id === selectedSession);
      const baseDate = session ? new Date(session.startsAt) : new Date();
      const from = new Date(baseDate);
      from.setDate(baseDate.getDate() - ((baseDate.getDay() + 6) % 7));
      const to = new Date(from);
      to.setDate(from.getDate() + 6);
      const fromStr = from.toISOString().slice(0, 10);
      const toStr = to.toISOString().slice(0, 10);
      const journalRes = await attendanceApi.journal(selectedGroup, fromStr, toStr);
      setJournal(journalRes.data);
    } catch { toast('error', 'Ошибка загрузки'); }
    setLoading(false);
  };

  const markAttendance = async (row: StudentRow, status: AttendanceStatus) => {
    if (!selectedSession) return;
    let comment = row.attendance?.comment || undefined;
    if (status === 'LATE' || status === 'EXCUSED') {
      const reason = window.prompt('Укажите причину:', row.attendance?.comment || '');
      if (reason === null) return;
      const trimmed = reason.trim();
      if (!trimmed) {
        toast('error', 'Для выбранного статуса нужно указать причину');
        return;
      }
      comment = trimmed;
    } else {
      comment = undefined;
    }
    setSaving(row.enrollment.id);
    try {
      if (row.attendance) {
        await attendanceApi.update(row.attendance.id, { status, comment, archived: false });
      } else {
        await attendanceApi.create({ lessonSessionId: selectedSession, enrollmentId: row.enrollment.id, status, comment });
      }
      await loadAttendance();
      toast('success', `${row.studentName}: ${STATUS_LABELS[status]}`);
    } catch { toast('error', 'Ошибка сохранения'); }
    setSaving(null);
  };

  const fmtSession = (s: LessonSessionResponse) => {
    const d = new Date(s.startsAt);
    return `${d.toLocaleDateString('ru', { day: '2-digit', month: '2-digit' })} ${d.toLocaleTimeString('ru', { hour: '2-digit', minute: '2-digit' })} — ${s.topic || 'Без темы'}`;
  };

  const stats = {
    present: students.filter(s => s.attendance?.status === 'PRESENT').length,
    absent: students.filter(s => s.attendance?.status === 'ABSENT').length,
    late: students.filter(s => s.attendance?.status === 'LATE').length,
    excused: students.filter(s => s.attendance?.status === 'EXCUSED').length,
    unmarked: students.filter(s => !s.attendance).length,
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <div className="bg-teal-500 text-white p-2.5 rounded-lg"><CheckSquare className="w-5 h-5" /></div>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Посещаемость</h1>
          <p className="text-sm text-gray-500">{canEdit ? 'Отмечайте присутствие и пропуски студентов' : 'Просмотр посещаемости'}</p>
        </div>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">Курс</label>
            <select disabled={isGroup} value={selectedCourse || ''} onChange={(e) => setSelectedCourse(Number(e.target.value))} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500 disabled:bg-gray-50">
              {courses.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">Семестр</label>
            <select disabled={isGroup} value={selectedSemester || ''} onChange={(e) => setSelectedSemester(Number(e.target.value))} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500 disabled:bg-gray-50">
              {semesters.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">Группа</label>
            <select disabled={isGroup} value={selectedGroup || ''} onChange={(e) => setSelectedGroup(Number(e.target.value))} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500 disabled:bg-gray-50">
              {groups.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">Занятие</label>
            <select value={selectedSession || ''} onChange={(e) => setSelectedSession(Number(e.target.value))} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
              {sessions.map((s) => <option key={s.id} value={s.id}>{fmtSession(s)}</option>)}
            </select>
          </div>
        </div>
      </div>

      {selectedSession && students.length > 0 && (
        <div className="grid grid-cols-5 gap-3">
          <div className="bg-emerald-50 border border-emerald-200 rounded-lg p-3 text-center">
            <div className="text-2xl font-bold text-emerald-600">{stats.present}</div>
            <div className="text-xs text-emerald-700">Присутствуют</div>
          </div>
          <div className="bg-red-50 border border-red-200 rounded-lg p-3 text-center">
            <div className="text-2xl font-bold text-red-600">{stats.absent}</div>
            <div className="text-xs text-red-700">Отсутствуют</div>
          </div>
          <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 text-center">
            <div className="text-2xl font-bold text-amber-600">{stats.late}</div>
            <div className="text-xs text-amber-700">Опоздали</div>
          </div>
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 text-center">
            <div className="text-2xl font-bold text-blue-600">{stats.excused}</div>
            <div className="text-xs text-blue-700">Ув. причина</div>
          </div>
          <div className="bg-gray-50 border border-gray-200 rounded-lg p-3 text-center">
            <div className="text-2xl font-bold text-gray-600">{stats.unmarked}</div>
            <div className="text-xs text-gray-600">Не отмечены</div>
          </div>
        </div>
      )}

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="flex justify-center py-16"><div className="animate-spin rounded-full h-8 w-8 border-2 border-primary-600 border-t-transparent" /></div>
        ) : !selectedSession ? (
          <EmptyState message="Выберите занятие для просмотра посещаемости" />
        ) : students.length === 0 ? (
          <EmptyState message="Нет активных студентов в этой группе" />
        ) : (
          <div className="divide-y divide-gray-100">
            {students.map((row) => {
              const isSaving = saving === row.enrollment.id;
              const currentStatus = row.attendance?.status;
              return (
                <div key={row.enrollment.id} className={`flex items-center justify-between px-5 py-3 ${isSaving ? 'opacity-60' : ''}`}>
                  <div className="flex items-center gap-3 min-w-0">
                    <div className={`w-9 h-9 rounded-full flex items-center justify-center text-sm font-bold flex-shrink-0 ${
                      currentStatus === 'PRESENT' ? 'bg-emerald-100 text-emerald-600' :
                      currentStatus === 'ABSENT' ? 'bg-red-100 text-red-600' :
                      currentStatus === 'LATE' ? 'bg-amber-100 text-amber-600' :
                      currentStatus === 'EXCUSED' ? 'bg-blue-100 text-blue-600' :
                      'bg-gray-100 text-gray-400'
                    }`}>
                      {row.studentName.split(' ').map(w => w[0]).join('').slice(0, 2)}
                    </div>
                    <div className="min-w-0">
                      <div className="font-medium text-gray-900 text-sm truncate">{row.studentName}</div>
                      <div className="text-xs text-gray-400">
                        {currentStatus ? STATUS_LABELS[currentStatus] : 'Не отмечен'}
                        {row.attendance?.comment && ` — ${row.attendance.comment}`}
                      </div>
                    </div>
                  </div>
                  {canEdit && (
                    <div className="flex items-center gap-1.5 flex-shrink-0">
                      {STATUSES.map((s) => {
                        const Icon = STATUS_ICONS[s];
                        const isActive = currentStatus === s;
                        return (
                          <button
                            key={s}
                            onClick={() => markAttendance(row, s)}
                            disabled={isSaving}
                            className={`p-2 rounded-lg border text-xs font-medium transition-all ${
                              isActive ? STATUS_STYLES[s] + ' ring-2 ring-offset-1 ring-current' : 'bg-white border-gray-200 text-gray-400 hover:border-gray-300 hover:text-gray-600'
                            }`}
                            title={STATUS_LABELS[s]}
                          >
                            <Icon className="w-4 h-4" />
                          </button>
                        );
                      })}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>

      {journal && journal.lessons.length > 0 && (
        <div className="bg-white rounded-xl border border-gray-200 p-4 overflow-x-auto">
          <h3 className="text-sm font-semibold text-gray-700 mb-3">
            Журнал по дням занятий ({new Date(journal.fromDate).toLocaleDateString('ru')} - {new Date(journal.toDate).toLocaleDateString('ru')})
          </h3>
          <table className="min-w-full text-xs border border-gray-200">
            <thead className="bg-gray-50">
            <tr>
              <th className="px-2 py-2 border text-left">Ученик</th>
              {journal.lessons.map((l) => (
                <th key={l.lessonSessionId} className="px-2 py-2 border">
                  {new Date(l.lessonDate).toLocaleDateString('ru', { day: '2-digit', month: '2-digit' })}
                </th>
              ))}
            </tr>
            </thead>
            <tbody>
            {journal.students.map((s) => (
              <tr key={s.enrollmentId}>
                <td className="px-2 py-2 border font-medium">{s.studentFullName}</td>
                {s.attendance.map((c) => (
                  <td key={`${s.enrollmentId}-${c.lessonSessionId}`} className="px-2 py-2 border text-center">
                    {c.status === 'PRESENT' ? 'П' : c.status === 'ABSENT' ? 'Н' : c.status === 'LATE' ? 'О' : c.status === 'EXCUSED' ? 'У' : '—'}
                  </td>
                ))}
              </tr>
            ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
