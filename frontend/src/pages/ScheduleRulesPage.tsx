import { useEffect, useState, type FormEvent } from 'react';
import { scheduleRulesApi, studyGroupsApi, semestersApi, coursesApi } from '../api';
import type { GroupScheduleRuleResponse, StudyGroupResponse, SemesterResponse, CourseResponse } from '../types';
import Modal from '../components/ui/Modal';
import Badge from '../components/ui/Badge';
import EmptyState from '../components/ui/EmptyState';
import { toast } from '../components/ui/Toast';
import { Plus, Pencil, CalendarClock, Clock, MapPin, Power, PowerOff } from 'lucide-react';

const DAYS = ['', 'Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота', 'Воскресенье'];
const DAY_SHORT = ['', 'Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'];
const DAY_COLORS = ['', 'bg-red-50 border-red-200', 'bg-orange-50 border-orange-200', 'bg-yellow-50 border-yellow-200', 'bg-green-50 border-green-200', 'bg-blue-50 border-blue-200', 'bg-indigo-50 border-indigo-200', 'bg-purple-50 border-purple-200'];
const DAY_TEXT = ['', 'text-red-700', 'text-orange-700', 'text-yellow-700', 'text-green-700', 'text-blue-700', 'text-indigo-700', 'text-purple-700'];

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
        setSelectedSemester(data.length ? data[0].id : null);
      });
    }
  }, [selectedCourse]);

  useEffect(() => {
    if (selectedSemester) {
      studyGroupsApi.listBySemester(selectedSemester).then(({ data }) => {
        setGroups(data);
        setSelectedGroup(data.length ? data[0].id : null);
      });
    } else { setGroups([]); setSelectedGroup(null); }
  }, [selectedSemester]);

  useEffect(() => {
    if (selectedGroup) {
      setLoading(true);
      scheduleRulesApi.listByGroup(selectedGroup).then(({ data }) => setRules(data)).finally(() => setLoading(false));
    } else { setRules([]); }
  }, [selectedGroup]);

  const reload = async () => {
    if (selectedGroup) {
      const { data } = await scheduleRulesApi.listByGroup(selectedGroup);
      setRules(data);
    }
  };

  const openCreate = () => {
    setEditing(null);
    setForm({ groupId: selectedGroup || 0, dayOfWeek: 1, startTime: '09:00', endTime: '10:30', room: '', active: true });
    setModalOpen(true);
  };

  const openEdit = (r: GroupScheduleRuleResponse) => {
    setEditing(r);
    setForm({ groupId: r.groupId, dayOfWeek: r.dayOfWeek, startTime: r.startTime, endTime: r.endTime, room: r.room || '', active: r.active });
    setModalOpen(true);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    try {
      const payload = { groupId: form.groupId, dayOfWeek: form.dayOfWeek, startTime: form.startTime, endTime: form.endTime, room: form.room || undefined, active: form.active };
      if (editing) {
        await scheduleRulesApi.update(editing.id, { ...payload, archived: editing.archived });
        toast('success', 'Правило обновлено');
      } else {
        await scheduleRulesApi.create(payload);
        toast('success', 'Правило создано');
      }
      setModalOpen(false);
      reload();
    } catch { toast('error', 'Ошибка сохранения'); }
  };

  const toggleActive = async (r: GroupScheduleRuleResponse) => {
    try {
      await scheduleRulesApi.update(r.id, {
        groupId: r.groupId, dayOfWeek: r.dayOfWeek, startTime: r.startTime, endTime: r.endTime,
        room: r.room ?? undefined, active: !r.active, archived: r.archived,
      });
      toast('success', r.active ? 'Правило приостановлено' : 'Правило активировано');
      reload();
    } catch { toast('error', 'Ошибка'); }
  };

  const activeRules = rules.filter(r => !r.archived);
  const byDay: Record<number, GroupScheduleRuleResponse[]> = {};
  activeRules.forEach(r => {
    if (!byDay[r.dayOfWeek]) byDay[r.dayOfWeek] = [];
    byDay[r.dayOfWeek].push(r);
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-orange-500 text-white p-2.5 rounded-lg"><CalendarClock className="w-5 h-5" /></div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Расписание</h1>
            <p className="text-sm text-gray-500">Правила повторяющегося расписания для учебных групп</p>
          </div>
        </div>
        <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium">
          <Plus className="w-4 h-4" /> Добавить правило
        </button>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-4">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">Курс</label>
            <select value={selectedCourse || ''} onChange={(e) => setSelectedCourse(Number(e.target.value))} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
              {courses.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">Семестр</label>
            <select value={selectedSemester || ''} onChange={(e) => setSelectedSemester(Number(e.target.value))} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
              {semesters.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">Группа</label>
            <select value={selectedGroup || ''} onChange={(e) => setSelectedGroup(Number(e.target.value))} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
              {groups.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
            </select>
          </div>
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center py-16"><div className="animate-spin rounded-full h-8 w-8 border-2 border-primary-600 border-t-transparent" /></div>
      ) : activeRules.length === 0 ? (
        <div className="bg-white rounded-xl border border-gray-200"><EmptyState message="Правила расписания не найдены. Добавьте первое правило." /></div>
      ) : (
        <div className="space-y-4">
          <div className="grid grid-cols-7 gap-2">
            {[1,2,3,4,5,6,7].map(day => {
              const dayRules = byDay[day] || [];
              return (
                <div key={day} className={`rounded-xl border p-3 min-h-[120px] ${dayRules.length > 0 ? DAY_COLORS[day] : 'bg-gray-50 border-gray-200'}`}>
                  <div className={`text-xs font-bold mb-2 ${dayRules.length > 0 ? DAY_TEXT[day] : 'text-gray-400'}`}>
                    {DAY_SHORT[day]}
                  </div>
                  {dayRules.length === 0 ? (
                    <p className="text-xs text-gray-300 italic">—</p>
                  ) : (
                    <div className="space-y-1.5">
                      {dayRules.map(r => (
                        <div key={r.id} className={`bg-white rounded-lg px-2 py-1.5 text-xs shadow-sm ${!r.active ? 'opacity-50' : ''}`}>
                          <div className="font-bold text-gray-800">{r.startTime}–{r.endTime}</div>
                          {r.room && <div className="text-gray-500 flex items-center gap-1"><MapPin className="w-2.5 h-2.5" />{r.room}</div>}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <div className="px-5 py-3 border-b border-gray-100 bg-gray-50">
              <h3 className="text-sm font-semibold text-gray-700">Все правила ({activeRules.length})</h3>
            </div>
            <div className="divide-y divide-gray-100">
              {activeRules.sort((a, b) => a.dayOfWeek - b.dayOfWeek || a.startTime.localeCompare(b.startTime)).map(r => (
                <div key={r.id} className={`flex items-center justify-between px-5 py-3 hover:bg-gray-50 ${!r.active ? 'opacity-50' : ''}`}>
                  <div className="flex items-center gap-4">
                    <div className={`w-10 text-center rounded-lg py-1 text-xs font-bold border ${DAY_COLORS[r.dayOfWeek]} ${DAY_TEXT[r.dayOfWeek]}`}>
                      {DAY_SHORT[r.dayOfWeek]}
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <Clock className="w-3.5 h-3.5 text-gray-400" />
                        <span className="text-sm font-medium text-gray-900">{r.startTime} — {r.endTime}</span>
                      </div>
                      {r.room && (
                        <div className="flex items-center gap-1.5 mt-0.5">
                          <MapPin className="w-3 h-3 text-gray-400" />
                          <span className="text-xs text-gray-500">{r.room}</span>
                        </div>
                      )}
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant={r.active ? 'green' : 'gray'}>{r.active ? 'Активно' : 'Пауза'}</Badge>
                    <button onClick={() => toggleActive(r)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-400" title={r.active ? 'Приостановить' : 'Активировать'}>
                      {r.active ? <PowerOff className="w-4 h-4" /> : <Power className="w-4 h-4 text-emerald-500" />}
                    </button>
                    <button onClick={() => openEdit(r)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-400"><Pencil className="w-4 h-4" /></button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Редактировать правило' : 'Новое правило расписания'}>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">День недели *</label>
            <select value={form.dayOfWeek} onChange={(e) => setForm({ ...form, dayOfWeek: Number(e.target.value) })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500">
              {[1,2,3,4,5,6,7].map((d) => <option key={d} value={d}>{DAYS[d]}</option>)}
            </select>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Начало *</label>
              <input type="time" value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Конец *</label>
              <input type="time" value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} required className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Аудитория</label>
            <input value={form.room} onChange={(e) => setForm({ ...form, room: e.target.value })} className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500" placeholder="Например: Ауд. 301" />
          </div>
          <div className="flex items-center gap-2">
            <input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} id="active" className="rounded border-gray-300 text-primary-600 focus:ring-primary-500" />
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
