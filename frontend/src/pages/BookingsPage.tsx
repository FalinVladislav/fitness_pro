import { useEffect, useState } from 'react';
import { api, errorMessage } from '../shared/api';
import { useAuth } from '../shared/auth';

export function BookingsPage() {
  const { user } = useAuth();
  const [items, setItems] = useState<any[]>([]);
  const [schedules, setSchedules] = useState<any[]>([]);
  const [scheduleId, setScheduleId] = useState('');
  const [message, setMessage] = useState('');
  const load = () => {
    if (user?.role === 'TRAINER') {
      return api.get('/schedule/my').then((r) => setSchedules(r.data));
    }
    return api.get(user?.role === 'CLIENT' ? '/bookings/my' : '/bookings').then((r) => setItems(r.data));
  };
  useEffect(() => { load(); }, []);
  useEffect(() => {
    if (user?.role === 'TRAINER' && scheduleId) {
      api.get(`/bookings/schedule/${scheduleId}`).then((r) => setItems(r.data));
    } else if (user?.role === 'TRAINER') {
      setItems([]);
    }
  }, [scheduleId, user?.role]);

  async function cancel(id: number) {
    try {
      await api.post(`/bookings/${id}/cancel`);
      setMessage('Запись отменена');
      load();
    } catch (err) { setMessage(errorMessage(err)); }
  }

  return (
    <section className="card">
      <h2>{user?.role === 'CLIENT' ? 'Мои записи' : 'Список записавшихся клиентов'}</h2>
      {message && <div className="notice">{message}</div>}
      {user?.role === 'TRAINER' && (
        <select value={scheduleId} onChange={(e) => setScheduleId(e.target.value)}>
          <option value="">Выберите занятие</option>
          {schedules.map((s) => <option key={s.id} value={s.id}>{s.date} {s.startTime} · {s.trainingTypeName}</option>)}
        </select>
      )}
      <div className="list">{items.map((b) => <article key={b.id}><b>{b.clientName}</b><span>{b.schedule.trainingTypeName} · {b.schedule.date} {b.schedule.startTime} · {b.status}</span>{user?.role !== 'TRAINER' && b.status === 'ACTIVE' && <button className="ghost" onClick={() => cancel(b.id)}>Отменить</button>}</article>)}</div>
      {user?.role === 'TRAINER' && scheduleId && items.length === 0 && <div className="notice">На выбранное занятие пока никто не записался.</div>}
    </section>
  );
}
