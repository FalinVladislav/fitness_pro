import { FormEvent, useEffect, useState } from 'react';
import { api, errorMessage } from '../shared/api';
import { useAuth } from '../shared/auth';
import { Schedule } from '../shared/types';

export function SchedulePage() {
  const { user } = useAuth();
  const [items, setItems] = useState<Schedule[]>([]);
  const [refs, setRefs] = useState({ trainingTypes: [] as any[], halls: [] as any[], trainers: [] as any[] });
  const [form, setForm] = useState({ trainingTypeId: '', trainerId: '', hallId: '', date: '', startTime: '', participantLimit: '10', trainerComment: '' });
  const [message, setMessage] = useState('');
  const canEdit = user?.role === 'ADMIN' || user?.role === 'TRAINER';
  const load = () => api.get(user?.role === 'TRAINER' ? '/schedule/my' : '/schedule').then((r) => setItems(r.data));

  useEffect(() => {
    load();
    Promise.all([api.get('/training-types'), api.get('/halls'), api.get('/trainers')]).then(([t, h, tr]) => setRefs({ trainingTypes: t.data, halls: h.data, trainers: tr.data }));
  }, []);

  async function create(e: FormEvent) {
    e.preventDefault();
    try {
      await api.post('/schedule', { ...form, trainingTypeId: Number(form.trainingTypeId), trainerId: Number(form.trainerId), hallId: Number(form.hallId), participantLimit: Number(form.participantLimit) });
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

  return (
    <section className="grid2">
      <div className="card">
        <h2>Расписание</h2>
        {message && <div className="notice">{message}</div>}
        <div className="schedule">{items.map((s) => (
          <article key={s.id} style={{ borderColor: s.status === 'CANCELLED' ? '#ef4444' : '#f97316' }}>
            <b>{s.trainingTypeName}</b><span>{s.date} · {s.startTime}-{s.endTime} · {s.trainerName}</span>
            <span>{s.hallName}, мест {s.bookedCount}/{s.participantLimit}, статус {s.status}</span>
            {user?.role === 'CLIENT' && <button onClick={() => book(s.id)}>Записаться</button>}
            {canEdit && <button className="ghost" onClick={() => api.post(`/schedule/${s.id}/cancel`).then(load)}>Отменить</button>}
          </article>
        ))}</div>
      </div>
      {canEdit && <form className="card" onSubmit={create}>
        <h2>Создать занятие</h2>
        <select value={form.trainingTypeId} onChange={(e) => setForm({ ...form, trainingTypeId: e.target.value })}><option value="">Тип</option>{refs.trainingTypes.map((x) => <option key={x.id} value={x.id}>{x.name}</option>)}</select>
        <select value={form.trainerId} onChange={(e) => setForm({ ...form, trainerId: e.target.value })}><option value="">Тренер</option>{refs.trainers.map((x) => <option key={x.id} value={x.id}>{x.user.fullName}</option>)}</select>
        <select value={form.hallId} onChange={(e) => setForm({ ...form, hallId: e.target.value })}><option value="">Зал</option>{refs.halls.map((x) => <option key={x.id} value={x.id}>{x.name}</option>)}</select>
        <input type="date" value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} />
        <input type="time" value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} />
        <input value={form.participantLimit} onChange={(e) => setForm({ ...form, participantLimit: e.target.value })} placeholder="Лимит" />
        <textarea value={form.trainerComment} onChange={(e) => setForm({ ...form, trainerComment: e.target.value })} placeholder="Комментарий тренера" />
        <button>Создать</button>
      </form>}
    </section>
  );
}
