import { useEffect, useState, type FormEvent } from 'react';
import { studyGroupsApi, semestersApi, coursesApi, enrollmentsApi, personsApi, teachersApi, lessonSessionsApi } from '../api';
import type { StudyGroupResponse, SemesterResponse, CourseResponse, EnrollmentResponse, PersonResponse, LessonSessionResponse } from '../types';
import { useAuth } from '../context/AuthContext';
import Modal from '../components/ui/Modal';
import Badge from '../components/ui/Badge';
import EmptyState from '../components/ui/EmptyState';
import { toast } from '../components/ui/Toast';
import { Plus, Pencil, Archive, ArchiveRestore, Users, ChevronDown, ChevronUp, GraduationCap, UserCheck, Clock, X } from 'lucide-react';

interface GroupDetail {
  teacherName: string | null;
  enrollments: (EnrollmentResponse & { studentName?: string })[];
  lessons: LessonSessionResponse[];
}

const STATUS_LABELS: Record<string, string> = { APPLICANT: 'Заявка', ACTIVE: 'Активен', COMPLETED: 'Завершён' };
const STATUS_COLORS: Record<string, 'yellow' | 'green' | 'blue'> = { APPLICANT: 'yellow', ACTIVE: 'green', COMPLETED: 'blue' };

export default function StudyGroupsPage() {
  const { isAdmin } = useAuth();
  const [courses, setCourses] = useState<CourseResponse[]>([]);
  const [semesters, setSemesters] = useState<SemesterResponse[]>([]);
  const [selectedCourse, setSelectedCourse] = useState<number | null>(null);
  const [selectedSemester, setSelectedSemester] = useState<number | null>(null);
  const [groups, setGroups] = useState<StudyGroupResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<StudyGroupResponse | null>(null);
  const [form, setForm] = useState({ semesterId: 0, teacherId: '', name: '', scheduleNotes: '' });
  const [expandedGroup, setExpandedGroup] = useState<number | null>(null);
  const [details, setDetails] = useState<Record<number, GroupDetail>>({});
  const [detailLoading, setDetailLoading] = useState<number | null>(null);
  const [teacherNames, setTeacherNames] = useState<Record<number, string>>({});

  useEffect(() => {
    coursesApi.list().then(({ data }) => {
      setCourses(data);
      if (data.length > 0) setSelectedCourse(data[0].id);
    });
  }, []);

  useEffect(() => {
    if (selectedCourse) {
      semestersApi.listByCourse(selectedCourse).then(({ data }) => {
        setSemesters(data);
        if (data.length > 0) setSelectedSemester(data[0].id);
        else { setSelectedSemester(null); setGroups([]); }
      });
    }
  }, [selectedCourse]);

  useEffect(() => {
    if (selectedSemester) {
      setLoading(true);
      studyGroupsApi.listBySemester(selectedSemester).then(({ data }) => {
        setGroups(data);
        resolveTeacherNames(data);
      }).finally(() => setLoading(false));
    }
  }, [selectedSemester]);

  const resolveTeacherNames = async (grps: StudyGroupResponse[]) => {
    const tIds = [...new Set(grps.map(g => g.teacherId).filter(Boolean))] as number[];
    const names: Record<number, string> = {};
    await Promise.all(tIds.map(async (tid) => {
      try {
        const { data: t } = await teachersApi.get(tid);
        const { data: p } = await personsApi.get(t.personId);
        names[tid] = [p.lastName, p.firstName, p.middleName].filter(Boolean).join(' ');
      } catch { names[tid] = `#${tid}`; }
    }));
    setTeacherNames(prev => ({ ...prev, ...names }));
  };

  const loadGroupDetail = async (groupId: number) => {
    if (expandedGroup === groupId) { setExpandedGroup(null); return; }
    setExpandedGroup(groupId);
    if (details[groupId]) return;
    setDetailLoading(groupId);
    try {
      const [enrollRes, lessonsRes] = await Promise.all([
        enrollmentsApi.listByGroup(groupId),
        lessonSessionsApi.listByGroup(groupId),
      ]);
      const group = groups.find(g => g.id === groupId);
      let teacherName: string | null = null;
      if (group?.teacherId && teacherNames[group.teacherId]) {
        teacherName = teacherNames[group.teacherId];
      }
      const enriched = await Promise.all(enrollRes.data.map(async (e) => {
        try {
          const { data: p } = await personsApi.get(e.studentId);
          return { ...e, studentName: [p.lastName, p.firstName, p.middleName].filter(Boolean).join(' ') };
        } catch { return { ...e, studentName: `Персона #${e.studentId}` }; }
      }));
      setDetails(prev => ({ ...prev, [groupId]: { teacherName, enrollments: enriched, lessons: lessonsRes.data } }));
    } catch { toast('error', 'Не удалось загрузить данные группы'); }
    setDetailLoading(null);
  };

  const reload = async () => {
    if (selectedSemester) {
      const { data } = await studyGroupsApi.listBySemester(selectedSemester);
      setGroups(data);
      resolveTeacherNames(data);
      setDetails({});
      setExpandedGroup(null);
    }
  };

  const openCreate = () => {
    setEditing(null);
    setForm({ semesterId: selectedSemester || 0, teacherId: '', name: '', scheduleNotes: '' });
    setModalOpen(true);
  };

  const openEdit = (g: StudyGroupResponse) => {
    setEditing(g);
    setForm({ semesterId: g.semesterId, teacherId: g.teacherId?.toString() || '', name: g.name, scheduleNotes: g.scheduleNotes || '' });
    setModalOpen(true);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    try {
      const payload = { semesterId: form.semesterId, teacherId: form.teacherId ? Number(form.teacherId) : undefined, name: form.name, scheduleNotes: form.scheduleNotes || undefined };
      if (editing) { await studyGroupsApi.update(editing.id, { ...payload, archived: editing.archived }); toast('success', 'Группа обновлена'); }
      else { await studyGroupsApi.create(payload); toast('success', 'Группа создана'); }
      setModalOpen(false);
      reload();
    } catch { toast('error', 'Ошибка сохранения'); }
  };

  const toggleArchive = async (g: StudyGroupResponse) => {
    try {
      await studyGroupsApi.archive(g.id, { archived: !g.archived });
      toast('success', g.archived ? 'Группа восстановлена' : 'Группа архивирована');
      reload();
    } catch { toast('error', 'Ошибка'); }
  };

  const fmtDate = (iso: string) => new Date(iso).toLocaleString('ru', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-violet-500 text-white p-2.5 rounded-lg"><Users className="w-5 h-5" /></div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Учебные группы</h1>
            <p className="text-sm text-gray-500">{isAdmin ? 'Управление группами, студентами, занятиями' : 'Просмотр групп'}</p>
          </div>
        </div>
        {isAdmin && (
          <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium">
            <Plus className="w-4 h-4" /> Добавить группу
          </button>
        )}
      </div>

      <div className="flex items-center gap-4 bg-white rounded-lg border border-gray-200 px-4 py-3">
        <div className="flex items-center gap-2">
          <label className="text-sm font-medium text-gray-600">Курс:</label>
          <select value={selectedCourse || ''} onChange={(e) => setSelectedCourse(Number(e.target.value))} className="px-3 py-1.5 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
            {courses.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>
        <div className="flex items-center gap-2">
          <label className="text-sm font-medium text-gray-600">Семестр:</label>
          <select value={selectedSemester || ''} onChange={(e) => setSelectedSemester(Number(e.target.value))} className="px-3 py-1.5 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
            {semesters.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center py-16"><div className="animate-spin rounded-full h-8 w-8 border-2 border-primary-600 border-t-transparent" /></div>
      ) : groups.length === 0 ? (
        <div className="bg-white rounded-xl border border-gray-200"><EmptyState message="Группы не найдены" /></div>
      ) : (
        <div className="space-y-3">
          {groups.map((g) => {
            const isExpanded = expandedGroup === g.id;
            const detail = details[g.id];
            const isLoadingDetail = detailLoading === g.id;
            return (
              <div key={g.id} className="bg-white rounded-xl border border-gray-200 overflow-hidden">
                <div
                  className="flex items-center justify-between px-5 py-4 cursor-pointer hover:bg-gray-50 transition-colors"
                  onClick={() => loadGroupDetail(g.id)}
                >
                  <div className="flex items-center gap-4">
                    <div className="bg-violet-100 text-violet-600 p-2 rounded-lg">
                      <Users className="w-5 h-5" />
                    </div>
                    <div>
                      <div className="font-semibold text-gray-900">{g.name}</div>
                      <div className="text-sm text-gray-500 flex items-center gap-3 mt-0.5">
                        <span className="flex items-center gap-1">
                          <GraduationCap className="w-3.5 h-3.5" />
                          {g.teacherId ? (teacherNames[g.teacherId] || `Преподаватель #${g.teacherId}`) : 'Без преподавателя'}
                        </span>
                        {g.scheduleNotes && <span className="text-gray-400">| {g.scheduleNotes}</span>}
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <Badge variant={g.archived ? 'gray' : 'green'}>{g.archived ? 'Архив' : 'Активна'}</Badge>
                    {isAdmin && (
                      <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
                        <button onClick={() => openEdit(g)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-400"><Pencil className="w-4 h-4" /></button>
                        <button onClick={() => toggleArchive(g)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-400">
                          {g.archived ? <ArchiveRestore className="w-4 h-4" /> : <Archive className="w-4 h-4" />}
                        </button>
                      </div>
                    )}
                    {isExpanded ? <ChevronUp className="w-5 h-5 text-gray-400" /> : <ChevronDown className="w-5 h-5 text-gray-400" />}
                  </div>
                </div>

                {isExpanded && (
                  <div className="border-t border-gray-100 px-5 py-4 bg-gray-50/50">
                    {isLoadingDetail ? (
                      <div className="flex justify-center py-8"><div className="animate-spin rounded-full h-6 w-6 border-2 border-primary-600 border-t-transparent" /></div>
                    ) : detail ? (
                      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                        <div>
                          <h4 className="text-sm font-semibold text-gray-700 mb-3 flex items-center gap-2">
                            <UserCheck className="w-4 h-4 text-emerald-500" />
                            Студенты ({detail.enrollments.filter(e => !e.archived).length})
                          </h4>
                          {detail.enrollments.filter(e => !e.archived).length === 0 ? (
                            <p className="text-sm text-gray-400 italic">Нет записанных студентов</p>
                          ) : (
                            <div className="space-y-1.5">
                              {detail.enrollments.filter(e => !e.archived).map((e) => (
                                <div key={e.id} className="flex items-center justify-between bg-white rounded-lg px-3 py-2 border border-gray-100">
                                  <div className="flex items-center gap-2">
                                    <div className="w-7 h-7 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center text-xs font-bold">
                                      {e.studentName?.[0] || '?'}
                                    </div>
                                    <span className="text-sm font-medium text-gray-800">{e.studentName}</span>
                                  </div>
                                  <div className="flex items-center gap-2">
                                    <Badge variant="purple">{e.level}</Badge>
                                    <Badge variant={STATUS_COLORS[e.status]}>{STATUS_LABELS[e.status]}</Badge>
                                  </div>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                        <div>
                          <h4 className="text-sm font-semibold text-gray-700 mb-3 flex items-center gap-2">
                            <Clock className="w-4 h-4 text-cyan-500" />
                            Занятия ({detail.lessons.filter(l => !l.archived).length})
                          </h4>
                          {detail.lessons.filter(l => !l.archived).length === 0 ? (
                            <p className="text-sm text-gray-400 italic">Нет запланированных занятий</p>
                          ) : (
                            <div className="space-y-1.5">
                              {detail.lessons.filter(l => !l.archived).slice(0, 8).map((l) => (
                                <div key={l.id} className="flex items-center justify-between bg-white rounded-lg px-3 py-2 border border-gray-100">
                                  <div>
                                    <span className="text-sm font-medium text-gray-800">{fmtDate(l.startsAt)}</span>
                                    <span className="text-xs text-gray-400 ml-2">{l.topic || 'Без темы'}</span>
                                  </div>
                                  <div className="flex items-center gap-2">
                                    {l.room && <span className="text-xs text-gray-500 bg-gray-100 px-2 py-0.5 rounded">{l.room}</span>}
                                    {l.canceled ? <Badge variant="red">Отмена</Badge> : <Badge variant="green">Активно</Badge>}
                                  </div>
                                </div>
                              ))}
                              {detail.lessons.filter(l => !l.archived).length > 8 && (
                                <p className="text-xs text-gray-400 text-center pt-1">...и ещё {detail.lessons.filter(l => !l.archived).length - 8}</p>
                              )}
                            </div>
                          )}
                        </div>
                      </div>
                    ) : null}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Редактировать группу' : 'Новая группа'}>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Название *</label>
            <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" placeholder="Например: Группа A-1" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Семестр</label>
            <select value={form.semesterId} onChange={(e) => setForm({ ...form, semesterId: Number(e.target.value) })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
              {semesters.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">ID преподавателя</label>
            <input value={form.teacherId} onChange={(e) => setForm({ ...form, teacherId: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" placeholder="Необязательно" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Заметки по расписанию</label>
            <textarea value={form.scheduleNotes} onChange={(e) => setForm({ ...form, scheduleNotes: e.target.value })} rows={2} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setModalOpen(false)} className="px-4 py-2 text-sm text-gray-600">Отмена</button>
            <button type="submit" className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium">Сохранить</button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
