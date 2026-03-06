import { useEffect, useState, type FormEvent } from 'react';
import { classProfilesApi, enrollmentsApi, studyGroupsApi, semestersApi, coursesApi, personsApi } from '../api';
import type { EnrollmentResponse, StudyGroupResponse, SemesterResponse, CourseResponse, EnrollmentStatus, ChineseLevel, PersonResponse } from '../types';
import { useAuth } from '../context/AuthContext';
import Modal from '../components/ui/Modal';
import Badge from '../components/ui/Badge';
import EmptyState from '../components/ui/EmptyState';
import { toast } from '../components/ui/Toast';
import { Plus, Pencil, Archive, ArchiveRestore, ClipboardList, UserPlus, Search, ChevronDown } from 'lucide-react';

const STATUS_LABELS: Record<EnrollmentStatus, string> = { APPLICANT: 'Заявка', ACTIVE: 'Активен', COMPLETED: 'Завершён' };
const STATUS_COLORS: Record<EnrollmentStatus, 'yellow' | 'green' | 'blue'> = { APPLICANT: 'yellow', ACTIVE: 'green', COMPLETED: 'blue' };
const LEVELS: ChineseLevel[] = ['HSK1', 'HSK2', 'HSK3', 'HSK4', 'HSK5', 'HSK6'];
const STATUSES: EnrollmentStatus[] = ['APPLICANT', 'ACTIVE', 'COMPLETED'];

interface EnrichedEnrollment extends EnrollmentResponse {
  studentName?: string;
  payerName?: string;
}

export default function EnrollmentsPage() {
  const { isAdmin, isGroup } = useAuth();
  const [courses, setCourses] = useState<CourseResponse[]>([]);
  const [semesters, setSemesters] = useState<SemesterResponse[]>([]);
  const [groups, setGroups] = useState<StudyGroupResponse[]>([]);
  const [selectedCourse, setSelectedCourse] = useState<number | null>(null);
  const [selectedSemester, setSelectedSemester] = useState<number | null>(null);
  const [selectedGroup, setSelectedGroup] = useState<number | null>(null);
  const [enrollments, setEnrollments] = useState<EnrichedEnrollment[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<EnrollmentResponse | null>(null);
  const [form, setForm] = useState({ studentId: '', payerId: '', semesterId: 0, groupId: '', status: 'APPLICANT' as EnrollmentStatus, level: 'HSK1' as ChineseLevel });
  const [personSearch, setPersonSearch] = useState('');
  const [personResults, setPersonResults] = useState<PersonResponse[]>([]);
  const [changingStatus, setChangingStatus] = useState<number | null>(null);

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
  }, [selectedCourse]);

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
    if (selectedGroup) loadEnrollments();
    else setEnrollments([]);
  }, [selectedGroup]);

  const loadEnrollments = async () => {
    if (!selectedGroup) return;
    setLoading(true);
    try {
      const { data } = await enrollmentsApi.listByGroup(selectedGroup);
      const enriched: EnrichedEnrollment[] = await Promise.all(data.map(async (e) => {
        let studentName: string | undefined;
        let payerName: string | undefined;
        try {
          const { data: p } = await personsApi.get(e.studentId);
          studentName = [p.lastName, p.firstName, p.middleName].filter(Boolean).join(' ');
        } catch { studentName = `#${e.studentId}`; }
        if (e.payerId) {
          try {
            const { data: p } = await personsApi.get(e.payerId);
            payerName = [p.lastName, p.firstName].filter(Boolean).join(' ');
          } catch { payerName = `#${e.payerId}`; }
        }
        return { ...e, studentName, payerName };
      }));
      setEnrollments(enriched);
    } catch { toast('error', 'Ошибка загрузки'); }
    setLoading(false);
  };

  const changeStatus = async (e: EnrichedEnrollment, newStatus: EnrollmentStatus) => {
    setChangingStatus(e.id);
    try {
      await enrollmentsApi.update(e.id, {
        payerId: e.payerId ?? undefined,
        semesterId: e.semesterId,
        groupId: e.groupId ?? undefined,
        status: newStatus,
        level: e.level,
        archived: e.archived,
      });
      toast('success', `Статус изменён: ${STATUS_LABELS[newStatus]}`);
      loadEnrollments();
    } catch { toast('error', 'Ошибка изменения статуса'); }
    setChangingStatus(null);
  };

  const changeLevel = async (e: EnrichedEnrollment, newLevel: ChineseLevel) => {
    try {
      await enrollmentsApi.update(e.id, {
        payerId: e.payerId ?? undefined,
        semesterId: e.semesterId,
        groupId: e.groupId ?? undefined,
        status: e.status,
        level: newLevel,
        archived: e.archived,
      });
      toast('success', `Уровень изменён: ${newLevel}`);
      loadEnrollments();
    } catch { toast('error', 'Ошибка'); }
  };

  const searchPersons = async () => {
    if (personSearch.trim().length < 1) return;
    try {
      const { data } = await personsApi.searchByLastName(personSearch.trim());
      setPersonResults(data);
    } catch { setPersonResults([]); }
  };

  const openCreate = () => {
    setEditing(null);
    setForm({ studentId: '', payerId: '', semesterId: selectedSemester || 0, groupId: selectedGroup?.toString() || '', status: 'ACTIVE', level: 'HSK1' });
    setPersonSearch('');
    setPersonResults([]);
    setModalOpen(true);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    try {
      if (editing) {
        await enrollmentsApi.update(editing.id, {
          payerId: form.payerId ? Number(form.payerId) : undefined,
          semesterId: form.semesterId, groupId: form.groupId ? Number(form.groupId) : undefined,
          status: form.status, level: form.level, archived: editing.archived,
        });
        toast('success', 'Запись обновлена');
      } else {
        await enrollmentsApi.create({
          studentId: Number(form.studentId), payerId: form.payerId ? Number(form.payerId) : undefined,
          semesterId: form.semesterId, groupId: form.groupId ? Number(form.groupId) : undefined,
          status: form.status, level: form.level,
        });
        toast('success', 'Студент записан');
      }
      setModalOpen(false);
      loadEnrollments();
    } catch { toast('error', 'Ошибка сохранения'); }
  };

  const toggleArchive = async (e: EnrollmentResponse) => {
    try {
      await enrollmentsApi.archive(e.id, { archived: !e.archived });
      toast('success', e.archived ? 'Запись восстановлена' : 'Запись архивирована');
      loadEnrollments();
    } catch { toast('error', 'Ошибка'); }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-rose-500 text-white p-2.5 rounded-lg"><ClipboardList className="w-5 h-5" /></div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Записи на курсы</h1>
          <p className="text-sm text-gray-500">{isAdmin ? 'Управление записями студентов в группы' : 'Просмотр записей'}</p>
          </div>
        </div>
        {isAdmin && (
          <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium">
            <UserPlus className="w-4 h-4" /> Записать студента
          </button>
        )}
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-4">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
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
        </div>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="flex justify-center py-16"><div className="animate-spin rounded-full h-8 w-8 border-2 border-primary-600 border-t-transparent" /></div>
        ) : enrollments.length === 0 ? (
          <EmptyState message="Записи не найдены для выбранной группы" />
        ) : (
          <div className="divide-y divide-gray-100">
            {enrollments.map((e) => {
              const isBusy = changingStatus === e.id;
              return (
                <div key={e.id} className={`flex items-center justify-between px-5 py-3.5 hover:bg-gray-50 transition-colors ${e.archived ? 'opacity-50' : ''} ${isBusy ? 'opacity-60 pointer-events-none' : ''}`}>
                  <div className="flex items-center gap-3 min-w-0">
                    <div className={`w-10 h-10 rounded-full flex items-center justify-center text-sm font-bold flex-shrink-0 ${
                      e.status === 'ACTIVE' ? 'bg-emerald-100 text-emerald-600' :
                      e.status === 'COMPLETED' ? 'bg-blue-100 text-blue-600' :
                      'bg-amber-100 text-amber-600'
                    }`}>
                      {e.studentName?.split(' ').map(w => w[0]).join('').slice(0, 2) || '?'}
                    </div>
                    <div className="min-w-0">
                      <div className="font-medium text-gray-900 text-sm">{e.studentName || `Студент #${e.studentId}`}</div>
                      <div className="text-xs text-gray-400">
                        Записан: {new Date(e.createdAt).toLocaleDateString('ru')}
                        {e.payerName ? ` | Плательщик: ${e.payerName}` : ''}
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center gap-2 flex-shrink-0">
                    {isAdmin ? (
                      <select
                        value={e.level}
                        onChange={(ev) => changeLevel(e, ev.target.value as ChineseLevel)}
                        className="px-2 py-1 rounded-md border border-purple-200 bg-purple-50 text-purple-700 text-xs font-medium outline-none cursor-pointer hover:border-purple-300"
                      >
                        {LEVELS.map((l) => <option key={l} value={l}>{l}</option>)}
                      </select>
                    ) : (
                      <Badge variant="purple">{e.level}</Badge>
                    )}
                    {isAdmin ? (
                      <select
                        value={e.status}
                        onChange={(ev) => changeStatus(e, ev.target.value as EnrollmentStatus)}
                        className={`px-2 py-1 rounded-md border text-xs font-medium outline-none cursor-pointer ${
                          e.status === 'ACTIVE' ? 'border-emerald-200 bg-emerald-50 text-emerald-700 hover:border-emerald-300' :
                          e.status === 'COMPLETED' ? 'border-blue-200 bg-blue-50 text-blue-700 hover:border-blue-300' :
                          'border-amber-200 bg-amber-50 text-amber-700 hover:border-amber-300'
                        }`}
                      >
                        {STATUSES.map((s) => <option key={s} value={s}>{STATUS_LABELS[s]}</option>)}
                      </select>
                    ) : (
                      <Badge variant={STATUS_COLORS[e.status]}>{STATUS_LABELS[e.status]}</Badge>
                    )}
                    {e.archived && <Badge variant="gray">Архив</Badge>}
                    {isAdmin && (
                      <button onClick={() => toggleArchive(e)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-400" title={e.archived ? 'Восстановить' : 'Архивировать'}>
                        {e.archived ? <ArchiveRestore className="w-4 h-4" /> : <Archive className="w-4 h-4" />}
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title="Записать студента" wide>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Поиск студента по фамилии</label>
            <div className="flex gap-2 mb-2">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  value={personSearch}
                  onChange={(ev) => setPersonSearch(ev.target.value)}
                  onKeyDown={(ev) => ev.key === 'Enter' && (ev.preventDefault(), searchPersons())}
                  placeholder="Введите фамилию..."
                  className="w-full pl-10 pr-4 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500"
                />
              </div>
              <button type="button" onClick={searchPersons} className="px-3 py-2 bg-gray-100 hover:bg-gray-200 rounded-lg text-sm text-gray-700">Найти</button>
            </div>
            {personResults.length > 0 && (
              <div className="border border-gray-200 rounded-lg max-h-40 overflow-y-auto divide-y divide-gray-100 mb-2">
                {personResults.map((p) => (
                  <button
                    key={p.id}
                    type="button"
                    onClick={() => { setForm(f => ({ ...f, studentId: p.id.toString() })); setPersonResults([]); setPersonSearch([p.lastName, p.firstName].join(' ')); }}
                    className={`w-full text-left px-3 py-2 text-sm hover:bg-primary-50 ${form.studentId === p.id.toString() ? 'bg-primary-50 text-primary-700' : 'text-gray-700'}`}
                  >
                    <span className="font-medium">{p.lastName} {p.firstName}</span>
                    {p.middleName && <span className="text-gray-400"> {p.middleName}</span>}
                    <span className="text-gray-400 ml-2">#{p.id}</span>
                  </button>
                ))}
              </div>
            )}
            {form.studentId && <p className="text-xs text-emerald-600">Выбран студент: ID {form.studentId}</p>}
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Статус</label>
              <select value={form.status} onChange={(ev) => setForm({ ...form, status: ev.target.value as EnrollmentStatus })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
                {STATUSES.map((s) => <option key={s} value={s}>{STATUS_LABELS[s]}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Уровень</label>
              <select value={form.level} onChange={(ev) => setForm({ ...form, level: ev.target.value as ChineseLevel })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
                {LEVELS.map((l) => <option key={l} value={l}>{l}</option>)}
              </select>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Группа</label>
              <select value={form.groupId} onChange={(ev) => setForm({ ...form, groupId: ev.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
                <option value="">Без группы</option>
                {groups.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">ID плательщика</label>
              <input value={form.payerId} onChange={(ev) => setForm({ ...form, payerId: ev.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" placeholder="Необязательно" />
            </div>
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setModalOpen(false)} className="px-4 py-2 text-sm text-gray-600">Отмена</button>
            <button type="submit" disabled={!form.studentId} className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium disabled:opacity-50">Записать</button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
