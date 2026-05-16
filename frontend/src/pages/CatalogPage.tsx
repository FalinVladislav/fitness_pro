import { FormEvent, useEffect, useState } from 'react';
import { api, errorMessage } from '../shared/api';

type CatalogKind = 'membership' | 'training' | 'hall' | 'trainer';

const emptyMembership = { name: '', durationDays: '30', visitCount: '', price: '', description: '', active: true, freezeAllowed: false, maxFreezeDays: '0' };
const emptyTraining = { name: '', durationMinutes: '60', description: '', color: '#f97316' };
const emptyHall = { name: '', capacity: '15', description: '' };
const emptyTrainer = { fullName: '', email: '', phone: '', password: 'trainer123', specialization: '', yearsOfExperience: '0', description: '' };

export function CatalogPage() {
  const [data, setData] = useState({ membershipTypes: [] as any[], trainingTypes: [] as any[], halls: [] as any[], trainers: [] as any[] });
  const [message, setMessage] = useState('');
  const [editing, setEditing] = useState<{ kind: CatalogKind; id: number } | null>(null);
  const [membership, setMembership] = useState(emptyMembership);
  const [training, setTraining] = useState(emptyTraining);
  const [hall, setHall] = useState(emptyHall);
  const [trainer, setTrainer] = useState(emptyTrainer);
  const [search, setSearch] = useState({ membership: '', training: '', hall: '', trainer: '' });

  const load = () => Promise.all([api.get('/membership-types'), api.get('/training-types'), api.get('/halls'), api.get('/trainers')])
    .then(([m, t, h, tr]) => setData({ membershipTypes: m.data, trainingTypes: t.data, halls: h.data, trainers: tr.data }));

  useEffect(() => { load(); }, []);

  async function save(e: FormEvent, kind: CatalogKind) {
    e.preventDefault();
    try {
      const id = editing?.kind === kind ? editing.id : null;
      if (kind === 'membership') {
        await api[id ? 'put' : 'post'](id ? `/membership-types/${id}` : '/membership-types', {
          ...membership,
          durationDays: Number(membership.durationDays),
          visitCount: membership.visitCount ? Number(membership.visitCount) : null,
          price: Number(membership.price),
          maxFreezeDays: Number(membership.maxFreezeDays || 0),
        });
        setMembership(emptyMembership);
      }
      if (kind === 'training') {
        await api[id ? 'put' : 'post'](id ? `/training-types/${id}` : '/training-types', { ...training, durationMinutes: Number(training.durationMinutes) });
        setTraining(emptyTraining);
      }
      if (kind === 'hall') {
        await api[id ? 'put' : 'post'](id ? `/halls/${id}` : '/halls', { ...hall, capacity: Number(hall.capacity) });
        setHall(emptyHall);
      }
      if (kind === 'trainer') {
        await api[id ? 'put' : 'post'](id ? `/trainers/${id}` : '/trainers', {
          ...trainer,
          yearsOfExperience: Number(trainer.yearsOfExperience || 0),
        });
        setTrainer(emptyTrainer);
      }
      setEditing(null);
      setMessage(id ? 'Запись обновлена' : 'Запись добавлена');
      load();
    } catch (err) {
      setMessage(errorMessage(err));
    }
  }

  async function remove(kind: CatalogKind, id: number) {
    const baseUrl = { membership: '/membership-types', training: '/training-types', hall: '/halls', trainer: '/trainers' }[kind];
    try {
      await api.delete(`${baseUrl}/${id}`);
      setMessage('Запись удалена');
      load();
    } catch (err) {
      setMessage(errorMessage(err));
    }
  }

  function edit(kind: CatalogKind, item: any) {
    setEditing({ kind, id: item.id });
    if (kind === 'membership') setMembership({ name: item.name, durationDays: String(item.durationDays), visitCount: item.visitCount ? String(item.visitCount) : '', price: String(item.price), description: item.description ?? '', active: item.active, freezeAllowed: !!item.freezeAllowed, maxFreezeDays: String(item.maxFreezeDays ?? 0) });
    if (kind === 'training') setTraining({ name: item.name, durationMinutes: String(item.durationMinutes), description: item.description ?? '', color: item.color ?? '#f97316' });
    if (kind === 'hall') setHall({ name: item.name, capacity: String(item.capacity), description: item.description ?? '' });
    if (kind === 'trainer') setTrainer({ fullName: item.user.fullName, email: item.user.email, phone: item.user.phone, password: '', specialization: item.specialization ?? '', yearsOfExperience: String(item.yearsOfExperience ?? 0), description: item.description ?? '' });
  }

  function cancelEdit() {
    setEditing(null);
    setMembership(emptyMembership);
    setTraining(emptyTraining);
    setHall(emptyHall);
    setTrainer(emptyTrainer);
  }

  return (
    <section className="stack">
      {message && <div className="notice">{message}</div>}
      {editing && <button className="ghost light" type="button" onClick={cancelEdit}>Отменить редактирование</button>}
      <div className="grid3">
        <form className="card" onSubmit={(e) => save(e, 'membership')}>
          <h2>{editing?.kind === 'membership' ? 'Изменить абонемент' : 'Тип абонемента'}</h2>
          <input placeholder="Название" value={membership.name} onChange={(e) => setMembership({ ...membership, name: e.target.value })} />
          <input placeholder="Дней" value={membership.durationDays} onChange={(e) => setMembership({ ...membership, durationDays: e.target.value })} />
          <input placeholder="Посещений, пусто = безлимит" value={membership.visitCount} onChange={(e) => setMembership({ ...membership, visitCount: e.target.value })} />
          <input placeholder="Цена" value={membership.price} onChange={(e) => setMembership({ ...membership, price: e.target.value })} />
          <textarea placeholder="Описание" value={membership.description} onChange={(e) => setMembership({ ...membership, description: e.target.value })} />
          <label className="inline-actions">
            <input type="checkbox" checked={membership.freezeAllowed} onChange={(e) => setMembership({ ...membership, freezeAllowed: e.target.checked })} />
            Разрешена заморозка
          </label>
          <input placeholder="Максимум дней заморозки" value={membership.maxFreezeDays} onChange={(e) => setMembership({ ...membership, maxFreezeDays: e.target.value })} disabled={!membership.freezeAllowed} />
          <button>{editing?.kind === 'membership' ? 'Сохранить' : 'Добавить'}</button>
        </form>

        <form className="card" onSubmit={(e) => save(e, 'training')}>
          <h2>{editing?.kind === 'training' ? 'Изменить тренировку' : 'Тип тренировки'}</h2>
          <input placeholder="Название" value={training.name} onChange={(e) => setTraining({ ...training, name: e.target.value })} />
          <input placeholder="Минут" value={training.durationMinutes} onChange={(e) => setTraining({ ...training, durationMinutes: e.target.value })} />
          <input type="color" value={training.color} onChange={(e) => setTraining({ ...training, color: e.target.value })} />
          <textarea placeholder="Описание" value={training.description} onChange={(e) => setTraining({ ...training, description: e.target.value })} />
          <button>{editing?.kind === 'training' ? 'Сохранить' : 'Добавить'}</button>
        </form>

        <form className="card" onSubmit={(e) => save(e, 'hall')}>
          <h2>{editing?.kind === 'hall' ? 'Изменить зал' : 'Зал'}</h2>
          <input placeholder="Название" value={hall.name} onChange={(e) => setHall({ ...hall, name: e.target.value })} />
          <input placeholder="Вместимость" value={hall.capacity} onChange={(e) => setHall({ ...hall, capacity: e.target.value })} />
          <textarea placeholder="Описание" value={hall.description} onChange={(e) => setHall({ ...hall, description: e.target.value })} />
          <button>{editing?.kind === 'hall' ? 'Сохранить' : 'Добавить'}</button>
        </form>
      </div>

      <form className="card" onSubmit={(e) => save(e, 'trainer')}>
        <h2>{editing?.kind === 'trainer' ? 'Изменить тренера' : 'Тренер'}</h2>
        <div className="grid3 compact">
          <input placeholder="ФИО" value={trainer.fullName} onChange={(e) => setTrainer({ ...trainer, fullName: e.target.value })} />
          <input placeholder="Email" value={trainer.email} onChange={(e) => setTrainer({ ...trainer, email: e.target.value })} />
          <input placeholder="Телефон" value={trainer.phone} onChange={(e) => setTrainer({ ...trainer, phone: e.target.value })} />
          <input placeholder="Пароль" value={trainer.password} onChange={(e) => setTrainer({ ...trainer, password: e.target.value })} />
          <input placeholder="Специализация" value={trainer.specialization} onChange={(e) => setTrainer({ ...trainer, specialization: e.target.value })} />
          <input placeholder="Опыт, лет" value={trainer.yearsOfExperience} onChange={(e) => setTrainer({ ...trainer, yearsOfExperience: e.target.value })} />
          <textarea placeholder="Описание" value={trainer.description} onChange={(e) => setTrainer({ ...trainer, description: e.target.value })} />
        </div>
        <button>{editing?.kind === 'trainer' ? 'Сохранить' : 'Добавить'}</button>
      </form>

      <div className="grid3">
        <CatalogList title="Абонементы" kind="membership" items={data.membershipTypes} query={search.membership} onQuery={(q) => setSearch({ ...search, membership: q })} onEdit={edit} onDelete={remove}
          render={(x) => `${x.name}: ${x.price} руб., ${x.visitCount ?? 'безлимит'}, ${x.durationDays} дн.${x.freezeAllowed ? ` · заморозка до ${x.maxFreezeDays} дн.` : ''}`} />
        <CatalogList title="Тренировки" kind="training" items={data.trainingTypes} query={search.training} onQuery={(q) => setSearch({ ...search, training: q })} onEdit={edit} onDelete={remove}
          render={(x) => `${x.name}: ${x.durationMinutes} мин`} />
        <CatalogList title="Залы" kind="hall" items={data.halls} query={search.hall} onQuery={(q) => setSearch({ ...search, hall: q })} onEdit={edit} onDelete={remove}
          render={(x) => `${x.name}: ${x.capacity} мест`} />
      </div>
      <CatalogList title="Тренеры" kind="trainer" items={data.trainers} query={search.trainer} onQuery={(q) => setSearch({ ...search, trainer: q })} onEdit={edit} onDelete={remove}
        render={(x) => `${x.user.fullName}: ${x.specialization ?? 'специализация не указана'} · опыт ${x.yearsOfExperience ?? 0} лет`} />
    </section>
  );
}

function CatalogList({ title, kind, items, query, onQuery, render, onEdit, onDelete }: {
  title: string;
  kind: CatalogKind;
  items: any[];
  query: string;
  onQuery: (q: string) => void;
  render: (item: any) => string;
  onEdit: (kind: CatalogKind, item: any) => void;
  onDelete: (kind: CatalogKind, id: number) => void;
}) {
  const filtered = items.filter((item) => render(item).toLowerCase().includes(query.toLowerCase()));
  return (
    <div className="card">
      <h2>{title}</h2>
      <input value={query} onChange={(e) => onQuery(e.target.value)} placeholder={`Поиск: ${title.toLowerCase()}`} />
      <div className="list">
        {filtered.map((item) => (
          <article key={item.id}>
            <span>{render(item)}</span>
            <div className="inline-actions">
              <button className="ghost light" type="button" onClick={() => onEdit(kind, item)}>Изменить</button>
              <button className="danger" type="button" onClick={() => onDelete(kind, item.id)}>Удалить</button>
            </div>
          </article>
        ))}
      </div>
    </div>
  );
}
