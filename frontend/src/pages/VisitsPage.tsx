import { FormEvent, useEffect, useState } from 'react';
import { api, errorMessage } from '../shared/api';
import { useAuth } from '../shared/auth';
import { Client, Schedule } from '../shared/types';

type VisitMode = 'GYM' | 'ONE_TIME';

export function VisitsPage() {
  const { user } = useAuth();
  const [clients, setClients] = useState<Client[]>([]);
  const [schedules, setSchedules] = useState<Schedule[]>([]);
  const [bookings, setBookings] = useState<any[]>([]);
  const [visits, setVisits] = useState<any[]>([]);
  const [clientId, setClientId] = useState('');
  const [scheduleId, setScheduleId] = useState('');
  const [mode, setMode] = useState<VisitMode>('GYM');
  const [filters, setFilters] = useState({ q: '', type: '' });
  const [message, setMessage] = useState('');

  const load = () => {
    if (user?.role === 'CLIENT') {
      return api.get('/visits/my').then((v) => setVisits(v.data));
    }
    if (user?.role === 'TRAINER') {
      return Promise.all([api.get('/schedule/my'), api.get('/visits/trainer/my')]).then(([s, v]) => {
        setSchedules(s.data);
        setVisits(v.data);
      });
    }
    return Promise.all([api.get('/clients'), api.get('/visits')]).then(([c, v]) => {
      setClients(c.data);
      setVisits(v.data);
    });
  };

  useEffect(() => { load(); }, []);

  useEffect(() => {
    if (user?.role === 'TRAINER' && scheduleId) {
      api.get(`/bookings/schedule/${scheduleId}`).then((r) => setBookings(r.data));
    } else {
      setBookings([]);
    }
  }, [scheduleId, user?.role]);

  async function adminCheckIn(e: FormEvent) {
    e.preventDefault();
    try {
      await api.post('/visits/check-in', { clientId: Number(clientId), scheduleId: null, visitType: mode });
      setMessage(mode === 'ONE_TIME' ? 'Разовый визит зафиксирован' : 'Проход в зал разрешен, посещение зафиксировано');
      setClientId('');
      load();
    } catch (err) {
      setMessage(errorMessage(err));
    }
  }

  async function trainerCheckIn(booking: any) {
    try {
      await api.post('/visits/check-in', { clientId: booking.clientId, scheduleId: Number(scheduleId), visitType: 'GROUP_TRAINING' });
      setMessage(`${booking.clientName}: посещение тренировки зафиксировано`);
      const [b, v] = await Promise.all([api.get(`/bookings/schedule/${scheduleId}`), api.get('/visits/trainer/my')]);
      setBookings(b.data);
      setVisits(v.data);
    } catch (err) {
      setMessage(errorMessage(err));
    }
  }

  async function cancelVisit(id: number) {
    try {
      await api.post(`/visits/${id}/cancel`);
      setMessage('Посещение отменено');
      load();
    } catch (err) {
      setMessage(errorMessage(err));
    }
  }

  const filteredVisits = visits.filter((v) =>
    (!filters.q || `${v.clientName} ${v.membershipId ?? ''} ${v.scheduleId ?? ''}`.toLowerCase().includes(filters.q.toLowerCase()))
    && (!filters.type || v.visitType === filters.type)
  );
  const selectedSchedule = schedules.find((schedule) => schedule.id === Number(scheduleId));
  const canCheckInSelectedSchedule = selectedSchedule?.status === 'PLANNED'
    && new Date(`${selectedSchedule.date}T${selectedSchedule.startTime}`) <= new Date()
    && new Date() < new Date(`${selectedSchedule.date}T${selectedSchedule.endTime}`);

  return (
    <section className="grid2">
      {user?.role === 'ADMIN' && (
        <form className="card" onSubmit={adminCheckIn}>
          <h2>Проход в зал</h2>
          <p>Администратор отмечает только посещение зала. Групповые тренировки фиксирует тренер.</p>
          <select value={clientId} onChange={(e) => setClientId(e.target.value)}>
            <option value="">Клиент</option>
            {clients.map((c) => <option key={c.id} value={c.id}>{c.user.fullName}</option>)}
          </select>
          <label>
            <input type="radio" name="mode" checked={mode === 'GYM'} onChange={() => setMode('GYM')} />
            По абонементу
          </label>
          <label>
            <input type="radio" name="mode" checked={mode === 'ONE_TIME'} onChange={() => setMode('ONE_TIME')} />
            Разовое посещение (без абонемента)
          </label>
          {message && <div className="notice">{message}</div>}
          <button>Отметить проход</button>
        </form>
      )}

      {user?.role === 'TRAINER' && (
        <div className="card">
          <h2>Отметка участников</h2>
          <select value={scheduleId} onChange={(e) => setScheduleId(e.target.value)}>
            <option value="">Выберите свое занятие</option>
            {schedules.map((s) => <option key={s.id} value={s.id}>{s.date} {s.startTime} · {s.trainingTypeName}</option>)}
          </select>
          {message && <div className="notice">{message}</div>}
          <div className="list">
            {bookings.filter((b) => b.status === 'ACTIVE').map((b) => (
              <article key={b.id}>
                <b>{b.clientName}</b>
                <span>{b.schedule.trainingTypeName} · запись активна</span>
                <button type="button" disabled={!canCheckInSelectedSchedule} onClick={() => trainerCheckIn(b)}>Отметить пришедшим</button>
              </article>
            ))}
            {scheduleId && bookings.filter((b) => b.status === 'ACTIVE').length === 0 && <article><span>Активных записей на это занятие нет.</span></article>}
          </div>
        </div>
      )}

      <div className="card">
        <h2>История посещений</h2>
        {user?.role === 'ADMIN' && (
          <div className="filters">
            <input value={filters.q} onChange={(e) => setFilters({ ...filters, q: e.target.value })} placeholder="Клиент, абонемент или занятие" />
            <select value={filters.type} onChange={(e) => setFilters({ ...filters, type: e.target.value })}><option value="">Любой тип</option><option value="GYM">Зал</option><option value="GROUP_TRAINING">Тренировка</option><option value="ONE_TIME">Разовое</option></select>
          </div>
        )}
        <div className="list">
          {filteredVisits.map((v) => (
            <article key={v.id}>
              <b>{v.clientName}</b>
              <span>{v.visitTime} · {v.visitType} · {v.membershipId ? `абонемент #${v.membershipId}` : 'без абонемента'}</span>
              {user?.role === 'ADMIN' && <button type="button" className="danger" onClick={() => cancelVisit(v.id)}>Отменить посещение</button>}
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
