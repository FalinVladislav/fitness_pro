import { Link, NavLink, Outlet } from 'react-router-dom';
import { Bell, CalendarDays, LogOut, Users } from 'lucide-react';
import { useAuth } from './auth';

const navByRole = {
  ADMIN: [
    ['Клиенты', '/clients'], ['Абонементы', '/memberships'], ['Расписание', '/schedule'], ['Посещения', '/visits'], ['Справочники', '/catalogs']
  ],
  MANAGER: [
    ['Отчеты', '/reports'], ['Клиенты', '/clients'], ['Расписание', '/schedule']
  ],
  TRAINER: [
    ['Мое расписание', '/schedule'], ['Участники', '/bookings'], ['Посещения', '/visits']
  ],
  CLIENT: [
    ['Расписание', '/schedule'], ['Мой абонемент', '/memberships'], ['Мои записи', '/bookings'], ['Уведомления', '/notifications']
  ],
} as const;

export function AppShell() {
  const { user, logout } = useAuth();
  const nav = user ? navByRole[user.role] : [];

  return (
    <div className="shell">
      <aside>
        <Link to="/" className="brand">Фитнес-Про</Link>
        <p className="role">{user?.fullName}<span>{user?.role}</span></p>
        <nav>
          {nav.map(([label, path]) => <NavLink key={path} to={path}>{label}</NavLink>)}
        </nav>
        <button className="ghost" onClick={logout}><LogOut size={16} /> Выйти</button>
      </aside>
      <main>
        <header className="topline">
          <div><Users size={18} /> Автоматизация фитнес-центра</div>
          <Link to="/notifications"><Bell size={18} /></Link>
          <Link to="/schedule"><CalendarDays size={18} /></Link>
        </header>
        <Outlet />
      </main>
    </div>
  );
}
