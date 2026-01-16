import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from './api';
import './App.css';

const Settings = () => {
    const navigate = useNavigate();
    const userId = localStorage.getItem('userId');
    const [activeTab, setActiveTab] = useState('profile');
    const [msg, setMsg] = useState({ type: '', text: '' });

    // --- PROFILE STATE ---
    const [profile, setProfile] = useState({
        firstName: '', lastName: '', email: '', phone: '', dob: ''
    });

    // --- CARD STATE ---
    const [cards, setCards] = useState([]);

    // --- ACCOUNT STATE ---
    const [accounts, setAccounts] = useState([]);

    // --- BENEFICIARY STATE ---
    const [beneficiaries, setBeneficiaries] = useState([]);
    const [newBen, setNewBen] = useState({ name: '', accountNumber: '', transactionLimit: '' });

    useEffect(() => {
        if (!userId) return;
        loadProfile();
        if (activeTab === 'cards') loadCards();
        if (activeTab === 'account') loadAccounts();
        if (activeTab === 'beneficiaries') loadBeneficiaries();
    }, [activeTab]);

    // --- API CALLS ---
    const loadProfile = async () => {
        const res = await api.get('/api/users/profile');
        setProfile(res.data);
    };
    const loadCards = async () => {
        const res = await api.get(`/api/cards/by-user/${userId}`);
        setCards(res.data);
    };
    const loadAccounts = async () => {
        const res = await api.get(`/api/accounts/by-user/${userId}`);
        setAccounts(res.data);
    };
    const loadBeneficiaries = async () => {
        const res = await api.get(`/api/beneficiaries/${userId}`);
        setBeneficiaries(res.data);
    };

    // --- HANDLERS ---
    
    // 1. Update Profile
    const handleProfileUpdate = async (e) => {
        e.preventDefault();
        try {
            await api.put(`/api/users/${userId}/update`, profile);
            setMsg({ type: 'success', text: 'Profile Updated!' });
        } catch (err) {
            setMsg({ type: 'error', text: 'Update Failed' });
        }
    };

    // 2. Toggle Card Status
    const toggleCard = async (card) => {
        const newStatus = card.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
        try {
            await api.put(`/api/cards/${card.id}/status?status=${newStatus}`);
            loadCards(); // Refresh
        } catch (err) {
            alert("Failed to update card status");
        }
    };

    // 3. Deactivate Account
    const deactivateAccount = async (accId) => {
        if (!window.confirm("Are you sure? This will close the account and deactivate linked cards.")) return;
        try {
            await api.delete(`/api/accounts/${accId}`);
            loadAccounts();
            alert("Account Closed Successfully");
        } catch (err) {
            alert("Failed to close account");
        }
    };

    // 4. Beneficiaries
    const addBeneficiary = async (e) => {
        e.preventDefault();
        try {
            await api.post('/api/beneficiaries/add', { ...newBen, userId });
            setNewBen({ name: '', accountNumber: '', transactionLimit: '' });
            loadBeneficiaries();
        } catch (err) {
            alert("Failed to add beneficiary");
        }
    };

    const deleteBeneficiary = async (id) => {
        try {
            await api.delete(`/api/beneficiaries/${id}`);
            loadBeneficiaries();
        } catch (err) {
            alert("Failed to delete");
        }
    };

    return (
        <section id="U-Home">
            {/* Right Navbar */}
            <div className="right-navbar show">
                <div className="nav-icon" onClick={() => navigate('/dashboard')}><img src="/img/Home.ico" alt="Home" /></div>
                <div className="nav-icon" onClick={() => navigate('/pay')}><img src="/img/Pay.ico" alt="Pay" style={{filter: 'invert(1)'}} /></div>
                <div className="nav-icon" onClick={() => navigate('/cards')}><img src="/img/Card.ico" alt="Cards" /></div>
                <div className="nav-icon" onClick={() => navigate('/transactions')}><img src="/img/Transaction_History.ico" alt="History" /></div>
                {/* Active Settings Icon */}
                <div className="nav-icon" style={{background: 'rgba(255,255,255,0.3)'}}><img src="/img/Settings.ico" alt="Settings" /></div>
                <div className="nav-icon logout-btn" onClick={() => { localStorage.clear(); navigate('/'); }}><img src="/img/Logout.ico" alt="Logout" /></div>
            </div>

            <div className="UserDetails-box settings-container">
                <h2>Settings</h2>
                
                {/* TABS */}
                <div className="settings-tabs">
                    {['profile', 'cards', 'account', 'beneficiaries'].map(tab => (
                        <button 
                            key={tab}
                            className={`tab-btn ${activeTab === tab ? 'active' : ''}`}
                            onClick={() => { setActiveTab(tab); setMsg({type:'', text:''}); }}
                        >
                            {tab.charAt(0).toUpperCase() + tab.slice(1)}
                        </button>
                    ))}
                </div>

                <div className="settings-content">
                    
                    {/* --- PROFILE TAB --- */}
                    {activeTab === 'profile' && (
                        <form onSubmit={handleProfileUpdate} className="settings-form">
                            <div className="input-group">
                                <label>First Name</label>
                                <input type="text" value={profile.firstName || ''} onChange={e => setProfile({...profile, firstName: e.target.value})} />
                            </div>
                            <div className="input-group">
                                <label>Last Name</label>
                                <input type="text" value={profile.lastName || ''} onChange={e => setProfile({...profile, lastName: e.target.value})} />
                            </div>
                            <div className="input-group">
                                <label>Email</label>
                                <input type="email" value={profile.email || ''} onChange={e => setProfile({...profile, email: e.target.value})} />
                            </div>
                            <div className="input-group">
                                <label>Phone</label>
                                <input type="text" value={profile.phone || ''} onChange={e => setProfile({...profile, phone: e.target.value})} />
                            </div>
                            <div className="input-group">
                                <label>Date of Birth</label>
                                <input type="date" value={profile.dob || ''} onChange={e => setProfile({...profile, dob: e.target.value})} />
                            </div>
                            {msg.text && <p className={msg.type === 'error' ? 'red-text' : 'green-text'}>{msg.text}</p>}
                            <button className="c-btn">Save Changes</button>
                        </form>
                    )}

                    {/* --- CARDS TAB --- */}
                    {activeTab === 'cards' && (
                        <div className="card-list">
                            {cards.map(card => (
                                <div key={card.id} className="settings-item">
                                    <div className="item-info">
                                        <span className="item-title">{card.cardType} Card</span>
                                        <span className="item-sub">Ends in {card.cardNumber.slice(-4)}</span>
                                    </div>
                                    <div className="item-action">
                                        <span className={card.status === 'ACTIVE' ? 'green-text' : 'red-text'} style={{marginRight: 10}}>
                                            {card.status}
                                        </span>
                                        <button 
                                            className={`toggle-btn ${card.status === 'ACTIVE' ? 'on' : 'off'}`}
                                            onClick={() => toggleCard(card)}
                                        >
                                            {card.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}

                    {/* --- ACCOUNT TAB --- */}
                    {activeTab === 'account' && (
                        <div className="account-list">
                            {accounts.map(acc => (
                                <div key={acc.id} className="settings-item">
                                    <div className="item-info">
                                        <span className="item-title">{acc.accountType} Account</span>
                                        <span className="item-sub">{acc.accountNumber}</span>
                                    </div>
                                    <div className="item-action">
                                        {acc.status === 'CLOSED' ? (
                                            <span className="red-text">CLOSED</span>
                                        ) : (
                                            <button className="danger-btn" onClick={() => deactivateAccount(acc.id)}>
                                                Close Account
                                            </button>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}

                    {/* --- BENEFICIARIES TAB --- */}
                    {activeTab === 'beneficiaries' && (
                        <div className="ben-section">
                            <form onSubmit={addBeneficiary} className="ben-form">
                                <input placeholder="Name" required value={newBen.name} onChange={e => setNewBen({...newBen, name: e.target.value})} />
                                <input placeholder="Account Number" required value={newBen.accountNumber} onChange={e => setNewBen({...newBen, accountNumber: e.target.value})} />
                                <input placeholder="Limit" type="number" required value={newBen.transactionLimit} onChange={e => setNewBen({...newBen, transactionLimit: e.target.value})} />
                                <button type="submit" className="c-btn" style={{width: 'auto', margin: 0}}>Add</button>
                            </form>
                            <div className="ben-list">
                                <div className="ben-header">
                                    <span>Name</span><span>Account</span><span>Limit</span><span>Action</span>
                                </div>
                                {beneficiaries.map(ben => (
                                    <div key={ben.id} className="ben-row">
                                        <span>{ben.name}</span>
                                        <span>{ben.accountNumber}</span>
                                        <span>${ben.transactionLimit}</span>
                                        <button className="delete-icon" onClick={() => deleteBeneficiary(ben.id)}>🗑</button>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}

                </div>
            </div>
        </section>
    );
};

export default Settings;