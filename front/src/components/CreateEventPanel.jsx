import React, { useState } from 'react';
import '../styles/edit.scss';
import { eventsApi, unwrap } from '../api/apis/api-client';

const EVENT_TYPES = ['CONCERT', 'FOOTBALL', 'BASKETBALL'];

export default function CreateEventPanel({ onCreated }) {
  const [v, setV] = useState({ name: '', ticketsCount: '', eventType: '' });
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState(null);

  const set = (k) => (e) => {
    setV((s) => ({ ...s, [k]: e.target.value }));
    setErrors((err) => ({ ...err, [k]: undefined }));
  };

  const validate = () => {
    const err = {};
    const asNum = (x) => (x === '' ? NaN : Number(x));
    if (!v.name.trim()) err.name = 'Название обязательно';
    const n = asNum(v.ticketsCount);
    if (!Number.isInteger(n) || !(n > 0)) err.ticketsCount = 'Целое число > 0';
    setErrors(err);
    return err;
  };

  const save = async () => {
    setMsg(null);
    const err = validate();
    if (Object.values(err).some(Boolean)) {
      setMsg({ ok: false, text: 'Исправьте выделенные поля' });
      return;
    }
    setBusy(true);
    try {
      const payload = {
        name: v.name.trim(),
        ticketsCount: Number(v.ticketsCount),
        eventType: v.eventType || undefined,
      };
      const res = await eventsApi.add(payload);
      const created = unwrap(res);
      setMsg({ ok: true, text: `Event id=${created?.id ?? '—'} создан` });
      setV({ name: '', ticketsCount: '', eventType: '' });
      setErrors({});
      onCreated?.();
    } catch (e) {
      setMsg({
        ok: false,
        text: `Ошибка: ${e.response?.status || ''} ${e.response?.data?.message || e.message}`,
      });
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="form-grid" style={{ maxWidth: 620 }}>
      <label>Название *</label>
      <input className={errors.name ? 'err' : ''} value={v.name} onChange={set('name')} />
      {errors.name && <span className="help-err">{errors.name}</span>}

      <label>ticketsCount *</label>
      <input
        className={errors.ticketsCount ? 'err' : ''}
        type="number"
        min="1"
        value={v.ticketsCount}
        onChange={set('ticketsCount')}
      />
      {errors.ticketsCount && <span className="help-err">{errors.ticketsCount}</span>}

      <label>eventType</label>
      <select value={v.eventType} onChange={set('eventType')}>
        <option value="">— не задан —</option>
        {EVENT_TYPES.map((t) => (
          <option key={t} value={t}>
            {t}
          </option>
        ))}
      </select>

      <div className="actions" style={{ gridColumn: '1 / -1' }}>
        <button className="btn" onClick={save} disabled={busy}>
          {busy ? 'Сохраняю...' : 'Создать Event'}
        </button>
        {msg && (
          <span style={{ marginLeft: 12, color: msg.ok ? 'green' : 'crimson' }}>{msg.text}</span>
        )}
      </div>
    </div>
  );
}
