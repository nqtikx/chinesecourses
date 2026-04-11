import { useEffect, useState } from 'react';
import { adminUsersApi, contractsApi, coursesApi, semestersApi, studyGroupsApi } from '../api';
import type { AdminUserListItemResponse, ContractDocumentResponse, CourseResponse, SemesterResponse, StudyGroupResponse } from '../types';
import { toast } from '../components/ui/Toast';
import EmptyState from '../components/ui/EmptyState';
import { FileText, Download } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function ContractsPage() {
  const { isAdmin } = useAuth();
  const [users, setUsers] = useState<AdminUserListItemResponse[]>([]);
  const [courses, setCourses] = useState<CourseResponse[]>([]);
  const [semesters, setSemesters] = useState<SemesterResponse[]>([]);
  const [groups, setGroups] = useState<StudyGroupResponse[]>([]);
  const [templates, setTemplates] = useState<string[]>([]);

  const [userId, setUserId] = useState<number | null>(null);
  const [courseId, setCourseId] = useState<number | null>(null);
  const [groupId, setGroupId] = useState<number | null>(null);
  const [templateName, setTemplateName] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [list, setList] = useState<ContractDocumentResponse[]>([]);

  useEffect(() => {
    if (isAdmin) {
      adminUsersApi.list().then(({ data }) => setUsers(data)).catch(() => toast('error', 'Не удалось загрузить пользователей'));
      coursesApi.list().then(({ data }) => setCourses(data)).catch(() => toast('error', 'Не удалось загрузить курсы'));
      contractsApi.all().then(({ data }) => setList(data)).catch(() => setList([]));
      contractsApi.templates().then(({ data }) => setTemplates(data)).catch(() => setTemplates([]));
    } else {
      contractsApi.my().then(({ data }) => setList(data)).catch(() => setList([]));
    }
  }, [isAdmin]);

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
      const { data } = await contractsApi.generate({
        userId,
        courseId,
        groupId: groupId ?? undefined,
        templateName: templateName || undefined,
      });
      const blob = new Blob([data], { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = templateName
        ? `doc-user-${userId}-${templateName}`
        : `contract-user-${userId}-course-${courseId}.docx`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
      toast('success', 'Документ сформирован');
      const refreshed = await contractsApi.all();
      setList(refreshed.data);
    } catch {
      toast('error', 'Не удалось сформировать документ');
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

      {isAdmin && (users.length === 0 || courses.length === 0) ? (
        <div className="bg-white rounded-xl border border-gray-200">
          <EmptyState message="Нет данных для генерации договора" />
        </div>
      ) : isAdmin ? (
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

          {/* Выбор шаблона документа */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Шаблон документа
              <span className="ml-1 text-xs text-gray-400 font-normal">(не выбрано — стандартный договор)</span>
            </label>
            <select
              value={templateName}
              onChange={(e) => setTemplateName(e.target.value)}
              className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500"
            >
              <option value="">Стандартный договор (генерируется автоматически)</option>
              {templates.map((t) => (
                <option key={t} value={t}>{t}</option>
              ))}
            </select>
            {templates.length === 0 && (
              <p className="text-xs text-gray-400 mt-1">Шаблоны не найдены. Настройте путь APP_STORAGE_TEMPLATES_DIR.</p>
            )}
          </div>

          <div className="pt-2">
            <button
              onClick={generateContract}
              disabled={loading || !userId || !courseId}
              className="inline-flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium disabled:opacity-50"
            >
              <Download className="w-4 h-4" />
              {loading ? 'Формирование...' : 'Сгенерировать документ'}
            </button>
          </div>
        </div>
      ) : null}

      <div className="bg-white rounded-xl border border-gray-200 p-5">
        <h2 className="text-lg font-semibold text-gray-900 mb-3">Сохраненные договоры</h2>
        {list.length === 0 ? (
          <p className="text-sm text-gray-400">Пока нет документов</p>
        ) : (
          <div className="space-y-2">
            {list.map((c) => (
              <div key={c.id} className="rounded-lg border border-gray-200 p-3 flex justify-between items-center">
                <div>
                  <div className="text-sm font-medium text-gray-900">{c.contractNumber}</div>
                  <div className="text-xs text-gray-500">Скидка: {c.discountPercent}% | Итого: {c.finalPrice}</div>
                </div>
                <button
                  onClick={async () => {
                    const { data } = await contractsApi.download(c.id);
                    const b = new Blob([data], { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' });
                    const u = URL.createObjectURL(b);
                    const a = document.createElement('a');
                    a.href = u;
                    a.download = c.fileName;
                    document.body.appendChild(a);
                    a.click();
                    a.remove();
                    URL.revokeObjectURL(u);
                  }}
                  className="px-3 py-1.5 border rounded text-sm inline-flex items-center gap-1"
                >
                  <Download className="w-4 h-4" /> Скачать
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
