import React, { useState } from 'react';
import '../styles/function.scss';
import { Link } from 'react-router-dom';
import { ticketsApi } from '../api/apis/api-client';

export default function FunctionsPanel() {
  const [msg, setMsg] = useState(null);
  const show = (ok, text) => setMsg({ ok, text });

  const [commentEq, setCommentEq] = useState('');
  const [commentLt, setCommentLt] = useState('');
  const [sell, setSell] = useState({ ticketId: '', personId: '', amount: '' });
  const [cloneId, setCloneId] = useState('');

  const [minTicket, setMinTicket] = useState(null);
  const [countLt, setCountLt] = useState(null);
  const [cloneResult, setCloneResult] = useState(null);

  const onDeleteByComment = async () => {
    try {
      await ticketsApi.deleteByComment(commentEq);
      show(true, 'Удалено');
      setCommentEq('');
    } catch (e) {
      const data = e.response.data;
      show(false, `${data.title}: ${data.message}` || e.message);
    }
  };

  const onFetchMinEvent = async () => {
    try {
      const t = await ticketsApi.minEvent();
      setMinTicket(t || null);
      setMsg(null);
    } catch (e) {
      const data = e.response.data;
      show(false, `${data.title}: ${data.message}` || e.message);
      setMinTicket(null);
    }
  };

  const onCountLess = async () => {
    try {
      const c = await ticketsApi.countCommentLess(commentLt);
      setCountLt(typeof c === 'number' ? c : (c?.count ?? c));
      setMsg(null);
    } catch (e) {
      const data = e.response.data;
      show(false, `${data.title}: ${data.message}` || e.message);
    }
  };

  const onSell = async () => {
    try {
      await ticketsApi.sell({
        ticketId: Number(sell.ticketId),
        personId: Number(sell.personId),
        amount: Number(sell.amount),
      });
      show(true, 'Продано');
      setSell({ ticketId: '', personId: '', amount: '' });
    } catch (e) {
      const data = e.response.data;
      show(false, `${data.title}: ${data.message}` || e.message);
    }
  };

  const onClone = async () => {
    try {
      const copy = await ticketsApi.cloneVip({ ticketId: Number(cloneId) });
      setCloneResult(copy);
      show(true, 'Копия создана');
      setCloneId('');
    } catch (e) {
      const data = e.response.data;
      show(false, `${data.title}: ${data.message}` || e.message);
    }
  };

  const renderMinTicket = (t) => {
    if (!t) return null;
    const ev = t.event ?? t;
    return (
      <div className="result-card">
        <div className="title">Билет с минимальным Event</div>
        <div className="kv">
          <span className="k">Ticket ID</span>
          <span className="v">{t.id}</span>
          {t.name && (
            <>
              <span className="k">Название</span>
              <span className="v">{t.name}</span>
            </>
          )}
          {t.price != null && (
            <>
              <span className="k">Цена</span>
              <span className="v">{t.price}</span>
            </>
          )}
          {ev?.id && (
            <>
              <span className="k">Event ID</span>
              <span className="v">{ev.id}</span>
            </>
          )}
          {ev?.name && (
            <>
              <span className="k">Event name</span>
              <span className="v">{ev.name}</span>
            </>
          )}
          {ev?.eventType && (
            <>
              <span className="k">Event type</span>
              <span className="v">{ev.eventType}</span>
            </>
          )}
        </div>
      </div>
    );
  };

  const renderCountResult = () => {
    if (countLt == null) return null;
    return (
      <div className="result-card">
        <div className="title">Результат подсчёта</div>
        <div className="kv">
          <span className="k">Количество объектов</span>
          <span className="v">{countLt}</span>
        </div>
      </div>
    );
  };

  return (
    <div className="funcs-screen">
      <div className="funcs-bar">
        <div className="func-row">
          <span>Удалить по comment:</span>
          <input value={commentEq} onChange={(e) => setCommentEq(e.target.value)} />
          <button className="btn" onClick={onDeleteByComment}>
            Удалить
          </button>
        </div>

        <div className="func-row">
          <span>Min event:</span>
          <button className="btn" onClick={onFetchMinEvent}>
            Показать
          </button>
        </div>
        {renderMinTicket(minTicket)}

        <div className="func-row">
          <span>Count comment &lt;:</span>
          <input value={commentLt} onChange={(e) => setCommentLt(e.target.value)} />
          <button className="btn" onClick={onCountLess}>
            Посчитать
          </button>
        </div>
        {renderCountResult()}

        <div className="func-row">
          <span>Продать:</span>
          <input
            placeholder="ticketId"
            value={sell.ticketId}
            onChange={(e) => setSell({ ...sell, ticketId: e.target.value })}
          />
          <input
            placeholder="personId"
            value={sell.personId}
            onChange={(e) => setSell({ ...sell, personId: e.target.value })}
          />
          <input
            placeholder="sum"
            value={sell.amount}
            onChange={(e) => setSell({ ...sell, amount: e.target.value })}
          />
          <button className="btn" onClick={onSell}>
            OK
          </button>
        </div>

        <div className="func-row">
          <span>Clone VIP:</span>
          <input
            placeholder="ticketId"
            value={cloneId}
            onChange={(e) => setCloneId(e.target.value)}
          />
          <button className="btn" onClick={onClone}>
            Клонировать
          </button>
          {cloneResult && (
            <div className="result-card">
              <div className="title">Копия создана</div>
              <div>ID: {cloneResult.id}</div>
            </div>
          )}
        </div>
      </div>

      {msg && <div className={`status ${msg.ok ? 'ok' : 'err'}`}>{msg.text}</div>}

      <div className="back-link">
        <Link to="/" className="btn">
          ← Назад
        </Link>
      </div>
    </div>
  );
}
