import React, { useState } from 'react';
import '../styles/edit.scss';
import { personsApi } from '../api/apis/api-client';

const COLORS = ['GREEN', 'RED', 'ORANGE', 'WHITE', 'BROWN'];
const COUNTRIES = ['GERMANY', 'INDIA', 'THAILAND', 'SOUTH_KOREA', 'JAPAN'];

export default function CreatePersonPanel({ onCreated }) {
  const [v, setV] = useState({
    passportID: '',
    weight: '',
    nationality: '',
    hairColor: '',
    eyeColor: '',
    locX: '',
    locY: '',
    locZ: '',
  });
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

    if (!v.passportID.trim()) err.passportID = 'Обязательное поле';

    const w = asNum(v.weight);
    if (!Number.isFinite(w) || !(w > 0)) err.weight = 'Число > 0';

    if (!v.nationality) err.nationality = 'Выберите страну';
    if (!v.hairColor) err.hairColor = 'Выберите цвет';

    const x = asNum(v.locX);
    const z = asNum(v.locZ);
    if (!Number.isFinite(x)) err.locX = 'Число';
    if (!Number.isFinite(z)) err.locZ = 'Число';

    setErrors(err);
    return err;
  };

  const buildPayload = () => ({
    passportID: v.passportID.trim(),
    weight: Number(v.weight),
    nationality: v.nationality, // строки-литералы ок
    hairColor: v.hairColor,
    eyeColor: v.eyeColor || undefined,
    location: {
      x: Number(v.locX),
      y: v.locY === '' ? 0 : Number(v.locY),
      z: Number(v.locZ),
    },
  });

  const save = async () => {
    setMsg(null);
    const err = validate();
    if (Object.values(err).some(Boolean)) {
      setMsg({ ok: false, text: 'Исправьте выделенные поля' });
      return;
    }
    setBusy(true);
    try {
      const created = await personsApi.add(buildPayload());
      setMsg({ ok: true, text: `Person id=${created?.id ?? '—'} создан` });
      setV({
        passportID: '',
        weight: '',
        nationality: '',
        hairColor: '',
        eyeColor: '',
        locX: '',
        locY: '',
        locZ: '',
      });
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
    <div className="form-grid" style={{ maxWidth: 760 }}>
      <label>passportID *</label>
      <input
        className={errors.passportID ? 'err' : ''}
        value={v.passportID}
        onChange={set('passportID')}
      />
      {errors.passportID && <span className="help-err">{errors.passportID}</span>}

      <label>weight *</label>
      <input
        className={errors.weight ? 'err' : ''}
        type="number"
        step="0.01"
        min="0.01"
        value={v.weight}
        onChange={set('weight')}
      />
      {errors.weight && <span className="help-err">{errors.weight}</span>}

      <label>nationality *</label>
      <select
        className={errors.nationality ? 'err' : ''}
        value={v.nationality}
        onChange={set('nationality')}
      >
        <option value="">— выберите —</option>
        {COUNTRIES.map((c) => (
          <option key={c} value={c}>
            {c}
          </option>
        ))}
      </select>
      {errors.nationality && <span className="help-err">{errors.nationality}</span>}

      <label>hairColor *</label>
      <select
        className={errors.hairColor ? 'err' : ''}
        value={v.hairColor}
        onChange={set('hairColor')}
      >
        <option value="">— выберите —</option>
        {COLORS.map((c) => (
          <option key={c} value={c}>
            {c}
          </option>
        ))}
      </select>
      {errors.hairColor && <span className="help-err">{errors.hairColor}</span>}

      <label>eyeColor</label>
      <select value={v.eyeColor} onChange={set('eyeColor')}>
        <option value="">— не задан —</option>
        {COLORS.map((c) => (
          <option key={c} value={c}>
            {c}
          </option>
        ))}
      </select>

      <div className="fieldset">Location</div>

      <label>X *</label>
      <input
        className={errors.locX ? 'err' : ''}
        type="number"
        step="1"
        value={v.locX}
        onChange={set('locX')}
      />
      {errors.locX && <span className="help-err">{errors.locX}</span>}

      <label>Y</label>
      <input type="number" step="0.01" value={v.locY} onChange={set('locY')} />

      <label>Z *</label>
      <input
        className={errors.locZ ? 'err' : ''}
        type="number"
        step="0.01"
        value={v.locZ}
        onChange={set('locZ')}
      />
      {errors.locZ && <span className="help-err">{errors.locZ}</span>}

      <div className="actions" style={{ gridColumn: '1 / -1' }}>
        <button className="btn" onClick={save} disabled={busy}>
          {busy ? 'Сохраняю...' : 'Создать Person'}
        </button>
        {msg && (
          <span style={{ marginLeft: 12, color: msg.ok ? 'green' : 'crimson' }}>{msg.text}</span>
        )}
      </div>
    </div>
  );
}
