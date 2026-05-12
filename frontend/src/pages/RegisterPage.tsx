import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { errorMessage } from '../shared/api';
import { useAuth } from '../shared/auth';

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: '', email: '', phone: '', password: '', birthDate: '' });
  const [error, setError] = useState('');
  const labels: Record<string, string> = { fullName: 'ФИО', email: 'Email', phone: 'Телефон', password: 'Пароль', birthDate: 'Дата рождения' };

  async function submit(e: FormEvent) {
    e.preventDefault();
    try {
      await register(form);
      navigate('/');
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <main className="authPage">
      <section className="authCard">
        <div className="badge">Личный кабинет</div>
        <h1>Регистрация клиента</h1>
        <form onSubmit={submit}>
          {Object.entries(form).map(([key, value]) => (
            <input key={key} type={key === 'password' ? 'password' : key === 'birthDate' ? 'date' : 'text'} value={value}
              placeholder={labels[key]}
              onChange={(e) => setForm({ ...form, [key]: e.target.value })} />
          ))}
          {error && <div className="error">{error}</div>}
          <button>Создать кабинет</button>
        </form>
        <Link to="/login">Уже есть аккаунт</Link>
      </section>
    </main>
  );
}
