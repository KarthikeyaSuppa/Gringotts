import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import Login from './Login';
import Signup from './Signup';
import UserDetails from './UserDetails';
import Dashboard from './Dashboard'; // Assuming you have this, or we will create a placeholder
import './App.css'; 
import Pay from './Pay';
import Transactions from './Transactions';
import Cards from './Cards';
import Settings from './Settings';

// --- GUARD COMPONENT ---
// This checks if a token exists. If not, it kicks you back to Login.
const PrivateRoute = ({ children }) => {
    const token = localStorage.getItem('token');
    return token ? children : <Navigate to="/" />;
};

function App() {
  return (
    <Router>
      <Routes>
        {/* Public Routes */}
        <Route path="/" element={<Login />} />
        <Route path="/signup" element={<Signup />} />

        {/* 🔒 Protected Routes (The Wires are Connected Here) */}
        <Route 
          path="/details" 
          element={
            <PrivateRoute>
              <UserDetails />
            </PrivateRoute>
          } 
        />

        <Route 
          path="/pay" 
          element={
            <PrivateRoute>
              <Pay />
            </PrivateRoute>
          } 
        />
        <Route 
          path="/transactions" 
          element={
            <PrivateRoute>
              <Transactions />
            </PrivateRoute>
          } 
        />

        <Route 
          path="/cards" 
          element={
            <PrivateRoute>
              <Cards />
            </PrivateRoute>
          } 
        />

        <Route 
          path="/settings" 
          element={
            <PrivateRoute>
              <Settings />
            </PrivateRoute>
          } />

        <Route 
          path="/dashboard" 
          element={
            <PrivateRoute>
              <Dashboard />
            </PrivateRoute>
          } 
        />
      </Routes>
    </Router>
  );
}

export default App;