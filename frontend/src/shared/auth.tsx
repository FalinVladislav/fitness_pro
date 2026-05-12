import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import type React from 'react';
import { api } from './api';
import { User } from './types';

type AuthContextValue = {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (payload: Record<string, string>) => Promise<void>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('fitnessProToken');
    if (!token) {
      setLoading(false);
      return;
    }
    api.get('/auth/me').then((res) => setUser(res.data)).catch(() => localStorage.removeItem('fitnessProToken')).finally(() => setLoading(false));
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    user,
    loading,
    async login(email, password) {
      const res = await api.post('/auth/login', { email, password });
      localStorage.setItem('fitnessProToken', res.data.token);
      setUser(res.data.user);
    },
    async register(payload) {
      const res = await api.post('/auth/register', payload);
      localStorage.setItem('fitnessProToken', res.data.token);
      setUser(res.data.user);
    },
    logout() {
      localStorage.removeItem('fitnessProToken');
      setUser(null);
    },
  }), [user, loading]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error('AuthContext is not available');
  return value;
}
