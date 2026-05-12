import { FormEvent, useEffect, useState } from 'react';
import { api, errorMessage } from '../shared/api';

export function CatalogPage() {
  const [data, setData] = useState({ membershipTypes: [] as any[], trainingTypes: [] as any[], halls: [] as any[], trainers: [] as any[] });
  const [message, setMessage] = useState('');
  const [membership, setMembership] = useState({ name: '', durationDays: '30', visitCount: '', price: '', description: '', active: true });
  const [training, setTraining] = useState({ name: '', durationMinutes: '60', description: '', color: '#f97316' });
  const [hall, setHall] = useState({ name: '', capacity: '15', description: '' });
  const load = () => Promise.all([api.get('/membership-types'), api.get('/training-types'), api.get('/halls'), api.get('/trainers')]).then(([m, t, h, tr]) => setData({ membershipTypes: m.data, trainingTypes: t.data, halls: h.data, trainers: tr.data }));
  useEffect(() => { load(); }, []);

  async function save(e: FormEvent, kind: 'membership' | 'training' | 'hall') {
    e.preventDefault();
    try {
      if (kind === 'membership') await api.post('/membership-types', { ...membership, durationDays: Number(membership.durationDays), visitCount: membership.visitCount ? Number(membership.visitCount) : null, price: Number(membership.price) });
      if (kind === 'training') await api.post('/training-types', { ...training, durationMinutes: Number(training.durationMinutes) });
      if (kind === 'hall') await api.post('/halls', { ...hall, capacity: Number(hall.capacity) });
      setMessage('Справочник обновлен');
      load();
    } catch (err) { setMessage(errorMessage(err)); }
  }

  return (
    <section className="stack">
      {message && <div className="notice">{message}</div>}
      <div className="grid3">
        <form className="card" onSubmit={(e) => save(e, 'membership')}><h2>Тип абонемента</h2><input placeholder="Название" value={membership.name} onChange={(e) => setMembership({ ...membership, name: e.target.value })} /><input placeholder="Дней" value={membership.durationDays} onChange={(e) => setMembership({ ...membership, durationDays: e.target.value })} /><input placeholder="Посещений, пусто = безлимит" value={membership.visitCount} onChange={(e) => setMembership({ ...membership, visitCount: e.target.value })} /><input placeholder="Цена" value={membership.price} onChange={(e) => setMembership({ ...membership, price: e.target.value })} /><textarea placeholder="Описание" value={membership.description} onChange={(e) => setMembership({ ...membership, description: e.target.value })} /><button>Добавить</button></form>
        <form className="card" onSubmit={(e) => save(e, 'training')}><h2>Тип тренировки</h2><input placeholder="Название" value={training.name} onChange={(e) => setTraining({ ...training, name: e.target.value })} /><input placeholder="Минут" value={training.durationMinutes} onChange={(e) => setTraining({ ...training, durationMinutes: e.target.value })} /><input type="color" value={training.color} onChange={(e) => setTraining({ ...training, color: e.target.value })} /><textarea placeholder="Описание" value={training.description} onChange={(e) => setTraining({ ...training, description: e.target.value })} /><button>Добавить</button></form>
        <form className="card" onSubmit={(e) => save(e, 'hall')}><h2>Зал</h2><input placeholder="Название" value={hall.name} onChange={(e) => setHall({ ...hall, name: e.target.value })} /><input placeholder="Вместимость" value={hall.capacity} onChange={(e) => setHall({ ...hall, capacity: e.target.value })} /><textarea placeholder="Описание" value={hall.description} onChange={(e) => setHall({ ...hall, description: e.target.value })} /><button>Добавить</button></form>
      </div>
      <div className="grid3">
        <List title="Абонементы" rows={data.membershipTypes.map((x) => `${x.name}: ${x.price} ₽, ${x.visitCount ?? 'безлимит'}`)} />
        <List title="Тренировки" rows={data.trainingTypes.map((x) => `${x.name}: ${x.durationMinutes} мин`)} />
        <List title="Залы и тренеры" rows={[...data.halls.map((x) => `${x.name}: ${x.capacity} мест`), ...data.trainers.map((x) => `Тренер: ${x.user.fullName}`)]} />
      </div>
    </section>
  );
}

function List({ title, rows }: { title: string; rows: string[] }) {
  return <div className="card"><h2>{title}</h2><div className="list">{rows.map((r) => <article key={r}><span>{r}</span></article>)}</div></div>;
}
