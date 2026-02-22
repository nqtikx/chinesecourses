import { useEffect, useState, type FormEvent } from 'react';
import { coursesApi } from '../api';
import type { CourseResponse } from '../types';
import { useAuth } from '../context/AuthContext';
import Modal from '../components/ui/Modal';
import Badge from '../components/ui/Badge';
import EmptyState from '../components/ui/EmptyState';
import { Plus, Pencil, Archive, ArchiveRestore, BookOpen } from 'lucide-react';

export default function CoursesPage() {
  const { isAdmin } = useAuth();
  const [courses, setCourses] = useState<CourseResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<CourseResponse | null>(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  const load = async () => {
    setLoading(true);
    try {
      const { data } = await coursesApi.list();
      setCourses(data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const openCreate = () => {
    setEditing(null);
    setName('');
    setDescription('');
    setModalOpen(true);
  };

  const openEdit = (c: CourseResponse) => {
    setEditing(c);
    setName(c.name);
    setDescription(c.description || '');
    setModalOpen(true);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (editing) {
      await coursesApi.update(editing.id, { name, description, archived: editing.archived });
    } else {
      await coursesApi.create({ name, description });
    }
    setModalOpen(false);
    load();
  };

  const toggleArchive = async (c: CourseResponse) => {
    await coursesApi.archive(c.id, { archived: !c.archived });
    load();
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-blue-500 text-white p-2.5 rounded-lg">
            <BookOpen className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Курсы</h1>
            <p className="text-sm text-gray-500">
              {isAdmin ? 'Управление курсами китайского языка' : 'Просмотр курсов'}
            </p>
          </div>
        </div>
        {isAdmin && (
          <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors text-sm font-medium">
            <Plus className="w-4 h-4" /> Добавить курс
          </button>
        )}
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="flex justify-center py-12">
            <div className="animate-spin rounded-full h-8 w-8 border-2 border-primary-600 border-t-transparent" />
          </div>
        ) : courses.length === 0 ? (
          <EmptyState message="Курсы не найдены" />
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="text-left py-3 px-4 font-medium text-gray-500">ID</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Название</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Описание</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Статус</th>
                {isAdmin && <th className="text-right py-3 px-4 font-medium text-gray-500">Действия</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {courses.map((c) => (
                <tr key={c.id} className="hover:bg-gray-50 transition-colors">
                  <td className="py-3 px-4 text-gray-500 font-mono">{c.id}</td>
                  <td className="py-3 px-4 font-medium text-gray-900">{c.name}</td>
                  <td className="py-3 px-4 text-gray-600 max-w-xs truncate">{c.description || '—'}</td>
                  <td className="py-3 px-4">
                    <Badge variant={c.archived ? 'gray' : 'green'}>{c.archived ? 'Архив' : 'Активен'}</Badge>
                  </td>
                  {isAdmin && (
                    <td className="py-3 px-4 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button onClick={() => openEdit(c)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-500 hover:text-gray-700" title="Редактировать">
                          <Pencil className="w-4 h-4" />
                        </button>
                        <button onClick={() => toggleArchive(c)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-500 hover:text-gray-700" title={c.archived ? 'Восстановить' : 'Архивировать'}>
                          {c.archived ? <ArchiveRestore className="w-4 h-4" /> : <Archive className="w-4 h-4" />}
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

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Редактировать курс' : 'Новый курс'}>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Название</label>
            <input value={name} onChange={(e) => setName(e.target.value)} required className="w-full px-3 py-2 rounded-lg border border-gray-300 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none text-sm" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Описание</label>
            <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={3} className="w-full px-3 py-2 rounded-lg border border-gray-300 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none text-sm" />
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setModalOpen(false)} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">Отмена</button>
            <button type="submit" className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium">Сохранить</button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
