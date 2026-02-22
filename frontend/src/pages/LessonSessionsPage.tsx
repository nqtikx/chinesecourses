import { useEffect, useState, type FormEvent } from 'react';
import { lessonSessionsApi, studyGroupsApi, semestersApi, coursesApi } from '../api';
import type { LessonSessionResponse, StudyGroupResponse, SemesterResponse, CourseResponse } from '../types';
import { useAuth } from '../context/AuthContext';
import Modal from '../components/ui/Modal';
import Badge from '../components/ui/Badge';
import EmptyState from '../components/ui/EmptyState';
import { Plus, Pencil, Archive, ArchiveRestore, Clock } from 'lucide-react';

function fmtDate(iso: string) { return new Date(iso).toLocaleString('ru', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }); }

export default function LessonSessionsPage() {
  const { isAdmin } = useAuth();
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
  const [form, setForm] = useState({ groupId: 0, teacherId: '', startsAt: '', endsAt: '', topic: '', room: '' });

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
      lessonSessionsApi.listByGroup(selectedGroup).then(({ data }) => setSessions(data)).finally(() => setLoading(false));
    } else { setSessions([]); }
  }, [selectedGroup]);

  const reload = async () => {
    if (selectedGroup) { const { data } = await lessonSessionsApi.listByGroup(selectedGroup); setSessions(data); }
  };

  const openCreate = () => {
    setEditing(null);
    setForm({ groupId: selectedGroup || 0, teacherId: '', startsAt: '', endsAt: '', topic: '', room: '' });
    setModalOpen(true);
  };

  const openEdit = (s: LessonSessionResponse) => {
    setEditing(s);
    setForm({
      groupId: s.groupId, teacherId: s.teacherId?.toString() || '',
      startsAt: s.startsAt.slice(0, 16), endsAt: s.endsAt.slice(0, 16),
      topic: s.topic || '', room: s.room || '',
    });
    setModalOpen(true);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const payload = {
      groupId: form.groupId, teacherId: form.teacherId ? Number(form.teacherId) : undefined,
      startsAt: new Date(form.startsAt).toISOString(), endsAt: new Date(form.endsAt).toISOString(),
      topic: form.topic || undefined, room: form.room || undefined,
    };
    if (editing) {
      await lessonSessionsApi.update(editing.id, { ...payload, canceled: editing.canceled, archived: editing.archived });
    } else {
      await lessonSessionsApi.create(payload);
    }
    setModalOpen(false);
    reload();
  };

  const toggleArchive = async (s: LessonSessionResponse) => {
    await lessonSessionsApi.archive(s.id, { archived: !s.archived });
    reload();
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-cyan-500 text-white p-2.5 rounded-lg"><Clock className="w-5 h-5" /></div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Занятия</h1>
            <p className="text-sm text-gray-500">{isAdmin ? 'Управление занятиями' : 'Просмотр занятий'}</p>
          </div>
        </div>
        {isAdmin && (
          <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium">
            <Plus className="w-4 h-4" /> Добавить занятие
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
        ) : sessions.length === 0 ? (
          <EmptyState message="Занятия не найдены" />
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="text-left py-3 px-4 font-medium text-gray-500">ID</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Начало</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Конец</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Тема</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Аудитория</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Статус</th>
                {isAdmin && <th className="text-right py-3 px-4 font-medium text-gray-500">Действия</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {sessions.map((s) => (
                <tr key={s.id} className="hover:bg-gray-50">
                  <td className="py-3 px-4 text-gray-500 font-mono">{s.id}</td>
                  <td className="py-3 px-4 text-gray-700">{fmtDate(s.startsAt)}</td>
                  <td className="py-3 px-4 text-gray-700">{fmtDate(s.endsAt)}</td>
                  <td className="py-3 px-4 text-gray-700 max-w-xs truncate">{s.topic || '—'}</td>
                  <td className="py-3 px-4 text-gray-600">{s.room || '—'}</td>
                  <td className="py-3 px-4">
                    {s.canceled ? <Badge variant="red">Отменено</Badge> : s.archived ? <Badge variant="gray">Архив</Badge> : <Badge variant="green">Активно</Badge>}
                  </td>
                  {isAdmin && (
                    <td className="py-3 px-4 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button onClick={() => openEdit(s)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-500"><Pencil className="w-4 h-4" /></button>
                        <button onClick={() => toggleArchive(s)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-500">
                          {s.archived ? <ArchiveRestore className="w-4 h-4" /> : <Archive className="w-4 h-4" />}
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

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Редактировать занятие' : 'Новое занятие'} wide>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Начало</label>
              <input type="datetime-local" value={form.startsAt} onChange={(e) => setForm({ ...form, startsAt: e.target.value })} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Конец</label>
              <input type="datetime-local" value={form.endsAt} onChange={(e) => setForm({ ...form, endsAt: e.target.value })} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Тема</label>
              <input value={form.topic} onChange={(e) => setForm({ ...form, topic: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Аудитория</label>
              <input value={form.room} onChange={(e) => setForm({ ...form, room: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">ID преподавателя (необязательно)</label>
            <input value={form.teacherId} onChange={(e) => setForm({ ...form, teacherId: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
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
