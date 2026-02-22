import { useEffect, useState, type FormEvent } from 'react';
import { enrollmentsApi, studyGroupsApi, semestersApi, coursesApi } from '../api';
import type { EnrollmentResponse, StudyGroupResponse, SemesterResponse, CourseResponse, EnrollmentStatus, ChineseLevel } from '../types';
import { useAuth } from '../context/AuthContext';
import Modal from '../components/ui/Modal';
import Badge from '../components/ui/Badge';
import EmptyState from '../components/ui/EmptyState';
import { Plus, Pencil, Archive, ArchiveRestore, ClipboardList } from 'lucide-react';

const STATUS_LABELS: Record<EnrollmentStatus, string> = { APPLICANT: 'Заявка', ACTIVE: 'Активен', COMPLETED: 'Завершён' };
const STATUS_COLORS: Record<EnrollmentStatus, 'yellow' | 'green' | 'blue'> = { APPLICANT: 'yellow', ACTIVE: 'green', COMPLETED: 'blue' };
const LEVELS: ChineseLevel[] = ['HSK1', 'HSK2', 'HSK3', 'HSK4', 'HSK5', 'HSK6'];
const STATUSES: EnrollmentStatus[] = ['APPLICANT', 'ACTIVE', 'COMPLETED'];

export default function EnrollmentsPage() {
  const { isAdmin } = useAuth();
  const [courses, setCourses] = useState<CourseResponse[]>([]);
  const [semesters, setSemesters] = useState<SemesterResponse[]>([]);
  const [groups, setGroups] = useState<StudyGroupResponse[]>([]);
  const [selectedCourse, setSelectedCourse] = useState<number | null>(null);
  const [selectedSemester, setSelectedSemester] = useState<number | null>(null);
  const [selectedGroup, setSelectedGroup] = useState<number | null>(null);
  const [enrollments, setEnrollments] = useState<EnrollmentResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<EnrollmentResponse | null>(null);
  const [form, setForm] = useState({ studentId: '', payerId: '', semesterId: 0, groupId: '', status: 'APPLICANT' as EnrollmentStatus, level: 'HSK1' as ChineseLevel });

  useEffect(() => { coursesApi.list().then(({ data }) => { setCourses(data); if (data.length) setSelectedCourse(data[0].id); }); }, []);

  useEffect(() => {
    if (selectedCourse) {
      semestersApi.listByCourse(selectedCourse).then(({ data }) => {
        setSemesters(data);
        if (data.length) setSelectedSemester(data[0].id); else { setSelectedSemester(null); setGroups([]); setSelectedGroup(null); }
      });
    }
  }, [selectedCourse]);

  useEffect(() => {
    if (selectedSemester) {
      studyGroupsApi.listBySemester(selectedSemester).then(({ data }) => {
        setGroups(data);
        if (data.length) setSelectedGroup(data[0].id); else setSelectedGroup(null);
      });
    }
  }, [selectedSemester]);

  useEffect(() => {
    if (selectedGroup) {
      setLoading(true);
      enrollmentsApi.listByGroup(selectedGroup).then(({ data }) => setEnrollments(data)).finally(() => setLoading(false));
    } else {
      setEnrollments([]);
    }
  }, [selectedGroup]);

  const openCreate = () => {
    setEditing(null);
    setForm({ studentId: '', payerId: '', semesterId: selectedSemester || 0, groupId: selectedGroup?.toString() || '', status: 'APPLICANT', level: 'HSK1' });
    setModalOpen(true);
  };

  const openEdit = (e: EnrollmentResponse) => {
    setEditing(e);
    setForm({ studentId: e.studentId.toString(), payerId: e.payerId?.toString() || '', semesterId: e.semesterId, groupId: e.groupId?.toString() || '', status: e.status, level: e.level });
    setModalOpen(true);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (editing) {
      await enrollmentsApi.update(editing.id, {
        payerId: form.payerId ? Number(form.payerId) : undefined,
        semesterId: form.semesterId, groupId: form.groupId ? Number(form.groupId) : undefined,
        status: form.status, level: form.level, archived: editing.archived,
      });
    } else {
      await enrollmentsApi.create({
        studentId: Number(form.studentId), payerId: form.payerId ? Number(form.payerId) : undefined,
        semesterId: form.semesterId, groupId: form.groupId ? Number(form.groupId) : undefined,
        status: form.status, level: form.level,
      });
    }
    setModalOpen(false);
    if (selectedGroup) { const { data } = await enrollmentsApi.listByGroup(selectedGroup); setEnrollments(data); }
  };

  const toggleArchive = async (e: EnrollmentResponse) => {
    await enrollmentsApi.archive(e.id, { archived: !e.archived });
    if (selectedGroup) { const { data } = await enrollmentsApi.listByGroup(selectedGroup); setEnrollments(data); }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-rose-500 text-white p-2.5 rounded-lg"><ClipboardList className="w-5 h-5" /></div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Записи на курсы</h1>
            <p className="text-sm text-gray-500">{isAdmin ? 'Управление записями студентов' : 'Просмотр записей'}</p>
          </div>
        </div>
        {isAdmin && (
          <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium">
            <Plus className="w-4 h-4" /> Добавить запись
          </button>
        )}
      </div>

      <div className="flex flex-wrap items-center gap-4">
        <div className="flex items-center gap-2">
          <label className="text-sm font-medium text-gray-600">Курс:</label>
          <select value={selectedCourse || ''} onChange={(e) => setSelectedCourse(Number(e.target.value))} className="px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
            {courses.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>
        <div className="flex items-center gap-2">
          <label className="text-sm font-medium text-gray-600">Семестр:</label>
          <select value={selectedSemester || ''} onChange={(e) => setSelectedSemester(Number(e.target.value))} className="px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
            {semesters.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
        </div>
        <div className="flex items-center gap-2">
          <label className="text-sm font-medium text-gray-600">Группа:</label>
          <select value={selectedGroup || ''} onChange={(e) => setSelectedGroup(Number(e.target.value))} className="px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
            {groups.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
          </select>
        </div>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="flex justify-center py-12"><div className="animate-spin rounded-full h-8 w-8 border-2 border-primary-600 border-t-transparent" /></div>
        ) : enrollments.length === 0 ? (
          <EmptyState message="Записи не найдены" />
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="text-left py-3 px-4 font-medium text-gray-500">ID</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Студент ID</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Уровень</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Статус</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Архив</th>
                {isAdmin && <th className="text-right py-3 px-4 font-medium text-gray-500">Действия</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {enrollments.map((e) => (
                <tr key={e.id} className="hover:bg-gray-50">
                  <td className="py-3 px-4 text-gray-500 font-mono">{e.id}</td>
                  <td className="py-3 px-4 text-gray-700">{e.studentId}</td>
                  <td className="py-3 px-4"><Badge variant="purple">{e.level}</Badge></td>
                  <td className="py-3 px-4"><Badge variant={STATUS_COLORS[e.status]}>{STATUS_LABELS[e.status]}</Badge></td>
                  <td className="py-3 px-4"><Badge variant={e.archived ? 'gray' : 'green'}>{e.archived ? 'Да' : 'Нет'}</Badge></td>
                  {isAdmin && (
                    <td className="py-3 px-4 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button onClick={() => openEdit(e)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-500"><Pencil className="w-4 h-4" /></button>
                        <button onClick={() => toggleArchive(e)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-500">
                          {e.archived ? <ArchiveRestore className="w-4 h-4" /> : <Archive className="w-4 h-4" />}
                        </button>
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Редактировать запись' : 'Новая запись'} wide>
        <form onSubmit={handleSubmit} className="space-y-4">
          {!editing && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">ID студента (персоны)</label>
              <input value={form.studentId} onChange={(e) => setForm({ ...form, studentId: e.target.value })} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
          )}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Статус</label>
              <select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value as EnrollmentStatus })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
                {STATUSES.map((s) => <option key={s} value={s}>{STATUS_LABELS[s]}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Уровень</label>
              <select value={form.level} onChange={(e) => setForm({ ...form, level: e.target.value as ChineseLevel })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
                {LEVELS.map((l) => <option key={l} value={l}>{l}</option>)}
              </select>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">ID плательщика (необязательно)</label>
              <input value={form.payerId} onChange={(e) => setForm({ ...form, payerId: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">ID группы (необязательно)</label>
              <input value={form.groupId} onChange={(e) => setForm({ ...form, groupId: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
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
