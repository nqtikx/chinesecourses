import { useState, type FormEvent } from 'react';
import { teachersApi, personsApi } from '../api';
import type { TeacherResponse, PersonResponse } from '../types';
import Modal from '../components/ui/Modal';
import Badge from '../components/ui/Badge';
import EmptyState from '../components/ui/EmptyState';
import { Plus, Archive, ArchiveRestore, GraduationCap, Search } from 'lucide-react';

export default function TeachersPage() {
  const [teacherId, setTeacherId] = useState('');
  const [teacher, setTeacher] = useState<TeacherResponse | null>(null);
  const [teacherPerson, setTeacherPerson] = useState<PersonResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [personId, setPersonId] = useState('');

  const searchTeacher = async () => {
    if (!teacherId.trim()) return;
    setLoading(true);
    setError('');
    setTeacher(null);
    setTeacherPerson(null);
    try {
      const { data } = await teachersApi.get(Number(teacherId));
      setTeacher(data);
      try {
        const personRes = await personsApi.get(data.personId);
        setTeacherPerson(personRes.data);
      } catch { /* person not found */ }
    } catch {
      setError('Преподаватель не найден');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async (e: FormEvent) => {
    e.preventDefault();
    await teachersApi.create(Number(personId));
    setModalOpen(false);
    setPersonId('');
  };

  const toggleArchive = async () => {
    if (!teacher) return;
    await teachersApi.archive(teacher.id, { archived: !teacher.archived });
    searchTeacher();
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-amber-500 text-white p-2.5 rounded-lg"><GraduationCap className="w-5 h-5" /></div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Преподаватели</h1>
            <p className="text-sm text-gray-500">Управление преподавателями</p>
          </div>
        </div>
        <button onClick={() => setModalOpen(true)} className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium">
          <Plus className="w-4 h-4" /> Создать преподавателя
        </button>
      </div>

      <div className="flex items-center gap-3">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            value={teacherId}
            onChange={(e) => setTeacherId(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && searchTeacher()}
            placeholder="Введите ID преподавателя..."
            className="w-full pl-10 pr-4 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
        <button onClick={searchTeacher} className="px-4 py-2 bg-gray-100 hover:bg-gray-200 rounded-lg text-sm font-medium text-gray-700 transition-colors">Найти</button>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        {loading ? (
          <div className="flex justify-center py-8"><div className="animate-spin rounded-full h-8 w-8 border-2 border-primary-600 border-t-transparent" /></div>
        ) : error ? (
          <p className="text-red-500 text-sm text-center py-8">{error}</p>
        ) : teacher ? (
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="font-semibold text-gray-900">Преподаватель #{teacher.id}</h3>
              <div className="flex items-center gap-2">
                <Badge variant={teacher.archived ? 'gray' : 'green'}>{teacher.archived ? 'Архив' : 'Активен'}</Badge>
                <button onClick={toggleArchive} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-500">
                  {teacher.archived ? <ArchiveRestore className="w-4 h-4" /> : <Archive className="w-4 h-4" />}
                </button>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div><span className="text-gray-500">ID персоны:</span> <span className="font-medium">{teacher.personId}</span></div>
              <div><span className="text-gray-500">Создан:</span> <span className="font-medium">{new Date(teacher.createdAt).toLocaleDateString('ru')}</span></div>
            </div>
            {teacherPerson && (
              <div className="bg-gray-50 rounded-lg p-4 mt-2">
                <h4 className="text-sm font-medium text-gray-700 mb-2">Данные персоны</h4>
                <div className="grid grid-cols-3 gap-3 text-sm">
                  <div><span className="text-gray-500">Фамилия:</span> {teacherPerson.lastName}</div>
                  <div><span className="text-gray-500">Имя:</span> {teacherPerson.firstName}</div>
                  <div><span className="text-gray-500">Отчество:</span> {teacherPerson.middleName || '—'}</div>
                  <div><span className="text-gray-500">Телефон:</span> {teacherPerson.phone || '—'}</div>
                  <div><span className="text-gray-500">Email:</span> {teacherPerson.email || '—'}</div>
                </div>
              </div>
            )}
          </div>
        ) : (
          <EmptyState message="Введите ID для поиска преподавателя" />
        )}
      </div>

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title="Создать преподавателя">
        <form onSubmit={handleCreate} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">ID персоны</label>
            <input value={personId} onChange={(e) => setPersonId(e.target.value)} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" placeholder="ID существующей персоны" />
          </div>
          <p className="text-xs text-gray-500">Преподаватель привязывается к существующей персоне. Сначала создайте персону в разделе «Люди».</p>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setModalOpen(false)} className="px-4 py-2 text-sm text-gray-600">Отмена</button>
            <button type="submit" className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium">Создать</button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
