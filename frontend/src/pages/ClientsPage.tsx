import { FormEvent, useEffect, useState } from 'react';
import { api, errorMessage } from '../shared/api';
import { Client } from '../shared/types';
import { useAuth } from '../shared/auth';

export function ClientsPage() {
  const { user } = useAuth();
  const [clients, setClients] = useState<Client[]>([]);
  const [q, setQ] = useState('');
  const [message, setMessage] = useState('');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [card, setCard] = useState<{ client: Client; memberships: any[]; visits: any[]; bookings: any[] } | null>(null);
  const [form, setForm] = useState({ fullName: '', email: '', phone: '', password: 'client123', rfidCard: '', birthDate: '' });

  const load = () => api.get(q ? `/clients/search?q=${encodeURIComponent(q)}` : '/clients').then((r) => setClients(r.data));
  useEffect(() => { load(); }, []);

  async function submit(e: FormEvent) {
    e.preventDefault();
    try {
      if (editingId) {
        await api.put(`/clients/${editingId}`, { ...form, password: form.password || null });
        setMessage('Данные клиента обновлены');
      } else {
        await api.post('/clients', form);
        setMessage('Клиент создан');
      }
      setEditingId(null);
      setForm({ fullName: '', email: '', phone: '', password: 'client123', rfidCard: '', birthDate: '' });
      load();
    } catch (err) {
      setMessage(errorMessage(err));
    }
  }

  function edit(c: Client) {
    setEditingId(c.id);
    setForm({ fullName: c.user.fullName, email: c.user.email, phone: c.user.phone, password: '', rfidCard: c.rfidCard ?? '', birthDate: c.birthDate ?? '' });
  }

  async function openCard(c: Client) {
    const [memberships, visits, bookings] = await Promise.all([
      api.get(`/memberships/client/${c.id}`),
      api.get(`/visits/client/${c.id}`),
      api.get('/bookings'),
    ]);
    setCard({ client: c, memberships: memberships.data, visits: visits.data, bookings: bookings.data.filter((b: any) => b.clientId === c.id) });
  }

  return (
    <section className="grid2">
      <div className="card">
        <h2>Клиенты</h2>
        <div className="inline">
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="ФИО, телефон, email или карта" />
          <button onClick={load}>Найти</button>
        </div>
        <div className="list">{clients.map((c) => <article key={c.id}><b>{c.user.fullName}</b><span>{c.user.phone} · {c.user.email} · карта {c.rfidCard ?? 'не задана'}</span><div className="inline-actions"><button type="button" className="ghost light" onClick={() => openCard(c)}>Карточка</button>{user?.role === 'ADMIN' && <button type="button" onClick={() => edit(c)}>Изменить</button>}</div></article>)}</div>
      </div>
      {user?.role === 'ADMIN' && <form className="card" onSubmit={submit}>
        <h2>{editingId ? 'Редактировать клиента' : 'Новый клиент'}</h2>
        {Object.entries(form).map(([k, v]) => <input key={k} value={v} type={k === 'birthDate' ? 'date' : k === 'password' ? 'password' : 'text'} placeholder={k} onChange={(e) => setForm({ ...form, [k]: e.target.value })} />)}
        {message && <div className="notice">{message}</div>}
        <div className="inline-actions"><button>{editingId ? 'Сохранить' : 'Зарегистрировать'}</button>{editingId && <button type="button" className="ghost light" onClick={() => { setEditingId(null); setForm({ fullName: '', email: '', phone: '', password: 'client123', rfidCard: '', birthDate: '' }); }}>Отмена</button>}</div>
      </form>}
      {card && <div className="card">
        <h2>Карточка клиента</h2>
        <b>{card.client.user.fullName}</b>
        <span>{card.client.user.phone} · {card.client.user.email}</span>
        <h3>Абонементы</h3>
        <div className="list">{card.memberships.map((m) => <article key={m.id}><span>{m.typeName} · {m.status} · до {m.expirationDate}</span></article>)}</div>
        <h3>Посещения</h3>
        <div className="list">{card.visits.map((v) => <article key={v.id}><span>{v.visitTime} · {v.visitType}</span></article>)}</div>
        <h3>Записи</h3>
        <div className="list">{card.bookings.map((b) => <article key={b.id}><span>{b.schedule.trainingTypeName} · {b.schedule.date} {b.schedule.startTime} · {b.status}</span></article>)}</div>
      </div>}
    </section>
  );
}
