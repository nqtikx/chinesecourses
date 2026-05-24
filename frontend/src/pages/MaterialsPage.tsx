import { useEffect, useState } from 'react';
import { classProfilesApi, materialsApi } from '../api';
import type { ClassProfileGroupItemResponse, StudyMaterialResponse } from '../types';
import { useAuth } from '../context/AuthContext';
import { toast } from '../components/ui/Toast';
import { Upload, Download, Eye } from 'lucide-react';
import Modal from '../components/ui/Modal';

const MaterialsPage = () => {
  const { user } = useAuth();
  const canUpload = user?.role === 'ROLE_ADMIN' || user?.role === 'ROLE_TEACHER';
  const [groups, setGroups] = useState<ClassProfileGroupItemResponse[]>([]);
  const [groupId, setGroupId] = useState<number | null>(null);
  const [list, setList] = useState<StudyMaterialResponse[]>([]);
  const [file, setFile] = useState<File | null>(null);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [previewType, setPreviewType] = useState<string>('application/octet-stream');
  const [previewName, setPreviewName] = useState<string>('');

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
    try {
      const res = await materialsApi.list(groupId);
      setList(res.data);
    } catch {
      toast('error', 'Не удалось загрузить материалы');
    }
  };

  const upload = async () => {
    if (!file || !groupId) return;
    try {
      await materialsApi.upload(groupId, file);
      setFile(null);
      toast('success', 'Файл загружен');
      await load();
    } catch {
      toast('error', 'Ошибка загрузки');
    }
  };

  const download = async (id: number, name: string) => {
    try {
      const res = await materialsApi.download(id);
      const blob = new Blob([res.data], { type: 'application/octet-stream' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = name;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch {
      toast('error', 'Ошибка скачивания');
    }
  };

  const closePreview = () => {
    setPreviewOpen(false);
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }
    setPreviewUrl(null);
    setPreviewType('application/octet-stream');
    setPreviewName('');
  };

  const preview = async (id: number, name: string) => {
    try {
      const res = await materialsApi.view(id);
      const contentType = (res.headers['content-type'] as string) || 'application/octet-stream';
      const blob = new Blob([res.data], { type: contentType });
      const url = URL.createObjectURL(blob);
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
      setPreviewUrl(url);
      setPreviewType(contentType);
      setPreviewName(name);
      setPreviewOpen(true);
    } catch {
      toast('error', 'Предпросмотр недоступен для этого файла');
    }
  };

  const canPreview = (mime: string | null) =>
    !!mime && (mime.startsWith('image/') || mime === 'application/pdf');

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-xl border border-gray-200 p-6 flex items-end gap-3 flex-wrap">
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
      </div>

      {canUpload && (
        <div className="bg-white rounded-xl border border-gray-200 p-6 flex items-center gap-3">
          <input type="file" onChange={(e) => setFile(e.target.files?.[0] || null)} className="text-sm" />
          <button onClick={upload} className="px-4 py-2 border rounded-lg text-sm inline-flex items-center gap-2">
            <Upload className="w-4 h-4" /> Загрузить
          </button>
        </div>
      )}

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <h2 className="text-lg font-semibold mb-4">Материалы</h2>
        <div className="space-y-2">
          {list.map((m) => (
            <div key={m.id} className="border rounded-lg p-3 flex items-center justify-between">
              <div>
                <div className="text-sm font-medium">{m.fileName}</div>
                <div className="text-xs text-gray-500">{new Date(m.createdAt).toLocaleString()}</div>
              </div>
              <div className="flex items-center gap-2">
                {canPreview(m.fileType) && (
                  <button onClick={() => preview(m.id, m.fileName)} className="px-3 py-1.5 border rounded text-sm inline-flex items-center gap-1">
                    <Eye className="w-4 h-4" /> Просмотр
                  </button>
                )}
                <button onClick={() => download(m.id, m.fileName)} className="px-3 py-1.5 border rounded text-sm inline-flex items-center gap-1">
                  <Download className="w-4 h-4" /> Скачать
                </button>
              </div>
            </div>
          ))}
          {list.length === 0 && <p className="text-sm text-gray-400">Список пуст</p>}
        </div>
      </div>

      <Modal open={previewOpen} onClose={closePreview} title={`Предпросмотр: ${previewName}`} wide>
        {!previewUrl ? (
          <div className="text-sm text-gray-500">Нет данных для предпросмотра</div>
        ) : previewType.startsWith('image/') ? (
          <img src={previewUrl} alt={previewName} className="max-h-[70vh] w-auto mx-auto rounded-lg border border-gray-200" />
        ) : previewType === 'application/pdf' ? (
          <iframe src={previewUrl} title={previewName} className="w-full h-[70vh] rounded-lg border border-gray-200" />
        ) : (
          <div className="text-sm text-gray-500">
            Предпросмотр поддерживается только для PDF и изображений.
          </div>
        )}
      </Modal>
    </div>
  );
};

export default MaterialsPage;
