// src/components/EditTicketPage.jsx
import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import '../styles/edit.scss';
import { eventsApi, personsApi, ticketsApi, venuesApi } from '../api/apis/api-client';

const pickArray = (data, key) => {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.[key])) return data[key];
  return [];
};

const num = (v) => {
  if (v == null) return undefined;
  if (typeof v === 'object' && 'parsedValue' in v) return Number(v.parsedValue);
  if (typeof v === 'string') {
    const n = Number(v);
    return Number.isNaN(n) ? undefined : n;
  }
  if (typeof v === 'number') return v;
  return undefined;
};

const TICKET_TYPES = ['VIP', 'USUAL', 'BUDGETARY', 'CHEAP'];

export default function EditTicketPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [form, setForm] = useState(null);
  const [errors, setErrors] = useState({});
  const [persons, setPersons] = useState([]);
  const [events, setEvents] = useState([]);
  const [venues, setVenues] = useState([]);

  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState(null);

  const [loaded, setLoaded] = useState(false);
  const [loadError, setLoadError] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const [t, p, e, ve] = await Promise.all([
          ticketsApi.getById(Number(id)),
          personsApi.list(),
          eventsApi.list(),
          venuesApi.list(),
        ]);
        if (cancelled) return;

        // заполнение формы
        setForm({
          name: t?.name ?? '',
          price: num(t?.price) ?? '',
          type: t?.type ?? '',
          number: num(t?.number) ?? '',
          discount: t?.discount != null ? num(t.discount) : '',
          comment: t?.comment ?? '',
          coordX: t?.coordinates?.x != null ? num(t.coordinates.x) : '',
          coordY: t?.coordinates?.y != null ? num(t.coordinates.y) : '',
          personId: t?.person?.id ?? '',
          eventId: t?.event?.id ?? '',
          venueId: t?.venue?.id ?? '',
        });

        const personsRaw = pickArray(p, 'personList').map((pp) => ({
          ...pp,
          weight: num(pp.weight),
          location: pp.location
            ? { ...pp.location, y: num(pp.location.y), z: num(pp.location.z) }
            : undefined,
        }));
        setPersons(personsRaw);
        setEvents(pickArray(e, 'eventList'));
        setVenues(pickArray(ve, 'venueList'));

        setStatus(null);
        setLoadError(false);
      } catch (e) {
        if (cancelled) return;
        const code = e?.response?.status;
        if (code === 404) setStatus(`Билет #${id} не найден (404).`);
        else if (code === 400) setStatus(`Некорректный запрос (400) ${e?.response?.data}`);
        else setStatus(`Не удалось загрузить данные: ${code || ''} ${e.message}`);
        setLoadError(true);
      } finally {
        if (!cancelled) setLoaded(true);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [id]);

  const set = (k) => (e) => {
    setForm((s) => ({ ...s, [k]: e.target.value }));
    setErrors((errs) => ({ ...errs, [k]: undefined }));
  };

  const validateAll = () => {
    const err = {};
    const isNonEmpty = (s) => typeof s === 'string' && s.trim().length > 0;
    const asNum = (v) => (v === '' ? NaN : Number(v));
    const isPosInt = (v) => Number.isInteger(v) && v > 0;

    if (!isNonEmpty(form.name)) err.name = 'Название обязательно';

    {
      const n = asNum(form.price);
      if (!Number.isFinite(n) || !(n > 0)) err.price = 'Цена должна быть числом > 0';
    }

    if (!TICKET_TYPES.includes(form.type)) err.type = 'Выберите корректный тип';

    {
      const n = asNum(form.number);
      if (!Number.isInteger(n) || !(n > 0)) err.number = 'Кол-во — целое число > 0';
    }

    if (form.discount !== '') {
      const d = asNum(form.discount);
      if (!Number.isFinite(d) || !(d > 0 && d <= 100)) err.discount = 'Скидка в (0; 100]';
    }

    {
      const x = asNum(form.coordX);
      const y = asNum(form.coordY);
      if (!Number.isFinite(x)) err.coordX = 'X должно быть числом';
      if (!Number.isFinite(y)) err.coordY = 'Y должно быть числом';
    }

    if (form.personId !== '') {
      const n = asNum(form.personId);
      if (!isPosInt(n)) err.personId = 'Person id должен быть положительным целым';
    }
    if (form.eventId !== '') {
      const n = asNum(form.eventId);
      if (!isPosInt(n)) err.eventId = 'Event id должен быть положительным целым';
    }
    if (form.venueId !== '') {
      const n = asNum(form.venueId);
      if (!isPosInt(n)) err.venueId = 'Venue id должен быть положительным целым';
    }

    setErrors(err);
    return err;
  };

  const buildFullPayload = () => {
    const discountVal = form.discount === '' ? null : Number(form.discount);
    return {
      name: form.name.trim(),
      price: Number(form.price),
      type: form.type,
      number: Number(form.number),
      discount: discountVal,
      comment: (form.comment || '').trim(),
      coordinates: {
        x: Number(form.coordX),
        y: Number(form.coordY),
      },
      person: form.personId === '' ? null : { id: Number(form.personId) },
      event: form.eventId === '' ? null : { id: Number(form.eventId) },
      venue: form.venueId === '' ? null : { id: Number(form.venueId) },
    };
  };

  const save = async () => {
    if (!form) return;

    const payload = buildFullPayload();
    await ticketsApi.update(Number(id), payload);

    const err = validateAll();
    if (Object.values(err).some(Boolean)) {
      setStatus('Проверь форму: исправь выделенные поля');
      return;
    }

    setBusy(true);
    setStatus(null);
    try {
      navigate('/');
    } catch (e) {
      setStatus(`Ошибка сохранения: ${e?.response?.status || ''} ${e.message}`);
    } finally {
      setBusy(false);
    }
  };

  if (!loaded) {
    return (
      <div className="edit-wrap">
        <div className="edit-head">
          <h2>Редактирование билета #{id}</h2>
          <Link className="link-back" to="/">
            ← Назад
          </Link>
        </div>
        <div className="status">Загрузка…</div>
      </div>
    );
  }

  if (loaded && loadError) {
    return (
      <div className="edit-wrap">
        <div className="edit-head">
          <h2>Редактирование билета #{id}</h2>
          <Link className="link-back" to="/">
            ← Назад
          </Link>
        </div>
        {status && <div className="status">{status}</div>}
        <div className="actions">
          <button className="btn" onClick={() => navigate('/')}>
            К таблице
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="edit-wrap">
      <div className="edit-head">
        <h2>Редактирование билета #{id}</h2>
        <Link className="link-back" to="/">
          ← Назад
        </Link>
      </div>

      {status && <div className="status">{status}</div>}

      <div className="edit-card">
        <div className="form-grid">
          <label>Название</label>
          <input
            className={errors.name ? 'err' : ''}
            aria-invalid={!!errors.name}
            value={form.name}
            onChange={set('name')}
          />
          {errors.name && <span className="help-err">{errors.name}</span>}

          <label>Цена</label>
          <input
            className={errors.price ? 'err' : ''}
            aria-invalid={!!errors.price}
            type="number"
            step="0.01"
            min="0"
            value={form.price}
            onChange={set('price')}
          />
          {errors.price && <span className="help-err">{errors.price}</span>}

          <label>Тип</label>
          <select
            className={errors.type ? 'err' : ''}
            aria-invalid={!!errors.type}
            value={form.type}
            onChange={set('type')}
          >
            <option value="">— не задан —</option>
            {TICKET_TYPES.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
          {errors.type && <span className="help-err">{errors.type}</span>}

          <label>Кол-во</label>
          <input
            className={errors.number ? 'err' : ''}
            aria-invalid={!!errors.number}
            type="number"
            min="0"
            value={form.number}
            onChange={set('number')}
          />
          {errors.number && <span className="help-err">{errors.number}</span>}

          <label>Скидка (0–100)</label>
          <input
            className={errors.discount ? 'err' : ''}
            aria-invalid={!!errors.discount}
            type="number"
            min="0"
            max="100"
            step="0.01"
            value={form.discount}
            onChange={set('discount')}
          />
          {errors.discount && <span className="help-err">{errors.discount}</span>}

          <label>Комментарий</label>
          <input value={form.comment} onChange={set('comment')} />

          <div className="fieldset">Координаты</div>

          <label>X</label>
          <input
            className={errors.coordX ? 'err' : ''}
            aria-invalid={!!errors.coordX}
            type="number"
            step="1"
            value={form.coordX}
            onChange={set('coordX')}
          />
          {errors.coordX && <span className="help-err">{errors.coordX}</span>}

          <label>Y</label>
          <input
            className={errors.coordY ? 'err' : ''}
            aria-invalid={!!errors.coordY}
            type="number"
            step="0.01"
            value={form.coordY}
            onChange={set('coordY')}
          />
          {errors.coordY && <span className="help-err">{errors.coordY}</span>}

          <div className="fieldset">Связанные сущности</div>

          <label>Person</label>
          <select
            className={errors.personId ? 'err' : ''}
            aria-invalid={!!errors.personId}
            value={form.personId}
            onChange={set('personId')}
          >
            <option value="">— не задан —</option>
            {persons.length > 0 &&
              persons.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.passportID || `Person #${p.id}`}
                </option>
              ))}
          </select>
          {errors.personId && <span className="help-err">{errors.personId}</span>}

          <label>Event</label>
          <select
            className={errors.eventId ? 'err' : ''}
            aria-invalid={!!errors.eventId}
            value={form.eventId}
            onChange={set('eventId')}
          >
            <option value="">— не задано —</option>
            {events.length > 0 &&
              events.map((ev) => (
                <option key={ev.id} value={ev.id}>
                  {ev.name ?? `Event #${ev.id}`}
                </option>
              ))}
          </select>
          {errors.eventId && <span className="help-err">{errors.eventId}</span>}

          <label>Venue</label>
          <select
            className={errors.venueId ? 'err' : ''}
            aria-invalid={!!errors.venueId}
            value={form.venueId}
            onChange={set('venueId')}
          >
            <option value="">— не задано —</option>
            {venues.length > 0 &&
              venues.map((v) => (
                <option key={v.id} value={v.id}>
                  {v.name ?? `Venue #${v.id}`}
                </option>
              ))}
          </select>
          {errors.venueId && <span className="help-err">{errors.venueId}</span>}
        </div>

        <div className="actions">
          <button className="btn" onClick={save} disabled={busy}>
            {busy ? 'Сохраняю…' : 'Сохранить изменения'}
          </button>
        </div>
      </div>
    </div>
  );
}
