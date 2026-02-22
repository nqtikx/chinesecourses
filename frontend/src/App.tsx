import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import ToastContainer from './components/ui/Toast';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import CoursesPage from './pages/CoursesPage';
import SemestersPage from './pages/SemestersPage';
import StudyGroupsPage from './pages/StudyGroupsPage';
import PersonsPage from './pages/PersonsPage';
import TeachersPage from './pages/TeachersPage';
import EnrollmentsPage from './pages/EnrollmentsPage';
import LessonSessionsPage from './pages/LessonSessionsPage';
import AttendancePage from './pages/AttendancePage';
import ScheduleRulesPage from './pages/ScheduleRulesPage';

function AdminOnlyRoute({ children }: { children: React.ReactNode }) {
  const { isAdmin } = useAuth();
  if (!isAdmin) return <Navigate to="/" replace />;
  return <>{children}</>;
}

function LoginGuard() {
  const { user, loading } = useAuth();
  if (loading) return null;
  if (user) return <Navigate to="/" replace />;
  return <LoginPage />;
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastContainer />
        <Routes>
          <Route path="/login" element={<LoginGuard />} />
          <Route
            element={
              <ProtectedRoute>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route path="/" element={<DashboardPage />} />
            <Route path="/courses" element={<CoursesPage />} />
            <Route path="/semesters" element={<SemestersPage />} />
            <Route path="/groups" element={<StudyGroupsPage />} />
            <Route path="/persons" element={<AdminOnlyRoute><PersonsPage /></AdminOnlyRoute>} />
            <Route path="/teachers" element={<AdminOnlyRoute><TeachersPage /></AdminOnlyRoute>} />
            <Route path="/enrollments" element={<EnrollmentsPage />} />
            <Route path="/lessons" element={<LessonSessionsPage />} />
            <Route path="/attendance" element={<AttendancePage />} />
            <Route path="/schedule" element={<AdminOnlyRoute><ScheduleRulesPage /></AdminOnlyRoute>} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
