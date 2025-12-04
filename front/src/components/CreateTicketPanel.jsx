import React, { useEffect, useState } from 'react';
import '../styles/edit.scss';
import { eventsApi, personsApi, ticketsApi, venuesApi } from '../api/apis/api-client';

const TICKET_TYPES = ['VIP', 'USUAL', 'BUDGETARY', 'CHEAP'];

export default function CreateTicketPanel({ onCreated, refreshToken }) {
  const [v, setV] = useState({
    name: '',
    price: '',
    type: '',
    number: '',
    discount: '',
    comment: '',
    coordX: '',
    coordY: '',
    personId: '',
    eventId: '',
    venueId: '',
  });
  const [errors, setErrors] = useState({});
  const [persons, setPersons] = useState([]);
  const [events, setEvents] = useState([]);
  const [venues, setVenues] = useState([]);
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState(null);

  const set = (k) => (e) => {
    setV((s) => ({ ...s, [k]: e.target.value }));
    setErrors((err) => ({ ...err, [k]: undefined }));
  };

  useEffect(() => {
    (async () => {
      try {
        const [p, e, ve] = await Promise.all([
          personsApi.list(),
          eventsApi.list(),
          venuesApi.list(),
        ]);

        setPersons(p.personList ?? []);
        setEvents(e.eventList ?? []);
        setVenues(ve.venueList ?? []);
      } catch {
        setMsg({ ok: false, text: 'Не загрузились справочники' });
      }
    })();
  }, [refreshToken]);

  const validate = () => {
    const err = {};
    const asNum = (x) => (x === '' ? NaN : Number(x));
    const pos = (n) => Number.isFinite(n) && n > 0;

    if (!v.name.trim()) err.name = 'Название обязательно';
    if (!pos(asNum(v.price))) err.price = 'Число > 0';
    if (!v.type) err.type = 'Выберите тип';
    if (!Number.isInteger(asNum(v.number)) || !(asNum(v.number) > 0)) err.number = 'Целое > 0';

    if (v.discount !== '') {
      const d = asNum(v.discount);
      if (!Number.isFinite(d) || !(d > 0 && d <= 100)) err.discount = '(0;100]';
    }
    if (!Number.isFinite(asNum(v.coordX))) err.coordX = 'Число';
    if (!Number.isFinite(asNum(v.coordY))) err.coordY = 'Число';

    setErrors(err);
    return err;
  };

  const buildPayload = () => ({
    name: v.name.trim(),
    price: Number(v.price),
    type: v.type,
    number: Number(v.number),
    discount: v.discount === '' ? null : Number(v.discount),
    comment: v.comment.trim() || undefined,
    coordinates: { x: Number(v.coordX), y: Number(v.coordY) },
    person: v.personId ? { id: Number(v.personId) } : null,
    event: v.eventId ? { id: Number(v.eventId) } : null,
    venue: v.venueId ? { id: Number(v.venueId) } : null,
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
      await ticketsApi.add(buildPayload());
      setMsg({ ok: true, text: `Билет создан` });
      setV({
        name: '',
        price: '',
        type: '',
        number: '',
        discount: '',
        comment: '',
        coordX: '',
        coordY: '',
        personId: '',
        eventId: '',
        venueId: '',
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
    <div className="form-grid" style={{ maxWidth: 820 }}>
      <label>Название *</label>
      <input className={errors.name ? 'err' : ''} value={v.name} onChange={set('name')} />
      {errors.name && <span className="help-err">{errors.name}</span>}

      <label>Цена *</label>
      <input
        className={errors.price ? 'err' : ''}
        type="number"
        step="0.01"
        min="0.01"
        value={v.price}
        onChange={set('price')}
      />
      {errors.price && <span className="help-err">{errors.price}</span>}

      <label>Тип *</label>
      <select className={errors.type ? 'err' : ''} value={v.type} onChange={set('type')}>
        <option value="">— выберите —</option>
        {TICKET_TYPES.map((t) => (
          <option key={t} value={t}>
            {t}
          </option>
        ))}
      </select>
      {errors.type && <span className="help-err">{errors.type}</span>}

      <label>Количество *</label>
      <input
        className={errors.number ? 'err' : ''}
        type="number"
        min="1"
        value={v.number}
        onChange={set('number')}
      />
      {errors.number && <span className="help-err">{errors.number}</span>}

      <label>Скидка (0–100)</label>
      <input
        className={errors.discount ? 'err' : ''}
        type="number"
        min="0.01"
        max="100"
        step="0.01"
        value={v.discount}
        onChange={set('discount')}
      />
      {errors.discount && <span className="help-err">{errors.discount}</span>}

      <label>Комментарий</label>
      <input value={v.comment} onChange={set('comment')} />

      <label>Координата X *</label>
      <input
        className={errors.coordX ? 'err' : ''}
        type="number"
        step="1"
        value={v.coordX}
        onChange={set('coordX')}
      />
      {errors.coordX && <span className="help-err">{errors.coordX}</span>}

      <label>Координата Y *</label>
      <input
        className={errors.coordY ? 'err' : ''}
        type="number"
        step="0.01"
        value={v.coordY}
        onChange={set('coordY')}
      />
      {errors.coordY && <span className="help-err">{errors.coordY}</span>}

      <label>Владелец </label>
      <select value={v.personId} onChange={set('personId')}>
        <option value="">— не задан —</option>
        {persons.map((p) => (
          <option key={p.id} value={p.id}>
            {p.passportID || `Person #${p.id}`}
          </option>
        ))}
      </select>

      <label>Событие</label>
      <select value={v.eventId} onChange={set('eventId')}>
        <option value="">— не задано —</option>
        {events.map((ev) => (
          <option key={ev.id} value={ev.id}>
            {ev.name}
          </option>
        ))}
      </select>

      <label>Площадка</label>
      <select value={v.venueId} onChange={set('venueId')}>
        <option value="">— не задано —</option>
        {venues.map((ve) => (
          <option key={ve.id} value={ve.id}>
            {ve.name}
          </option>
        ))}
      </select>

      <div className="actions" style={{ gridColumn: '1 / -1' }}>
        <button className="btn" onClick={save} disabled={busy}>
          {busy ? 'Сохраняю...' : 'Создать Ticket'}
        </button>
        {msg && (
          <span style={{ marginLeft: 12, color: msg.ok ? 'green' : 'crimson' }}>{msg.text}</span>
        )}
      </div>
    </div>
  );
}
