import { useEffect, useState } from 'react';
import { api } from '../shared/api';

export function ReportsPage() {
  const [reports, setReports] = useState<Record<string, any[]>>({});
  useEffect(() => {
    Promise.all([
      api.get('/reports/revenue'), api.get('/reports/attendance'), api.get('/reports/trainers-load'), api.get('/reports/popular-training-types')
    ]).then(([revenue, attendance, trainers, popular]) => setReports({ revenue: revenue.data, attendance: attendance.data, trainers: trainers.data, popular: popular.data }));
  }, []);
  return (
    <section className="grid2">
      <Report title="Доходы" rows={reports.revenue} />
      <Report title="Посещаемость" rows={reports.attendance} />
      <Report title="Загрузка тренеров" rows={reports.trainers} />
      <Report title="Популярность тренировок" rows={reports.popular} />
    </section>
  );
}

function Report({ title, rows = [] }: { title: string; rows?: any[] }) {
  return <div className="card metric"><h2>{title}</h2>{rows.map((r) => <article key={r.label}><b>{r.value}</b><span>{r.label}</span></article>)}</div>;
}
