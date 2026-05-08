import { useEffect, useState } from 'react';
import { classProfilesApi, scheduleTableApi } from '../api';
import type { ClassProfileGroupItemResponse, GroupScheduleTableResponse } from '../types';
import { toast } from '../components/ui/Toast';
import { Download } from 'lucide-react';

const ScheduleTablePage = () => {
  const [groups, setGroups] = useState<ClassProfileGroupItemResponse[]>([]);
  const [groupId, setGroupId] = useState<number | null>(null);
  const [weekStart, setWeekStart] = useState<string>(new Date().toISOString().slice(0, 10));
  const [data, setData] = useState<GroupScheduleTableResponse | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    classProfilesApi.groups()
      .then(({ data }) => {
        setGroups(data);
        if (data.length > 0) setGroupId(data[0].groupId);
      })
      .catch(() => toast('error', 'Не удалось загрузить список групп'));
  }, []);

  const load = async () => {
    if (!groupId) return;
    setLoading(true);
    try {
      const res = await scheduleTableApi.get(groupId, weekStart || undefined);
      setData(res.data);
    } catch {
      toast('error', 'Не удалось загрузить расписание');
    } finally {
      setLoading(false);
    }
  };

  const exportXlsx = async () => {
    if (!groupId) return;
    try {
      const res = await scheduleTableApi.exportXlsx(groupId, weekStart || undefined);
      const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `schedule-group-${groupId}.xlsx`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch {
      toast('error', 'Не удалось выгрузить XLSX');
    }
  };

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-xl border border-gray-200 p-6 flex gap-3 items-end flex-wrap">
        <div>
          <label className="block text-sm text-gray-600 mb-1">Начало недели</label>
          <input type="date" value={weekStart} onChange={(e) => setWeekStart(e.target.value)}
                 className="px-3 py-2 rounded-lg border border-gray-300 text-sm w-44" />
        </div>
        <div className="flex-1 min-w-[280px]">
          <label className="block text-sm text-gray-600 mb-1">Группа</label>
          <select
            value={groupId ?? ''}
            onChange={(e) => setGroupId(e.target.value ? Number(e.target.value) : null)}
            className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm"
          >
            <option value="">Выберите группу...</option>
            {groups.map((g) => (
              <option key={g.groupId} value={g.groupId}>
                {g.groupName} | {g.courseName} | {g.semesterName} | {g.teacherName}
              </option>
            ))}
          </select>
        </div>
        <button onClick={load} disabled={!groupId} className="px-4 py-2 bg-primary-600 text-white rounded-lg text-sm disabled:opacity-50">Показать</button>
        <button onClick={exportXlsx} disabled={!groupId} className="px-4 py-2 border rounded-lg text-sm inline-flex items-center gap-2 disabled:opacity-50">
          <Download className="w-4 h-4" /> Export XLSX
        </button>
      </div>

      {loading && <div className="text-sm text-gray-500">Загрузка...</div>}

      {data && (
        <div className="bg-white rounded-xl border border-gray-200 p-6 overflow-x-auto">
          <h2 className="text-lg font-semibold text-gray-900 mb-1">{data.groupName}</h2>
          <p className="text-sm text-gray-500 mb-4">{data.courseName} | {data.semesterName} | {data.teacherName} | Неделя с {new Date(data.weekStart).toLocaleDateString('ru')}</p>
          <table className="min-w-full text-sm border border-gray-200">
            <thead className="bg-gray-50">
            <tr>
              <th className="px-3 py-2 border">Дата</th>
              <th className="px-3 py-2 border">День</th>
              <th className="px-3 py-2 border">Начало</th>
              <th className="px-3 py-2 border">Конец</th>
              <th className="px-3 py-2 border">Аудитория/Ссылка</th>
              <th className="px-3 py-2 border">Тип</th>
              <th className="px-3 py-2 border">День</th>
            </tr>
            </thead>
            <tbody>
            {data.rows.map((r, idx) => (
              <tr key={`${r.dayOfWeek}-${idx}`}>
                <td className="px-3 py-2 border">{new Date(r.date).toLocaleDateString('ru')}</td>
                <td className="px-3 py-2 border">{r.dayLabel}</td>
                <td className="px-3 py-2 border">{r.startTime}</td>
                <td className="px-3 py-2 border">{r.endTime}</td>
                <td className="px-3 py-2 border">{r.room || '-'}</td>
                <td className="px-3 py-2 border">{r.lessonType}</td>
                <td className={`px-3 py-2 border ${r.holiday ? 'text-red-600 font-medium' : ''}`}>
                  {r.holiday ? `Праздник: ${r.holidayTitle ?? ''}` : 'Учебный'}
                </td>
              </tr>
            ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default ScheduleTablePage;
