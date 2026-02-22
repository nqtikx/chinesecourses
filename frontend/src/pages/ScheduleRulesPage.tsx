import { useEffect, useState, type FormEvent } from 'react';
import { scheduleRulesApi, studyGroupsApi, semestersApi, coursesApi } from '../api';
import type { GroupScheduleRuleResponse, StudyGroupResponse, SemesterResponse, CourseResponse } from '../types';
import Modal from '../components/ui/Modal';
import Badge from '../components/ui/Badge';
import EmptyState from '../components/ui/EmptyState';
import { Plus, Pencil, Archive, ArchiveRestore, CalendarClock } from 'lucide-react';

const DAYS = ['', 'Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота', 'Воскресенье'];

export default function ScheduleRulesPage() {
  const [courses, setCourses] = useState<CourseResponse[]>([]);
  const [semesters, setSemesters] = useState<SemesterResponse[]>([]);
  const [groups, setGroups] = useState<StudyGroupResponse[]>([]);
  const [selectedCourse, setSelectedCourse] = useState<number | null>(null);
  const [selectedSemester, setSelectedSemester] = useState<number | null>(null);
  const [selectedGroup, setSelectedGroup] = useState<number | null>(null);
  const [rules, setRules] = useState<GroupScheduleRuleResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<GroupScheduleRuleResponse | null>(null);
  const [form, setForm] = useState({ groupId: 0, dayOfWeek: 1, startTime: '', endTime: '', room: '', active: true });

  useEffect(() => { coursesApi.list().then(({ data }) => { setCourses(data); if (data.length) setSelectedCourse(data[0].id); }); }, []);

  useEffect(() => {
    if (selectedCourse) {
      semestersApi.listByCourse(selectedCourse).then(({ data }) => {
        setSemesters(data);
        if (data.length) setSelectedSemester(data[0].id); else { setSelectedSemester(null); setGroups([]); setSelectedGroup(null); }
      });
    }
  }, [selectedCourse]);

  useEffect(() => {
    if (selectedSemester) {
      studyGroupsApi.listBySemester(selectedSemester).then(({ data }) => {
        setGroups(data);
        if (data.length) setSelectedGroup(data[0].id); else setSelectedGroup(null);
      });
    }
  }, [selectedSemester]);

  useEffect(() => {
    if (selectedGroup) {
      setLoading(true);
      scheduleRulesApi.listByGroup(selectedGroup).then(({ data }) => setRules(data)).finally(() => setLoading(false));
    } else { setRules([]); }
  }, [selectedGroup]);

  const reload = async () => {
    if (selectedGroup) { const { data } = await scheduleRulesApi.listByGroup(selectedGroup); setRules(data); }
  };

  const openCreate = () => {
    setEditing(null);
    setForm({ groupId: selectedGroup || 0, dayOfWeek: 1, startTime: '', endTime: '', room: '', active: true });
    setModalOpen(true);
  };

  const openEdit = (r: GroupScheduleRuleResponse) => {
    setEditing(r);
    setForm({ groupId: r.groupId, dayOfWeek: r.dayOfWeek, startTime: r.startTime, endTime: r.endTime, room: r.room || '', active: r.active });
    setModalOpen(true);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const payload = { groupId: form.groupId, dayOfWeek: form.dayOfWeek, startTime: form.startTime, endTime: form.endTime, room: form.room || undefined, active: form.active };
    if (editing) {
      await scheduleRulesApi.update(editing.id, { ...payload, archived: editing.archived });
    } else {
      await scheduleRulesApi.create(payload);
    }
    setModalOpen(false);
    reload();
  };

  const toggleArchive = async (r: GroupScheduleRuleResponse) => {
    await scheduleRulesApi.archive(r.id, { archived: !r.archived });
    reload();
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-orange-500 text-white p-2.5 rounded-lg"><CalendarClock className="w-5 h-5" /></div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Расписание</h1>
            <p className="text-sm text-gray-500">Правила расписания для учебных групп</p>
          </div>
        </div>
        <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium">
          <Plus className="w-4 h-4" /> Добавить правило
        </button>
      </div>

      <div className="flex flex-wrap items-center gap-4">
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
        <div className="flex items-center gap-2">
          <label className="text-sm font-medium text-gray-600">Группа:</label>
          <select value={selectedGroup || ''} onChange={(e) => setSelectedGroup(Number(e.target.value))} className="px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
            {groups.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
          </select>
        </div>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="flex justify-center py-12"><div className="animate-spin rounded-full h-8 w-8 border-2 border-primary-600 border-t-transparent" /></div>
        ) : rules.length === 0 ? (
          <EmptyState message="Правила расписания не найдены" />
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="text-left py-3 px-4 font-medium text-gray-500">ID</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">День</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Начало</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Конец</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Аудитория</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Активно</th>
                <th className="text-left py-3 px-4 font-medium text-gray-500">Архив</th>
                <th className="text-right py-3 px-4 font-medium text-gray-500">Действия</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {rules.map((r) => (
                <tr key={r.id} className="hover:bg-gray-50">
                  <td className="py-3 px-4 text-gray-500 font-mono">{r.id}</td>
                  <td className="py-3 px-4 text-gray-900 font-medium">{DAYS[r.dayOfWeek]}</td>
                  <td className="py-3 px-4 text-gray-700">{r.startTime}</td>
                  <td className="py-3 px-4 text-gray-700">{r.endTime}</td>
                  <td className="py-3 px-4 text-gray-600">{r.room || '—'}</td>
                  <td className="py-3 px-4"><Badge variant={r.active ? 'green' : 'gray'}>{r.active ? 'Да' : 'Нет'}</Badge></td>
                  <td className="py-3 px-4"><Badge variant={r.archived ? 'gray' : 'green'}>{r.archived ? 'Да' : 'Нет'}</Badge></td>
                  <td className="py-3 px-4 text-right">
                    <div className="flex items-center justify-end gap-1">
                      <button onClick={() => openEdit(r)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-500"><Pencil className="w-4 h-4" /></button>
                      <button onClick={() => toggleArchive(r)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-500">
                        {r.archived ? <ArchiveRestore className="w-4 h-4" /> : <Archive className="w-4 h-4" />}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Редактировать правило' : 'Новое правило расписания'}>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">День недели</label>
            <select value={form.dayOfWeek} onChange={(e) => setForm({ ...form, dayOfWeek: Number(e.target.value) })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
              {[1,2,3,4,5,6,7].map((d) => <option key={d} value={d}>{DAYS[d]}</option>)}
            </select>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Начало</label>
              <input type="time" value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Конец</label>
              <input type="time" value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Аудитория</label>
            <input value={form.room} onChange={(e) => setForm({ ...form, room: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
          </div>
          <div className="flex items-center gap-2">
            <input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} id="active" className="rounded border-gray-300" />
            <label htmlFor="active" className="text-sm text-gray-700">Активное правило</label>
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
