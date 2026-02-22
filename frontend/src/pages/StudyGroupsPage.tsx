import { useEffect, useState, type FormEvent } from 'react';
import { studyGroupsApi, semestersApi, coursesApi } from '../api';
import type { StudyGroupResponse, SemesterResponse, CourseResponse } from '../types';
import { useAuth } from '../context/AuthContext';
import Modal from '../components/ui/Modal';
import Badge from '../components/ui/Badge';
import EmptyState from '../components/ui/EmptyState';
import { Plus, Pencil, Archive, ArchiveRestore, Users } from 'lucide-react';

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
      studyGroupsApi.listBySemester(selectedSemester).then(({ data }) => setGroups(data)).finally(() => setLoading(false));
    }
  }, [selectedSemester]);

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
    const payload = {
      semesterId: form.semesterId,
      teacherId: form.teacherId ? Number(form.teacherId) : undefined,
      name: form.name,
      scheduleNotes: form.scheduleNotes || undefined,
    };
    if (editing) {
      await studyGroupsApi.update(editing.id, { ...payload, archived: editing.archived });
    } else {
      await studyGroupsApi.create(payload);
    }
    setModalOpen(false);
    if (selectedSemester) {
      const { data } = await studyGroupsApi.listBySemester(selectedSemester);
      setGroups(data);
    }
  };

  const toggleArchive = async (g: StudyGroupResponse) => {
    await studyGroupsApi.archive(g.id, { archived: !g.archived });
    if (selectedSemester) {
      const { data } = await studyGroupsApi.listBySemester(selectedSemester);
      setGroups(data);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-violet-500 text-white p-2.5 rounded-lg"><Users className="w-5 h-5" /></div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Учебные группы</h1>
            <p className="text-sm text-gray-500">{isAdmin ? 'Управление группами' : 'Просмотр групп'}</p>
          </div>
        </div>
        {isAdmin && (
          <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium">
            <Plus className="w-4 h-4" /> Добавить группу
          </button>
        )}
      </div>

      <div className="flex items-center gap-4">
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
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="flex justify-center py-12"><div className="animate-spin rounded-full h-8 w-8 border-2 border-primary-600 border-t-transparent" /></div>
        ) : groups.length === 0 ? (
          <EmptyState message="Группы не найдены" />
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="text-left py-3 px-4 font-medium text-gray-500">ID</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Название</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Преподаватель ID</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Заметки</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Статус</th>
                {isAdmin && <th className="text-right py-3 px-4 font-medium text-gray-500">Действия</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {groups.map((g) => (
                <tr key={g.id} className="hover:bg-gray-50">
                  <td className="py-3 px-4 text-gray-500 font-mono">{g.id}</td>
                  <td className="py-3 px-4 font-medium text-gray-900">{g.name}</td>
                  <td className="py-3 px-4 text-gray-600">{g.teacherId || '—'}</td>
                  <td className="py-3 px-4 text-gray-600 max-w-xs truncate">{g.scheduleNotes || '—'}</td>
                  <td className="py-3 px-4"><Badge variant={g.archived ? 'gray' : 'green'}>{g.archived ? 'Архив' : 'Активна'}</Badge></td>
                  {isAdmin && (
                    <td className="py-3 px-4 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button onClick={() => openEdit(g)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-500"><Pencil className="w-4 h-4" /></button>
                        <button onClick={() => toggleArchive(g)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-500">
                          {g.archived ? <ArchiveRestore className="w-4 h-4" /> : <Archive className="w-4 h-4" />}
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

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Редактировать группу' : 'Новая группа'}>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Семестр</label>
            <select value={form.semesterId} onChange={(e) => setForm({ ...form, semesterId: Number(e.target.value) })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
              {semesters.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Название</label>
            <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">ID преподавателя (необязательно)</label>
            <input value={form.teacherId} onChange={(e) => setForm({ ...form, teacherId: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" placeholder="Оставьте пустым" />
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
