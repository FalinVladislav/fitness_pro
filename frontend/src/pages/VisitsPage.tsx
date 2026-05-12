import { FormEvent, useEffect, useState } from 'react';
import { api, errorMessage } from '../shared/api';
import { useAuth } from '../shared/auth';
import { Client } from '../shared/types';

export function VisitsPage() {
  const { user } = useAuth();
  const [clients, setClients] = useState<Client[]>([]);
  const [visits, setVisits] = useState<any[]>([]);
  const [form, setForm] = useState({ clientId: '', scheduleId: '' });
  const [message, setMessage] = useState('');
  const load = () => {
    if (user?.role === 'CLIENT') {
      return api.get('/visits/my').then((v) => setVisits(v.data));
    }
    return Promise.all([api.get('/clients'), api.get('/visits')]).then(([c, v]) => { setClients(c.data); setVisits(v.data); });
  };
  useEffect(() => { load(); }, []);

  async function checkIn(e: FormEvent) {
    e.preventDefault();
    try {
      await api.post('/visits/check-in', { clientId: Number(form.clientId), scheduleId: form.scheduleId ? Number(form.scheduleId) : null });
      setMessage('Проход разрешен, посещение зафиксировано');
      load();
    } catch (err) { setMessage(errorMessage(err)); }
  }

  return (
    <section className="grid2">
      {user?.role !== 'CLIENT' && <form className="card" onSubmit={checkIn}>
        <h2>Отметка посещения</h2>
        <select value={form.clientId} onChange={(e) => setForm({ ...form, clientId: e.target.value })}><option value="">Клиент</option>{clients.map((c) => <option key={c.id} value={c.id}>{c.user.fullName}</option>)}</select>
        <input value={form.scheduleId} onChange={(e) => setForm({ ...form, scheduleId: e.target.value })} placeholder="ID занятия, если групповая тренировка" />
        {message && <div className="notice">{message}</div>}
        <button>Отметить проход</button>
      </form>}
      <div className="card">
        <h2>История посещений</h2>
        <div className="list">{visits.map((v) => <article key={v.id}><b>{v.clientName}</b><span>{v.visitTime} · {v.visitType} · абонемент #{v.membershipId}</span></article>)}</div>
      </div>
    </section>
  );
}
