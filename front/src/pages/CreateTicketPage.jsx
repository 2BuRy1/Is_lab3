import React from 'react';
import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import CreateTicketPanel from '../components/CreateTicketPanel';
import CreateEventPanel from '../components/CreateEventPanel';
import CreateVenuePanel from '../components/CreateVenuePanel';
import CreatePersonPanel from '../components/CreatePersonPanel';

export default function CreateTicketPage() {
  const navigate = useNavigate();

  const [showPerson, setShowPerson] = useState(false);
  const [showEvent, setShowEvent] = useState(false);
  const [showVenue, setShowVenue] = useState(false);

  const [dictReloadKey, setDictReloadKey] = useState(0);
  const bumpDicts = () => setDictReloadKey((k) => k + 1);

  return (
    <div style={{ padding: 16, display: 'grid', gap: 16, maxWidth: 1000, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2 style={{ margin: 0 }}>Создание билета</h2>
        <Link to="/">← Назад к таблице</Link>
      </div>

      <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
        <button onClick={() => setShowPerson((v) => !v)}>
          {showPerson ? 'Скрыть Person' : 'Добавить нового Person'}
        </button>
        <button onClick={() => setShowEvent((v) => !v)}>
          {showEvent ? 'Скрыть Event' : 'Добавить новый Event'}
        </button>
        <button onClick={() => setShowVenue((v) => !v)}>
          {showVenue ? 'Скрыть Venue' : 'Добавить новый Venue'}
        </button>
      </div>

      {showPerson && (
        <div style={{ border: '1px solid #ddd', borderRadius: 8, padding: 12 }}>
          <h3 style={{ marginTop: 0 }}>Новый Person</h3>
          <CreatePersonPanel
            onCreated={() => {
              setShowPerson(false);
              bumpDicts();
            }}
          />
        </div>
      )}

      {showEvent && (
        <div style={{ border: '1px solid #ddd', borderRadius: 8, padding: 12 }}>
          <h3 style={{ marginTop: 0 }}>Новое Event</h3>
          <CreateEventPanel
            onCreated={() => {
              setShowEvent(false);
              bumpDicts();
            }}
          />
        </div>
      )}

      {showVenue && (
        <div style={{ border: '1px solid #ddd', borderRadius: 8, padding: 12 }}>
          <h3 style={{ marginTop: 0 }}>Новый Venue</h3>
          <CreateVenuePanel
            onCreated={() => {
              setShowVenue(false);
              bumpDicts();
            }}
          />
        </div>
      )}

      <div style={{ border: '1px solid #ddd', borderRadius: 8, padding: 16 }}>
        <CreateTicketPanel refreshToken={dictReloadKey} onCreated={() => navigate('/')} />
      </div>
    </div>
  );
}
