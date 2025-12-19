import { useState, useEffect, useRef } from 'react';
import api from './api';
import { useNavigate } from 'react-router-dom';

const UserDetails = () => {
    const navigate = useNavigate();
    const formRef = useRef(null);
    const profileRef = useRef(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [showPinModal, setShowPinModal] = useState(false);
    const [createdCard, setCreatedCard] = useState(null);
    const [showRetryModal, setShowRetryModal] = useState(false);
    const [lastAccountId, setLastAccountId] = useState(null);

    // 1. Get User ID
    const [userId, setUserId] = useState(null); 
    useEffect(() => {
        const storedId = localStorage.getItem('userId');
        if (storedId) {
            setUserId(storedId);
        } else {
            console.log("No User ID found - defaulting to 1");
            setUserId(1); 
        }
        // If there's a previously uploaded profile image URL, show it as preview
        const storedProfile = localStorage.getItem('profileImageUrl');
        if (storedProfile) setPreviewUrl(storedProfile);
    }, []);

    // 2. State for Text Data
    const [formData, setFormData] = useState({
        firstName: '', lastName: '', phoneNumber: '', address: ''
    });

    // 3. State for Image Preview
    const [previewUrl, setPreviewUrl] = useState(null);

    // 4. NEW: State for Animation Trigger
    const [iconExiting, setIconExiting] = useState(false);
    const [boxExiting, setBoxExiting] = useState(false);

    // Format numeric strings into groups of 'group' digits (defaults to 4)
    const formatDigits = (value, group = 4) => {
        if (!value && value !== 0) return value;
        const digits = String(value).replace(/\D/g, '');
        if (!digits) return value;
        return digits.replace(new RegExp(`(.{${group}})`, 'g'), '$1 ').trim();
    };

    // Helper to wait for a CSS transition/animation to finish with a fallback timeout
    const waitForAnimation = (el, timeout = 1600) => new Promise((resolve) => {
        if (!el) return setTimeout(resolve, timeout);
        let done = false;
        const onEnd = (ev) => {
            if (done) return;
            done = true;
            el.removeEventListener('transitionend', onEnd);
            el.removeEventListener('animationend', onEnd);
            resolve();
        };
        el.addEventListener('transitionend', onEnd);
        el.addEventListener('animationend', onEnd);
        // safety fallback
        setTimeout(() => { if (!done) { done = true; resolve(); } }, timeout + 300);
    });

    // --- LOGIC 1: HANDLE TEXT CHANGES ---
    const handleInputChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    // --- LOGIC 2: IMMEDIATE IMAGE UPLOAD & DISPLAY ---
    const handleImageChange = async (e) => {
        const file = e.target.files[0];
        
        if (file) {
            // A. Immediate Visual Feedback
            setPreviewUrl(URL.createObjectURL(file)); 

            // B. Immediate Backend Upload
            if (userId) {
                // Ensure there's an auth token — image upload requires auth
                const token = localStorage.getItem('token');
                if (!token) {
                    alert('You must be logged in to upload an image. Please login and try again.');
                    return;
                }
                const authHeaders = { Authorization: `Bearer ${token}` };
                 const imageData = new FormData();
                 imageData.append("file", file);

                 try {
                    const resp = await api.post(`/api/users/${userId}/image`, imageData, {
                        headers: { "Content-Type": "multipart/form-data", ...authHeaders }
                    });
                    console.log("Image auto-uploaded successfully!", resp?.data);
                    // If API returned filename, build public URL and store it
                    const filename = resp?.data?.profileImageUrl;
                    if (filename) {
                        const backendBase = api.defaults?.baseURL || 'http://localhost:8050';
                        const full = `${backendBase.replace(/\/$/, '')}/uploads/${filename}`;
                        localStorage.setItem('profileImageUrl', full);
                        console.debug('[UserDetails] stored profileImageUrl', full);
                    }
                 } catch (error) {
                    console.error("Image upload failed", error?.response || error);
                     alert("Failed to upload image.");
                 }
             }
         }
     };

    // --- LOGIC 3: ANIMATION + SUBMIT ---
    const handleContinue = async (e) => {
        e.preventDefault(); 

        if (!userId) {
            alert("User ID missing");
            return;
        }

        if (isSubmitting) return; // prevent double submit
        setIsSubmitting(true);

     // Start animation immediately
    // 1. Trigger Icon
    setIconExiting(true);
    // 2. Wait 1.5s
    await new Promise(resolve => setTimeout(resolve, 1500));
    // 3. Trigger Box
    setBoxExiting(true);
    // 4. Wait 1.5s
    await new Promise(resolve => setTimeout(resolve, 1500));
    // 5. Do not navigate yet — wait for API calls to finish. Navigation happens after success or when PIN modal closed.

        // Prepare form data
        try {
             // 1) Update user details
            // Ensure token exists before calling protected endpoint
            const token = localStorage.getItem('token');
            if (!token) { throw { message: 'No auth token, please login', response: { status: 401 } }; }
            const authHeaders = { Authorization: `Bearer ${token}` };
            const updateRes = await api.put(`/api/users/${userId}`, formData, { headers: { ...authHeaders } });
               console.log('User update response', updateRes?.data);
             // If backend returned profileImageUrl, save it to localStorage for Dashboard
             const returnedImage = updateRes?.data?.profileImageUrl;
             if (returnedImage) {
                const backendBase = api.defaults?.baseURL || 'http://localhost:8050';
                const full = `${backendBase.replace(/\/$/, '')}/uploads/${returnedImage}`;
                localStorage.setItem('profileImageUrl', full);
                console.debug('[UserDetails] stored profileImageUrl from update', full);
             }

            // 2) Create account for user
            // Backend expects POST /api/accounts/{userId}
            const accRes = await api.post(`/api/accounts/${userId}`, { accountType: 'SAVINGS' }, { headers: { ...authHeaders } });
            const account = accRes?.data;
            if (!account || !account.accountNumber) throw new Error('Account creation failed or missing accountNumber');

            // 3) Create card for account
            const accountId = account.id || account.accountId || account.account_id;
            const cardPayload = { accountId };
            let cardRes;
            try {
                cardRes = await api.post('/api/cards', cardPayload, { headers: { ...authHeaders } });
            } catch (cardErr) {
                // Attempt rollback: delete the account we created
                try {
                    if (accountId) await api.delete(`/api/accounts/${accountId}`);
                } catch (delErr) {
                    console.error('Rollback failed', delErr);
                }
                // Show Retry modal to allow the user to retry creating the card manually
                setLastAccountId(accountId);
                setShowRetryModal(true);
                setIsSubmitting(false);
                return; // Stop further error throws so UI can handle retry
            }
            const card = cardRes?.data;
            if (!card || !card.cardNumber) throw new Error('Card creation failed or missing cardNumber');

            // Save last account id for potential retry or UX
            setLastAccountId(accountId);

            // Persist account & card for Dashboard to read
            try { localStorage.setItem('gr_account', JSON.stringify(account)); } catch (e) { console.warn('persist account failed', e); }
            try { localStorage.setItem('gr_card', JSON.stringify(card)); } catch (e) { console.warn('persist card failed', e); }

            // Show PIN modal to the user (one-time display)
            setCreatedCard(card);
            setShowPinModal(true);

            // Mark profile as completed for future logins
            try { localStorage.setItem('needsProfile', '0'); } catch (e) { }

            // Wait for the animation to complete on the form/profile element
            const el = document.querySelector('.UserDetails-box') || document.querySelector('.user-icon-placeholder');
            await waitForAnimation(el, 1600);

            // If we show the PIN modal, navigation will happen when the user acknowledges it
            if (!showPinModal) navigate('/dashboard');

        } catch (error) {
            console.error('Error in Continue flow:', error?.response || error);
            // If it's a 403/401, likely missing token — ask user to re-login
            const status = error?.response?.status;
            if (status === 401 || status === 403) {
                alert('Authorization error. Please login again.');
                // Optionally clear token and redirect to login
                localStorage.removeItem('token');
                setIconExiting(false);
                setBoxExiting(false);
                setIsSubmitting(false);
                navigate('/');
                return;
            }
            // If backend returned a clear message, show it
            const backendMsg = error?.response?.data?.error || error?.response?.data || error?.message;
            alert('Failed to complete setup: ' + (backendMsg || 'See console'));
            setBoxExiting(false);
            setIconExiting(false);
            setIsSubmitting(false);
        }
    };

    // Retry create card for the last account id
    const retryCreateCard = async () => {
        if (!lastAccountId) return;
        setIsSubmitting(true);
        try {
            const cardRes = await api.post('/api/cards', { accountId: lastAccountId });
            const card = cardRes?.data;
            if (!card || !card.cardNumber) throw new Error('Card creation failed');
            localStorage.setItem('gr_card', JSON.stringify(card));
            setCreatedCard(card);
            setShowRetryModal(false);
            setShowPinModal(true);
        } catch (err) {
            alert('Retry failed: ' + (err?.response?.data || err.message || err));
        } finally {
            setIsSubmitting(false);
        }
    };

    const onPinModalClose = () => {
        setShowPinModal(false);
        // Remove temp pin from localStorage for safety? keep card details but clear tempPin
        try {
            const c = JSON.parse(localStorage.getItem('gr_card') || '{}');
            if (c) { delete c.tempPin; localStorage.setItem('gr_card', JSON.stringify(c)); }
        } catch (e) { }
        navigate('/dashboard');
    };

    return (
        <section id="U-Home">
            
            {/* --- NEW: RIGHT NAVBAR (Hidden until exit animation starts) --- */}
            <div className={`right-navbar ${boxExiting ? 'show' : ''}`}>
                <div className="nav-icon">🃏</div>
                <div className="nav-icon">⚙️</div> 
                <div className="nav-icon">🔔</div> 
                <div className="nav-icon logout-btn">🛑</div> 
            </div>

            {/* --- FORM BOX (Shrinks when isExiting is true) --- */}
            <div ref={formRef} className={`UserDetails-box ${boxExiting ? 'minimized' : ''}`}>
                <form onSubmit={handleContinue}>
                    
                    <div className="user-image-upload">
                        <input 
                            type="file" 
                            id="userImageInput" 
                            accept="image/*" 
                            style={{ display: 'none' }} 
                            onChange={handleImageChange}
                        />    
                        {/* --- PROFILE ICON (Flies to corner when isExiting is true) --- */}
                        <label 
                            htmlFor="userImageInput" 
                            ref={profileRef}
                            className={`user-icon-placeholder ${iconExiting ? 'moving-to-corner' : ''}`}
                        >
                            {previewUrl ? (
                                <img id="uploadedImagePreview" src={previewUrl} alt="Preview" />
                            ) : (
                                <span id="uploadIcon"> 
                                    <img src="/img/profile.png" alt="Upload" /> 
                                </span>
                            )}
                        </label>
                    </div>

                    <h2>Personal Information</h2>

                    <div className="name-container">
                        <div className="input-box">
                            <input 
                                type="text" name="firstName" required placeholder=" "
                                value={formData.firstName} onChange={handleInputChange} 
                            />
                            <label>Firstname</label>
                        </div>
                        <div className="input-box">
                            <input 
                                type="text" name="lastName" required placeholder=" "
                                value={formData.lastName} onChange={handleInputChange} 
                            />
                            <label>Lastname</label>
                        </div>
                    </div>

                    <div className="contact-container">
                        <div className="input-box">
                            <input 
                                type="tel" name="phoneNumber" id="phoneNumber" required placeholder=" "
                                value={formData.phoneNumber} onChange={handleInputChange} 
                            />
                            <label>Phone number</label>
                        </div>

                        <div className="input-box">
                            <textarea 
                                id="address" name="address" required placeholder=" "
                                value={formData.address} onChange={handleInputChange} 
                            ></textarea>
                            <label>Address</label>
                        </div>
                    </div>
                    
                    <button type="submit" disabled={isSubmitting} className="c-btn">{isSubmitting ? 'Processing...' : 'Continue'}</button>
                </form>
            </div>

            {/* PIN Modal (display one-time temp PIN) */}
            {showPinModal && createdCard && (
                <div className="modal-overlay">
                    <div className="modal">
                        <h3>Card Created</h3>
                        <p>Card Number: {formatDigits(createdCard.cardNumber)}</p>
                        <p>Temporary PIN: <strong>{createdCard.tempPin}</strong></p>
                        <p>CVV: {createdCard.cvv} • Expiry: {createdCard.expiry}</p>
                        <button onClick={onPinModalClose} className="c-btn">Continue</button>
                    </div>
                </div>
            )}

            {/* Retry Modal */}
            {showRetryModal && (
                <div className="modal-overlay">
                    <div className="modal">
                        <h3>Card creation failed</h3>
                        <p>Would you like to retry creating the card for your account?</p>
                        <div style={{ display: 'flex', gap: 10 }}>
                            <button onClick={retryCreateCard} disabled={isSubmitting} className="c-btn">{isSubmitting ? 'Retrying...' : 'Retry'}</button>
                            <button onClick={() => { setShowRetryModal(false); navigate('/details'); }} className="btn">Cancel</button>
                        </div>
                    </div>
                </div>
            )}
        </section>
    );
};

export default UserDetails;
