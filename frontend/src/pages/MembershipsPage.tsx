import { FormEvent, useEffect, useState } from 'react';
import { api, errorMessage } from '../shared/api';
import { useAuth } from '../shared/auth';
import { Client, MembershipType } from '../shared/types';

export function MembershipsPage() {
  const { user } = useAuth();
  const [clients, setClients] = useState<Client[]>([]);
  const [types, setTypes] = useState<MembershipType[]>([]);
  const [memberships, setMemberships] = useState<any[]>([]);
  const [form, setForm] = useState({ clientId: '', membershipTypeId: '', activationDate: '', paymentMethod: 'CARD' });
  const [message, setMessage] = useState('');

  const load = () => {
    if (user?.role === 'CLIENT') {
      return Promise.all([api.get('/membership-types'), api.get('/memberships/my')]).then(([t, m]) => { setTypes(t.data); setMemberships(m.data); });
    }
    return Promise.all([api.get('/clients'), api.get('/membership-types'), api.get('/memberships')]).then(([c, t, m]) => { setClients(c.data); setTypes(t.data); setMemberships(m.data); });
  };
  useEffect(() => { load(); }, []);

  async function sell(e: FormEvent) {
    e.preventDefault();
    try {
      await api.post('/memberships/sell', { ...form, clientId: Number(form.clientId), membershipTypeId: Number(form.membershipTypeId), activationDate: form.activationDate || null });
      setMessage('Абонемент продан');
      load();
    } catch (err) { setMessage(errorMessage(err)); }
  }

  return (
    <section className="grid2">
      {user?.role !== 'CLIENT' && <form className="card" onSubmit={sell}>
        <h2>Продажа абонемента</h2>
        <select value={form.clientId} onChange={(e) => setForm({ ...form, clientId: e.target.value })}><option value="">Клиент</option>{clients.map((c) => <option key={c.id} value={c.id}>{c.user.fullName}</option>)}</select>
        <select value={form.membershipTypeId} onChange={(e) => setForm({ ...form, membershipTypeId: e.target.value })}><option value="">Тип абонемента</option>{types.map((t) => <option key={t.id} value={t.id}>{t.name} · {t.price} ₽</option>)}</select>
        <input type="date" value={form.activationDate} onChange={(e) => setForm({ ...form, activationDate: e.target.value })} />
        {message && <div className="notice">{message}</div>}
        <button>Продать</button>
      </form>}
      <div className="card">
        <h2>{user?.role === 'CLIENT' ? 'Мой абонемент' : 'Абонементы'}</h2>
        {user?.role === 'CLIENT' && <p>Для продления выберите актуальный абонемент и обратитесь к администратору или используйте продление существующего.</p>}
        <div className="list">{memberships.map((m) => <article key={m.id}><b>{m.clientName}</b><span>{m.typeName}: {m.status}, до {m.expirationDate}, остаток {m.remainingVisits ?? 'безлимит'}</span></article>)}</div>
      </div>
    </section>
  );
}
