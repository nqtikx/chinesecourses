import { useEffect, useState } from 'react';
import { classProfilesApi, materialsApi } from '../api';
import type { ClassProfileGroupItemResponse, ClassProfileResponse } from '../types';
import { toast } from '../components/ui/Toast';
import { Users, BookOpen, Upload, Send, Download } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const ClassProfilePage = () => {
  const { isAdmin, isTeacher } = useAuth();
  const canManage = isAdmin || isTeacher;
  const [groups, setGroups] = useState<ClassProfileGroupItemResponse[]>([]);
  const [selectedGroupId, setSelectedGroupId] = useState<number | null>(null);
  const [data, setData] = useState<ClassProfileResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [noteText, setNoteText] = useState('');
  const [posting, setPosting] = useState(false);

  const loadProfile = async (groupId?: number) => {
    try {
      const res = groupId ? await classProfilesApi.byGroup(groupId) : await classProfilesApi.me();
      setData(res.data);
      if (groupId) {
        setSelectedGroupId(groupId);
      } else {
        setSelectedGroupId(res.data.groupId);
      }
    } catch {
      toast('error', 'Не удалось загрузить профиль группы');
    }
  };

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const listRes = await classProfilesApi.groups();
        setGroups(listRes.data);
        if (listRes.data.length > 0) {
          await loadProfile(listRes.data[0].groupId);
        } else {
          await loadProfile();
        }
      } catch {
        toast('error', 'Не удалось загрузить профиль группы');
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const uploadMaterial = async () => {
    if (!uploadFile || !selectedGroupId) return;
    setUploading(true);
    try {
      await materialsApi.upload(selectedGroupId, uploadFile);
      setUploadFile(null);
      toast('success', 'Файл загружен в группу');
      await loadProfile(selectedGroupId);
    } catch {
      toast('error', 'Ошибка загрузки файла');
    } finally {
      setUploading(false);
    }
  };

  const createNote = async () => {
    if (!selectedGroupId || !noteText.trim()) return;
    setPosting(true);
    try {
      await classProfilesApi.addNote(selectedGroupId, noteText.trim());
      setNoteText('');
      toast('success', 'Текст добавлен в профиль класса');
      await loadProfile(selectedGroupId);
    } catch {
      toast('error', 'Не удалось сохранить текст');
    } finally {
      setPosting(false);
    }
  };

  const download = async (id: number, fileName: string) => {
    try {
      const res = await materialsApi.download(id);
      const blob = new Blob([res.data], { type: 'application/octet-stream' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch {
      toast('error', 'Ошибка скачивания файла');
    }
  };

  if (loading) return <div className="p-6 text-sm text-gray-500">Загрузка...</div>;
  if (!data) return <div className="p-6 text-sm text-gray-500">Нет данных о группе</div>;

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-xl border border-gray-200 p-4">
        <label className="block text-sm text-gray-600 mb-1">Выберите группу</label>
        <select
          value={selectedGroupId ?? ''}
          onChange={async (e) => {
            const id = Number(e.target.value);
            setSelectedGroupId(id);
            await loadProfile(id);
          }}
          className="w-full md:w-[420px] px-3 py-2 rounded-lg border border-gray-300 text-sm"
        >
          <option value="">Выберите...</option>
          {groups.map((g) => (
            <option key={g.groupId} value={g.groupId}>
              {g.groupName} | {g.courseName} | {g.semesterName} | {g.teacherName}
            </option>
          ))}
        </select>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <h1 className="text-xl font-semibold text-gray-900">{data.groupName}</h1>
        <p className="text-sm text-gray-500 mt-1">
          Курс: {data.courseName} | Семестр: {data.semesterName}
        </p>
        <p className="text-sm text-gray-600 mt-2">Преподаватель: {data.teacherName}</p>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
          <Users className="w-5 h-5 text-primary-600" />
          Список слушателей
        </h2>
        <div className="grid md:grid-cols-2 gap-2">
          {data.students.map((s) => (
            <div key={s.id} className="rounded-lg border border-gray-200 p-3 text-sm text-gray-700">
              {s.lastName} {s.firstName} {s.middleName || ''}
            </div>
          ))}
        </div>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
          <BookOpen className="w-5 h-5 text-primary-600" />
          Материалы группы
        </h2>
        {canManage && (
          <div className="mb-4 flex items-center gap-2">
            <input type="file" onChange={(e) => setUploadFile(e.target.files?.[0] || null)} className="text-sm" />
            <button
              onClick={uploadMaterial}
              disabled={!uploadFile || uploading || !selectedGroupId}
              className="px-3 py-1.5 rounded-lg border text-sm inline-flex items-center gap-1 disabled:opacity-50"
            >
              <Upload className="w-4 h-4" /> {uploading ? 'Загрузка...' : 'Загрузить'}
            </button>
          </div>
        )}
        {data.materials.length === 0 ? (
          <p className="text-sm text-gray-400 italic">Материалов пока нет</p>
        ) : (
          <ul className="space-y-2">
            {data.materials.map((m) => (
              <li key={m.id} className="text-sm text-gray-700 flex items-center justify-between border rounded-lg px-3 py-2">
                <span>{m.fileName}</span>
                <button onClick={() => download(m.id, m.fileName)} className="px-2 py-1 border rounded text-xs inline-flex items-center gap-1">
                  <Download className="w-3 h-3" /> Скачать
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Тексты преподавателя</h2>
        {canManage && (
          <div className="mb-4 space-y-2">
            <textarea
              value={noteText}
              onChange={(e) => setNoteText(e.target.value)}
              placeholder="Напишите текст для группы..."
              rows={4}
              className="w-full rounded-lg border border-gray-300 p-3 text-sm"
            />
            <button
              onClick={createNote}
              disabled={posting || !noteText.trim() || !selectedGroupId}
              className="px-3 py-1.5 rounded-lg border text-sm inline-flex items-center gap-1 disabled:opacity-50"
            >
              <Send className="w-4 h-4" /> {posting ? 'Сохранение...' : 'Опубликовать'}
            </button>
          </div>
        )}
        {data.notes.length === 0 ? (
          <p className="text-sm text-gray-400 italic">Пока нет текстовых записей</p>
        ) : (
          <div className="space-y-2">
            {data.notes.map((n) => (
              <div key={n.id} className="rounded-lg border border-gray-200 p-3">
                <div className="text-xs text-gray-500 mb-1">{new Date(n.createdAt).toLocaleString()}</div>
                <div className="text-sm text-gray-700 whitespace-pre-wrap">{n.text}</div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default ClassProfilePage;
