import { useEffect, useState, type FormEvent } from 'react';
import { semestersApi, coursesApi } from '../api';
import type { SemesterResponse, CourseResponse } from '../types';
import { useAuth } from '../context/AuthContext';
import Modal from '../components/ui/Modal';
import Badge from '../components/ui/Badge';
import EmptyState from '../components/ui/EmptyState';
import { Plus, Pencil, Archive, ArchiveRestore, Calendar } from 'lucide-react';

export default function SemestersPage() {
  const { isAdmin } = useAuth();
  const [courses, setCourses] = useState<CourseResponse[]>([]);
  const [selectedCourse, setSelectedCourse] = useState<number | null>(null);
  const [semesters, setSemesters] = useState<SemesterResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<SemesterResponse | null>(null);
  const [form, setForm] = useState({ courseId: 0, name: '', startDate: '', endDate: '' });

  useEffect(() => {
    coursesApi.list().then(({ data }) => {
      setCourses(data);
      if (data.length > 0) setSelectedCourse(data[0].id);
    });
  }, []);

  useEffect(() => {
    if (selectedCourse) {
      setLoading(true);
      semestersApi.listByCourse(selectedCourse).then(({ data }) => setSemesters(data)).finally(() => setLoading(false));
    }
  }, [selectedCourse]);

  const openCreate = () => {
    setEditing(null);
    setForm({ courseId: selectedCourse || 0, name: '', startDate: '', endDate: '' });
    setModalOpen(true);
  };

  const openEdit = (s: SemesterResponse) => {
    setEditing(s);
    setForm({ courseId: s.courseId, name: s.name, startDate: s.startDate, endDate: s.endDate });
    setModalOpen(true);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (editing) {
      await semestersApi.update(editing.id, { ...form, archived: editing.archived });
    } else {
      await semestersApi.create(form);
    }
    setModalOpen(false);
    if (selectedCourse) {
      const { data } = await semestersApi.listByCourse(selectedCourse);
      setSemesters(data);
    }
  };

  const toggleArchive = async (s: SemesterResponse) => {
    await semestersApi.archive(s.id, { archived: !s.archived });
    if (selectedCourse) {
      const { data } = await semestersApi.listByCourse(selectedCourse);
      setSemesters(data);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-indigo-500 text-white p-2.5 rounded-lg">
            <Calendar className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Семестры</h1>
            <p className="text-sm text-gray-500">{isAdmin ? 'Управление семестрами' : 'Просмотр семестров'}</p>
          </div>
        </div>
        {isAdmin && (
          <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors text-sm font-medium">
            <Plus className="w-4 h-4" /> Добавить семестр
          </button>
        )}
      </div>

      <div className="flex items-center gap-3">
        <label className="text-sm font-medium text-gray-600">Курс:</label>
        <select
          value={selectedCourse || ''}
          onChange={(e) => setSelectedCourse(Number(e.target.value))}
          className="px-3 py-2 rounded-lg border border-gray-300 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
        >
          {courses.map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </select>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="flex justify-center py-12"><div className="animate-spin rounded-full h-8 w-8 border-2 border-primary-600 border-t-transparent" /></div>
        ) : semesters.length === 0 ? (
          <EmptyState message="Семестры не найдены" />
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="text-left py-3 px-4 font-medium text-gray-500">ID</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Название</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Начало</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Конец</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Статус</th>
                {isAdmin && <th className="text-right py-3 px-4 font-medium text-gray-500">Действия</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {semesters.map((s) => (
                <tr key={s.id} className="hover:bg-gray-50">
                  <td className="py-3 px-4 text-gray-500 font-mono">{s.id}</td>
                  <td className="py-3 px-4 font-medium text-gray-900">{s.name}</td>
                  <td className="py-3 px-4 text-gray-600">{s.startDate}</td>
                  <td className="py-3 px-4 text-gray-600">{s.endDate}</td>
                  <td className="py-3 px-4"><Badge variant={s.archived ? 'gray' : 'green'}>{s.archived ? 'Архив' : 'Активен'}</Badge></td>
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

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Редактировать семестр' : 'Новый семестр'}>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Курс</label>
            <select value={form.courseId} onChange={(e) => setForm({ ...form, courseId: Number(e.target.value) })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
              {courses.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Название</label>
            <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Начало</label>
              <input type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Конец</label>
              <input type="date" value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
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
