import React, { useState } from 'react';
import '../styles/edit.scss';
import { venuesApi } from '../api/apis/api-client';

const VENUE_TYPES = ['LOFT', 'OPEN_AREA', 'STADIUM'];

export default function CreateVenuePanel({ onCreated }) {
  const [v, setV] = useState({ name: '', capacity: '', venueType: '' });
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
    if (v.capacity !== '') {
      const n = asNum(v.capacity);
      if (!Number.isFinite(n) || !(n > 0)) err.capacity = 'Число > 0';
    }
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
        capacity: v.capacity === '' ? undefined : Number(v.capacity),
        venueType: v.venueType || undefined,
      };
      const res = await venuesApi.add(payload);
      console.log(res);
      setMsg({ ok: true, text: `Venue создана` });
      setV({ name: '', capacity: '', venueType: '' });
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

      <label>capacity</label>
      <input
        className={errors.capacity ? 'err' : ''}
        type="number"
        min="1"
        value={v.capacity}
        onChange={set('capacity')}
      />
      {errors.capacity && <span className="help-err">{errors.capacity}</span>}

      <label>venueType</label>
      <select value={v.venueType} onChange={set('venueType')}>
        <option value="">— не задан —</option>
        {VENUE_TYPES.map((t) => (
          <option key={t} value={t}>
            {t}
          </option>
        ))}
      </select>

      <div className="actions" style={{ gridColumn: '1 / -1' }}>
        <button className="btn" onClick={save} disabled={busy}>
          {busy ? 'Сохраняю...' : 'Создать Venue'}
        </button>
        {msg && (
          <span style={{ marginLeft: 12, color: msg.ok ? 'green' : 'crimson' }}>{msg.text}</span>
        )}
      </div>
    </div>
  );
}
