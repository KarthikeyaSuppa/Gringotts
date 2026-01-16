import React from 'react';
import { useNavigate } from 'react-router-dom';
import './App.css'; 

const TransactionButton = () => {
  const navigate = useNavigate();

  return (
    // 'interactive' class enables the hover animation
    <div className="swipe-container interactive" onClick={() => navigate('/pay')} style={{marginBottom: 20}}>
      <div className="swipe-left" style={{ backgroundColor: '#2a2a2a' }}> {/* Dark Machine Background */}
        
        <div className="swipe-card" style={{ backgroundColor: '#d4af37' }}> {/* Gold Card */}
          <div className="swipe-card-line" style={{ backgroundColor: '#f7ef8a' }} />
          <div className="swipe-buttons" />
        </div>

        <div className="swipe-post">
          <div className="swipe-post-line" />
          <div className="swipe-screen">
            <div className="swipe-dollar" style={{ color: '#d4af37' }}>$</div>
          </div>
          <div className="swipe-numbers" />
          <div className="swipe-numbers-line2" />
        </div>

      </div>
      
      <div className="swipe-right">
        <div className="swipe-text">New Transaction</div>
        <svg viewBox="0 0 451.846 451.847" className="swipe-arrow">
          <path d="M345.441 248.292L151.154 442.573c-12.359 12.365-32.397 12.365-44.75 0-12.354-12.354-12.354-32.391 0-44.744L278.318 225.92 106.409 54.017c-12.354-12.359-12.354-32.394 0-44.748 12.354-12.359 32.391-12.359 44.75 0l194.287 194.284c6.177 6.18 9.262 14.271 9.262 22.366 0 8.099-3.091 16.196-9.267 22.373z" />
        </svg>
      </div>
    </div>
  );
};

export default TransactionButton;