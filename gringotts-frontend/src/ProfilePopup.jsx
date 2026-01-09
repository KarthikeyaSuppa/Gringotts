import React, { useEffect, useState } from 'react';
//import axios from 'axios';
import api from './api';

const ProfilePopup = ({ onClose }) => {
  const [userDetails, setUserDetails] = useState({});

  useEffect(() => {
    // ✅ Use 'api' instead of 'axios'
    // ✅ Use correct URL '/api/users/profile' (plural)
    api.get('/api/users/profile')
      .then(response => setUserDetails(response.data))
      .catch(error => console.error('Error fetching user details:', error));
  }, []);

  return (
    <div className="profile-popup">
      <div className="row">
        <span className="label">First Name:</span>
        <span className="value">{userDetails.firstName || 'N/A'}</span>
      </div>
      <div className="row">
        <span className="label">Last Name:</span>
        <span className="value">{userDetails.lastName || 'N/A'}</span>
      </div>
      <div className="row">
        <span className="label">Phone:</span>
        <span className="value">{userDetails.phone || 'N/A'}</span>
      </div>
      <div className="row">
        <span className="label">Email:</span>
        <span className="value">{userDetails.email || 'N/A'}</span>
      </div>
      <div className="row">
        <span className="label">Address:</span>
        <span className="value">{userDetails.address || 'N/A'}</span>
      </div>
      <div className="row">
        <span className="label">DOB:</span>
        <span className="value">{userDetails.dob || 'N/A'}</span>
      </div>
      <button className="edit-btn" onClick={() => window.location.href = '/user-details'}>
        Edit
      </button>
    </div>
  );
};

export default ProfilePopup;
