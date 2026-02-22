import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  LayoutDashboard, BookOpen, Calendar, Users, GraduationCap,
  UserCheck, ClipboardList, Clock, CheckSquare, CalendarClock,
  LogOut, Shield, ChevronLeft, ChevronRight,
} from 'lucide-react';
import { useState } from 'react';
import Badge from './ui/Badge';

interface NavItem {
  to: string;
  label: string;
  icon: React.ReactNode;
  adminOnly?: boolean;
}

const navItems: NavItem[] = [
  { to: '/', label: 'Панель', icon: <LayoutDashboard className="w-5 h-5" /> },
  { to: '/courses', label: 'Курсы', icon: <BookOpen className="w-5 h-5" /> },
  { to: '/semesters', label: 'Семестры', icon: <Calendar className="w-5 h-5" /> },
  { to: '/groups', label: 'Учебные группы', icon: <Users className="w-5 h-5" /> },
  { to: '/persons', label: 'Люди', icon: <UserCheck className="w-5 h-5" />, adminOnly: true },
  { to: '/teachers', label: 'Преподаватели', icon: <GraduationCap className="w-5 h-5" />, adminOnly: true },
  { to: '/enrollments', label: 'Записи', icon: <ClipboardList className="w-5 h-5" /> },
  { to: '/lessons', label: 'Занятия', icon: <Clock className="w-5 h-5" /> },
  { to: '/attendance', label: 'Посещаемость', icon: <CheckSquare className="w-5 h-5" /> },
  { to: '/schedule', label: 'Расписание', icon: <CalendarClock className="w-5 h-5" />, adminOnly: true },
];

export default function Layout() {
  const { user, logout, isAdmin, isTeacher } = useAuth();
  const navigate = useNavigate();
  const [collapsed, setCollapsed] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const visibleItems = navItems.filter((item) => {
    if (item.adminOnly && !isAdmin) return false;
    return true;
  });

  return (
    <div className="flex h-screen bg-gray-50">
      <aside className={`${collapsed ? 'w-16' : 'w-64'} bg-white border-r border-gray-200 flex flex-col transition-all duration-200 flex-shrink-0`}>
        <div className={`flex items-center ${collapsed ? 'justify-center' : 'px-5'} h-16 border-b border-gray-100`}>
          {!collapsed && (
            <div className="flex items-center gap-2">
              <span className="text-2xl">🀄</span>
              <span className="font-bold text-gray-900 text-sm leading-tight">
                Курсы<br/>китайского
              </span>
            </div>
          )}
          {collapsed && <span className="text-2xl">🀄</span>}
        </div>

        <nav className="flex-1 py-4 overflow-y-auto">
          {visibleItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                `flex items-center gap-3 mx-2 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-primary-50 text-primary-700'
                    : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                } ${collapsed ? 'justify-center' : ''}`
              }
              title={collapsed ? item.label : undefined}
            >
              {item.icon}
              {!collapsed && <span>{item.label}</span>}
            </NavLink>
          ))}
        </nav>

        <div className="border-t border-gray-100 p-3">
          <button
            onClick={() => setCollapsed(!collapsed)}
            className="flex items-center justify-center w-full p-2 rounded-lg hover:bg-gray-50 text-gray-400 transition-colors"
          >
            {collapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronLeft className="w-4 h-4" />}
          </button>
        </div>
      </aside>

      <div className="flex-1 flex flex-col min-w-0">
        <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6 flex-shrink-0">
          <div />
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2">
              <Shield className="w-4 h-4 text-gray-400" />
              <Badge variant={isAdmin ? 'red' : isTeacher ? 'blue' : 'gray'}>
                {isAdmin ? 'Администратор' : isTeacher ? 'Преподаватель' : 'Пользователь'}
              </Badge>
            </div>
            <span className="text-sm text-gray-600 font-medium">{user?.username}</span>
            <button
              onClick={handleLogout}
              className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-red-600 transition-colors"
            >
              <LogOut className="w-4 h-4" />
              <span>Выйти</span>
            </button>
          </div>
        </header>

        <main className="flex-1 overflow-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
