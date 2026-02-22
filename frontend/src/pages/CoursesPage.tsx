import { useEffect, useState, type FormEvent } from 'react';
import { coursesApi, semestersApi } from '../api';
import type { CourseResponse, SemesterResponse } from '../types';
import { useAuth } from '../context/AuthContext';
import Modal from '../components/ui/Modal';
import Badge from '../components/ui/Badge';
import EmptyState from '../components/ui/EmptyState';
import ConfirmDialog from '../components/ui/ConfirmDialog';
import { toast } from '../components/ui/Toast';
import { Plus, Pencil, Archive, ArchiveRestore, BookOpen, Calendar, ChevronDown, ChevronUp } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export default function CoursesPage() {
  const { isAdmin } = useAuth();
  const navigate = useNavigate();
  const [courses, setCourses] = useState<CourseResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<CourseResponse | null>(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [archiveTarget, setArchiveTarget] = useState<CourseResponse | null>(null);
  const [semesterCounts, setSemesterCounts] = useState<Record<number, number>>({});
  const [expandedCourse, setExpandedCourse] = useState<number | null>(null);
  const [courseSemesters, setCourseSemesters] = useState<Record<number, SemesterResponse[]>>({});

  const load = async () => {
    setLoading(true);
    try {
      const { data } = await coursesApi.list();
      setCourses(data);
      const counts: Record<number, number> = {};
      await Promise.all(data.map(async (c) => {
        try {
          const { data: sems } = await semestersApi.listByCourse(c.id);
          counts[c.id] = sems.filter(s => !s.archived).length;
        } catch { counts[c.id] = 0; }
      }));
      setSemesterCounts(counts);
    } catch { toast('error', 'Не удалось загрузить курсы'); }
    setLoading(false);
  };

  useEffect(() => { load(); }, []);

  const toggleExpand = async (courseId: number) => {
    if (expandedCourse === courseId) { setExpandedCourse(null); return; }
    setExpandedCourse(courseId);
    if (!courseSemesters[courseId]) {
      try {
        const { data } = await semestersApi.listByCourse(courseId);
        setCourseSemesters(prev => ({ ...prev, [courseId]: data }));
      } catch {}
    }
  };

  const openCreate = () => { setEditing(null); setName(''); setDescription(''); setModalOpen(true); };
  const openEdit = (c: CourseResponse) => { setEditing(c); setName(c.name); setDescription(c.description || ''); setModalOpen(true); };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    try {
      if (editing) {
        await coursesApi.update(editing.id, { name, description, archived: editing.archived });
        toast('success', 'Курс обновлён');
      } else {
        await coursesApi.create({ name, description });
        toast('success', 'Курс создан');
      }
      setModalOpen(false);
      load();
    } catch { toast('error', 'Ошибка сохранения'); }
  };

  const confirmArchive = async () => {
    if (!archiveTarget) return;
    try {
      await coursesApi.archive(archiveTarget.id, { archived: !archiveTarget.archived });
      toast('success', archiveTarget.archived ? 'Курс восстановлен' : 'Курс архивирован');
      load();
    } catch { toast('error', 'Ошибка'); }
    setArchiveTarget(null);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-blue-500 text-white p-2.5 rounded-lg"><BookOpen className="w-5 h-5" /></div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Курсы</h1>
            <p className="text-sm text-gray-500">{isAdmin ? 'Управление программами обучения' : 'Просмотр программ обучения'}</p>
          </div>
        </div>
        {isAdmin && (
          <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors text-sm font-medium">
            <Plus className="w-4 h-4" /> Добавить курс
          </button>
        )}
      </div>

      {loading ? (
        <div className="flex justify-center py-16"><div className="animate-spin rounded-full h-8 w-8 border-2 border-primary-600 border-t-transparent" /></div>
      ) : courses.length === 0 ? (
        <div className="bg-white rounded-xl border border-gray-200"><EmptyState message="Курсы не найдены" /></div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {courses.map((c) => {
            const isExpanded = expandedCourse === c.id;
            const sems = courseSemesters[c.id] || [];
            return (
              <div key={c.id} className={`bg-white rounded-xl border ${c.archived ? 'border-gray-200 opacity-60' : 'border-gray-200'} overflow-hidden hover:shadow-md transition-shadow`}>
                <div className="p-5">
                  <div className="flex items-start justify-between">
                    <div className="flex items-center gap-3">
                      <div className={`p-2.5 rounded-lg ${c.archived ? 'bg-gray-100 text-gray-400' : 'bg-blue-100 text-blue-600'}`}>
                        <BookOpen className="w-5 h-5" />
                      </div>
                      <div>
                        <h3 className="font-semibold text-gray-900">{c.name}</h3>
                        <div className="flex items-center gap-2 mt-0.5">
                          <Badge variant={c.archived ? 'gray' : 'green'}>{c.archived ? 'В архиве' : 'Активен'}</Badge>
                          <span className="text-xs text-gray-400">ID: {c.id}</span>
                        </div>
                      </div>
                    </div>
                    {isAdmin && (
                      <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
                        <button onClick={() => openEdit(c)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-400"><Pencil className="w-4 h-4" /></button>
                        <button onClick={() => setArchiveTarget(c)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-400">
                          {c.archived ? <ArchiveRestore className="w-4 h-4" /> : <Archive className="w-4 h-4" />}
                        </button>
                      </div>
                    )}
                  </div>
                  {c.description && <p className="text-sm text-gray-600 mt-3">{c.description}</p>}
                  <div className="flex items-center justify-between mt-4 pt-3 border-t border-gray-100">
                    <div className="flex items-center gap-4 text-sm">
                      <span className="flex items-center gap-1.5 text-gray-500">
                        <Calendar className="w-4 h-4" />
                        {semesterCounts[c.id] ?? 0} семестр(ов)
                      </span>
                      <span className="text-xs text-gray-400">
                        Создан: {new Date(c.createdAt).toLocaleDateString('ru')}
                      </span>
                    </div>
                    <button
                      onClick={() => toggleExpand(c.id)}
                      className="flex items-center gap-1 text-xs text-primary-600 hover:text-primary-700 font-medium"
                    >
                      {isExpanded ? 'Скрыть' : 'Семестры'}
                      {isExpanded ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
                    </button>
                  </div>
                </div>
                {isExpanded && (
                  <div className="border-t border-gray-100 bg-gray-50/50 px-5 py-3">
                    {sems.length === 0 ? (
                      <p className="text-sm text-gray-400 italic">Нет семестров</p>
                    ) : (
                      <div className="space-y-2">
                        {sems.map((s) => (
                          <div key={s.id} className="flex items-center justify-between bg-white rounded-lg px-3 py-2 border border-gray-100">
                            <div>
                              <span className="text-sm font-medium text-gray-800">{s.name}</span>
                              <span className="text-xs text-gray-400 ml-2">
                                {new Date(s.startDate + 'T00:00').toLocaleDateString('ru')} — {new Date(s.endDate + 'T00:00').toLocaleDateString('ru')}
                              </span>
                            </div>
                            <Badge variant={s.archived ? 'gray' : 'green'}>{s.archived ? 'Архив' : 'Активен'}</Badge>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Редактировать курс' : 'Новый курс'}>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Название курса *</label>
            <input value={name} onChange={(e) => setName(e.target.value)} required className="w-full px-3 py-2 rounded-lg border border-gray-300 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none text-sm" placeholder="Например: Базовый курс HSK1" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Описание</label>
            <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={3} className="w-full px-3 py-2 rounded-lg border border-gray-300 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none text-sm" placeholder="Краткое описание программы курса" />
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setModalOpen(false)} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800 transition-colors">Отмена</button>
            <button type="submit" className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium transition-colors">{editing ? 'Сохранить' : 'Создать'}</button>
          </div>
        </form>
      </Modal>

      {archiveTarget && (
        <ConfirmDialog
          open
          title={archiveTarget.archived ? 'Восстановить курс?' : 'Архивировать курс?'}
          message={archiveTarget.archived
            ? `Курс «${archiveTarget.name}» будет восстановлен из архива.`
            : `Курс «${archiveTarget.name}» будет перемещён в архив.`}
          confirmLabel={archiveTarget.archived ? 'Восстановить' : 'Архивировать'}
          onConfirm={confirmArchive}
          onCancel={() => setArchiveTarget(null)}
        />
      )}
    </div>
  );
}
