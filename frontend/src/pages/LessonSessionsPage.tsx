import { useEffect, useState, type FormEvent } from 'react';
import { lessonSessionsApi, studyGroupsApi, semestersApi, coursesApi, teachersApi, personsApi } from '../api';
import type { LessonSessionResponse, StudyGroupResponse, SemesterResponse, CourseResponse } from '../types';
import { useAuth } from '../context/AuthContext';
import Modal from '../components/ui/Modal';
import Badge from '../components/ui/Badge';
import EmptyState from '../components/ui/EmptyState';
import { toast } from '../components/ui/Toast';
import { Plus, Pencil, Archive, ArchiveRestore, Clock, CalendarDays, MapPin, GraduationCap, XCircle, CheckCircle } from 'lucide-react';

export default function LessonSessionsPage() {
  const { isAdmin, isTeacher } = useAuth();
  const canManageStatus = isAdmin || isTeacher;
  const [courses, setCourses] = useState<CourseResponse[]>([]);
  const [semesters, setSemesters] = useState<SemesterResponse[]>([]);
  const [groups, setGroups] = useState<StudyGroupResponse[]>([]);
  const [selectedCourse, setSelectedCourse] = useState<number | null>(null);
  const [selectedSemester, setSelectedSemester] = useState<number | null>(null);
  const [selectedGroup, setSelectedGroup] = useState<number | null>(null);
  const [sessions, setSessions] = useState<LessonSessionResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<LessonSessionResponse | null>(null);
  const [form, setForm] = useState({
    groupId: 0, teacherId: '', startsAt: '', endsAt: '', actualStartsAt: '', actualEndsAt: '', topic: '', room: '',
  });
  const [teacherNames, setTeacherNames] = useState<Record<number, string>>({});
  const [showArchivedOnly, setShowArchivedOnly] = useState(false);

  useEffect(() => { coursesApi.list().then(({ data }) => { setCourses(data); if (data.length) setSelectedCourse(data[0].id); }); }, []);

  useEffect(() => {
    if (selectedCourse) {
      semestersApi.listByCourse(selectedCourse).then(({ data }) => {
        setSemesters(data);
        setSelectedSemester(data.length ? data[0].id : null);
      });
    }
  }, [selectedCourse]);

  useEffect(() => {
    if (selectedSemester) {
      studyGroupsApi.listBySemester(selectedSemester).then(({ data }) => {
        setGroups(data);
        setSelectedGroup(data.length ? data[0].id : null);
        resolveTeacherNames(data);
      });
    } else { setGroups([]); setSelectedGroup(null); }
  }, [selectedSemester]);

  useEffect(() => {
    if (selectedGroup) {
      setLoading(true);
      lessonSessionsApi.listByGroup(selectedGroup).then(({ data }) => {
        const sorted = [...data].sort((a, b) => new Date(a.startsAt).getTime() - new Date(b.startsAt).getTime());
        setSessions(sorted);
      }).finally(() => setLoading(false));
    } else { setSessions([]); }
  }, [selectedGroup]);

  const resolveTeacherNames = async (grps: StudyGroupResponse[]) => {
    const tIds = [...new Set(grps.map(g => g.teacherId).filter(Boolean))] as number[];
    const names: Record<number, string> = {};
    await Promise.all(tIds.map(async (tid) => {
      try {
        const { data: t } = await teachersApi.get(tid);
        const { data: p } = await personsApi.get(t.personId);
        names[tid] = [p.lastName, p.firstName].filter(Boolean).join(' ');
      } catch { names[tid] = `#${tid}`; }
    }));
    setTeacherNames(prev => ({ ...prev, ...names }));
  };

  const reload = async () => {
    if (selectedGroup) {
      const { data } = await lessonSessionsApi.listByGroup(selectedGroup);
      const sorted = [...data].sort((a, b) => new Date(a.startsAt).getTime() - new Date(b.startsAt).getTime());
      setSessions(sorted);
    }
  };

  const openCreate = () => {
    const g = groups.find(gr => gr.id === selectedGroup);
    setEditing(null);
    setForm({
      groupId: selectedGroup || 0,
      teacherId: g?.teacherId?.toString() || '',
      startsAt: '', endsAt: '', actualStartsAt: '', actualEndsAt: '', topic: '', room: '',
    });
    setModalOpen(true);
  };

  const openEdit = (s: LessonSessionResponse) => {
    setEditing(s);
    setForm({
      groupId: s.groupId,
      teacherId: s.teacherId?.toString() || '',
      startsAt: s.startsAt.slice(0, 16),
      endsAt: s.endsAt.slice(0, 16),
      actualStartsAt: s.actualStartsAt ? s.actualStartsAt.slice(0, 16) : '',
      actualEndsAt: s.actualEndsAt ? s.actualEndsAt.slice(0, 16) : '',
      topic: s.topic || '',
      room: s.room || '',
    });
    setModalOpen(true);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    try {
      const payload = {
        groupId: form.groupId,
        teacherId: form.teacherId ? Number(form.teacherId) : undefined,
        startsAt: new Date(form.startsAt).toISOString(),
        endsAt: new Date(form.endsAt).toISOString(),
        actualStartsAt: form.actualStartsAt ? new Date(form.actualStartsAt).toISOString() : undefined,
        actualEndsAt: form.actualEndsAt ? new Date(form.actualEndsAt).toISOString() : undefined,
        topic: form.topic || undefined,
        room: form.room || undefined,
      };
      if (editing) {
        await lessonSessionsApi.update(editing.id, { ...payload, canceled: editing.canceled, archived: editing.archived });
        toast('success', 'Занятие обновлено');
      } else {
        await lessonSessionsApi.create(payload);
        toast('success', 'Занятие создано');
      }
      setModalOpen(false);
      reload();
    } catch { toast('error', 'Ошибка сохранения'); }
  };

  const toggleArchive = async (s: LessonSessionResponse) => {
    try {
      await lessonSessionsApi.archive(s.id, { archived: !s.archived });
      toast('success', s.archived ? 'Занятие восстановлено' : 'Занятие архивировано');
      reload();
    } catch { toast('error', 'Ошибка'); }
  };

  const toggleCancel = async (s: LessonSessionResponse) => {
    try {
      await lessonSessionsApi.patchStatus(s.id, {
        canceled: !s.canceled,
        actualStartsAt: s.actualStartsAt ?? undefined,
        actualEndsAt: s.actualEndsAt ?? undefined,
      });
      toast('success', s.canceled ? 'Занятие возобновлено' : 'Занятие отменено');
      reload();
    } catch { toast('error', 'Ошибка'); }
  };

  const fmtDate = (iso: string) => {
    const d = new Date(iso);
    return d.toLocaleDateString('ru', { weekday: 'short', day: '2-digit', month: '2-digit', year: 'numeric' });
  };
  const fmtTime = (iso: string) => new Date(iso).toLocaleTimeString('ru', { hour: '2-digit', minute: '2-digit' });

  const groupedByDate: Record<string, LessonSessionResponse[]> = {};
  const visibleSessions = showArchivedOnly
    ? sessions.filter((s) => s.archived)
    : sessions.filter((s) => !s.archived);
  visibleSessions.forEach(s => {
    const key = fmtDate(s.startsAt);
    if (!groupedByDate[key]) groupedByDate[key] = [];
    groupedByDate[key].push(s);
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-cyan-500 text-white p-2.5 rounded-lg"><Clock className="w-5 h-5" /></div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Занятия</h1>
            <p className="text-sm text-gray-500">{isAdmin ? 'Создание и управление занятиями' : 'Просмотр расписания занятий'}</p>
          </div>
        </div>
        {isAdmin && (
          <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium">
            <Plus className="w-4 h-4" /> Добавить занятие
          </button>
        )}
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-4">
        <div className="grid grid-cols-1 sm:grid-cols-4 gap-3">
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">Курс</label>
            <select value={selectedCourse || ''} onChange={(e) => setSelectedCourse(Number(e.target.value))} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
              {courses.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">Семестр</label>
            <select value={selectedSemester || ''} onChange={(e) => setSelectedSemester(Number(e.target.value))} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
              {semesters.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">Группа</label>
            <select value={selectedGroup || ''} onChange={(e) => setSelectedGroup(Number(e.target.value))} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
              {groups.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">Архив</label>
            <button
              onClick={() => setShowArchivedOnly((v) => !v)}
              className={`w-full px-3 py-2 rounded-lg border text-sm transition-colors ${
                showArchivedOnly
                  ? 'border-amber-300 bg-amber-50 text-amber-700'
                  : 'border-gray-300 bg-white text-gray-700 hover:bg-gray-50'
              }`}
            >
              {showArchivedOnly ? 'Показывать только архивные' : 'Скрыть архивные'}
            </button>
          </div>
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center py-16"><div className="animate-spin rounded-full h-8 w-8 border-2 border-primary-600 border-t-transparent" /></div>
      ) : visibleSessions.length === 0 ? (
        <div className="bg-white rounded-xl border border-gray-200">
          <EmptyState message={showArchivedOnly ? 'Архивные занятия не найдены для выбранной группы' : 'Активные занятия не найдены для выбранной группы'} />
        </div>
      ) : (
        <div className="space-y-4">
          {Object.entries(groupedByDate).map(([date, dayLessons]) => (
            <div key={date}>
              <div className="flex items-center gap-2 mb-2">
                <CalendarDays className="w-4 h-4 text-gray-400" />
                <h3 className="text-sm font-semibold text-gray-600">{date}</h3>
                <span className="text-xs text-gray-400">({dayLessons.length} зан.)</span>
              </div>
              <div className="space-y-2">
                {dayLessons.map((s) => (
                  <div key={s.id} className={`bg-white rounded-xl border ${s.canceled ? 'border-red-200 bg-red-50/30' : s.archived ? 'border-gray-200 bg-gray-50/50' : 'border-gray-200'} p-4`}>
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-4">
                        <div className={`text-center min-w-[60px] ${s.canceled ? 'text-red-400 line-through' : 'text-gray-900'}`}>
                          <div className="text-lg font-bold">{fmtTime(s.startsAt)}</div>
                          <div className="text-xs text-gray-400">{fmtTime(s.endsAt)}</div>
                        </div>
                        <div className="h-10 w-px bg-gray-200" />
                        <div>
                          <div className={`font-medium ${s.canceled ? 'text-red-400 line-through' : 'text-gray-900'}`}>
                            {s.topic || 'Без темы'}
                          </div>
                          <div className="flex items-center gap-3 mt-1 text-xs text-gray-500">
                            {s.room && (
                              <span className="flex items-center gap-1"><MapPin className="w-3 h-3" /> {s.room}</span>
                            )}
                            {s.teacherId && (
                              <span className="flex items-center gap-1"><GraduationCap className="w-3 h-3" /> {teacherNames[s.teacherId] || `#${s.teacherId}`}</span>
                            )}
                          </div>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        {s.canceled ? <Badge variant="red">Отменено</Badge> : s.archived ? <Badge variant="gray">Архив</Badge> : <Badge variant="green">Активно</Badge>}
                        {canManageStatus && (
                          <div className="flex items-center gap-1 ml-2">
                            {isAdmin && (
                              <button onClick={() => openEdit(s)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-400" title="Редактировать"><Pencil className="w-4 h-4" /></button>
                            )}
                            <button onClick={() => toggleCancel(s)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-400" title={s.canceled ? 'Возобновить' : 'Отменить'}>
                              {s.canceled ? <CheckCircle className="w-4 h-4 text-emerald-500" /> : <XCircle className="w-4 h-4 text-red-400" />}
                            </button>
                            {isAdmin && (
                              <button onClick={() => toggleArchive(s)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-400" title={s.archived ? 'Восстановить' : 'Архивировать'}>
                                {s.archived ? <ArchiveRestore className="w-4 h-4" /> : <Archive className="w-4 h-4" />}
                              </button>
                            )}
                          </div>
                        )}
                      </div>
                    </div>
                    {s.actualStartsAt && s.actualEndsAt && (
                      <div className="mt-2 text-xs text-gray-500">
                        Факт: {fmtDate(s.actualStartsAt)} {fmtTime(s.actualStartsAt)}-{fmtTime(s.actualEndsAt)}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Редактировать занятие' : 'Новое занятие'} wide>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Группа</label>
              <select value={form.groupId} onChange={(e) => {
                const gId = Number(e.target.value);
                const g = groups.find(gr => gr.id === gId);
                setForm({ ...form, groupId: gId, teacherId: g?.teacherId?.toString() || form.teacherId });
              }} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
                {groups.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Преподаватель (ID)</label>
              <input value={form.teacherId} onChange={(e) => setForm({ ...form, teacherId: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" placeholder="Авто из группы" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Начало *</label>
              <input type="datetime-local" value={form.startsAt} onChange={(e) => setForm({ ...form, startsAt: e.target.value })} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Конец *</label>
              <input type="datetime-local" value={form.endsAt} onChange={(e) => setForm({ ...form, endsAt: e.target.value })} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Фактическое начало</label>
              <input type="datetime-local" value={form.actualStartsAt} onChange={(e) => setForm({ ...form, actualStartsAt: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Фактический конец</label>
              <input type="datetime-local" value={form.actualEndsAt} onChange={(e) => setForm({ ...form, actualEndsAt: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Тема</label>
              <input value={form.topic} onChange={(e) => setForm({ ...form, topic: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" placeholder="Тема занятия" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Аудитория</label>
              <input value={form.room} onChange={(e) => setForm({ ...form, room: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" placeholder="Номер аудитории" />
            </div>
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setModalOpen(false)} className="px-4 py-2 text-sm text-gray-600">Отмена</button>
            <button type="submit" className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium">{editing ? 'Сохранить' : 'Создать'}</button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
