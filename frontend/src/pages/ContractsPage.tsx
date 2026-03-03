import { useEffect, useState } from 'react';
import { adminUsersApi, contractsApi, coursesApi, semestersApi, studyGroupsApi } from '../api';
import type { AdminUserListItemResponse, CourseResponse, SemesterResponse, StudyGroupResponse } from '../types';
import { toast } from '../components/ui/Toast';
import EmptyState from '../components/ui/EmptyState';
import { FileText, Download } from 'lucide-react';

export default function ContractsPage() {
  const [users, setUsers] = useState<AdminUserListItemResponse[]>([]);
  const [courses, setCourses] = useState<CourseResponse[]>([]);
  const [semesters, setSemesters] = useState<SemesterResponse[]>([]);
  const [groups, setGroups] = useState<StudyGroupResponse[]>([]);

  const [userId, setUserId] = useState<number | null>(null);
  const [courseId, setCourseId] = useState<number | null>(null);
  const [groupId, setGroupId] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    adminUsersApi.list().then(({ data }) => setUsers(data)).catch(() => toast('error', 'Не удалось загрузить пользователей'));
    coursesApi.list().then(({ data }) => setCourses(data)).catch(() => toast('error', 'Не удалось загрузить курсы'));
  }, []);

  useEffect(() => {
    if (!courseId) {
      setSemesters([]);
      setGroups([]);
      setGroupId(null);
      return;
    }
    semestersApi.listByCourse(courseId).then(({ data }) => {
      setSemesters(data);
      const first = data.find(s => !s.archived);
      if (!first) {
        setGroups([]);
        setGroupId(null);
        return;
      }
      studyGroupsApi.listBySemester(first.id).then(({ data: gs }) => {
        setGroups(gs.filter(g => !g.archived));
      }).catch(() => setGroups([]));
    }).catch(() => {
      setSemesters([]);
      setGroups([]);
    });
  }, [courseId]);

  const generateContract = async () => {
    if (!userId || !courseId) {
      toast('error', 'Выберите пользователя и курс');
      return;
    }
    setLoading(true);
    try {
      const { data } = await contractsApi.generate({ userId, courseId, groupId: groupId ?? undefined });
      const blob = new Blob([data], { type: 'application/pdf' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `contract-user-${userId}-course-${courseId}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
      toast('success', 'PDF-договор сформирован');
    } catch {
      toast('error', 'Не удалось сформировать договор');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6 max-w-3xl">
      <div className="flex items-center gap-3">
        <div className="bg-slate-700 text-white p-2.5 rounded-lg"><FileText className="w-5 h-5" /></div>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Сгенерировать договор</h1>
          <p className="text-sm text-gray-500">Только для администратора</p>
        </div>
      </div>

      {users.length === 0 || courses.length === 0 ? (
        <div className="bg-white rounded-xl border border-gray-200">
          <EmptyState message="Нет данных для генерации договора" />
        </div>
      ) : (
        <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Пользователь *</label>
            <select
              value={userId ?? ''}
              onChange={(e) => setUserId(Number(e.target.value))}
              className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500"
            >
              <option value="">Выберите пользователя...</option>
              {users.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.username} ({u.role}) {u.fullName ? `- ${u.fullName}` : ''}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Курс *</label>
            <select
              value={courseId ?? ''}
              onChange={(e) => setCourseId(Number(e.target.value))}
              className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500"
            >
              <option value="">Выберите курс...</option>
              {courses.filter(c => !c.archived).map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Группа (необязательно)</label>
            <select
              value={groupId ?? ''}
              onChange={(e) => setGroupId(e.target.value ? Number(e.target.value) : null)}
              className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500"
            >
              <option value="">Без выбора группы</option>
              {groups.map((g) => (
                <option key={g.id} value={g.id}>{g.name}</option>
              ))}
            </select>
            {semesters.length === 0 && courseId && (
              <p className="text-xs text-amber-600 mt-1">Для курса не найден активный семестр.</p>
            )}
          </div>

          <div className="pt-2">
            <button
              onClick={generateContract}
              disabled={loading || !userId || !courseId}
              className="inline-flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium disabled:opacity-50"
            >
              <Download className="w-4 h-4" />
              {loading ? 'Формирование...' : 'Сгенерировать договор'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
