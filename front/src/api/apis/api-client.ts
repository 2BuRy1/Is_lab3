// src/apiClient.js

import { TicketsApi } from './tickets-api';
import { EventsApi } from './events-api';
import { Configuration } from '../configuration';
import { PersonsApi } from './persons-api';
import { VenuesApi } from './venues-api';

const config = new Configuration({
  basePath: process.env.REACT_APP_API_BASE || 'http://localhost:8080',
});

const _tickets = new TicketsApi(config);
const _events = new EventsApi(config);
const _persons = new PersonsApi(config);
const _venues = new VenuesApi(config);

export const unwrap = (axiosResponse) => {
  const d = axiosResponse?.data;
  if (
    d &&
    typeof d === 'object' &&
    ('data' in d || 'status' in d || 'message' in d || 'title' in d)
  ) {
    return d.data !== undefined ? d.data : d;
  }
  return d;
};

const callFirst = (obj, names, ...args) => {
  for (const n of names) {
    const fn = obj?.[n];
    if (typeof fn === 'function') return fn.apply(obj, args);
  }
  throw new Error(`None of methods found: ${names.join(', ')}`);
};

export const ticketsApi = {
  add: (payload) => callFirst(_tickets, ['addTicket'], payload).then(unwrap),
  getById: (id) => callFirst(_tickets, ['getTicketById'], id).then(unwrap),
  list: () => callFirst(_tickets, ['getTickets']).then(unwrap),
  update: (id, payload) =>
    callFirst(
      _tickets,
      ['updateTicket', 'updateTicketId', 'update_ticket', 'updateTicket1'],
      id,
      payload
    ).then(unwrap),
  delete: (id) => callFirst(_tickets, ['deleteTicket'], id).then(unwrap),
  deleteByComment: (commentEq) => callFirst(_tickets, ['deleteByComment'], commentEq).then(unwrap),
  minEvent: () => callFirst(_tickets, ['minEventTicket']).then(unwrap),
  countCommentLess: (comment) => callFirst(_tickets, ['countCommentLess'], comment).then(unwrap),
  sell: (body) => callFirst(_tickets, ['sellTicket'], body).then(unwrap),
  cloneVip: (body) => callFirst(_tickets, ['cloneVip'], body).then(unwrap),
};

export const eventsApi = {
  add: (payload) => callFirst(_events, ['addEvent'], payload).then(unwrap),
  list: () => callFirst(_events, ['getEvents']).then(unwrap),
};

export const personsApi = {
  add: (payload) => callFirst(_persons, ['addPerson'], payload).then(unwrap),
  list: () => callFirst(_persons, ['getPersons']).then(unwrap),
};

export const venuesApi = {
  add: (payload) => callFirst(_venues, ['addVenue'], payload).then(unwrap),
  list: () => callFirst(_venues, ['getVenues']).then(unwrap),
};
