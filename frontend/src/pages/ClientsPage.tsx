import { FormEvent, useEffect, useState } from 'react';
import { api, errorMessage } from '../shared/api';
import { Client } from '../shared/types';

export function ClientsPage() {
  const [clients, setClients] = useState<Client[]>([]);
  const [q, setQ] = useState('');
  const [message, setMessage] = useState('');
  const [form, setForm] = useState({ fullName: '', email: '', phone: '', password: 'client123', rfidCard: '', birthDate: '' });

  const load = () => api.get(q ? `/clients/search?q=${encodeURIComponent(q)}` : '/clients').then((r) => setClients(r.data));
  useEffect(() => { load(); }, []);

  async function submit(e: FormEvent) {
    e.preventDefault();
    try {
      await api.post('/clients', form);
      setMessage('Клиент создан');
      setForm({ fullName: '', email: '', phone: '', password: 'client123', rfidCard: '', birthDate: '' });
      load();
    } catch (err) {
      setMessage(errorMessage(err));
    }
  }

  return (
    <section className="grid2">
      <div className="card">
        <h2>Клиенты</h2>
        <div className="inline">
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="ФИО, телефон, email или карта" />
          <button onClick={load}>Найти</button>
        </div>
        <div className="list">{clients.map((c) => <article key={c.id}><b>{c.user.fullName}</b><span>{c.user.phone} · {c.user.email} · карта {c.rfidCard ?? 'не задана'}</span></article>)}</div>
      </div>
      <form className="card" onSubmit={submit}>
        <h2>Новый клиент</h2>
        {Object.entries(form).map(([k, v]) => <input key={k} value={v} type={k === 'birthDate' ? 'date' : k === 'password' ? 'password' : 'text'} placeholder={k} onChange={(e) => setForm({ ...form, [k]: e.target.value })} />)}
        {message && <div className="notice">{message}</div>}
        <button>Зарегистрировать</button>
      </form>
    </section>
  );
}
