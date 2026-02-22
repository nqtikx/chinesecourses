import { useState, type FormEvent } from 'react';
import { attendanceApi } from '../api';
import type { AttendanceResponse, AttendanceStatus } from '../types';
import { useAuth } from '../context/AuthContext';
import Modal from '../components/ui/Modal';
import Badge from '../components/ui/Badge';
import EmptyState from '../components/ui/EmptyState';
import { Plus, Pencil, Archive, ArchiveRestore, CheckSquare, Search } from 'lucide-react';

const STATUS_LABELS: Record<AttendanceStatus, string> = { PRESENT: 'Присутствовал', ABSENT: 'Отсутствовал', LATE: 'Опоздал', EXCUSED: 'Уваж. причина' };
const STATUS_COLORS: Record<AttendanceStatus, 'green' | 'red' | 'yellow' | 'blue'> = { PRESENT: 'green', ABSENT: 'red', LATE: 'yellow', EXCUSED: 'blue' };
const STATUSES: AttendanceStatus[] = ['PRESENT', 'ABSENT', 'LATE', 'EXCUSED'];

export default function AttendancePage() {
  const { isAdmin, isTeacher } = useAuth();
  const canEdit = isAdmin || isTeacher;
  const [sessionId, setSessionId] = useState('');
  const [records, setRecords] = useState<AttendanceResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AttendanceResponse | null>(null);
  const [form, setForm] = useState({ lessonSessionId: '', enrollmentId: '', status: 'PRESENT' as AttendanceStatus, comment: '' });

  const searchBySession = async () => {
    if (!sessionId.trim()) return;
    setLoading(true);
    try {
      const { data } = await attendanceApi.listBySession(Number(sessionId));
      setRecords(data);
    } finally { setLoading(false); }
  };

  const reload = async () => {
    if (sessionId.trim()) {
      const { data } = await attendanceApi.listBySession(Number(sessionId));
      setRecords(data);
    }
  };

  const openCreate = () => {
    setEditing(null);
    setForm({ lessonSessionId: sessionId, enrollmentId: '', status: 'PRESENT', comment: '' });
    setModalOpen(true);
  };

  const openEdit = (a: AttendanceResponse) => {
    setEditing(a);
    setForm({ lessonSessionId: a.lessonSessionId.toString(), enrollmentId: a.enrollmentId.toString(), status: a.status, comment: a.comment || '' });
    setModalOpen(true);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (editing) {
      await attendanceApi.update(editing.id, { status: form.status, comment: form.comment || undefined, archived: editing.archived });
    } else {
      await attendanceApi.create({ lessonSessionId: Number(form.lessonSessionId), enrollmentId: Number(form.enrollmentId), status: form.status, comment: form.comment || undefined });
    }
    setModalOpen(false);
    reload();
  };

  const toggleArchive = async (a: AttendanceResponse) => {
    await attendanceApi.archive(a.id, { archived: !a.archived });
    reload();
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-teal-500 text-white p-2.5 rounded-lg"><CheckSquare className="w-5 h-5" /></div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Посещаемость</h1>
            <p className="text-sm text-gray-500">
              {canEdit ? 'Управление посещаемостью занятий' : 'Просмотр посещаемости'}
            </p>
          </div>
        </div>
        {canEdit && (
          <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium">
            <Plus className="w-4 h-4" /> Отметить посещение
          </button>
        )}
      </div>

      <div className="flex items-center gap-3">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            value={sessionId}
            onChange={(e) => setSessionId(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && searchBySession()}
            placeholder="ID занятия..."
            className="w-full pl-10 pr-4 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
        <button onClick={searchBySession} className="px-4 py-2 bg-gray-100 hover:bg-gray-200 rounded-lg text-sm font-medium text-gray-700 transition-colors">Загрузить</button>
      </div>

      {canEdit && (
        <div className="bg-blue-50 border border-blue-200 rounded-xl p-4 text-sm text-blue-700">
          {isTeacher && !isAdmin
            ? 'Как преподаватель, вы можете создавать и редактировать записи посещаемости для занятий своих групп.'
            : 'Администратор может управлять всеми записями посещаемости.'}
        </div>
      )}

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="flex justify-center py-12"><div className="animate-spin rounded-full h-8 w-8 border-2 border-primary-600 border-t-transparent" /></div>
        ) : records.length === 0 ? (
          <EmptyState message="Введите ID занятия для просмотра посещаемости" />
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="text-left py-3 px-4 font-medium text-gray-500">ID</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Запись ID</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Статус</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Комментарий</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Отмечено</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Архив</th>
                {canEdit && <th className="text-right py-3 px-4 font-medium text-gray-500">Действия</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {records.map((a) => (
                <tr key={a.id} className="hover:bg-gray-50">
                  <td className="py-3 px-4 text-gray-500 font-mono">{a.id}</td>
                  <td className="py-3 px-4 text-gray-700">{a.enrollmentId}</td>
                  <td className="py-3 px-4"><Badge variant={STATUS_COLORS[a.status]}>{STATUS_LABELS[a.status]}</Badge></td>
                  <td className="py-3 px-4 text-gray-600 max-w-xs truncate">{a.comment || '—'}</td>
                  <td className="py-3 px-4 text-gray-600">{new Date(a.markedAt).toLocaleString('ru')}</td>
                  <td className="py-3 px-4"><Badge variant={a.archived ? 'gray' : 'green'}>{a.archived ? 'Да' : 'Нет'}</Badge></td>
                  {canEdit && (
                    <td className="py-3 px-4 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button onClick={() => openEdit(a)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-500"><Pencil className="w-4 h-4" /></button>
                        <button onClick={() => toggleArchive(a)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-500">
                          {a.archived ? <ArchiveRestore className="w-4 h-4" /> : <Archive className="w-4 h-4" />}
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

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Редактировать посещение' : 'Отметить посещение'}>
        <form onSubmit={handleSubmit} className="space-y-4">
          {!editing && (
            <>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">ID занятия</label>
                <input value={form.lessonSessionId} onChange={(e) => setForm({ ...form, lessonSessionId: e.target.value })} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">ID записи (enrollment)</label>
                <input value={form.enrollmentId} onChange={(e) => setForm({ ...form, enrollmentId: e.target.value })} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
              </div>
            </>
          )}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Статус</label>
            <select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value as AttendanceStatus })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
              {STATUSES.map((s) => <option key={s} value={s}>{STATUS_LABELS[s]}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Комментарий</label>
            <input value={form.comment} onChange={(e) => setForm({ ...form, comment: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
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
