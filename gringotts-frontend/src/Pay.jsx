import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from './api';
import './App.css';

const Pay = () => {
    const navigate = useNavigate();
    
    // Data State
    const [accounts, setAccounts] = useState([]);
    const [selectedAccount, setSelectedAccount] = useState(null);
    const [userProfileUrl, setUserProfileUrl] = useState(null);
    
    // Form State
    const [targetAccountNumber, setTargetAccountNumber] = useState('');
    const [amount, setAmount] = useState('');
    const [targetAccountType, setTargetAccountType] = useState('SAVINGS'); 
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [msg, setMsg] = useState({ type: '', text: '' });
    
    // UI State
    const [profileExpanded, setProfileExpanded] = useState(false);

    // 1. Fetch User Accounts & Profile Image
    useEffect(() => {
        const userId = localStorage.getItem('userId');
        const storedProfile = localStorage.getItem('profileImageUrl');
        if (storedProfile) setUserProfileUrl(storedProfile);

        const fetchAccounts = async () => {
            if (!userId) return;
            try {
                const res = await api.get(`/api/accounts/by-user/${userId}`);
                if (res.data && res.data.length > 0) {
                    setAccounts(res.data);
                    setSelectedAccount(res.data[0]); 
                    // Animate pill expansion after data loads
                    setTimeout(() => setProfileExpanded(true), 500);
                }
            } catch (error) {
                console.error("Failed to load accounts", error);
            }
        };
        fetchAccounts();
    }, []);

    const handleLogout = async () => {
        try { await api.post('/api/auth/logout'); } catch (e) {}
        localStorage.clear();
        navigate('/');
    };

    // Prevent invalid chars in number input
    const blockInvalidChar = e => ['e', 'E', '+', '-'].includes(e.key) && e.preventDefault();

    // Helper: Chunk numbers for display
    const formatDigits = (value) => {
        return value ? String(value).replace(/\D/g, '').replace(/(.{4})/g, '$1 ').trim() : '';
    };

    const handleTransfer = async (e) => {
        e.preventDefault();
        if (!selectedAccount) return;
        
        setIsSubmitting(true);
        setMsg({ type: '', text: '' });

        try {
            const payload = {
                fromAccountId: selectedAccount.id,
                toAccountNumber: targetAccountNumber,
                amount: amount
            };

            await api.post('/api/transactions/transfer', payload);
            setMsg({ type: 'success', text: 'Transfer Successful!' });
            
            // Refresh balance
            const res = await api.get(`/api/accounts/by-user/${localStorage.getItem('userId')}`);
            setAccounts(res.data);
            const updated = res.data.find(a => a.id === selectedAccount.id);
            if (updated) setSelectedAccount(updated);

            // Clear inputs
            setTargetAccountNumber('');
            setAmount('');
        } catch (error) {
            setMsg({ type: 'error', text: error.response?.data || 'Transfer Failed' });
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <section id="U-Home">
            
            {/* --- RIGHT NAVBAR (Matches Dashboard) --- */}
            <div className="right-navbar show">
                <div className="nav-icon" role="button" title="Dashboard" onClick={() => navigate('/dashboard')}>
                    <img src="/img/Home.ico" alt="Home"  />
                </div>
                <div className="nav-icon" role="button" title="Settings" onClick={() => navigate('/settings')}>
                    <img src="/img/Settings.ico" alt="Settings"  />
                </div>
                {/* We keep 'Pay' active or distinct if desired, but using standard icons for now */}
                <div className="nav-icon" role="button" title="Cards" onClick={() => navigate('/cards')}>
                    <img src="/img/Card.ico" alt="Cards"  />
                </div>
                <div className="nav-icon" role="button" title="Transactions" onClick={() => navigate('/transactions')}>
                    <img src="/img/Transaction_History.ico" alt="History"  />
                </div>
                <div className="nav-icon logout-btn" role="button" title="Logout" onClick={handleLogout}>
                    <img src="/img/Logout.ico" alt="Logout"  />
                </div>
             </div>

            {/* --- PROFILE PILL (Shows Account Info) --- */}
            <div className={`profile-pill ${profileExpanded ? 'expanded account-pill' : ''}`}>
                <img src={userProfileUrl || '/img/profile.png'} alt="profile" />
                <div className="welcome-text">
                    <div className="greet">Account Number:</div>
                    <div className="name rubik-number" style={{ fontSize: '15px' }}>
                        {selectedAccount ? formatDigits(selectedAccount.accountNumber) : 'Loading...'}
                    </div>
                    <div className="greet" style={{ fontSize: '11px', opacity: 0.8 }}>
                        {selectedAccount?.accountType || ''}
                    </div>
                </div>
            </div>

            {/* --- TRANSFER FORM --- */}
            <div className="UserDetails-box pay-box">
                <form onSubmit={handleTransfer}>
                    <h2>Transfer Money</h2>

                    {/* BALANCE DISPLAY */}
                    <div className="balance-display">
                        <span className="balance-label">Available Balance</span>
                        <span className="balance-amount rubik-number">
                            ${selectedAccount?.balance?.toFixed(2) || '0.00'}
                        </span>
                    </div>

                    {/* ROW: To Account + Type */}
                    <div className="pay-row">
                        <div className="input-box">
                            <input 
                                type="text" 
                                required 
                                placeholder=" "
                                value={targetAccountNumber}
                                onChange={(e) => setTargetAccountNumber(e.target.value.replace(/\D/g, ''))}
                                minLength={6}
                            />
                            <label>To Account Number</label>
                        </div>

                        <div className="input-box dropdown-box">
                             <div className="select-wrapper">
                                <select 
                                    value={targetAccountType}
                                    onChange={(e) => setTargetAccountType(e.target.value)}
                                >
                                    <option value="SAVINGS">Savings Account</option>
                                    <option value="CHECKING">Checking Account</option>
                                </select>
                             </div>

                        </div>
                    </div>

                    {/* CENTERED: Amount */}
                    <div className="input-box centered-input">
                        <input 
                            type="number" 
                            required 
                            placeholder=" "
                            value={amount}
                            onChange={(e) => setAmount(e.target.value)}
                            onKeyDown={blockInvalidChar}
                            min="1"
                            step="0.01"
                        />
                        <label>Amount ($)</label>
                    </div>

                    {msg.text && (
                        <div className={`msg-text ${msg.type}`}>
                            {msg.text}
                        </div>
                    )}

                    <button type="submit" disabled={isSubmitting} className="c-btn">
                        {isSubmitting ? 'Sending...' : 'Transfer Funds'}
                    </button>
                </form>
            </div>
        </section>
    );
};

export default Pay;