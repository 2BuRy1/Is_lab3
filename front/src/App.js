import React from 'react';
import { Routes, Route } from 'react-router-dom';
import MainPage from './pages/MainPage';
import CreateTicketPage from './pages/CreateTicketPage';
import EditTicketPage from './components/EditTicketPage';
import FunctionsPanel from './components/FunctionsPanel';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<MainPage />} />
      <Route path="/tickets/new" element={<CreateTicketPage />} />
      <Route path="/tickets/:id/edit" element={<EditTicketPage />} />
      <Route path="/functions" element={<FunctionsPanel />} />
    </Routes>
  );
}
