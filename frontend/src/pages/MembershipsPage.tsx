import { FormEvent, useEffect, useState } from 'react';
import { api, errorMessage } from '../shared/api';
import { useAuth } from '../shared/auth';
import { Client, Membership, MembershipPurchaseRequest, MembershipType } from '../shared/types';

export function MembershipsPage() {
  const { user } = useAuth();
  const [clients, setClients] = useState<Client[]>([]);
  const [types, setTypes] = useState<MembershipType[]>([]);
  const [memberships, setMemberships] = useState<Membership[]>([]);
  const [requests, setRequests] = useState<MembershipPurchaseRequest[]>([]);
  const [form, setForm] = useState({ clientId: '', membershipTypeId: '', activationDate: '', paymentMethod: 'CARD' });
  const [purchase, setPurchase] = useState({ membershipTypeId: '', desiredActivationDate: '', comment: '' });
  const [freezeForm, setFreezeForm] = useState<{ membershipId: number | null; startDate: string; endDate: string; reason: string }>({ membershipId: null, startDate: '', endDate: '', reason: '' });
  const [filters, setFilters] = useState({ q: '', status: '', typeId: '' });
  const [message, setMessage] = useState('');

  const load = () => {
    if (user?.role === 'CLIENT') {
      return Promise.all([
        api.get('/membership-types'),
        api.get('/memberships/my'),
        api.get('/membership-requests/my'),
      ]).then(([t, m, r]) => {
        setTypes(t.data);
        setMemberships(m.data);
        setRequests(r.data);
      });
    }
    if (user?.role === 'ADMIN') {
      return Promise.all([
        api.get('/clients'),
        api.get('/membership-types'),
        api.get('/memberships'),
        api.get('/membership-requests'),
      ]).then(([c, t, m, r]) => {
        setClients(c.data);
        setTypes(t.data);
        setMemberships(m.data);
        setRequests(r.data);
      });
    }
    return Promise.all([
      api.get('/clients'),
      api.get('/membership-types'),
      api.get('/memberships'),
    ]).then(([c, t, m]) => {
      setClients(c.data);
      setTypes(t.data);
      setMemberships(m.data);
    });
  };
  useEffect(() => { load(); }, []);

  async function sell(e: FormEvent) {
    e.preventDefault();
    try {
      await api.post('/memberships/sell', { ...form, clientId: Number(form.clientId), membershipTypeId: Number(form.membershipTypeId), activationDate: form.activationDate || null });
      setMessage('Абонемент продан');
      setForm({ clientId: '', membershipTypeId: '', activationDate: '', paymentMethod: 'CARD' });
      load();
    } catch (err) { setMessage(errorMessage(err)); }
  }

  async function submitPurchase(e: FormEvent) {
    e.preventDefault();
    try {
      await api.post('/memberships/buy', {
        membershipTypeId: Number(purchase.membershipTypeId),
        desiredActivationDate: purchase.desiredActivationDate || null,
        comment: purchase.comment || null,
      });
      setMessage('Абонемент куплен и активирован');
      setPurchase({ membershipTypeId: '', desiredActivationDate: '', comment: '' });
      load();
    } catch (err) { setMessage(errorMessage(err)); }
  }

  async function cancelRequest(id: number) {
    try {
      await api.post(`/membership-requests/${id}/cancel`);
      setMessage('Заявка отменена');
      load();
    } catch (err) { setMessage(errorMessage(err)); }
  }

  async function approveRequest(id: number) {
    try {
      await api.post(`/membership-requests/${id}/approve`, {});
      setMessage('Заявка одобрена, абонемент активирован');
      load();
    } catch (err) { setMessage(errorMessage(err)); }
  }

  async function rejectRequest(id: number) {
    const reason = window.prompt('Причина отклонения (необязательно):') ?? '';
    try {
      await api.post(`/membership-requests/${id}/reject`, { comment: reason });
      setMessage('Заявка отклонена');
      load();
    } catch (err) { setMessage(errorMessage(err)); }
  }

  async function renew(id: number) {
    try {
      await api.post(`/memberships/${id}/renew`);
      setMessage('Абонемент продлен');
      load();
    } catch (err) { setMessage(errorMessage(err)); }
  }

  async function unfreeze(id: number) {
    try {
      await api.post(`/memberships/${id}/unfreeze`);
      setMessage('Абонемент разморожен');
      load();
    } catch (err) { setMessage(errorMessage(err)); }
  }

  async function cancelMembership(id: number) {
    if (!window.confirm('Отменить абонемент?')) return;
    try {
      await api.post(`/memberships/${id}/cancel`);
      setMessage('Абонемент отменен');
      load();
    } catch (err) { setMessage(errorMessage(err)); }
  }

  async function submitFreeze(e: FormEvent) {
    e.preventDefault();
    if (!freezeForm.membershipId) return;
    try {
      await api.post(`/memberships/${freezeForm.membershipId}/freeze`, {
        startDate: freezeForm.startDate,
        endDate: freezeForm.endDate,
        reason: freezeForm.reason,
      });
      setMessage('Абонемент заморожен');
      setFreezeForm({ membershipId: null, startDate: '', endDate: '', reason: '' });
      load();
    } catch (err) { setMessage(errorMessage(err)); }
  }

  const myActiveTypes = new Set(memberships.filter((m) => m.status === 'ACTIVE' || m.status === 'FROZEN').map((m) => m.membershipTypeId));
  const myPendingTypes = new Set(requests.filter((r) => r.status === 'PENDING').map((r) => r.membershipTypeId));
  const pendingRequests = requests.filter((r) => r.status === 'PENDING');
  const filteredMemberships = memberships.filter((m) =>
    (!filters.q || `${m.clientName} ${m.typeName}`.toLowerCase().includes(filters.q.toLowerCase()))
    && (!filters.status || m.status === filters.status)
    && (!filters.typeId || m.membershipTypeId === Number(filters.typeId))
  );

  return (
    <section className="grid2">
      {user?.role === 'ADMIN' && (
        <form className="card" onSubmit={sell}>
          <h2>Продажа абонемента</h2>
          <select value={form.clientId} onChange={(e) => setForm({ ...form, clientId: e.target.value })}>
            <option value="">Клиент</option>
            {clients.map((c) => <option key={c.id} value={c.id}>{c.user.fullName}</option>)}
          </select>
          <select value={form.membershipTypeId} onChange={(e) => setForm({ ...form, membershipTypeId: e.target.value })}>
            <option value="">Тип абонемента</option>
            {types.map((t) => <option key={t.id} value={t.id}>{t.name} · {t.price} ₽{t.freezeAllowed ? ` · заморозка до ${t.maxFreezeDays} дн.` : ''}</option>)}
          </select>
          <input type="date" value={form.activationDate} onChange={(e) => setForm({ ...form, activationDate: e.target.value })} />
          {message && <div className="notice">{message}</div>}
          <button>Продать</button>
        </form>
      )}

      {user?.role === 'CLIENT' && (
        <form className="card" onSubmit={submitPurchase}>
          <h2>Купить абонемент</h2>
          <p>Выберите тариф и дату активации. Абонемент оформится сразу, без ожидания администратора.</p>
          <select value={purchase.membershipTypeId} onChange={(e) => setPurchase({ ...purchase, membershipTypeId: e.target.value })} required>
            <option value="">Тип абонемента</option>
            {types.filter((t) => t.active).map((t) => (
              <option key={t.id} value={t.id} disabled={myActiveTypes.has(t.id) || myPendingTypes.has(t.id)}>
                {t.name} · {t.price} ₽ · {t.durationDays} дн.{t.visitCount ? ` · ${t.visitCount} посещений` : ' · безлимит'}
                {myActiveTypes.has(t.id) ? ' (уже активен)' : myPendingTypes.has(t.id) ? ' (заявка на рассмотрении)' : ''}
              </option>
            ))}
          </select>
          <label>Желаемая дата активации
            <input type="date" value={purchase.desiredActivationDate} onChange={(e) => setPurchase({ ...purchase, desiredActivationDate: e.target.value })} />
          </label>
          <textarea placeholder="Комментарий администратору (необязательно)" value={purchase.comment} onChange={(e) => setPurchase({ ...purchase, comment: e.target.value })} />
          {message && <div className="notice">{message}</div>}
          <button>Купить онлайн</button>
        </form>
      )}

      <div className="card">
        <h2>{user?.role === 'CLIENT' ? 'Мои абонементы' : 'Абонементы'}</h2>
        {message && user?.role !== 'CLIENT' && user?.role !== 'ADMIN' && <div className="notice">{message}</div>}
        {user?.role !== 'CLIENT' && (
          <div className="filters">
            <input value={filters.q} onChange={(e) => setFilters({ ...filters, q: e.target.value })} placeholder="Клиент или абонемент" />
            <select value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })}><option value="">Любой статус</option><option value="ACTIVE">Активен</option><option value="FROZEN">Заморожен</option><option value="EXPIRED">Истек</option><option value="DEPLETED">Исчерпан</option><option value="CANCELLED">Отменен</option></select>
            <select value={filters.typeId} onChange={(e) => setFilters({ ...filters, typeId: e.target.value })}><option value="">Все типы</option>{types.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}</select>
          </div>
        )}
        <div className="list">
          {filteredMemberships.length === 0 && <article><span>Абонементов пока нет.</span></article>}
          {filteredMemberships.map((m) => (
            <article key={m.id}>
              <b>{m.clientName}</b>
              <span>{m.typeName}: {m.status}, до {m.expirationDate}, остаток {m.remainingVisits ?? 'безлимит'}</span>
              {m.freezeAllowed && (
                <span>Заморозка: использовано {m.usedFreezeDays}/{m.maxFreezeDays} дн.</span>
              )}
              <div className="inline-actions">
                {(m.status === 'ACTIVE' || m.status === 'EXPIRED' || m.status === 'DEPLETED') && (
                  <button type="button" onClick={() => renew(m.id)}>Продлить</button>
                )}
                {m.freezeAllowed && m.status === 'ACTIVE' && (
                  <button type="button" className="ghost light"
                    onClick={() => setFreezeForm({ membershipId: m.id, startDate: '', endDate: '', reason: '' })}>
                    Заморозить
                  </button>
                )}
                {m.status === 'FROZEN' && (
                  <button type="button" className="ghost light" onClick={() => unfreeze(m.id)}>Разморозить</button>
                )}
                {user?.role === 'ADMIN' && m.status !== 'CANCELLED' && (
                  <button type="button" className="danger" onClick={() => cancelMembership(m.id)}>Отменить</button>
                )}
              </div>
            </article>
          ))}
        </div>
      </div>

      {user?.role === 'CLIENT' && (
        <div className="card">
          <h2>Мои заявки</h2>
          <div className="list">
            {requests.length === 0 && <article><span>Заявок ещё нет.</span></article>}
            {requests.map((r) => (
              <article key={r.id}>
                <b>{r.membershipTypeName}</b>
                <span>{r.price} ₽ · подано {new Date(r.createdAt).toLocaleString()}</span>
                <span>Статус: {r.status}{r.decisionComment ? ` — ${r.decisionComment}` : ''}</span>
                {r.desiredActivationDate && <span>Желаемая дата активации: {r.desiredActivationDate}</span>}
                {r.status === 'PENDING' && (
                  <div className="inline-actions">
                    <button type="button" className="ghost light" onClick={() => cancelRequest(r.id)}>Отозвать</button>
                  </div>
                )}
              </article>
            ))}
          </div>
        </div>
      )}

      {user?.role === 'ADMIN' && (
        <div className="card">
          <h2>Заявки от клиентов</h2>
          {pendingRequests.length === 0 && <p>Новых заявок нет.</p>}
          <div className="list">
            {pendingRequests.map((r) => (
              <article key={r.id}>
                <b>{r.clientName}</b>
                <span>{r.membershipTypeName} · {r.price} ₽</span>
                <span>Заявка от {new Date(r.createdAt).toLocaleString()}{r.desiredActivationDate ? ` · с ${r.desiredActivationDate}` : ''}</span>
                {r.comment && <span>Комментарий клиента: {r.comment}</span>}
                <div className="inline-actions">
                  <button type="button" onClick={() => approveRequest(r.id)}>Одобрить (без оплаты)</button>
                  <button type="button" className="danger" onClick={() => rejectRequest(r.id)}>Отклонить</button>
                </div>
              </article>
            ))}
          </div>
          <details>
            <summary>История заявок</summary>
            <div className="list" style={{ marginTop: 12 }}>
              {requests.filter((r) => r.status !== 'PENDING').map((r) => (
                <article key={r.id}>
                  <b>{r.clientName}</b>
                  <span>{r.membershipTypeName} · {r.status}{r.decisionBy ? ` (${r.decisionBy})` : ''}</span>
                  {r.decisionComment && <span>{r.decisionComment}</span>}
                </article>
              ))}
            </div>
          </details>
        </div>
      )}

      {freezeForm.membershipId !== null && (
        <form className="card" onSubmit={submitFreeze}>
          <h2>Заморозка абонемента #{freezeForm.membershipId}</h2>
          <label>С
            <input type="date" value={freezeForm.startDate} onChange={(e) => setFreezeForm({ ...freezeForm, startDate: e.target.value })} required />
          </label>
          <label>По
            <input type="date" value={freezeForm.endDate} onChange={(e) => setFreezeForm({ ...freezeForm, endDate: e.target.value })} required />
          </label>
          <input placeholder="Причина" value={freezeForm.reason} onChange={(e) => setFreezeForm({ ...freezeForm, reason: e.target.value })} />
          <div className="inline-actions">
            <button>Заморозить</button>
            <button type="button" className="ghost light" onClick={() => setFreezeForm({ membershipId: null, startDate: '', endDate: '', reason: '' })}>Отмена</button>
          </div>
        </form>
      )}
    </section>
  );
}
