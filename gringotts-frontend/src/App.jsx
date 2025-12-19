import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import Login from './Login';
import Signup from './Signup';
import UserDetails from './UserDetails';
import Dashboard from './Dashboard'; // Assuming you have this, or we will create a placeholder
import './App.css'; 

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