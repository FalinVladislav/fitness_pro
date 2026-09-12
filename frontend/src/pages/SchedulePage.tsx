import { FormEvent, useEffect, useState } from 'react';
import { api, errorMessage } from '../shared/api';
import { useAuth } from '../shared/auth';
import { Schedule } from '../shared/types';

export function SchedulePage() {
  const { user } = useAuth();
  const [items, setItems] = useState<Schedule[]>([]);
  const [bookings, setBookings] = useState<any[]>([]);
  const [refs, setRefs] = useState({ trainingTypes: [] as any[], halls: [] as any[], trainers: [] as any[] });
  const [form, setForm] = useState({ trainingTypeId: '', trainerId: '', hallId: '', date: '', startTime: '', endTime: '', participantLimit: '10', trainerComment: '' });
  const [filters, setFilters] = useState({ q: '', trainerId: '', trainingTypeId: '', status: 'PLANNED' });
  const [message, setMessage] = useState('');
  const canEdit = user?.role === 'ADMIN' || user?.role === 'TRAINER';
  const load = () => Promise.all([
    api.get(user?.role === 'TRAINER' ? '/schedule/my' : '/schedule'),
    user?.role === 'CLIENT' ? api.get('/bookings/my') : Promise.resolve({ data: [] }),
  ]).then(([s, b]) => {
    setItems(s.data);
    setBookings(b.data);
  });

  useEffect(() => {
    load();
    Promise.all([api.get('/training-types'), api.get('/halls'), api.get('/trainers')]).then(([t, h, tr]) => setRefs({ trainingTypes: t.data, halls: h.data, trainers: tr.data }));
  }, []);

  async function create(e: FormEvent) {
    e.preventDefault();
    try {
      await api.post('/schedule', {
        ...form,
        trainingTypeId: Number(form.trainingTypeId),
        trainerId: user?.role === 'TRAINER' ? null : Number(form.trainerId),
        hallId: Number(form.hallId),
        endTime: form.endTime || null,
        participantLimit: Number(form.participantLimit),
      });
      setMessage('Занятие создано');
      load();
    } catch (err) { setMessage(errorMessage(err)); }
  }

  async function book(id: number) {
    try {
      await api.post('/bookings', { scheduleId: id });
      setMessage('Запись подтверждена');
      load();
    } catch (err) { setMessage(errorMessage(err)); }
  }

  const bookingBySchedule = new Map(bookings.map((b) => [b.schedule.id, b]));
  const filtered = items.filter((s) => {
    const haystack = `${s.trainingTypeName} ${s.trainerName} ${s.hallName}`.toLowerCase();
    return (!filters.q || haystack.includes(filters.q.toLowerCase()))
      && (!filters.trainerId || s.trainerId === Number(filters.trainerId))
      && (!filters.trainingTypeId || s.trainingTypeId === Number(filters.trainingTypeId))
      && (!filters.status || s.status === filters.status);
  });

  return (
    <section className="grid2">
      <div className="card">
        <h2>Расписание</h2>
        {message && <div className="notice">{message}</div>}
        <div className="filters">
          <input value={filters.q} onChange={(e) => setFilters({ ...filters, q: e.target.value })} placeholder="Поиск по тренировке, тренеру или залу" />
          <select value={filters.trainingTypeId} onChange={(e) => setFilters({ ...filters, trainingTypeId: e.target.value })}><option value="">Все тренировки</option>{refs.trainingTypes.map((x) => <option key={x.id} value={x.id}>{x.name}</option>)}</select>
          <select value={filters.trainerId} onChange={(e) => setFilters({ ...filters, trainerId: e.target.value })}><option value="">Все тренеры</option>{refs.trainers.map((x) => <option key={x.id} value={x.id}>{x.user.fullName}</option>)}</select>
          <select value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })}><option value="">Любой статус</option><option value="PLANNED">Запланировано</option><option value="COMPLETED">Проведено</option><option value="CANCELLED">Отменено</option></select>
        </div>
        <div className="schedule">{filtered.map((s) => {
           const myBooking = bookingBySchedule.get(s.id);
           const now = new Date();
           const startsAt = new Date(`${s.date}T${s.startTime}`);
           const endsAt = new Date(`${s.date}T${s.endTime}`);
           const canBook = user?.role === 'CLIENT' && s.status === 'PLANNED' && startsAt > now
             && myBooking?.status !== 'ACTIVE' && s.bookedCount < s.participantLimit;
           const canComplete = canEdit && s.status === 'PLANNED' && endsAt <= now;
          return (
          <article key={s.id} style={{ borderColor: s.status === 'CANCELLED' ? '#ef4444' : '#f97316' }}>
            <b>{s.trainingTypeName}</b><span>{s.date} · {s.startTime}-{s.endTime} · {s.trainerName}</span>
            <span>{s.hallName}, мест {s.bookedCount}/{s.participantLimit}, статус {s.status}</span>
            {user?.role === 'CLIENT' && myBooking?.status === 'ACTIVE' && <span className="pill">Вы записаны</span>}
            {user?.role === 'CLIENT' && myBooking?.status === 'CANCELLED' && <span className="pill muted">Вы отменили запись, можно записаться снова</span>}
            {canBook && <button onClick={() => book(s.id)}>{myBooking?.status === 'CANCELLED' ? 'Записаться снова' : 'Записаться'}</button>}
            {canEdit && s.status !== 'COMPLETED' && s.status !== 'CANCELLED' && (
              <button className="ghost" onClick={() => api.post(`/schedule/${s.id}/cancel`).then(load)}>Отменить</button>
            )}
            {canComplete && (
              <button type="button" onClick={() => api.post(`/schedule/${s.id}/complete`).then(load).catch((err) => setMessage(errorMessage(err)))}>Отметить как проведённое</button>
            )}
          </article>
        );})}</div>
      </div>
      {canEdit && <form className="card" onSubmit={create}>
        <h2>Создать занятие</h2>
        <select value={form.trainingTypeId} onChange={(e) => setForm({ ...form, trainingTypeId: e.target.value })}><option value="">Тип</option>{refs.trainingTypes.map((x) => <option key={x.id} value={x.id}>{x.name}</option>)}</select>
        {user?.role === 'ADMIN' && <select value={form.trainerId} onChange={(e) => setForm({ ...form, trainerId: e.target.value })}><option value="">Тренер</option>{refs.trainers.map((x) => <option key={x.id} value={x.id}>{x.user.fullName}</option>)}</select>}
        <select value={form.hallId} onChange={(e) => setForm({ ...form, hallId: e.target.value })}><option value="">Зал</option>{refs.halls.map((x) => <option key={x.id} value={x.id}>{x.name}</option>)}</select>
        <input type="date" value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} />
        <input type="time" value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} />
        <input type="time" value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} />
        <input value={form.participantLimit} onChange={(e) => setForm({ ...form, participantLimit: e.target.value })} placeholder="Лимит" />
        <textarea value={form.trainerComment} onChange={(e) => setForm({ ...form, trainerComment: e.target.value })} placeholder="Комментарий тренера" />
        <button>Создать</button>
      </form>}
    </section>
  );
}
