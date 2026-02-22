import { useEffect, useState, type FormEvent } from 'react';
import { semestersApi, coursesApi } from '../api';
import type { SemesterResponse, CourseResponse } from '../types';
import { useAuth } from '../context/AuthContext';
import Modal from '../components/ui/Modal';
import Badge from '../components/ui/Badge';
import EmptyState from '../components/ui/EmptyState';
import ConfirmDialog from '../components/ui/ConfirmDialog';
import { toast } from '../components/ui/Toast';
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
  const [archiveTarget, setArchiveTarget] = useState<SemesterResponse | null>(null);

  useEffect(() => { coursesApi.list().then(({ data }) => { setCourses(data); if (data.length) setSelectedCourse(data[0].id); }); }, []);

  const reload = async (courseId?: number) => {
    const id = courseId ?? selectedCourse;
    if (!id) return;
    setLoading(true);
    try { const { data } = await semestersApi.listByCourse(id); setSemesters(data); } catch { toast('error', 'Ошибка загрузки'); } finally { setLoading(false); }
  };

  useEffect(() => { if (selectedCourse) reload(selectedCourse); }, [selectedCourse]);

  const openCreate = () => { setEditing(null); setForm({ courseId: selectedCourse || 0, name: '', startDate: '', endDate: '' }); setModalOpen(true); };

  const openEdit = (s: SemesterResponse) => { setEditing(s); setForm({ courseId: s.courseId, name: s.name, startDate: s.startDate, endDate: s.endDate }); setModalOpen(true); };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    try {
      if (editing) { await semestersApi.update(editing.id, { ...form, archived: editing.archived }); toast('success', 'Семестр обновлён'); }
      else { await semestersApi.create(form); toast('success', 'Семестр создан'); }
      setModalOpen(false); reload();
    } catch { toast('error', 'Ошибка сохранения'); }
  };

  const confirmArchive = async () => {
    if (!archiveTarget) return;
    try { await semestersApi.archive(archiveTarget.id, { archived: !archiveTarget.archived }); toast('success', archiveTarget.archived ? 'Восстановлен' : 'Архивирован'); reload(); } catch { toast('error', 'Ошибка'); }
    setArchiveTarget(null);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-indigo-500 text-white p-2.5 rounded-lg"><Calendar className="w-5 h-5" /></div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Семестры</h1>
            <p className="text-sm text-gray-500">{isAdmin ? 'Управление учебными периодами' : 'Просмотр учебных периодов'}</p>
          </div>
        </div>
        {isAdmin && (
          <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors text-sm font-medium">
            <Plus className="w-4 h-4" /> Добавить семестр
          </button>
        )}
      </div>

      <div className="flex items-center gap-3 bg-white rounded-lg border border-gray-200 px-4 py-2.5 w-fit">
        <label className="text-sm font-medium text-gray-600">Курс:</label>
        <select value={selectedCourse || ''} onChange={(e) => setSelectedCourse(Number(e.target.value))} className="px-3 py-1.5 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
          {courses.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="flex justify-center py-16"><div className="animate-spin rounded-full h-8 w-8 border-2 border-primary-600 border-t-transparent" /></div>
        ) : semesters.length === 0 ? (
          <EmptyState message={courses.length === 0 ? 'Сначала создайте курс' : 'Семестры не найдены для этого курса'} />
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="text-left py-3 px-4 font-medium text-gray-500">ID</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Название</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Начало</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Окончание</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Статус</th>
                {isAdmin && <th className="text-right py-3 px-4 font-medium text-gray-500">Действия</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {semesters.map((s) => (
                <tr key={s.id} className="hover:bg-gray-50">
                  <td className="py-3 px-4 text-gray-400 font-mono text-xs">{s.id}</td>
                  <td className="py-3 px-4 font-medium text-gray-900">{s.name}</td>
                  <td className="py-3 px-4 text-gray-600">{new Date(s.startDate + 'T00:00').toLocaleDateString('ru')}</td>
                  <td className="py-3 px-4 text-gray-600">{new Date(s.endDate + 'T00:00').toLocaleDateString('ru')}</td>
                  <td className="py-3 px-4"><Badge variant={s.archived ? 'gray' : 'green'}>{s.archived ? 'В архиве' : 'Активен'}</Badge></td>
                  {isAdmin && (
                    <td className="py-3 px-4 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button onClick={() => openEdit(s)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-500 transition-colors"><Pencil className="w-4 h-4" /></button>
                        <button onClick={() => setArchiveTarget(s)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-500 transition-colors">
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
            <label className="block text-sm font-medium text-gray-700 mb-1">Курс *</label>
            <select value={form.courseId} onChange={(e) => setForm({ ...form, courseId: Number(e.target.value) })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
              {courses.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Название семестра *</label>
            <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" placeholder="Например: Осень 2025" />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Дата начала *</label>
              <input type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Дата окончания *</label>
              <input type="date" value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setModalOpen(false)} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800 transition-colors">Отмена</button>
            <button type="submit" className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium transition-colors">{editing ? 'Сохранить' : 'Создать'}</button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog open={!!archiveTarget} title={archiveTarget?.archived ? 'Восстановить?' : 'Архивировать?'} message={`Семестр «${archiveTarget?.name}» будет ${archiveTarget?.archived ? 'восстановлен' : 'архивирован'}.`} confirmLabel={archiveTarget?.archived ? 'Восстановить' : 'Архивировать'} onConfirm={confirmArchive} onCancel={() => setArchiveTarget(null)} />
    </div>
  );
}
