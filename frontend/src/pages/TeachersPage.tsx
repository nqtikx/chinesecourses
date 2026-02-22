import { useState, type FormEvent } from 'react';
import { teachersApi, personsApi } from '../api';
import type { TeacherResponse, PersonResponse } from '../types';
import Modal from '../components/ui/Modal';
import Badge from '../components/ui/Badge';
import EmptyState from '../components/ui/EmptyState';
import ConfirmDialog from '../components/ui/ConfirmDialog';
import { toast } from '../components/ui/Toast';
import { Plus, Archive, ArchiveRestore, GraduationCap, Search } from 'lucide-react';

export default function TeachersPage() {
  const [teacherId, setTeacherId] = useState('');
  const [teacher, setTeacher] = useState<TeacherResponse | null>(null);
  const [teacherPerson, setTeacherPerson] = useState<PersonResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [personId, setPersonId] = useState('');
  const [archiveTarget, setArchiveTarget] = useState<TeacherResponse | null>(null);

  const searchTeacher = async () => {
    if (!teacherId.trim()) return;
    setLoading(true); setSearched(true); setTeacher(null); setTeacherPerson(null);
    try {
      const { data } = await teachersApi.get(Number(teacherId));
      setTeacher(data);
      try { const pr = await personsApi.get(data.personId); setTeacherPerson(pr.data); } catch { /* ok */ }
    } catch { toast('error', 'Преподаватель не найден'); } finally { setLoading(false); }
  };

  const handleCreate = async (e: FormEvent) => {
    e.preventDefault();
    try { await teachersApi.create(Number(personId)); toast('success', 'Преподаватель создан'); setModalOpen(false); setPersonId(''); } catch { toast('error', 'Ошибка создания'); }
  };

  const confirmArchive = async () => {
    if (!archiveTarget) return;
    try { await teachersApi.archive(archiveTarget.id, { archived: !archiveTarget.archived }); toast('success', archiveTarget.archived ? 'Восстановлен' : 'Архивирован'); searchTeacher(); } catch { toast('error', 'Ошибка'); }
    setArchiveTarget(null);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-amber-500 text-white p-2.5 rounded-lg"><GraduationCap className="w-5 h-5" /></div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Преподаватели</h1>
            <p className="text-sm text-gray-500">Управление преподавательским составом</p>
          </div>
        </div>
        <button onClick={() => setModalOpen(true)} className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium transition-colors">
          <Plus className="w-4 h-4" /> Создать преподавателя
        </button>
      </div>

      <div className="flex items-center gap-3">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input value={teacherId} onChange={(e) => setTeacherId(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && searchTeacher()} placeholder="Введите ID преподавателя..." className="w-full pl-10 pr-4 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
        </div>
        <button onClick={searchTeacher} className="px-4 py-2 bg-gray-100 hover:bg-gray-200 rounded-lg text-sm font-medium text-gray-700 transition-colors">Найти</button>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        {loading ? (
          <div className="flex justify-center py-8"><div className="animate-spin rounded-full h-8 w-8 border-2 border-primary-600 border-t-transparent" /></div>
        ) : teacher ? (
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="font-semibold text-gray-900">Преподаватель #{teacher.id}</h3>
              <div className="flex items-center gap-2">
                <Badge variant={teacher.archived ? 'gray' : 'green'}>{teacher.archived ? 'В архиве' : 'Активен'}</Badge>
                <button onClick={() => setArchiveTarget(teacher)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-500 transition-colors">
                  {teacher.archived ? <ArchiveRestore className="w-4 h-4" /> : <Archive className="w-4 h-4" />}
                </button>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div><span className="text-gray-500">ID персоны:</span> <span className="font-medium ml-1">{teacher.personId}</span></div>
              <div><span className="text-gray-500">Создан:</span> <span className="font-medium ml-1">{new Date(teacher.createdAt).toLocaleDateString('ru')}</span></div>
            </div>
            {teacherPerson && (
              <div className="bg-gray-50 rounded-lg p-4 mt-2">
                <h4 className="text-sm font-semibold text-gray-700 mb-2">Личные данные</h4>
                <div className="grid grid-cols-2 md:grid-cols-3 gap-3 text-sm">
                  <div><span className="text-gray-500">Фамилия:</span> <span className="font-medium ml-1">{teacherPerson.lastName}</span></div>
                  <div><span className="text-gray-500">Имя:</span> <span className="font-medium ml-1">{teacherPerson.firstName}</span></div>
                  <div><span className="text-gray-500">Отчество:</span> <span className="font-medium ml-1">{teacherPerson.middleName || '—'}</span></div>
                  <div><span className="text-gray-500">Телефон:</span> <span className="font-medium ml-1">{teacherPerson.phone || '—'}</span></div>
                  <div><span className="text-gray-500">Email:</span> <span className="font-medium ml-1">{teacherPerson.email || '—'}</span></div>
                </div>
              </div>
            )}
          </div>
        ) : (
          <EmptyState message={searched ? 'Преподаватель не найден' : 'Введите ID для просмотра данных преподавателя'} />
        )}
      </div>

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title="Создать преподавателя">
        <form onSubmit={handleCreate} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">ID персоны *</label>
            <input value={personId} onChange={(e) => setPersonId(e.target.value)} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" placeholder="ID существующей персоны" />
          </div>
          <p className="text-xs text-gray-500">Преподаватель привязывается к существующей записи персоны. Сначала создайте персону в разделе «Абитуриенты и слушатели».</p>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setModalOpen(false)} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800 transition-colors">Отмена</button>
            <button type="submit" className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium transition-colors">Создать</button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog open={!!archiveTarget} title={archiveTarget?.archived ? 'Восстановить?' : 'Архивировать?'} message={`Преподаватель #${archiveTarget?.id} будет ${archiveTarget?.archived ? 'восстановлен' : 'архивирован'}.`} confirmLabel={archiveTarget?.archived ? 'Восстановить' : 'Архивировать'} onConfirm={confirmArchive} onCancel={() => setArchiveTarget(null)} />
    </div>
  );
}
