import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';
import { coursesApi, teachersApi } from '../api';
import type { CourseResponse, TeacherResponse } from '../types';
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
  { to: '/courses', label: 'Курсы', description: 'Программы обучения китайскому языку', icon: <BookOpen className="w-6 h-6" />, color: 'bg-blue-500', adminAction: 'Создание, редактирование, архивация', teacherAction: 'Просмотр' },
  { to: '/semesters', label: 'Семестры', description: 'Учебные периоды в рамках курсов', icon: <Calendar className="w-6 h-6" />, color: 'bg-indigo-500', adminAction: 'Создание, редактирование, архивация', teacherAction: 'Просмотр' },
  { to: '/groups', label: 'Учебные группы', description: 'Группы слушателей с преподавателями и студентами', icon: <Users className="w-6 h-6" />, color: 'bg-violet-500', adminAction: 'Управление группами, просмотр студентов', teacherAction: 'Просмотр' },
  { to: '/lessons', label: 'Занятия', description: 'Расписание занятий с темами и аудиториями', icon: <Clock className="w-6 h-6" />, color: 'bg-cyan-500', adminAction: 'Создание, редактирование, отмена', teacherAction: 'Просмотр' },
  { to: '/attendance', label: 'Посещаемость', description: 'Отметка присутствия и пропусков студентов', icon: <CheckSquare className="w-6 h-6" />, color: 'bg-teal-500', adminAction: 'Полное управление', teacherAction: 'Создание, редактирование' },
  { to: '/enrollments', label: 'Зачисления', description: 'Записи студентов на курсы и в группы', icon: <ClipboardList className="w-6 h-6" />, color: 'bg-rose-500', adminAction: 'Создание, редактирование, архивация', teacherAction: 'Просмотр' },
  { to: '/persons', label: 'Абитуриенты и слушатели', description: 'База данных всех лиц в системе', icon: <UserCheck className="w-6 h-6" />, color: 'bg-emerald-500', adminOnly: true, adminAction: 'Полное управление (CRUD)' },
  { to: '/teachers', label: 'Преподаватели', description: 'Преподавательский состав с контактами', icon: <GraduationCap className="w-6 h-6" />, color: 'bg-amber-500', adminOnly: true, adminAction: 'Создание, архивация' },
  { to: '/schedule', label: 'Расписание', description: 'Правила повторяющегося расписания', icon: <CalendarClock className="w-6 h-6" />, color: 'bg-orange-500', adminOnly: true, adminAction: 'Полное управление' },
];

export default function DashboardPage() {
  const { user, isAdmin, isTeacher } = useAuth();
  const visible = quickLinks.filter((l) => !l.adminOnly || isAdmin);
  const [coursesCount, setCoursesCount] = useState<number | null>(null);
  const [teachersCount, setTeachersCount] = useState<number | null>(null);

  useEffect(() => {
    coursesApi.list().then(({ data }) => setCoursesCount(data.filter(c => !c.archived).length)).catch(() => {});
    if (isAdmin) {
      teachersApi.list().then(({ data }) => setTeachersCount(data.filter(t => !t.archived).length)).catch(() => {});
    }
  }, []);

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
                ? 'Вы вошли как администратор. У вас полный доступ: управление курсами, группами, занятиями, посещаемостью и контингентом.'
                : isTeacher
                  ? 'Вы вошли как преподаватель. Доступен просмотр курсов, групп, занятий и ведение посещаемости.'
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

      {(coursesCount !== null || teachersCount !== null) && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {coursesCount !== null && (
            <Link to="/courses" className="bg-white rounded-xl border border-gray-200 p-5 hover:shadow-md transition-shadow">
              <div className="flex items-center gap-3">
                <div className="bg-blue-100 text-blue-600 p-2 rounded-lg"><BookOpen className="w-5 h-5" /></div>
                <div>
                  <div className="text-2xl font-bold text-gray-900">{coursesCount}</div>
                  <div className="text-xs text-gray-500">Активных курсов</div>
                </div>
              </div>
            </Link>
          )}
          {teachersCount !== null && (
            <Link to="/teachers" className="bg-white rounded-xl border border-gray-200 p-5 hover:shadow-md transition-shadow">
              <div className="flex items-center gap-3">
                <div className="bg-amber-100 text-amber-600 p-2 rounded-lg"><GraduationCap className="w-5 h-5" /></div>
                <div>
                  <div className="text-2xl font-bold text-gray-900">{teachersCount}</div>
                  <div className="text-xs text-gray-500">Преподавателей</div>
                </div>
              </div>
            </Link>
          )}
        </div>
      )}

      {isAdmin && (
        <div className="bg-gradient-to-br from-primary-600 to-primary-800 rounded-2xl p-6 text-white">
          <h2 className="text-lg font-semibold mb-3">Ваши возможности</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 text-sm">
            <div className="bg-white/10 rounded-lg p-3 backdrop-blur">
              <PenLine className="w-4 h-4 mb-1.5 opacity-80" />
              Курсы, семестры, группы, занятия — полное управление
            </div>
            <div className="bg-white/10 rounded-lg p-3 backdrop-blur">
              <UserCheck className="w-4 h-4 mb-1.5 opacity-80" />
              Контингент слушателей, преподаватели, зачисления
            </div>
            <div className="bg-white/10 rounded-lg p-3 backdrop-blur">
              <CheckSquare className="w-4 h-4 mb-1.5 opacity-80" />
              Посещаемость — удобная отметка по занятиям
            </div>
          </div>
        </div>
      )}

      {isTeacher && !isAdmin && (
        <div className="bg-gradient-to-br from-blue-600 to-blue-800 rounded-2xl p-6 text-white">
          <h2 className="text-lg font-semibold mb-3">Ваши возможности</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 text-sm">
            <div className="bg-white/10 rounded-lg p-3 backdrop-blur">
              <Eye className="w-4 h-4 mb-1.5 opacity-80" />
              Просмотр курсов, семестров, групп
            </div>
            <div className="bg-white/10 rounded-lg p-3 backdrop-blur">
              <Eye className="w-4 h-4 mb-1.5 opacity-80" />
              Просмотр зачислений и занятий
            </div>
            <div className="bg-white/10 rounded-lg p-3 backdrop-blur">
              <CheckSquare className="w-4 h-4 mb-1.5 opacity-80" />
              Ведение посещаемости студентов
            </div>
          </div>
        </div>
      )}

      <div>
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Быстрый доступ</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {visible.map((link) => (
            <Link key={link.to} to={link.to} className="group bg-white rounded-xl border border-gray-200 p-5 hover:shadow-md hover:border-gray-300 transition-all">
              <div className="flex items-center justify-between">
                <div className={`${link.color} text-white p-2.5 rounded-lg`}>{link.icon}</div>
                <ArrowRight className="w-4 h-4 text-gray-300 group-hover:text-gray-500 group-hover:translate-x-0.5 transition-all" />
              </div>
              <h3 className="mt-4 font-semibold text-gray-900">{link.label}</h3>
              <p className="text-sm text-gray-500 mt-0.5">{link.description}</p>
              {(isAdmin ? link.adminAction : link.teacherAction) && (
                <p className="text-xs text-gray-400 mt-2">{isAdmin ? link.adminAction : link.teacherAction}</p>
              )}
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
}
