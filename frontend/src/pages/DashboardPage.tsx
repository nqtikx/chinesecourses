import { useAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';
import {
  BookOpen, Calendar, Users, GraduationCap, UserCheck,
  ClipboardList, Clock, CheckSquare, CalendarClock,
  Shield, ArrowRight,
} from 'lucide-react';

interface QuickLink {
  to: string;
  label: string;
  description: string;
  icon: React.ReactNode;
  color: string;
  adminOnly?: boolean;
}

const quickLinks: QuickLink[] = [
  {
    to: '/courses', label: 'Курсы', description: 'Управление курсами',
    icon: <BookOpen className="w-6 h-6" />, color: 'bg-blue-500',
  },
  {
    to: '/semesters', label: 'Семестры', description: 'Периоды обучения',
    icon: <Calendar className="w-6 h-6" />, color: 'bg-indigo-500',
  },
  {
    to: '/groups', label: 'Учебные группы', description: 'Группы студентов',
    icon: <Users className="w-6 h-6" />, color: 'bg-violet-500',
  },
  {
    to: '/persons', label: 'Люди', description: 'Студенты и контакты',
    icon: <UserCheck className="w-6 h-6" />, color: 'bg-emerald-500', adminOnly: true,
  },
  {
    to: '/teachers', label: 'Преподаватели', description: 'Управление преподавателями',
    icon: <GraduationCap className="w-6 h-6" />, color: 'bg-amber-500', adminOnly: true,
  },
  {
    to: '/enrollments', label: 'Записи', description: 'Записи студентов на курсы',
    icon: <ClipboardList className="w-6 h-6" />, color: 'bg-rose-500',
  },
  {
    to: '/lessons', label: 'Занятия', description: 'Расписание занятий',
    icon: <Clock className="w-6 h-6" />, color: 'bg-cyan-500',
  },
  {
    to: '/attendance', label: 'Посещаемость', description: 'Отметки посещений',
    icon: <CheckSquare className="w-6 h-6" />, color: 'bg-teal-500',
  },
  {
    to: '/schedule', label: 'Расписание', description: 'Правила расписания групп',
    icon: <CalendarClock className="w-6 h-6" />, color: 'bg-orange-500', adminOnly: true,
  },
];

export default function DashboardPage() {
  const { user, isAdmin, isTeacher } = useAuth();

  const visible = quickLinks.filter((l) => !l.adminOnly || isAdmin);

  return (
    <div className="space-y-8">
      <div className="bg-white rounded-2xl border border-gray-200 p-8">
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">
              Добро пожаловать, {user?.username}!
            </h1>
            <p className="text-gray-500 mt-1">
              {isAdmin
                ? 'Вы вошли как администратор. У вас полный доступ ко всем функциям системы.'
                : isTeacher
                ? 'Вы вошли как преподаватель. Вам доступен просмотр курсов, групп, занятий и управление посещаемостью.'
                : 'Вы вошли как пользователь.'}
            </p>
          </div>
          <div className="flex items-center gap-2 px-4 py-2 bg-gray-50 rounded-xl">
            <Shield className="w-5 h-5 text-gray-400" />
            <span className="text-sm font-medium text-gray-700">
              {isAdmin ? 'Администратор' : isTeacher ? 'Преподаватель' : 'Пользователь'}
            </span>
          </div>
        </div>
      </div>

      {isAdmin && (
        <div className="bg-gradient-to-r from-primary-600 to-primary-700 rounded-2xl p-6 text-white">
          <h2 className="text-lg font-semibold mb-2">Возможности администратора</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 text-sm">
            <div className="bg-white/10 rounded-lg p-3">Создание и редактирование курсов, семестров, групп</div>
            <div className="bg-white/10 rounded-lg p-3">Управление людьми, преподавателями, записями</div>
            <div className="bg-white/10 rounded-lg p-3">Создание занятий, расписание, архивация данных</div>
          </div>
        </div>
      )}

      {isTeacher && (
        <div className="bg-gradient-to-r from-blue-600 to-blue-700 rounded-2xl p-6 text-white">
          <h2 className="text-lg font-semibold mb-2">Возможности преподавателя</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 text-sm">
            <div className="bg-white/10 rounded-lg p-3">Просмотр курсов, семестров и учебных групп</div>
            <div className="bg-white/10 rounded-lg p-3">Просмотр записей студентов и занятий</div>
            <div className="bg-white/10 rounded-lg p-3">Создание и управление посещаемостью</div>
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
                <ArrowRight className="w-4 h-4 text-gray-300 group-hover:text-gray-500 transition-colors" />
              </div>
              <h3 className="mt-4 font-semibold text-gray-900">{link.label}</h3>
              <p className="text-sm text-gray-500 mt-0.5">{link.description}</p>
            </Link>
          ))}
        </div>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Разграничение прав доступа</h2>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-200">
                <th className="text-left py-3 px-4 font-medium text-gray-500">Функция</th>
                <th className="text-center py-3 px-4 font-medium text-red-600">Админ</th>
                <th className="text-center py-3 px-4 font-medium text-blue-600">Преподаватель</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {[
                ['Курсы — создание/редактирование', true, false],
                ['Курсы — просмотр', true, true],
                ['Семестры — создание/редактирование', true, false],
                ['Семестры — просмотр', true, true],
                ['Учебные группы — создание/редактирование', true, false],
                ['Учебные группы — просмотр', true, true],
                ['Люди — CRUD', true, false],
                ['Преподаватели — управление', true, false],
                ['Записи — создание/редактирование', true, false],
                ['Записи — просмотр', true, true],
                ['Занятия — создание/редактирование', true, false],
                ['Занятия — просмотр', true, true],
                ['Посещаемость — создание/редактирование', true, true],
                ['Расписание — управление', true, false],
              ].map(([label, admin, teacher], i) => (
                <tr key={i} className="hover:bg-gray-50">
                  <td className="py-2.5 px-4 text-gray-700">{label as string}</td>
                  <td className="py-2.5 px-4 text-center">{admin ? '✅' : '—'}</td>
                  <td className="py-2.5 px-4 text-center">{teacher ? '✅' : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
