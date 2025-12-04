// src/pages/MainPage.jsx
import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Table from '../components/Table';
import '../styles/main.scss';
import { ticketsApi } from '../api/apis/api-client';

const parseTickets = (data) => {
  if (!data) return [];
  if (data && typeof data === 'object' && 'data' in data) return parseTickets(data.data);
  if (Array.isArray(data)) return data;
  if (Array.isArray(data.ticketList)) return data.ticketList;
  if (Array.isArray(data.tickets)) return data.tickets;
  return [];
};

export default function MainPage() {
  const [tickets, setTickets] = useState([]);
  const [reloadKey, setReloadKey] = useState(0);
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState(null);
  const fileInputRef = useRef(null);

  const [searchId, setSearchId] = useState('');
  const [found, setFound] = useState(null);

  const [editId, setEditId] = useState('');
  const [editErr, setEditErr] = useState('');

  const [deleteId, setDeleteId] = useState('');
  const [deleteErr, setDeleteErr] = useState('');

  const navigate = useNavigate();
  const bump = () => setReloadKey((k) => k + 1);

  const extractMessage = (payload, fallback = '') => {
    if (!payload) return fallback;
    if (typeof payload === 'string') return payload;
    if (typeof payload === 'object') {
      if (payload.message) return payload.message;
      if (payload.text) return payload.text;
      if (payload.data) {
        if (typeof payload.data === 'string') return payload.data;
        if (payload.data && typeof payload.data === 'object' && payload.data.message) {
          return payload.data.message;
        }
      }
      if (payload.title && fallback) return `${payload.title}. ${fallback}`;
    }
    return fallback;
  };

  const sendBulkImport = async (ticketsPayload) => {
    const base = (process.env.REACT_APP_API_BASE || 'http://localhost:8080').replace(/\/+$/, '');
    const response = await fetch(`${base}/tickets/import`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(ticketsPayload),
    });
    const contentType = response.headers.get('content-type') || '';
    let body = null;
    try {
      if (contentType.includes('application/json')) {
        body = await response.json();
      } else {
        body = await response.text();
      }
    } catch (readErr) {
      console.warn('Не удалось обработать ответ импорта', readErr);
    }

    if (!response.ok) {
      const message = extractMessage(body, `Ошибка импорта (${response.status})`);
      const error = new Error(message || `Ошибка импорта (${response.status})`);
      error.payload = body;
      throw error;
    }

    return body;
  };

  const handleImportFile = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) {
      return;
    }

    setBusy(true);
    setMsg({ ok: true, text: `Импорт из ${file.name}...` });

    try {
      const text = await file.text();
      let parsed;
      try {
        parsed = JSON.parse(text);
      } catch (error) {
        throw new Error('Файл не является корректным JSON');
      }

      const entries = parseTickets(parsed);
      if (!entries.length) {
        throw new Error('JSON не содержит массив ticket');
      }

      const responsePayload = await sendBulkImport(entries);
      const successMessage = extractMessage(
        responsePayload,
        `Отправлено ${entries.length} билет(ов)`
      );
      setMsg({ ok: true, text: successMessage || `Отправлено ${entries.length} билет(ов)` });
      bump();
    } catch (error) {
      setMsg({ ok: false, text: error.message || 'Ошибка импорта' });
    } finally {
      setBusy(false);
    }
  };

  const triggerImportDialog = () => {
    if (busy) {
      return;
    }
    fileInputRef.current?.click();
  };

  const asId = (v) => {
    if (typeof v !== 'string') return NaN;
    const n = Number(v.trim());
    return Number.isInteger(n) && n > 0 ? n : NaN;
  };

  const loadTickets = async () => {
    try {
      const list = await ticketsApi.list();
      setTickets(parseTickets(list));
    } catch (e) {
      setTickets([]);
    }
  };

  useEffect(() => {
    loadTickets();
  }, [reloadKey]);

  useEffect(() => {
    const base = (process.env.REACT_APP_API_BASE || 'http://localhost:8080').replace(/\/+$/, '');
    const url = `${base}/tickets/stream`;
    let closed = false;
    const es = new EventSource(url, { withCredentials: false });

    es.onmessage = (ev) => {
      console.log(ev);
      try {
        const payload = ev?.data ? JSON.parse(ev.data) : null;
        const type = payload?.event || payload?.type;
        if (type) setReloadKey((k) => k + 1);
        else setReloadKey((k) => k + 1);
      } catch {
        console.log(ev);

        setReloadKey((k) => k + 1);
      }
    };

    es.onerror = () => {
      if (!closed) es.close();
    };

    return () => {
      closed = true;
      es.close();
    };
  }, []);

  const onEditPageClick = () => {
    const id = asId(editId);
    if (Number.isNaN(id)) {
      setEditErr('Введите положительный целый ID');
      return;
    }
    setEditErr('');
    navigate(`/tickets/${id}/edit`);
  };

  const onDeleteClick = async () => {
    const id = asId(deleteId);
    if (Number.isNaN(id)) {
      setDeleteErr('Введите положительный целый ID');
      return;
    }
    setDeleteErr('');
    if (!window.confirm(`Удалить билет ${id}?`)) return;

    setBusy(true);
    setMsg(null);
    try {
      await ticketsApi.delete(id);
      setMsg({ ok: true, text: `Билет ${id} удалён` });
      setDeleteId('');
      bump();
    } catch (e) {
      setMsg({
        ok: false,
        text: e?.response?.data
          ? `${e.response.data.title ?? 'Ошибка'} ${e.response.data.message ?? e.response.data.errorMessage ?? ''}`
          : 'Ошибка запроса',
      });
    } finally {
      setBusy(false);
    }
  };

  const onSearchById = async () => {
    const id = asId(searchId);
    if (Number.isNaN(id)) {
      setMsg({ ok: false, text: 'Введите корректный ID' });
      return;
    }

    setBusy(true);
    setMsg(null);
    try {
      const ticket = await ticketsApi.getById(id);
      setFound(ticket || null);
    } catch {
      setFound(null);
      setMsg({ ok: false, text: 'Ошибка. Объект не найден' });
    } finally {
      setBusy(false);
    }
  };

  const clearSearch = () => {
    setSearchId('');
    setFound(null);
  };

  return (
    <div className="page">
      <div className="toolbar">
        <button className="btn" onClick={() => navigate('/tickets/new')} disabled={busy}>
          Создать билет
        </button>

        <button className="btn" onClick={triggerImportDialog} disabled={busy}>
          Импорт JSON
        </button>
        <input
          ref={fileInputRef}
          type="file"
          accept="application/json,.json"
          onChange={handleImportFile}
          style={{ display: 'none' }}
        />

        <div className="inline-form">
          <input
            className={`input-id ${editErr ? 'err' : ''}`}
            placeholder="ID для редактирования"
            inputMode="numeric"
            value={editId}
            onChange={(e) => {
              setEditId(e.target.value);
              setEditErr('');
            }}
          />
          <button className="btn" onClick={onEditPageClick} disabled={busy}>
            Редактировать
          </button>
        </div>
        {editErr && <span className="help-err">{editErr}</span>}

        <div className="inline-form">
          <input
            className={`input-id ${deleteErr ? 'err' : ''}`}
            placeholder="ID для удаления"
            inputMode="numeric"
            value={deleteId}
            onChange={(e) => {
              setDeleteId(e.target.value);
              setDeleteErr('');
            }}
          />
          <button className="btn btn-danger" onClick={onDeleteClick} disabled={busy}>
            Удалить
          </button>
        </div>
        {deleteErr && <span className="help-err">{deleteErr}</span>}

        <button className="btn" onClick={() => navigate('/functions')} disabled={busy}>
          Дополнительные функции
        </button>

        <div className="inline-form">
          <input
            className="search-input"
            placeholder="ID билета"
            inputMode="numeric"
            value={searchId}
            onChange={(e) => setSearchId(e.target.value)}
          />
          <button className="btn" onClick={onSearchById} disabled={busy}>
            Найти
          </button>
          <button className="btn btn-secondary" onClick={clearSearch}>
            Очистить
          </button>
        </div>

        {msg && <span className={msg.ok ? 'status-ok' : 'status-err'}>{msg.text}</span>}
      </div>

      {found && (
        <div className="details-card">
          <h3>Билет #{found.id}</h3>
          <p>
            <b>Название:</b> {found.name}
          </p>
          <p>
            <b>Цена:</b> {found.price}
          </p>
          <p>
            <b>Тип:</b> {found.type}
          </p>
          <p>
            <b>Количество:</b> {found.number}
          </p>
          <p>
            <b>Скидка:</b> {found.discount ?? '—'}
          </p>
          <p>
            <b>Комментарий:</b> {found.comment ?? '—'}
          </p>
          <p>
            <b>Координаты:</b> X={found.coordinates?.x}, Y={found.coordinates?.y}
          </p>

          {found.person && (
            <div className="nested">
              <h4>Person</h4>
              <p>ID: {found.person.id}</p>
              <p>Passport: {found.person.passportID}</p>
              <p>Weight: {found.person.weight}</p>
              <p>Nationality: {found.person.nationality}</p>
              <p>Hair: {found.person.hairColor}</p>
              {found.person.eyeColor && <p>Eye: {found.person.eyeColor}</p>}
            </div>
          )}

          {found.event && (
            <div className="nested">
              <h4>Event</h4>
              <p>ID: {found.event.id}</p>
              <p>Название: {found.event.name}</p>
              {found.event.ticketsCount != null && <p>Tickets: {found.event.ticketsCount}</p>}
              {found.event.eventType && <p>Тип: {found.event.eventType}</p>}
            </div>
          )}

          {found.venue && (
            <div className="nested">
              <h4>Venue</h4>
              <p>ID: {found.venue.id}</p>
              <p>Название: {found.venue.name}</p>
              {found.venue.capacity != null && <p>Вместимость: {found.venue.capacity}</p>}
              {found.venue.type && <p>Тип: {found.venue.type}</p>}
            </div>
          )}
        </div>
      )}

      <Table tableName="mainTable" data={tickets} />
    </div>
  );
}
