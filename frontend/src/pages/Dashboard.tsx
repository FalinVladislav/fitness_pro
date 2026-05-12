import { Link } from 'react-router-dom';
import { useAuth } from '../shared/auth';

const copy = {
  ADMIN: ['Панель администратора', 'Создавайте клиентов, продавайте абонементы, отмечайте проход и управляйте справочниками.'],
  MANAGER: ['Панель руководителя', 'Следите за доходами, посещаемостью, популярностью направлений и загрузкой тренеров.'],
  TRAINER: ['Панель тренера', 'Ваше расписание, участники занятий и фиксация фактической явки собраны рядом.'],
  CLIENT: ['Личный кабинет клиента', 'Выбирайте тренировки, записывайтесь онлайн и отслеживайте уведомления клуба.'],
} as const;

export function Dashboard() {
  const { user } = useAuth();
  const [title, subtitle] = copy[user!.role];
  return (
    <section className="hero">
      <div>
        <span className="badge">Система автоматизации</span>
        <h1>{title}</h1>
        <p>{subtitle}</p>
        <Link className="buttonLink" to={user!.role === 'MANAGER' ? '/reports' : '/schedule'}>Начать работу</Link>
      </div>
      <div className="heroPanel">
        <h3>Сегодня в фокусе</h3>
        <p>Быстрая запись, контроль абонементов, прозрачные отчеты и меньше ручной рутины на ресепшене.</p>
      </div>
    </section>
  );
}
