import { useAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';
import {
  BookOpen, Calendar, Users, GraduationCap, UserCheck,
  ClipboardList, Clock, CheckSquare, CalendarClock,
  ArrowRight, ShieldCheck, Eye, PenLine,
} from 'lucide-react';

interface QuickLink {
  to: string;
  label: string;
  description: string;
  icon: React.ReactNode;
  color: string;
  adminOnly?: boolean;
  teacherAction?: string;
  adminAction?: string;
}

const quickLinks: QuickLink[] = [
  {
    to: '/courses', label: 'Курсы', description: 'Программы обучения китайскому языку',
    icon: <BookOpen className="w-6 h-6" />, color: 'bg-blue-500',
    adminAction: 'Создание, редактирование, архивация', teacherAction: 'Просмотр',
  },
  {
    to: '/semesters', label: 'Семестры', description: 'Учебные периоды в рамках курсов',
    icon: <Calendar className="w-6 h-6" />, color: 'bg-indigo-500',
    adminAction: 'Создание, редактирование, архивация', teacherAction: 'Просмотр',
  },
  {
    to: '/groups', label: 'Учебные группы', description: 'Группы слушателей с преподавателями',
    icon: <Users className="w-6 h-6" />, color: 'bg-violet-500',
    adminAction: 'Создание, редактирование, архивация', teacherAction: 'Просмотр',
  },
  {
    to: '/persons', label: 'Абитуриенты и слушатели', description: 'База данных всех лиц в системе',
    icon: <UserCheck className="w-6 h-6" />, color: 'bg-emerald-500', adminOnly: true,
    adminAction: 'Полное управление (CRUD)', teacherAction: '',
  },
  {
    to: '/teachers', label: 'Преподаватели', description: 'Привязка преподавателей к персонам',
    icon: <GraduationCap className="w-6 h-6" />, color: 'bg-amber-500', adminOnly: true,
    adminAction: 'Создание, архивация', teacherAction: '',
  },
  {
    to: '/enrollments', label: 'Зачисления', description: 'Записи слушателей на курсы и группы',
    icon: <ClipboardList className="w-6 h-6" />, color: 'bg-rose-500',
    adminAction: 'Создание, редактирование, архивация', teacherAction: 'Просмотр',
  },
  {
    to: '/lessons', label: 'Занятия', description: 'Сессии занятий учебных групп',
    icon: <Clock className="w-6 h-6" />, color: 'bg-cyan-500',
    adminAction: 'Создание, редактирование, архивация', teacherAction: 'Просмотр',
  },
  {
    to: '/attendance', label: 'Посещаемость', description: 'Учёт посещений слушателей',
    icon: <CheckSquare className="w-6 h-6" />, color: 'bg-teal-500',
    adminAction: 'Полное управление', teacherAction: 'Создание, редактирование',
  },
  {
    to: '/schedule', label: 'Расписание', description: 'Правила повторяющегося расписания',
    icon: <CalendarClock className="w-6 h-6" />, color: 'bg-orange-500', adminOnly: true,
    adminAction: 'Полное управление', teacherAction: '',
  },
];

const permMatrix: [string, boolean, boolean][] = [
  ['Курсы — создание / редактирование / архивация', true, false],
  ['Курсы — просмотр списка и деталей', true, true],
  ['Семестры — создание / редактирование / архивация', true, false],
  ['Семестры — просмотр', true, true],
  ['Учебные группы — создание / редактирование / архивация', true, false],
  ['Учебные группы — просмотр', true, true],
  ['Абитуриенты и слушатели — полный CRUD', true, false],
  ['Преподаватели — создание / архивация', true, false],
  ['Зачисления — создание / редактирование / архивация', true, false],
  ['Зачисления — просмотр по группе', true, true],
  ['Занятия — создание / редактирование / архивация', true, false],
  ['Занятия — просмотр по группе', true, true],
  ['Посещаемость — создание / редактирование / архивация', true, true],
  ['Расписание — полное управление правилами', true, false],
];

export default function DashboardPage() {
  const { user, isAdmin, isTeacher } = useAuth();
  const visible = quickLinks.filter((l) => !l.adminOnly || isAdmin);

  return (
    <div className="space-y-8 max-w-6xl">
      <div className="bg-white rounded-2xl border border-gray-200 p-8">
        <div className="flex items-start justify-between flex-wrap gap-4">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">
              Добро пожаловать, {user?.username}!
            </h1>
            <p className="text-gray-500 mt-1 max-w-xl">
              {isAdmin
                ? 'Вы вошли как администратор. У вас полный доступ ко всем функциям: управление курсами, семестрами, группами, контингентом слушателей, зачислениями, занятиями, посещаемостью и расписанием.'
                : isTeacher
                  ? 'Вы вошли как преподаватель. Вам доступен просмотр курсов, семестров, учебных групп, занятий и зачислений, а также создание и редактирование записей посещаемости.'
                  : 'Вы вошли в систему.'}
            </p>
          </div>
          <div className={`flex items-center gap-2 px-4 py-2.5 rounded-xl ${isAdmin ? 'bg-red-50' : isTeacher ? 'bg-blue-50' : 'bg-gray-50'}`}>
            <ShieldCheck className={`w-5 h-5 ${isAdmin ? 'text-red-500' : isTeacher ? 'text-blue-500' : 'text-gray-400'}`} />
            <span className={`text-sm font-semibold ${isAdmin ? 'text-red-700' : isTeacher ? 'text-blue-700' : 'text-gray-600'}`}>
              {isAdmin ? 'Администратор' : isTeacher ? 'Преподаватель' : 'Пользователь'}
            </span>
          </div>
        </div>
      </div>

      {isAdmin && (
        <div className="bg-gradient-to-br from-primary-600 to-primary-800 rounded-2xl p-6 text-white">
          <h2 className="text-lg font-semibold mb-3">Ваши возможности (Администратор)</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 text-sm">
            <div className="bg-white/10 rounded-lg p-3 backdrop-blur">
              <PenLine className="w-4 h-4 mb-1.5 opacity-80" />
              Создание и редактирование курсов, семестров, учебных групп
            </div>
            <div className="bg-white/10 rounded-lg p-3 backdrop-blur">
              <UserCheck className="w-4 h-4 mb-1.5 opacity-80" />
              Ведение базы абитуриентов, слушателей, преподавателей, зачислений
            </div>
            <div className="bg-white/10 rounded-lg p-3 backdrop-blur">
              <CalendarClock className="w-4 h-4 mb-1.5 opacity-80" />
              Управление занятиями, расписанием, посещаемостью и архивацией
            </div>
          </div>
        </div>
      )}

      {isTeacher && !isAdmin && (
        <div className="bg-gradient-to-br from-blue-600 to-blue-800 rounded-2xl p-6 text-white">
          <h2 className="text-lg font-semibold mb-3">Ваши возможности (Преподаватель)</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 text-sm">
            <div className="bg-white/10 rounded-lg p-3 backdrop-blur">
              <Eye className="w-4 h-4 mb-1.5 opacity-80" />
              Просмотр курсов, семестров, учебных групп
            </div>
            <div className="bg-white/10 rounded-lg p-3 backdrop-blur">
              <Eye className="w-4 h-4 mb-1.5 opacity-80" />
              Просмотр зачислений слушателей и занятий
            </div>
            <div className="bg-white/10 rounded-lg p-3 backdrop-blur">
              <CheckSquare className="w-4 h-4 mb-1.5 opacity-80" />
              Создание и редактирование записей посещаемости
            </div>
          </div>
        </div>
      )}

      <div>
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Быстрый доступ</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {visible.map((link) => (
            <Link
              key={link.to}
              to={link.to}
              className="group bg-white rounded-xl border border-gray-200 p-5 hover:shadow-md hover:border-gray-300 transition-all"
            >
              <div className="flex items-center justify-between">
                <div className={`${link.color} text-white p-2.5 rounded-lg`}>
                  {link.icon}
                </div>
                <ArrowRight className="w-4 h-4 text-gray-300 group-hover:text-gray-500 group-hover:translate-x-0.5 transition-all" />
              </div>
              <h3 className="mt-4 font-semibold text-gray-900">{link.label}</h3>
              <p className="text-sm text-gray-500 mt-0.5">{link.description}</p>
              {(isAdmin ? link.adminAction : link.teacherAction) && (
                <p className="text-xs text-gray-400 mt-2">
                  {isAdmin ? link.adminAction : link.teacherAction}
                </p>
              )}
            </Link>
          ))}
        </div>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-1">Матрица прав доступа</h2>
        <p className="text-sm text-gray-500 mb-4">Полная таблица разграничения функций между ролями</p>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-200">
                <th className="text-left py-3 px-4 font-semibold text-gray-600">Функция</th>
                <th className="text-center py-3 px-4 font-semibold text-red-600 whitespace-nowrap">
                  <span className="inline-flex items-center gap-1"><ShieldCheck className="w-3.5 h-3.5" /> Админ</span>
                </th>
                <th className="text-center py-3 px-4 font-semibold text-blue-600 whitespace-nowrap">
                  <span className="inline-flex items-center gap-1"><GraduationCap className="w-3.5 h-3.5" /> Преподаватель</span>
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {permMatrix.map(([label, admin, teacher], i) => (
                <tr key={i} className={`${isAdmin && admin ? 'bg-red-50/30' : isTeacher && teacher ? 'bg-blue-50/30' : ''} hover:bg-gray-50 transition-colors`}>
                  <td className="py-2.5 px-4 text-gray-700">{label}</td>
                  <td className="py-2.5 px-4 text-center">{admin ? <span className="text-emerald-600 font-medium">Да</span> : <span className="text-gray-300">—</span>}</td>
                  <td className="py-2.5 px-4 text-center">{teacher ? <span className="text-emerald-600 font-medium">Да</span> : <span className="text-gray-300">—</span>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
