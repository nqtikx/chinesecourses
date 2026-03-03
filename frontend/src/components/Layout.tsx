import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  LayoutDashboard, BookOpen, Calendar, Users, GraduationCap,
  UserCheck, ClipboardList, Clock, CheckSquare, CalendarClock,
  LogOut, Shield, ChevronLeft, ChevronRight, FileText, UserCircle,
} from 'lucide-react';
import { useState } from 'react';
import Badge from './ui/Badge';

interface NavItem {
  to: string;
  label: string;
  icon: React.ReactNode;
  adminOnly?: boolean;
  section?: string;
}

const navItems: NavItem[] = [
  { to: '/', label: 'Главная', icon: <LayoutDashboard className="w-5 h-5" />, section: 'Навигация' },
  { to: '/courses', label: 'Курсы', icon: <BookOpen className="w-5 h-5" />, section: 'Обучение' },
  { to: '/semesters', label: 'Семестры', icon: <Calendar className="w-5 h-5" /> },
  { to: '/groups', label: 'Учебные группы', icon: <Users className="w-5 h-5" /> },
  { to: '/lessons', label: 'Занятия', icon: <Clock className="w-5 h-5" /> },
  { to: '/profile', label: 'Профиль', icon: <UserCircle className="w-5 h-5" /> },
  { to: '/schedule', label: 'Расписание', icon: <CalendarClock className="w-5 h-5" />, adminOnly: true },
  { to: '/contracts', label: 'Договоры PDF', icon: <FileText className="w-5 h-5" />, adminOnly: true },
  { to: '/persons', label: 'Абитуриенты и слушатели', icon: <UserCheck className="w-5 h-5" />, adminOnly: true, section: 'Контингент' },
  { to: '/teachers', label: 'Преподаватели', icon: <GraduationCap className="w-5 h-5" />, adminOnly: true },
  { to: '/enrollments', label: 'Зачисления', icon: <ClipboardList className="w-5 h-5" />, section: 'Учёт' },
  { to: '/attendance', label: 'Посещаемость', icon: <CheckSquare className="w-5 h-5" /> },
];

export default function Layout() {
  const { user, logout, isAdmin, isTeacher } = useAuth();
  const navigate = useNavigate();
  const [collapsed, setCollapsed] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const visibleItems = navItems.filter((item) => !item.adminOnly || isAdmin);

  let lastSection = '';

  return (
    <div className="flex h-screen bg-gray-50">
      <aside className={`${collapsed ? 'w-16' : 'w-64'} bg-white border-r border-gray-200 flex flex-col transition-all duration-200 flex-shrink-0`}>
        <div className={`flex items-center ${collapsed ? 'justify-center' : 'px-5'} h-16 border-b border-gray-100`}>
          {!collapsed ? (
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center text-white font-bold text-sm">
                CC
              </div>
              <div>
                <div className="font-bold text-gray-900 text-sm leading-none">ChineseCourses</div>
                <div className="text-[10px] text-gray-400 mt-0.5">Панель управления</div>
              </div>
            </div>
          ) : (
            <div className="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center text-white font-bold text-xs">
              CC
            </div>
          )}
        </div>

        <nav className="flex-1 py-3 overflow-y-auto">
          {visibleItems.map((item) => {
            const showSection = !collapsed && item.section && item.section !== lastSection;
            if (item.section) lastSection = item.section;
            return (
              <div key={item.to}>
                {showSection && (
                  <div className="px-5 pt-4 pb-1.5 text-[10px] font-semibold text-gray-400 uppercase tracking-wider">
                    {item.section}
                  </div>
                )}
                <NavLink
                  to={item.to}
                  end={item.to === '/'}
                  className={({ isActive }) =>
                    `flex items-center gap-3 mx-2 px-3 py-2 rounded-lg text-[13px] font-medium transition-colors ${
                      isActive
                        ? 'bg-primary-50 text-primary-700 shadow-sm'
                        : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                    } ${collapsed ? 'justify-center' : ''}`
                  }
                  title={collapsed ? item.label : undefined}
                >
                  {item.icon}
                  {!collapsed && <span>{item.label}</span>}
                </NavLink>
              </div>
            );
          })}
        </nav>

        <div className="border-t border-gray-100 p-2">
          <button
            onClick={() => setCollapsed(!collapsed)}
            className="flex items-center justify-center w-full p-2 rounded-lg hover:bg-gray-50 text-gray-400 transition-colors"
          >
            {collapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronLeft className="w-4 h-4" />}
          </button>
        </div>
      </aside>

      <div className="flex-1 flex flex-col min-w-0">
        <header className="h-14 bg-white border-b border-gray-200 flex items-center justify-between px-6 flex-shrink-0">
          <div className="text-xs text-gray-400">
            WEB-приложение администратора курсов китайского языка
          </div>
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2">
              <Shield className="w-4 h-4 text-gray-400" />
              <Badge variant={isAdmin ? 'red' : isTeacher ? 'blue' : 'gray'}>
                {isAdmin ? 'Администратор' : isTeacher ? 'Преподаватель' : 'Пользователь'}
              </Badge>
            </div>
            <div className="w-px h-5 bg-gray-200" />
            <span className="text-sm text-gray-700 font-medium">{user?.username}</span>
            <button
              onClick={handleLogout}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm text-gray-500 hover:bg-red-50 hover:text-red-600 transition-colors"
            >
              <LogOut className="w-4 h-4" />
              Выйти
            </button>
          </div>
        </header>

        <main className="flex-1 overflow-auto p-6">
          <Outlet />
        </main>

        <footer className="h-10 bg-white border-t border-gray-100 flex items-center justify-center px-6 flex-shrink-0">
          <span className="text-[11px] text-gray-400">
            БНТУ — Курсы китайского языка — Админ-панель
          </span>
        </footer>
      </div>
    </div>
  );
}
