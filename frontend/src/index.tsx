import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';
import axios from "axios";

// from .env.development or .env.production
axios.defaults.baseURL = process.env.REACT_APP_API_URL

// Dev-only: send HTTP Basic credentials so the shop keeps working with the secured API.
// (Shipping admin credentials in a public SPA is a teaching simplification, not production-safe.)
axios.defaults.auth = {username: "admin", password: "admin"}

const root = ReactDOM.createRoot(
  document.getElementById('root') as HTMLElement
);
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
