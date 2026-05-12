import { useEffect, useState } from 'react';
import { api, errorMessage } from '../shared/api';
import { useAuth } from '../shared/auth';

export function BookingsPage() {
  const { user } = useAuth();
  const [items, setItems] = useState<any[]>([]);
  const [message, setMessage] = useState('');
  const load = () => api.get(user?.role === 'CLIENT' ? '/bookings/my' : '/bookings').then((r) => setItems(r.data));
  useEffect(() => { load(); }, []);

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
      <div className="list">{items.map((b) => <article key={b.id}><b>{b.clientName}</b><span>{b.schedule.trainingTypeName} · {b.schedule.date} {b.schedule.startTime} · {b.status}</span><button className="ghost" onClick={() => cancel(b.id)}>Отменить</button></article>)}</div>
    </section>
  );
}
