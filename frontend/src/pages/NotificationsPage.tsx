import { useEffect, useState } from 'react';
import { api } from '../shared/api';

export function NotificationsPage() {
  const [items, setItems] = useState<any[]>([]);
  const load = () => api.get('/notifications').then((r) => setItems(r.data));
  useEffect(() => { load(); }, []);
  return (
    <section className="card">
      <h2>Уведомления</h2>
      <div className="list">{items.map((n) => <article key={n.id} className={n.readStatus ? '' : 'unread'}><b>{n.title}</b><span>{n.message}</span><button className="ghost" onClick={() => api.post(`/notifications/${n.id}/read`).then(load)}>Прочитано</button></article>)}</div>
    </section>
  );
}
