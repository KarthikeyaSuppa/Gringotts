import { useEffect, useState } from 'react';
    import { useNavigate } from 'react-router-dom';
import api from './api';
import './App.css';

const Dashboard = () => {
    const [userId, setUserId] = useState(null);
    const [data, setData] = useState(null);
    const [profileUrl, setProfileUrl] = useState(null);
    const [profileExpanded, setProfileExpanded] = useState(false);
    const [profilePopupOpen, setProfilePopupOpen] = useState(false);
    const [userInfo, setUserInfo] = useState(null);
    const navigate = useNavigate();
    const [showAnimation, setShowAnimation] = useState(false);
    
    useEffect(() => {
        // Wait 50ms to let the DOM render the "hidden" state, then trigger the "active" state
        const timer = setTimeout(() => setShowAnimation(true), 50);
        return () => clearTimeout(timer);
    }, []);

    
    useEffect(() => {
        const storedId = localStorage.getItem('userId');
        if (storedId) setUserId(storedId);

        const storedProfile = localStorage.getItem('profileImageUrl');
        if (storedProfile) setProfileUrl(storedProfile);
    }, []);

    // If profileUrl was already stored (returning user), expand the pill and populate userInfo
    useEffect(() => {
            // Fetch user profile from the new secured endpoint
            const fetchProfile = async () => {
                try {
                    // ✅ CALL THE NEW API (No ID needed, it uses the Token)
                    const res = await api.get('/api/users/profile');

                    const data = res.data;

                    // 1. Handle Profile Image
                    if (data.profileImageUrl) {
                        const backendBase = api.defaults?.baseURL || 'http://localhost:8050';
                        const full = `${backendBase.replace(/\/$/, '')}/uploads/${data.profileImageUrl}`;
                        localStorage.setItem('profileImageUrl', full);
                        setProfileUrl(full);
                    }

                    // 2. Populate User Info for the Popup
                    setUserInfo({
                        firstName: data.firstName,
                        lastName: data.lastName,
                        phoneNumber: data.phoneNumber,
                        email: data.email,
                        address: data.address,
                        dateOfBirth: data.dateOfBirth
                    });

                    // 3. Expand the Welcome Pill
                    setTimeout(() => setProfileExpanded(true), 700);

                } catch (e) {
                    console.warn('Could not fetch profile', e);
                }
            };

            fetchProfile();
        }, []);

    useEffect(() => {
        // first try localStorage
        const storedAcc = localStorage.getItem('gr_account');
        const storedCard = localStorage.getItem('gr_card');
        if (storedAcc || storedCard) {
            try {
                const acc = storedAcc ? JSON.parse(storedAcc) : null;
                const card = storedCard ? JSON.parse(storedCard) : null;
                setData({ account: acc, card });
            } catch (e) { console.warn('Failed parse stored account/card', e); }
        }

        if (!userId) return;
        // Try to fetch account/card summary from backend
        const fetchSummary = async () => {
            try {
                const accRes = await api.get(`/api/accounts/by-user/${userId}`);
                const cardRes = await api.get(`/api/cards/by-user/${userId}`);
                // prefer stored tempPin if present
                const fetchedAccount = Array.isArray(accRes.data) ? accRes.data[0] : accRes.data;
                const fetchedCard = Array.isArray(cardRes.data) ? cardRes.data[0] : cardRes.data;
                setData(prev => ({ account: prev?.account || fetchedAccount, card: prev?.card || fetchedCard }));
            } catch (err) {
                console.warn('Could not fetch account/card summary', err?.response?.status, err?.response?.data || err.message || err);
            }
        }
        fetchSummary();
    }, [userId]);

    useEffect(() => {
        // If no profileUrl in localStorage, fetch user info from backend
        const fetchProfile = async () => {
            if (!userId) return;
            if (profileUrl) return;
            try {
                const res = await api.get(`/api/users/${userId}`);
                const url = res.data?.profileImageUrl;
                if (url) {
                    // store the full public URL (uploads served at /uploads/filename)
                    const backendBase = api.defaults?.baseURL || 'http://localhost:8050';
                    const full = `${backendBase.replace(/\/$/, '')}/uploads/${url}`;
                    localStorage.setItem('profileImageUrl', full);
                    setProfileUrl(full);
                    // store user info for popup
                    setUserInfo({
                        firstName: res.data.firstName,
                        lastName: res.data.lastName,
                        phoneNumber: res.data.phoneNumber,
                        address: res.data.address,
                        email: res.data.email,
                        dateOfBirth: res.data.dateOfBirth
                    });
                    // trigger brief delay then expand pill
                    setTimeout(() => setProfileExpanded(true), 700);
                }
            } catch (e) { console.warn('Could not fetch profile', e); }
        };
        fetchProfile();
    }, [userId, profileUrl]);

    const syncFromServer = async () => {
        if (!userId) return;
        try {
            const accRes = await api.get(`/api/accounts/by-user/${userId}`);
            const cardRes = await api.get(`/api/cards/by-user/${userId}`);
            const fetchedAccount = Array.isArray(accRes.data) ? accRes.data[0] : accRes.data;
            const fetchedCard = Array.isArray(cardRes.data) ? cardRes.data[0] : cardRes.data;
            setData({ account: fetchedAccount, card: fetchedCard });
        } catch (e) { alert('Sync failed: ' + (e?.message || e)); }
    }

    const handleLogout = async () => {
        try {
            await api.post('/api/auth/logout');
        } catch (e) {
            console.warn('Logout API call failed', e);
        }
        // Clear client-side storage and redirect
        localStorage.removeItem('token');
        localStorage.removeItem('userId');
        localStorage.removeItem('gr_account');
        localStorage.removeItem('gr_card');
        localStorage.removeItem('needsProfile');
        localStorage.removeItem('profileImageUrl');
        navigate('/');
    };

    // Format a numeric string into groups of 'group' digits separated by spaces
    const formatDigits = (value, group = 4) => {
        if (!value && value !== 0) return value;
        const digits = String(value).replace(/\D/g, '');
        if (!digits) return value;
        return digits.replace(new RegExp(`(.{${group}})`, 'g'), '$1 ').trim();
    };

    useEffect(() => {
        // If there's no auth token, redirect to login to avoid 403
        const token = localStorage.getItem('token');
        if (!token) {
            console.warn('[dashboard] No auth token present — redirecting to login');
            navigate('/');
            return;
        }
    }, [navigate]);

    return (
        <section id="U-Home"> {/* Use same background */}

            {/* Right Navbar (visible) */}
            <div className="right-navbar show">
                <div className="nav-icon" role="button" title="Settings" onClick={() => navigate('/settings')}>
                    <img src="/img/Settings.ico" alt="Settings"  />
                </div>
                <div className="nav-icon" role="button" title="Cards" onClick={() => navigate('/cards')}>
                    <img src="/img/Card.ico" alt="Cards"  />
                </div>
                <div className="nav-icon" role="button" title="Transactions" onClick={() => navigate('/transactions')}>
                    <img src="/img/Transaction_History.ico" alt="Transactions"  />
                </div>
                <div className="nav-icon" role="button" title="Pay" onClick={() => navigate('/pay')}>
                    <img src="/img/Pay.ico" alt="Pay"  />
                </div>
                <div className="nav-icon logout-btn" role="button" title="Logout" onClick={handleLogout}>
                    <img src="/img/Logout.ico" alt="Logout"  />
                </div>
             </div>

            {/* Profile Pill (circle -> expands into welcome) */}
            <div className={`profile-pill ${profileExpanded ? 'expanded' : ''}`} onClick={() => setProfilePopupOpen(p => !p)}>
                <img src={profileUrl || '/img/profile.png'} alt="profile" />
                <div className="welcome-text">
                    <div className="greet">Welcome,</div>
                    <div className="name">{userInfo?.firstName || localStorage.getItem('firstName') || 'User'}</div>
                </div>
            </div>

            {/* Profile popup with details */}
            {profilePopupOpen && (
                <div className="profile-popup">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                        <strong style={{ color: 'white' }}>
                            {userInfo?.firstName || localStorage.getItem('firstName') || 'User'} {userInfo?.lastName || ''}
                        </strong>
                        <button
                            onClick={() => setProfilePopupOpen(false)}
                            style={{ background: 'transparent', border: 'none', color: '#ccc', cursor: 'pointer' }}
                        >
                            ✕
                        </button>
                    </div>
                    <div className="row"><div className="label">First Name</div><div className="value">{userInfo?.firstName || '—'}</div></div>
                    <div className="row"><div className="label">Last Name</div><div className="value">{userInfo?.lastName || '—'}</div></div>
                    <div className="row"><div className="label">Phone</div><div className="value">{userInfo?.phoneNumber || '—'}</div></div>
                    <div className="row"><div className="label">Email</div><div className="value">{userInfo?.email || '—'}</div></div>
                    <div className="row"><div className="label">Address</div><div className="value">{userInfo?.address || '—'}</div></div>
                    <div className="row"><div className="label">DOB</div><div className="value">{userInfo?.dateOfBirth || '—'}</div></div>
                    <button
                        className="edit-btn"
                        onClick={() => navigate('/user-details', { state: { userInfo } })}
                    >
                        Edit
                    </button>
                </div>
            )}


            <div 
            className={`UserDetails-box ${showAnimation ? 'dashboard-enter-active' : 'dashboard-enter'}`} style={{ minWidth: 600 }}>
                <h2 style={{ color: 'whitesmoke', textAlign: 'center', marginTop: 10 }}>Account Summary</h2>

                <div style={{ padding: 20 }}>
                    {/* ACCOUNT ROW: Account Number | Account Type */}
                    <div style={{ display: 'flex', gap: 20, marginBottom: 12 }}>
                        <div style={{ flex: 1, background: 'rgba(255,255,255,0.04)', padding: 12, borderRadius: 8 }}>
                            <div style={{ color: '#ddd', fontSize: 12 }}>Account Number</div>
                            <div className="rubik-number" style={{ color: 'white', fontSize: 16 }}>{formatDigits(data?.account?.accountNumber) ?? '—'}</div>
                        </div>
                        <div style={{ flex: 1, background: 'rgba(255,255,255,0.04)', padding: 12, borderRadius: 8 }}>
                            <div style={{ color: '#ddd', fontSize: 12 }}>Account Type</div>
                            <div style={{ color: 'white', fontSize: 16 }}>{data?.account?.type ?? '—'}</div>
                        </div>
                    </div>

                    {/* CARD ROW: Card Number | Temporary PIN */}
                    <div style={{ display: 'flex', gap: 20, marginBottom: 12 }}>
                        <div style={{ flex: 1, background: 'rgba(255,255,255,0.04)', padding: 12, borderRadius: 8 }}>
                            <div style={{ color: '#ddd', fontSize: 12 }}>Card Number</div>
                            <div className="rubik-number" style={{ color: 'white', fontSize: 16 }}>{formatDigits(data?.card?.cardNumber) ?? '—'}</div>
                        </div>
                        <div style={{ flex: 1, background: 'rgba(255,255,255,0.04)', padding: 12, borderRadius: 8 }}>
                            <div style={{ color: '#ddd', fontSize: 12 }}>Temporary PIN</div>
                            <div className="rubik-number" style={{ color: 'white', fontSize: 16 }}>{data?.card?.tempPin ?? '—'}</div>
                        </div>
                    </div>

                    {/* CVV | Expiry */}
                    <div style={{ display: 'flex', gap: 20 }}>
                        <div style={{ flex: 1, background: 'rgba(255,255,255,0.04)', padding: 12, borderRadius: 8 }}>
                            <div style={{ color: '#ddd', fontSize: 12 }}>CVV</div>
                            <div className="rubik-number" style={{ color: 'white', fontSize: 16 }}>{data?.card?.cvv ?? '—'}</div>
                        </div>
                        <div style={{ flex: 1, background: 'rgba(255,255,255,0.04)', padding: 12, borderRadius: 8 }}>
                            <div style={{ color: '#ddd', fontSize: 12 }}>Expiry</div>
                            <div className="rubik-number" style={{ color: 'white', fontSize: 16 }}>{data?.card?.expiry ?? '—'}</div>
                        </div>
                    </div>

                </div>
            </div>

        </section>
    );
}

export default Dashboard;

