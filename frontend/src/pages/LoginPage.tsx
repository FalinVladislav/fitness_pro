import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { errorMessage } from '../shared/api';
import { useAuth } from '../shared/auth';

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('admin@example.com');
  const [password, setPassword] = useState('admin123');
  const [error, setError] = useState('');

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError('');
    try {
      await login(email, password);
      navigate('/');
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <main className="authPage">
      <section className="authCard">
        <div className="badge">Фитнес-Про</div>
        <h1>Вход в систему</h1>
        <p>Ресепшен, тренеры, клиенты и руководитель работают в едином расписании.</p>
        <form onSubmit={submit}>
          <input value={email} onChange={(e) => setEmail(e.target.value)} placeholder="Email" />
          <input value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Пароль" type="password" />
          {error && <div className="error">{error}</div>}
          <button>Войти</button>
        </form>
        <Link to="/register">Зарегистрироваться как клиент</Link>
      </section>
    </main>
  );
}
