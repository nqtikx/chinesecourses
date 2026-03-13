import { useEffect, useState } from 'react';
import { adminEnrollmentsApi, adminUsersApi, contractsApi, coursesApi, profileApi, semestersApi, studyGroupsApi } from '../api';
import type { AdminUserListItemResponse, ContractDocumentResponse, CourseResponse, PersonGuardianResponse, SemesterResponse, StudyGroupResponse, UserProfileResponse } from '../types';
import { useAuth } from '../context/AuthContext';
import Badge from '../components/ui/Badge';
import EmptyState from '../components/ui/EmptyState';
import { toast } from '../components/ui/Toast';
import { UserCircle, BookOpen, GraduationCap, History, Save, Download } from 'lucide-react';

function Period({ from, to }: { from: string | null; to: string | null }) {
  if (!from && !to) return <span className="text-gray-400">—</span>;
  return (
    <span>
      {from ? new Date(from + 'T00:00').toLocaleDateString('ru') : '—'} - {to ? new Date(to + 'T00:00').toLocaleDateString('ru') : '...'}
    </span>
  );
}

export default function ProfilePage() {
  const { isAdmin } = useAuth();
  const [profile, setProfile] = useState<UserProfileResponse | null>(null);
  const [users, setUsers] = useState<AdminUserListItemResponse[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [courses, setCourses] = useState<CourseResponse[]>([]);
  const [groups, setGroups] = useState<StudyGroupResponse[]>([]);
  const [assignCourseId, setAssignCourseId] = useState<number | null>(null);
  const [assignGroupId, setAssignGroupId] = useState<number | null>(null);
  const [assigning, setAssigning] = useState(false);
  const [completing, setCompleting] = useState(false);
  const [contracts, setContracts] = useState<ContractDocumentResponse[]>([]);
  const [savingProfile, setSavingProfile] = useState(false);
  const [editForm, setEditForm] = useState({
    firstName: '',
    lastName: '',
    middleName: '',
    birthDate: '',
    email: '',
    phone: '',
    residentialAddress: '',
    documentType: '',
    documentSeries: '',
    documentNumber: '',
    documentIssueDate: '',
    documentIssuedBy: '',
    documentIdentificationNumber: '',
  });
  const [guardians, setGuardians] = useState<PersonGuardianResponse[]>([]);

  const loadMe = async () => {
    setLoading(true);
    try {
      const { data } = await profileApi.me();
      setProfile(data);
      setEditForm({
        firstName: data.fullName?.split(' ')[1] || '',
        lastName: data.fullName?.split(' ')[0] || '',
        middleName: data.fullName?.split(' ').slice(2).join(' ') || '',
        birthDate: '',
        email: data.email || '',
        phone: data.phone || '',
        residentialAddress: data.residentialAddress || '',
        documentType: data.documentType || '',
        documentSeries: data.documentSeries || '',
        documentNumber: data.documentNumber || '',
        documentIssueDate: data.documentIssueDate || '',
        documentIssuedBy: data.documentIssuedBy || '',
        documentIdentificationNumber: data.documentIdentificationNumber || '',
      });
      setGuardians(data.guardians || []);
      const contractsRes = await contractsApi.my();
      setContracts(contractsRes.data);
    } catch {
      toast('error', 'Не удалось загрузить профиль');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMe();
    if (isAdmin) {
      adminUsersApi.list().then(({ data }) => setUsers(data)).catch(() => {});
      coursesApi.list().then(({ data }) => setCourses(data.filter(c => !c.archived))).catch(() => {});
    }
  }, [isAdmin]);

  useEffect(() => {
    if (!assignCourseId) {
      setGroups([]);
      setAssignGroupId(null);
      return;
    }
    semestersApi.listByCourse(assignCourseId).then(({ data }) => {
      const active = data.find((s: SemesterResponse) => !s.archived);
      if (!active) {
        setGroups([]);
        setAssignGroupId(null);
        return;
      }
      studyGroupsApi.listBySemester(active.id).then(({ data: gs }) => {
        setGroups(gs.filter(g => !g.archived));
      }).catch(() => setGroups([]));
    }).catch(() => setGroups([]));
  }, [assignCourseId]);

  const loadByUser = async (id: number) => {
    setLoading(true);
    try {
      const { data } = await adminUsersApi.profile(id);
      setProfile(data);
      setEditForm({
        firstName: data.fullName?.split(' ')[1] || '',
        lastName: data.fullName?.split(' ')[0] || '',
        middleName: data.fullName?.split(' ').slice(2).join(' ') || '',
        birthDate: '',
        email: data.email || '',
        phone: data.phone || '',
        residentialAddress: data.residentialAddress || '',
        documentType: data.documentType || '',
        documentSeries: data.documentSeries || '',
        documentNumber: data.documentNumber || '',
        documentIssueDate: data.documentIssueDate || '',
        documentIssuedBy: data.documentIssuedBy || '',
        documentIdentificationNumber: data.documentIdentificationNumber || '',
      });
      setGuardians(data.guardians || []);
      if (isAdmin && data.currentCourse?.groupId) {
        const contractsRes = await contractsApi.byGroup(data.currentCourse.groupId);
        setContracts(contractsRes.data.filter(c => c.userId === data.userId));
      } else {
        const contractsRes = await contractsApi.my();
        setContracts(contractsRes.data);
      }
    } catch {
      toast('error', 'Не удалось загрузить профиль пользователя');
    } finally {
      setLoading(false);
    }
  };

  const assignInProgress = async () => {
    if (!profile || !isAdmin || !assignCourseId) return;
    setAssigning(true);
    try {
      await adminEnrollmentsApi.create({
        userId: profile.userId,
        courseId: assignCourseId,
        groupId: assignGroupId ?? undefined,
      });
      toast('success', 'Курс назначен (IN_PROGRESS)');
      await loadByUser(profile.userId);
    } catch {
      toast('error', 'Не удалось назначить курс');
    } finally {
      setAssigning(false);
    }
  };

  const completeCurrent = async () => {
    if (!profile?.currentCourse || !isAdmin) return;
    setCompleting(true);
    try {
      await adminEnrollmentsApi.complete(profile.currentCourse.enrollmentId, { status: 'COMPLETED' });
      toast('success', 'Текущий курс завершён');
      await loadByUser(profile.userId);
    } catch {
      toast('error', 'Не удалось завершить курс');
    } finally {
      setCompleting(false);
    }
  };

  const saveMyProfile = async () => {
    setSavingProfile(true);
    try {
      await profileApi.updateMe({
        firstName: editForm.firstName,
        lastName: editForm.lastName,
        middleName: editForm.middleName,
        email: editForm.email,
        phone: editForm.phone,
        birthDate: editForm.birthDate || undefined,
        residentialAddress: editForm.residentialAddress || undefined,
        documentType: editForm.documentType || undefined,
        documentSeries: editForm.documentSeries || undefined,
        documentNumber: editForm.documentNumber || undefined,
        documentIssueDate: editForm.documentIssueDate || undefined,
        documentIssuedBy: editForm.documentIssuedBy || undefined,
        documentIdentificationNumber: editForm.documentIdentificationNumber || undefined,
        guardians: guardians.map((g, idx) => ({
          id: g.id,
          fullName: g.fullName,
          phone: g.phone || undefined,
          relationType: g.relationType || undefined,
          primaryGuardian: idx === 0 ? true : g.primaryGuardian,
          archived: g.archived,
        })),
      });
      toast('success', 'Профиль обновлён');
      if (profile) {
        if (isAdmin && selectedUserId) {
          await loadByUser(selectedUserId);
        } else {
          await loadMe();
        }
      }
    } catch {
      toast('error', 'Не удалось сохранить профиль');
    } finally {
      setSavingProfile(false);
    }
  };

  const downloadContract = async (id: number, filename: string) => {
    try {
      const { data } = await contractsApi.download(id);
      const blob = new Blob([data], { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch {
      toast('error', 'Ошибка скачивания договора');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <div className="bg-slate-700 text-white p-2.5 rounded-lg"><UserCircle className="w-5 h-5" /></div>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Профиль</h1>
          <p className="text-sm text-gray-500">Текущий курс и история пройденных курсов</p>
        </div>
      </div>

      {isAdmin && (
        <div className="bg-white rounded-xl border border-gray-200 p-4">
          <label className="block text-xs font-medium text-gray-500 mb-1">Посмотреть профиль пользователя</label>
          <select
            value={selectedUserId ?? ''}
            onChange={(e) => {
              const id = Number(e.target.value);
              setSelectedUserId(id);
              loadByUser(id);
            }}
            className="px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500"
          >
            <option value="">Выберите пользователя...</option>
            {users.map((u) => (
              <option key={u.id} value={u.id}>
                {u.username} ({u.role}) {u.fullName ? `- ${u.fullName}` : ''}
              </option>
            ))}
          </select>
        </div>
      )}

      {loading ? (
        <div className="flex justify-center py-16"><div className="animate-spin rounded-full h-8 w-8 border-2 border-primary-600 border-t-transparent" /></div>
      ) : !profile ? (
        <div className="bg-white rounded-xl border border-gray-200"><EmptyState message="Профиль не найден" /></div>
      ) : (
        <>
          <div className="bg-white rounded-xl border border-gray-200 p-5">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-lg font-semibold text-gray-900">{profile.fullName ?? profile.username}</h2>
                <p className="text-sm text-gray-500">{profile.email ?? 'Без email'} {profile.phone ? `| ${profile.phone}` : ''}</p>
                <p className="text-xs text-gray-500 mt-1">
                  Преподаватель: {profile.teacherFullName ?? '—'}
                  {profile.teacherPhone ? ` | ${profile.teacherPhone}` : ''}
                  {profile.teacherEmail ? ` | ${profile.teacherEmail}` : ''}
                </p>
              </div>
              <Badge variant={profile.role === 'ROLE_ADMIN' ? 'red' : profile.role === 'ROLE_TEACHER' ? 'blue' : 'green'}>
                {profile.role}
              </Badge>
            </div>
          </div>

          <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-4">
            <h3 className="text-sm font-semibold text-gray-700">Редактировать профиль</h3>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              <input value={editForm.lastName} onChange={(e) => setEditForm({ ...editForm, lastName: e.target.value })} placeholder="Фамилия" className="px-3 py-2 rounded-lg border border-gray-300 text-sm" />
              <input value={editForm.firstName} onChange={(e) => setEditForm({ ...editForm, firstName: e.target.value })} placeholder="Имя" className="px-3 py-2 rounded-lg border border-gray-300 text-sm" />
              <input value={editForm.middleName} onChange={(e) => setEditForm({ ...editForm, middleName: e.target.value })} placeholder="Отчество" className="px-3 py-2 rounded-lg border border-gray-300 text-sm" />
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              <input type="date" value={editForm.birthDate} onChange={(e) => setEditForm({ ...editForm, birthDate: e.target.value })} className="px-3 py-2 rounded-lg border border-gray-300 text-sm" />
              <input value={editForm.email} onChange={(e) => setEditForm({ ...editForm, email: e.target.value })} placeholder="Email" className="px-3 py-2 rounded-lg border border-gray-300 text-sm" />
              <input value={editForm.phone} onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })} placeholder="Телефон" className="px-3 py-2 rounded-lg border border-gray-300 text-sm" />
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <input value={editForm.residentialAddress} onChange={(e) => setEditForm({ ...editForm, residentialAddress: e.target.value })} placeholder="Адрес проживания" className="px-3 py-2 rounded-lg border border-gray-300 text-sm" />
              <input value={editForm.documentType} onChange={(e) => setEditForm({ ...editForm, documentType: e.target.value })} placeholder="Документ: вид" className="px-3 py-2 rounded-lg border border-gray-300 text-sm" />
              <input value={editForm.documentSeries} onChange={(e) => setEditForm({ ...editForm, documentSeries: e.target.value })} placeholder="Серия" className="px-3 py-2 rounded-lg border border-gray-300 text-sm" />
              <input value={editForm.documentNumber} onChange={(e) => setEditForm({ ...editForm, documentNumber: e.target.value })} placeholder="Номер" className="px-3 py-2 rounded-lg border border-gray-300 text-sm" />
              <input type="date" value={editForm.documentIssueDate} onChange={(e) => setEditForm({ ...editForm, documentIssueDate: e.target.value })} className="px-3 py-2 rounded-lg border border-gray-300 text-sm" />
              <input value={editForm.documentIdentificationNumber} onChange={(e) => setEditForm({ ...editForm, documentIdentificationNumber: e.target.value })} placeholder="Идентификационный номер" className="px-3 py-2 rounded-lg border border-gray-300 text-sm" />
              <input value={editForm.documentIssuedBy} onChange={(e) => setEditForm({ ...editForm, documentIssuedBy: e.target.value })} placeholder="Кем выдан" className="px-3 py-2 rounded-lg border border-gray-300 text-sm md:col-span-2" />
            </div>
            <div className="space-y-2">
              <div className="text-xs font-medium text-gray-500">Родители/законные представители (для детей)</div>
              {guardians.map((g, idx) => (
                <div key={g.id ?? idx} className="grid grid-cols-1 md:grid-cols-4 gap-2">
                  <input value={g.fullName} onChange={(e) => setGuardians(guardians.map((x, i) => i === idx ? { ...x, fullName: e.target.value } : x))} placeholder="ФИО" className="px-3 py-2 rounded-lg border border-gray-300 text-sm md:col-span-2" />
                  <input value={g.phone ?? ''} onChange={(e) => setGuardians(guardians.map((x, i) => i === idx ? { ...x, phone: e.target.value } : x))} placeholder="Телефон" className="px-3 py-2 rounded-lg border border-gray-300 text-sm" />
                  <input value={g.relationType ?? ''} onChange={(e) => setGuardians(guardians.map((x, i) => i === idx ? { ...x, relationType: e.target.value } : x))} placeholder="Связь (мать/отец)" className="px-3 py-2 rounded-lg border border-gray-300 text-sm" />
                </div>
              ))}
              <button
                type="button"
                onClick={() => setGuardians([...guardians, {
                  id: 0,
                  childPersonId: profile.personId ?? 0,
                  fullName: '',
                  phone: '',
                  relationType: '',
                  primaryGuardian: guardians.length === 0,
                  archived: false,
                  createdAt: new Date().toISOString(),
                }])}
                className="px-3 py-1.5 rounded-lg border border-gray-300 text-xs text-gray-700"
              >
                Добавить представителя
              </button>
            </div>
            <button onClick={saveMyProfile} disabled={savingProfile} className="inline-flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium disabled:opacity-50">
              <Save className="w-4 h-4" /> {savingProfile ? 'Сохранение...' : 'Сохранить'}
            </button>
          </div>

          <div className="bg-white rounded-xl border border-gray-200 p-5">
            <h3 className="text-sm font-semibold text-gray-700 mb-3 flex items-center gap-2">
              <BookOpen className="w-4 h-4 text-emerald-500" /> Текущий курс
            </h3>
            {!profile.currentCourse ? (
              <p className="text-sm text-gray-400 italic">Нет активного курса</p>
            ) : (
              <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3">
                <div className="font-medium text-gray-900">{profile.currentCourse.courseName ?? 'Курс'}</div>
                <div className="text-sm text-gray-600 mt-1">
                  Группа: {profile.currentCourse.groupName ?? '—'}
                  {profile.currentCourse.groupTeacherName ? (
                    <span> | Преподаватель: {profile.currentCourse.groupTeacherName}</span>
                  ) : null}
                </div>
                <div className="text-xs text-gray-500 mt-1">
                  Период: <Period from={profile.currentCourse.startDate} to={profile.currentCourse.endDate} />
                </div>
              </div>
            )}
          </div>

          <div className="bg-white rounded-xl border border-gray-200 p-5">
            <h3 className="text-sm font-semibold text-gray-700 mb-3 flex items-center gap-2">
              <History className="w-4 h-4 text-blue-500" /> История завершенных курсов
            </h3>
            {profile.completedCourses.length === 0 ? (
              <p className="text-sm text-gray-400 italic">Завершённых курсов нет</p>
            ) : (
              <div className="space-y-2">
                {profile.completedCourses.map((c) => (
                  <div key={c.enrollmentId} className="rounded-lg border border-gray-200 px-4 py-3">
                    <div className="font-medium text-gray-900">{c.courseName ?? 'Курс'}</div>
                    <div className="text-sm text-gray-600 mt-1 flex items-center gap-1">
                      <GraduationCap className="w-3.5 h-3.5 text-gray-400" />
                      Группа: {c.groupName ?? '—'}
                      {c.groupTeacherName ? <span> | Руководитель: {c.groupTeacherName}</span> : null}
                    </div>
                    <div className="text-xs text-gray-500 mt-1">
                      Период: <Period from={c.startDate} to={c.endDate} />
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="bg-white rounded-xl border border-gray-200 p-5">
            <h3 className="text-sm font-semibold text-gray-700 mb-3">Договоры</h3>
            {contracts.length === 0 ? (
              <p className="text-sm text-gray-400 italic">Договоры не найдены</p>
            ) : (
              <div className="space-y-2">
                {contracts.map((c) => (
                  <div key={c.id} className="rounded-lg border border-gray-200 px-4 py-3 flex items-center justify-between">
                    <div>
                      <div className="text-sm font-medium text-gray-900">{c.contractNumber}</div>
                      <div className="text-xs text-gray-500">
                        Стоимость: {c.basePrice} | Скидка: {c.discountPercent}% | Итого: {c.finalPrice}
                      </div>
                    </div>
                    <button onClick={() => downloadContract(c.id, c.fileName)} className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-gray-200 text-sm text-gray-700 hover:bg-gray-50">
                      <Download className="w-4 h-4" /> Скачать
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {isAdmin && (
            <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-4">
              <h3 className="text-sm font-semibold text-gray-700">Управление курсом пользователя (ADMIN)</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-gray-500 mb-1">Назначить курс</label>
                  <select
                    value={assignCourseId ?? ''}
                    onChange={(e) => setAssignCourseId(e.target.value ? Number(e.target.value) : null)}
                    className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500"
                  >
                    <option value="">Выберите курс...</option>
                    {courses.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-500 mb-1">Группа (опционально)</label>
                  <select
                    value={assignGroupId ?? ''}
                    onChange={(e) => setAssignGroupId(e.target.value ? Number(e.target.value) : null)}
                    className="w-full px-3 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-primary-500"
                  >
                    <option value="">Без группы</option>
                    {groups.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
                  </select>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <button
                  onClick={assignInProgress}
                  disabled={assigning || !assignCourseId}
                  className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium disabled:opacity-50"
                >
                  {assigning ? 'Назначение...' : 'Назначить IN_PROGRESS'}
                </button>
                <button
                  onClick={completeCurrent}
                  disabled={completing || !profile.currentCourse}
                  className="px-4 py-2 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 text-sm font-medium disabled:opacity-50"
                >
                  {completing ? 'Завершение...' : 'Завершить текущий курс'}
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
