import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import '../styles/main.scss';

const apiBase = () => (process.env.REACT_APP_API_BASE || 'http://localhost:8080').replace(/\/+$/, '');

const statusClass = (status) => {
  if (!status) return '';
  switch (status.toLowerCase()) {
    case 'success':
      return 'status-ok';
    case 'failed':
      return 'status-err';
    default:
      return '';
  }
};

export default function ImportLogPage() {
  const [entries, setEntries] = useState([]);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null);
  const [cacheLogging, setCacheLogging] = useState(false);
  const navigate = useNavigate();

  const base = useMemo(() => apiBase(), []);

  const extractData = (payload) => {
    if (!payload) return null;
    if (Array.isArray(payload)) return payload;
    if (payload.data) return payload.data;
    return null;
  };

  const fetchLogs = async () => {
    setBusy(true);
    try {
      const res = await fetch(`${base}/import/logs`);
      if (!res.ok) {
        throw new Error(`Ошибка загрузки журнала (${res.status})`);
      }
      const body = await res.json();
      const list = extractData(body);
      setEntries(Array.isArray(list) ? list : []);
      setMessage({ ok: true, text: `Обновлено (${list?.length ?? 0})` });
    } catch (error) {
      setMessage({ ok: false, text: error.message || 'Ошибка загрузки журнала' });
    } finally {
      setBusy(false);
    }
  };

  const fetchCacheLogging = async () => {
    try {
      const res = await fetch(`${base}/cache/l2/logging`);
      if (!res.ok) return;
      const data = await res.json();
      if (typeof data?.enabled === 'boolean') {
        setCacheLogging(data.enabled);
      } else if (typeof data?.data?.enabled === 'boolean') {
        setCacheLogging(data.data.enabled);
      }
    } catch (err) {
      console.warn('Не удалось получить статус логирования кэша', err);
    }
  };

  useEffect(() => {
    fetchLogs();
    fetchCacheLogging();
  });

  const updateCacheLogging = async (enabled) => {
    try {
      const res = await fetch(`${base}/cache/l2/logging`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ enabled }),
      });
      if (!res.ok) {
        throw new Error('Не удалось обновить настройки логирования');
      }
      setCacheLogging(enabled);
      setMessage({ ok: true, text: enabled ? 'Логирование кэша включено' : 'Логирование кэша выключено' });
    } catch (error) {
      setMessage({ ok: false, text: error.message });
    }
  };

  const renderDownloadLink = (entry) => {
    const href = `${base}${entry.downloadPath || `/import/logs/${entry.id}/file`}`;
    return (
      <a href={href} target="_blank" rel="noreferrer">
        Скачать
      </a>
    );
  };

  return (
    <div className="page">
      <div className="toolbar">
        <button className="btn" onClick={() => navigate('/')} disabled={busy}>
          Назад
        </button>
        <button className="btn" onClick={fetchLogs} disabled={busy}>
          Обновить журнал
        </button>
        <label className="toggle">
          <input
            type="checkbox"
            checked={cacheLogging}
            onChange={(e) => updateCacheLogging(e.target.checked)}
          />
          <span>Логирование L2 cache</span>
        </label>
        {message && <span className={message.ok ? 'status-ok' : 'status-err'}>{message.text}</span>}
      </div>

      <div className="log-wrapper">
        <table className="log-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Имя файла</th>
              <th>Размер</th>
              <th>Статус</th>
              <th>Запрошено</th>
              <th>Импортировано</th>
              <th>Создан</th>
              <th>Завершён</th>
              <th>Файл</th>
            </tr>
          </thead>
          <tbody>
            {entries.length === 0 && (
              <tr>
                <td colSpan={9} style={{ textAlign: 'center' }}>
                  Нет записей
                </td>
              </tr>
            )}
            {entries.map((entry) => (
              <tr key={entry.id}>
                <td>{entry.id}</td>
                <td>{entry.filename || '—'}</td>
                <td>{entry.size ? `${(entry.size / 1024).toFixed(1)} КБ` : '—'}</td>
                <td className={statusClass(entry.status || '')}>{entry.status || '—'}</td>
                <td>{entry.requested ?? '—'}</td>
                <td>{entry.imported ?? '—'}</td>
                <td>{entry.createdAt ? new Date(entry.createdAt).toLocaleString() : '—'}</td>
                <td>{entry.completedAt ? new Date(entry.completedAt).toLocaleString() : '—'}</td>
                <td>{entry.status === 'SUCCESS' ? renderDownloadLink(entry) : '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
