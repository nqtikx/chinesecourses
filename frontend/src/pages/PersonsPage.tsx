import { useState, type FormEvent } from 'react';
import { personsApi } from '../api';
import type { PersonResponse } from '../types';
import Modal from '../components/ui/Modal';
import Badge from '../components/ui/Badge';
import EmptyState from '../components/ui/EmptyState';
import { Plus, Pencil, Archive, ArchiveRestore, UserCheck, Search } from 'lucide-react';

export default function PersonsPage() {
  const [persons, setPersons] = useState<PersonResponse[]>([]);
  const [searchPrefix, setSearchPrefix] = useState('');
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<PersonResponse | null>(null);
  const [form, setForm] = useState({ lastName: '', firstName: '', middleName: '', birthDate: '', phone: '', email: '' });

  const search = async () => {
    if (!searchPrefix.trim()) return;
    setLoading(true);
    try {
      const { data } = await personsApi.search(searchPrefix.trim());
      setPersons(data);
    } finally {
      setLoading(false);
    }
  };

  const openCreate = () => {
    setEditing(null);
    setForm({ lastName: '', firstName: '', middleName: '', birthDate: '', phone: '', email: '' });
    setModalOpen(true);
  };

  const openEdit = (p: PersonResponse) => {
    setEditing(p);
    setForm({
      lastName: p.lastName, firstName: p.firstName, middleName: p.middleName || '',
      birthDate: p.birthDate || '', phone: p.phone || '', email: p.email || '',
    });
    setModalOpen(true);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const payload = {
      lastName: form.lastName, firstName: form.firstName,
      middleName: form.middleName || undefined, birthDate: form.birthDate || undefined,
      phone: form.phone || undefined, email: form.email || undefined,
    };
    if (editing) {
      await personsApi.update(editing.id, { ...payload, archived: editing.archived });
    } else {
      await personsApi.create(payload);
    }
    setModalOpen(false);
    if (searchPrefix.trim()) search();
  };

  const toggleArchive = async (p: PersonResponse) => {
    await personsApi.archive(p.id, { archived: !p.archived });
    if (searchPrefix.trim()) search();
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-emerald-500 text-white p-2.5 rounded-lg"><UserCheck className="w-5 h-5" /></div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Люди</h1>
            <p className="text-sm text-gray-500">Управление студентами и контактами</p>
          </div>
        </div>
        <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium">
          <Plus className="w-4 h-4" /> Добавить
        </button>
      </div>

      <div className="flex items-center gap-3">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            value={searchPrefix}
            onChange={(e) => setSearchPrefix(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && search()}
            placeholder="Поиск по фамилии..."
            className="w-full pl-10 pr-4 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
        <button onClick={search} className="px-4 py-2 bg-gray-100 hover:bg-gray-200 rounded-lg text-sm font-medium text-gray-700 transition-colors">Найти</button>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="flex justify-center py-12"><div className="animate-spin rounded-full h-8 w-8 border-2 border-primary-600 border-t-transparent" /></div>
        ) : persons.length === 0 ? (
          <EmptyState message="Введите фамилию для поиска" />
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="text-left py-3 px-4 font-medium text-gray-500">ID</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Фамилия</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Имя</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Отчество</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Телефон</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Email</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Статус</th>
                <th className="text-right py-3 px-4 font-medium text-gray-500">Действия</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {persons.map((p) => (
                <tr key={p.id} className="hover:bg-gray-50">
                  <td className="py-3 px-4 text-gray-500 font-mono">{p.id}</td>
                  <td className="py-3 px-4 font-medium text-gray-900">{p.lastName}</td>
                  <td className="py-3 px-4 text-gray-700">{p.firstName}</td>
                  <td className="py-3 px-4 text-gray-600">{p.middleName || '—'}</td>
                  <td className="py-3 px-4 text-gray-600">{p.phone || '—'}</td>
                  <td className="py-3 px-4 text-gray-600">{p.email || '—'}</td>
                  <td className="py-3 px-4"><Badge variant={p.archived ? 'gray' : 'green'}>{p.archived ? 'Архив' : 'Активен'}</Badge></td>
                  <td className="py-3 px-4 text-right">
                    <div className="flex items-center justify-end gap-1">
                      <button onClick={() => openEdit(p)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-500"><Pencil className="w-4 h-4" /></button>
                      <button onClick={() => toggleArchive(p)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-500">
                        {p.archived ? <ArchiveRestore className="w-4 h-4" /> : <Archive className="w-4 h-4" />}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Редактировать' : 'Новый человек'} wide>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Фамилия</label>
              <input value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Имя</label>
              <input value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Отчество</label>
              <input value={form.middleName} onChange={(e) => setForm({ ...form, middleName: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
          </div>
          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Дата рождения</label>
              <input type="date" value={form.birthDate} onChange={(e) => setForm({ ...form, birthDate: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Телефон</label>
              <input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
              <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
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
