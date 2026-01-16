import React from 'react';
import './App.css';

const PaymentStatus = ({ 
    status = 'success',   // 'success' or 'error' (Controls the Icon ✓/X)
    type = 'default',     // 'default' (Green) or 'credit' (Blue) - Legacy prop
    theme = null,         // 'red', 'green', 'blue' (Explicit color override)
    title = null          // Custom text override
}) => {
  
  // --- 1. Determine Colors ---
  let activeColor, cardColor, dollarColor;

  // Resolve Theme
  let effectiveTheme = theme;
  if (!effectiveTheme) {
      if (status === 'error') effectiveTheme = 'red';
      else if (type === 'credit') effectiveTheme = 'blue';
      else effectiveTheme = 'green';
  }

  // Apply Colors based on Theme
  switch (effectiveTheme) {
      case 'red': // Debit / Error
          activeColor = '#ff6b6b';
          cardColor = '#ffcccc';
          dollarColor = '#b22e2e';
          break;
      case 'blue': // Credit Card
          activeColor = '#4aa3df';
          cardColor = '#aaccff';
          dollarColor = '#2e7db2';
          break;
      case 'green': // Default / Received
      default:
          activeColor = '#5de2a3';
          cardColor = '#c7ffbc';
          dollarColor = '#4b953b';
          break;
  }

  // --- 2. Determine Text & Symbol ---
  const displayMsg = title || (status === 'success' ? 'Transaction Successful' : 'Transaction Failed');
  const symbol = (effectiveTheme === 'red' && status === 'error') ? '!' : '$';
  const icon = status === 'success' ? '✓' : '✕';

  return (
    <div style={{ display: 'flex', justifyContent: 'center', width: '100%' }}>
      {/* 'autoplay' forces animation on load */}
      <div className="ps-container autoplay" style={{ cursor: 'default', margin: 0 }}>
        
        {/* Left Side: Animation */}
        <div className="ps-left-side" style={{ backgroundColor: activeColor }}>
          <div className="ps-card" style={{ backgroundColor: cardColor }}>
            <div className="ps-card-line" />
            <div className="ps-buttons" />
          </div>

          <div className="ps-post">
            <div className="ps-post-line" />
            <div className="ps-screen">
              <div className="ps-dollar" style={{ color: dollarColor }}>{symbol}</div>
            </div>
            <div className="ps-numbers" />
            <div className="ps-numbers-line2" />
          </div>
        </div>

        {/* Right Side: Text */}
        <div className="ps-right-side">
          <div className="ps-new">{displayMsg}</div>
          <div style={{ fontSize: '20px', color: '#ccc', fontWeight: 'bold' }}>
            {icon}
          </div>
        </div>

      </div>
    </div>
  );
};

export default PaymentStatus;