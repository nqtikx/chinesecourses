import { useEffect, useState, type FormEvent } from 'react';
import { teachersApi, personsApi } from '../api';
import type { TeacherResponse, PersonResponse } from '../types';
import Modal from '../components/ui/Modal';
import Badge from '../components/ui/Badge';
import EmptyState from '../components/ui/EmptyState';
import ConfirmDialog from '../components/ui/ConfirmDialog';
import { toast } from '../components/ui/Toast';
import { Plus, Archive, ArchiveRestore, GraduationCap, Phone, Mail, Search } from 'lucide-react';

interface TeacherRow extends TeacherResponse {
  person?: PersonResponse;
}

export default function TeachersPage() {
  const [teachers, setTeachers] = useState<TeacherRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [personSearch, setPersonSearch] = useState('');
  const [personResults, setPersonResults] = useState<PersonResponse[]>([]);
  const [selectedPersonId, setSelectedPersonId] = useState('');
  const [archiveTarget, setArchiveTarget] = useState<TeacherRow | null>(null);

  const loadTeachers = async () => {
    setLoading(true);
    try {
      const { data } = await teachersApi.list();
      const enriched: TeacherRow[] = await Promise.all(data.map(async (t) => {
        try {
          const { data: p } = await personsApi.get(t.personId);
          return { ...t, person: p };
        } catch { return t; }
      }));
      enriched.sort((a, b) => {
        const na = a.person ? a.person.lastName : '';
        const nb = b.person ? b.person.lastName : '';
        return na.localeCompare(nb);
      });
      setTeachers(enriched);
    } catch { toast('error', 'Не удалось загрузить преподавателей'); }
    setLoading(false);
  };

  useEffect(() => { loadTeachers(); }, []);

  const searchPersons = async () => {
    if (personSearch.trim().length < 1) return;
    try {
      const { data } = await personsApi.search(personSearch.trim());
      setPersonResults(data);
    } catch { setPersonResults([]); }
  };

  const handleCreate = async (e: FormEvent) => {
    e.preventDefault();
    if (!selectedPersonId) return;
    try {
      await teachersApi.create(Number(selectedPersonId));
      toast('success', 'Преподаватель создан');
      setModalOpen(false);
      setSelectedPersonId('');
      setPersonSearch('');
      setPersonResults([]);
      loadTeachers();
    } catch { toast('error', 'Ошибка создания. Возможно, преподаватель уже существует для этой персоны.'); }
  };

  const confirmArchive = async () => {
    if (!archiveTarget) return;
    try {
      await teachersApi.archive(archiveTarget.id, { archived: !archiveTarget.archived });
      toast('success', archiveTarget.archived ? 'Преподаватель восстановлен' : 'Преподаватель архивирован');
      loadTeachers();
    } catch { toast('error', 'Ошибка'); }
    setArchiveTarget(null);
  };

  const fullName = (p?: PersonResponse) => p ? [p.lastName, p.firstName, p.middleName].filter(Boolean).join(' ') : 'Неизвестно';

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-amber-500 text-white p-2.5 rounded-lg"><GraduationCap className="w-5 h-5" /></div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Преподаватели</h1>
            <p className="text-sm text-gray-500">Преподавательский состав ({teachers.filter(t => !t.archived).length} активных)</p>
          </div>
        </div>
        <button onClick={() => { setModalOpen(true); setSelectedPersonId(''); setPersonSearch(''); setPersonResults([]); }} className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium transition-colors">
          <Plus className="w-4 h-4" /> Создать преподавателя
        </button>
      </div>

      {loading ? (
        <div className="flex justify-center py-16"><div className="animate-spin rounded-full h-8 w-8 border-2 border-primary-600 border-t-transparent" /></div>
      ) : teachers.length === 0 ? (
        <div className="bg-white rounded-xl border border-gray-200"><EmptyState message="Преподаватели не найдены" /></div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {teachers.map((t) => (
            <div key={t.id} className={`bg-white rounded-xl border ${t.archived ? 'border-gray-200 opacity-60' : 'border-gray-200'} p-5 hover:shadow-md transition-shadow`}>
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <div className={`w-12 h-12 rounded-full flex items-center justify-center text-lg font-bold ${t.archived ? 'bg-gray-100 text-gray-400' : 'bg-amber-100 text-amber-600'}`}>
                    {t.person ? `${t.person.lastName[0]}${t.person.firstName[0]}` : '??'}
                  </div>
                  <div>
                    <div className="font-semibold text-gray-900">{fullName(t.person)}</div>
                    <div className="text-xs text-gray-400">ID: {t.id} | Персона: #{t.personId}</div>
                  </div>
                </div>
                <div className="flex items-center gap-1">
                  <Badge variant={t.archived ? 'gray' : 'green'}>{t.archived ? 'Архив' : 'Активен'}</Badge>
                  <button onClick={() => setArchiveTarget(t)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-400 transition-colors">
                    {t.archived ? <ArchiveRestore className="w-4 h-4" /> : <Archive className="w-4 h-4" />}
                  </button>
                </div>
              </div>
              {t.person && (
                <div className="mt-3 pt-3 border-t border-gray-100 space-y-1.5">
                  {t.person.phone && (
                    <div className="flex items-center gap-2 text-sm text-gray-600">
                      <Phone className="w-3.5 h-3.5 text-gray-400" /> {t.person.phone}
                    </div>
                  )}
                  {t.person.email && (
                    <div className="flex items-center gap-2 text-sm text-gray-600">
                      <Mail className="w-3.5 h-3.5 text-gray-400" /> {t.person.email}
                    </div>
                  )}
                  <div className="text-xs text-gray-400">
                    Создан: {new Date(t.createdAt).toLocaleDateString('ru')}
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title="Создать преподавателя">
        <form onSubmit={handleCreate} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Поиск персоны по фамилии</label>
            <div className="flex gap-2 mb-2">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  value={personSearch}
                  onChange={(e) => setPersonSearch(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), searchPersons())}
                  placeholder="Введите фамилию..."
                  className="w-full pl-10 pr-4 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500"
                />
              </div>
              <button type="button" onClick={searchPersons} className="px-3 py-2 bg-gray-100 hover:bg-gray-200 rounded-lg text-sm text-gray-700">Найти</button>
            </div>
            {personResults.length > 0 && (
              <div className="border border-gray-200 rounded-lg max-h-48 overflow-y-auto divide-y divide-gray-100">
                {personResults.map((p) => (
                  <button
                    key={p.id}
                    type="button"
                    onClick={() => { setSelectedPersonId(p.id.toString()); setPersonResults([]); setPersonSearch([p.lastName, p.firstName].join(' ')); }}
                    className={`w-full text-left px-3 py-2.5 text-sm hover:bg-primary-50 ${selectedPersonId === p.id.toString() ? 'bg-primary-50 text-primary-700' : 'text-gray-700'}`}
                  >
                    <span className="font-medium">{p.lastName} {p.firstName}</span>
                    {p.middleName && <span className="text-gray-400"> {p.middleName}</span>}
                    <span className="text-gray-400 ml-2">#{p.id}</span>
                    {p.email && <span className="text-gray-400 ml-2">({p.email})</span>}
                  </button>
                ))}
              </div>
            )}
            {selectedPersonId && <p className="text-xs text-emerald-600 mt-1">Выбрана персона: ID {selectedPersonId}</p>}
          </div>
          <p className="text-xs text-gray-500">Преподаватель привязывается к существующей записи персоны. Сначала создайте персону в разделе «Абитуриенты и слушатели».</p>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setModalOpen(false)} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800 transition-colors">Отмена</button>
            <button type="submit" disabled={!selectedPersonId} className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium transition-colors disabled:opacity-50">Создать</button>
          </div>
        </form>
      </Modal>

      {archiveTarget && (
        <ConfirmDialog
          open
          title={archiveTarget.archived ? 'Восстановить?' : 'Архивировать?'}
          message={`Преподаватель ${fullName(archiveTarget.person)} будет ${archiveTarget.archived ? 'восстановлен' : 'архивирован'}.`}
          confirmLabel={archiveTarget.archived ? 'Восстановить' : 'Архивировать'}
          onConfirm={confirmArchive}
          onCancel={() => setArchiveTarget(null)}
        />
      )}
    </div>
  );
}
