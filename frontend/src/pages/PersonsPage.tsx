import { useEffect, useState, type FormEvent } from 'react';
import { classProfilesApi, enrollmentsApi, personsApi } from '../api';
import type { ClassProfileGroupItemResponse, PersonResponse } from '../types';
import Modal from '../components/ui/Modal';
import Badge from '../components/ui/Badge';
import EmptyState from '../components/ui/EmptyState';
import { toast } from '../components/ui/Toast';
import { Plus, Pencil, UserCheck, Search, Phone, Mail, Calendar } from 'lucide-react';

export default function PersonsPage() {
  const [persons, setPersons] = useState<PersonResponse[]>([]);
  const [searchPrefix, setSearchPrefix] = useState('');
  const [searched, setSearched] = useState(false);
  const [groupOptions, setGroupOptions] = useState<ClassProfileGroupItemResponse[]>([]);
  const [groupFilter, setGroupFilter] = useState<string>('ALL');
  const [personIdsInGroup, setPersonIdsInGroup] = useState<Set<number> | null>(null);
  const [genderFilter, setGenderFilter] = useState<'ALL' | 'MALE' | 'FEMALE' | 'UNKNOWN'>('ALL');
  const [sortBy, setSortBy] = useState<'ID' | 'LAST_NAME'>('ID');
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<PersonResponse | null>(null);
  const [form, setForm] = useState({
    lastName: '', firstName: '', middleName: '', birthDate: '', phone: '', email: '',
    residentialAddress: '', documentType: '', documentSeries: '', documentNumber: '', documentIssueDate: '',
    documentIssuedBy: '', documentIdentificationNumber: '', guardianFullName: '', guardianPhone: '', guardianRelation: '',
  });
  const [viewMode, setViewMode] = useState<'cards' | 'table'>('cards');

  const search = async () => {
    setLoading(true);
    setSearched(true);
    try { const { data } = await personsApi.search(searchPrefix.trim()); setPersons(data); } catch { toast('error', 'Ошибка поиска'); } finally { setLoading(false); }
  };

  useEffect(() => { search(); }, []); // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => {
    classProfilesApi.groups()
      .then(({ data }) => setGroupOptions(data))
      .catch(() => setGroupOptions([]));
  }, []);

  useEffect(() => {
    if (groupFilter === 'ALL') {
      setPersonIdsInGroup(null);
      return;
    }
    enrollmentsApi.listByGroup(Number(groupFilter))
      .then(({ data }) => {
        const ids = new Set<number>();
        data.forEach((e) => {
          if (!e.archived) ids.add(e.studentId);
        });
        setPersonIdsInGroup(ids);
      })
      .catch(() => setPersonIdsInGroup(new Set<number>()));
  }, [groupFilter]);

  const openCreate = () => {
    setEditing(null);
    setForm({
      lastName: '', firstName: '', middleName: '', birthDate: '', phone: '', email: '',
      residentialAddress: '', documentType: '', documentSeries: '', documentNumber: '', documentIssueDate: '',
      documentIssuedBy: '', documentIdentificationNumber: '', guardianFullName: '', guardianPhone: '', guardianRelation: '',
    });
    setModalOpen(true);
  };

  const openEdit = (p: PersonResponse) => {
    setEditing(p);
    const primaryGuardian = p.guardians?.find(g => g.primaryGuardian) || p.guardians?.[0];
    setForm({
      lastName: p.lastName,
      firstName: p.firstName,
      middleName: p.middleName || '',
      birthDate: p.birthDate || '',
      phone: p.phone || '',
      email: p.email || '',
      residentialAddress: p.residentialAddress || '',
      documentType: p.documentType || '',
      documentSeries: p.documentSeries || '',
      documentNumber: p.documentNumber || '',
      documentIssueDate: p.documentIssueDate || '',
      documentIssuedBy: p.documentIssuedBy || '',
      documentIdentificationNumber: p.documentIdentificationNumber || '',
      guardianFullName: primaryGuardian?.fullName || '',
      guardianPhone: primaryGuardian?.phone || '',
      guardianRelation: primaryGuardian?.relationType || '',
    });
    setModalOpen(true);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const payload = {
      lastName: form.lastName,
      firstName: form.firstName,
      middleName: form.middleName || undefined,
      birthDate: form.birthDate || undefined,
      phone: form.phone || undefined,
      email: form.email || undefined,
      residentialAddress: form.residentialAddress || undefined,
      documentType: form.documentType || undefined,
      documentSeries: form.documentSeries || undefined,
      documentNumber: form.documentNumber || undefined,
      documentIssueDate: form.documentIssueDate || undefined,
      documentIssuedBy: form.documentIssuedBy || undefined,
      documentIdentificationNumber: form.documentIdentificationNumber || undefined,
      guardians: form.guardianFullName ? [{
        fullName: form.guardianFullName,
        phone: form.guardianPhone || undefined,
        relationType: form.guardianRelation || undefined,
        primaryGuardian: true,
        archived: false,
      }] : [],
    };
    try {
      if (editing) { await personsApi.update(editing.id, { ...payload, archived: editing.archived }); toast('success', 'Данные обновлены'); }
      else { await personsApi.create(payload); toast('success', 'Персона добавлена'); }
      setModalOpen(false);
      search();
    } catch { toast('error', 'Ошибка сохранения'); }
  };

  const fullName = (p: PersonResponse) => [p.lastName, p.firstName, p.middleName].filter(Boolean).join(' ');
  const detectGender = (p: PersonResponse): 'MALE' | 'FEMALE' | 'UNKNOWN' => {
    const middle = (p.middleName || '').trim().toLowerCase();
    if (!middle) return 'UNKNOWN';
    return middle.endsWith('на') ? 'FEMALE' : 'MALE';
  };
  const filteredPersons = persons
    .filter((p) => {
      if (groupFilter !== 'ALL' && personIdsInGroup && !personIdsInGroup.has(p.id)) return false;
      if (genderFilter !== 'ALL' && detectGender(p) !== genderFilter) return false;
      return true;
    })
    .sort((a, b) => {
      if (sortBy === 'LAST_NAME') {
        const ln = a.lastName.localeCompare(b.lastName, 'ru', { sensitivity: 'base' });
        if (ln !== 0) return ln;
        return a.id - b.id;
      }
      return a.id - b.id;
    });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-emerald-500 text-white p-2.5 rounded-lg"><UserCheck className="w-5 h-5" /></div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Абитуриенты и слушатели</h1>
            <p className="text-sm text-gray-500">База данных всех лиц в системе</p>
          </div>
        </div>
        <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium transition-colors">
          <Plus className="w-4 h-4" /> Добавить персону
        </button>
      </div>

      <div className="flex items-center gap-3 flex-wrap">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input value={searchPrefix} onChange={(e) => setSearchPrefix(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && search()} placeholder="Поиск по фамилии..." className="w-full pl-10 pr-4 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
        </div>
        <button onClick={search} className="px-4 py-2 bg-gray-100 hover:bg-gray-200 rounded-lg text-sm font-medium text-gray-700 transition-colors">Найти</button>
        <select
          value={groupFilter}
          onChange={(e) => setGroupFilter(e.target.value)}
          className="px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500"
        >
          <option value="ALL">Все группы</option>
          {groupOptions.map((g) => (
            <option key={g.groupId} value={g.groupId}>
              {g.groupName}
            </option>
          ))}
        </select>
        <select
          value={genderFilter}
          onChange={(e) => setGenderFilter(e.target.value as 'ALL' | 'MALE' | 'FEMALE' | 'UNKNOWN')}
          className="px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500"
        >
          <option value="ALL">Любой пол</option>
          <option value="MALE">Мужской</option>
          <option value="FEMALE">Женский</option>
          <option value="UNKNOWN">Не указан</option>
        </select>
        <select
          value={sortBy}
          onChange={(e) => setSortBy(e.target.value as 'ID' | 'LAST_NAME')}
          className="px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500"
        >
          <option value="ID">Сортировка: ID</option>
          <option value="LAST_NAME">Сортировка: Фамилия</option>
        </select>
        <div className="flex items-center gap-1 bg-gray-100 rounded-lg p-0.5">
          <button onClick={() => setViewMode('cards')} className={`px-3 py-1.5 rounded-md text-xs font-medium transition-colors ${viewMode === 'cards' ? 'bg-white shadow text-gray-900' : 'text-gray-500'}`}>Карточки</button>
          <button onClick={() => setViewMode('table')} className={`px-3 py-1.5 rounded-md text-xs font-medium transition-colors ${viewMode === 'table' ? 'bg-white shadow text-gray-900' : 'text-gray-500'}`}>Таблица</button>
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center py-16"><div className="animate-spin rounded-full h-8 w-8 border-2 border-primary-600 border-t-transparent" /></div>
      ) : filteredPersons.length === 0 ? (
        <div className="bg-white rounded-xl border border-gray-200"><EmptyState message={searched ? 'Ничего не найдено' : 'Список пуст'} /></div>
      ) : viewMode === 'cards' ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredPersons.map((p) => (
            <div key={p.id} className={`bg-white rounded-xl border ${p.archived ? 'border-gray-200 opacity-60' : 'border-gray-200'} p-5 hover:shadow-md transition-shadow`}>
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <div className={`w-11 h-11 rounded-full flex items-center justify-center text-sm font-bold ${p.archived ? 'bg-gray-100 text-gray-400' : 'bg-emerald-100 text-emerald-600'}`}>
                    {p.lastName[0]}{p.firstName[0]}
                  </div>
                  <div>
                    <div className="font-semibold text-gray-900">{p.lastName} {p.firstName}</div>
                    {p.middleName && <div className="text-sm text-gray-500">{p.middleName}</div>}
                  </div>
                </div>
                <Badge variant={p.archived ? 'gray' : 'green'}>{p.archived ? 'Архив' : 'Активен'}</Badge>
              </div>
              <div className="mt-3 pt-3 border-t border-gray-100 space-y-1.5">
                {p.birthDate && (
                  <div className="flex items-center gap-2 text-sm text-gray-600">
                    <Calendar className="w-3.5 h-3.5 text-gray-400" />
                    {new Date(p.birthDate + 'T00:00').toLocaleDateString('ru')}
                  </div>
                )}
                {p.phone && (
                  <div className="flex items-center gap-2 text-sm text-gray-600">
                    <Phone className="w-3.5 h-3.5 text-gray-400" /> {p.phone}
                  </div>
                )}
                {p.email && (
                  <div className="flex items-center gap-2 text-sm text-gray-600">
                    <Mail className="w-3.5 h-3.5 text-gray-400" /> {p.email}
                  </div>
                )}
                {p.residentialAddress && (
                  <div className="text-sm text-gray-600">Адрес: {p.residentialAddress}</div>
                )}
                <div className="flex items-center justify-between pt-2">
                  <span className="text-xs text-gray-400">ID: {p.id}</span>
                  <div className="flex items-center gap-1">
                    <button onClick={() => openEdit(p)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-400 transition-colors"><Pencil className="w-4 h-4" /></button>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="text-left py-3 px-4 font-medium text-gray-500">ID</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">ФИО</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Телефон</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Email</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Дата рожд.</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Статус</th>
                <th className="text-right py-3 px-4 font-medium text-gray-500">Действия</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filteredPersons.map((p) => (
                <tr key={p.id} className="hover:bg-gray-50">
                  <td className="py-3 px-4 text-gray-400 font-mono text-xs">{p.id}</td>
                  <td className="py-3 px-4 font-medium text-gray-900">{fullName(p)}</td>
                  <td className="py-3 px-4 text-gray-600">{p.phone || '—'}</td>
                  <td className="py-3 px-4 text-gray-600">{p.email || '—'}</td>
                  <td className="py-3 px-4 text-gray-600 text-xs">{p.birthDate ? new Date(p.birthDate + 'T00:00').toLocaleDateString('ru') : '—'}</td>
                  <td className="py-3 px-4"><Badge variant={p.archived ? 'gray' : 'green'}>{p.archived ? 'Архив' : 'Активен'}</Badge></td>
                  <td className="py-3 px-4 text-right">
                    <div className="flex items-center justify-end gap-1">
                      <button onClick={() => openEdit(p)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-500 transition-colors"><Pencil className="w-4 h-4" /></button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Редактировать данные' : 'Добавить персону'} wide>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-3 gap-4">
            <div><label className="block text-sm font-medium text-gray-700 mb-1">Фамилия *</label><input value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" /></div>
            <div><label className="block text-sm font-medium text-gray-700 mb-1">Имя *</label><input value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" /></div>
            <div><label className="block text-sm font-medium text-gray-700 mb-1">Отчество</label><input value={form.middleName} onChange={(e) => setForm({ ...form, middleName: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" /></div>
          </div>
          <div className="grid grid-cols-3 gap-4">
            <div><label className="block text-sm font-medium text-gray-700 mb-1">Дата рождения</label><input type="date" value={form.birthDate} onChange={(e) => setForm({ ...form, birthDate: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" /></div>
            <div><label className="block text-sm font-medium text-gray-700 mb-1">Телефон</label><input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" placeholder="+375..." /></div>
            <div><label className="block text-sm font-medium text-gray-700 mb-1">Email</label><input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" /></div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div><label className="block text-sm font-medium text-gray-700 mb-1">Адрес проживания</label><input value={form.residentialAddress} onChange={(e) => setForm({ ...form, residentialAddress: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" /></div>
            <div><label className="block text-sm font-medium text-gray-700 mb-1">Вид документа</label><input value={form.documentType} onChange={(e) => setForm({ ...form, documentType: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" /></div>
            <div><label className="block text-sm font-medium text-gray-700 mb-1">Серия</label><input value={form.documentSeries} onChange={(e) => setForm({ ...form, documentSeries: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" /></div>
            <div><label className="block text-sm font-medium text-gray-700 mb-1">Номер</label><input value={form.documentNumber} onChange={(e) => setForm({ ...form, documentNumber: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" /></div>
            <div><label className="block text-sm font-medium text-gray-700 mb-1">Дата выдачи</label><input type="date" value={form.documentIssueDate} onChange={(e) => setForm({ ...form, documentIssueDate: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" /></div>
            <div><label className="block text-sm font-medium text-gray-700 mb-1">Идентификационный номер</label><input value={form.documentIdentificationNumber} onChange={(e) => setForm({ ...form, documentIdentificationNumber: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" /></div>
            <div className="col-span-2"><label className="block text-sm font-medium text-gray-700 mb-1">Кем выдан</label><input value={form.documentIssuedBy} onChange={(e) => setForm({ ...form, documentIssuedBy: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" /></div>
          </div>
          <div className="grid grid-cols-3 gap-4">
            <div><label className="block text-sm font-medium text-gray-700 mb-1">ФИО родителя</label><input value={form.guardianFullName} onChange={(e) => setForm({ ...form, guardianFullName: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" /></div>
            <div><label className="block text-sm font-medium text-gray-700 mb-1">Телефон родителя</label><input value={form.guardianPhone} onChange={(e) => setForm({ ...form, guardianPhone: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" /></div>
            <div><label className="block text-sm font-medium text-gray-700 mb-1">Кто это</label><input value={form.guardianRelation} onChange={(e) => setForm({ ...form, guardianRelation: e.target.value })} placeholder="мать/отец" className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" /></div>
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setModalOpen(false)} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800 transition-colors">Отмена</button>
            <button type="submit" className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium transition-colors">{editing ? 'Сохранить' : 'Создать'}</button>
          </div>
        </form>
      </Modal>

    </div>
  );
}
