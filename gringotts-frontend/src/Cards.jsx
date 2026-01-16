import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from './api';
import './App.css'; 

const Cards = () => {
    const navigate = useNavigate();
    
    // Data State
    const [cards, setCards] = useState([]);
    const [selectedCard, setSelectedCard] = useState(null);
    const [selectedIndex, setSelectedIndex] = useState(0);
    
    // Profile Pill State
    const [profileUrl, setProfileUrl] = useState(localStorage.getItem('profileImageUrl'));
    const [profileExpanded, setProfileExpanded] = useState(false);
    const [userInfo, setUserInfo] = useState({ 
        firstName: localStorage.getItem('firstName') || 'User' 
    });

    // Form/UI State
    const [newPin, setNewPin] = useState('');
    const [txLimit, setTxLimit] = useState('');
    
    // Bill Pay State
    const [userAccounts, setUserAccounts] = useState([]); 
    const [billAmount, setBillAmount] = useState('');
    const [paySourceId, setPaySourceId] = useState('');
    
    const [msg, setMsg] = useState({ type: '', text: '' });
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [cardAccount, setCardAccount] = useState(null);

    // Initial Load
    useEffect(() => {
        fetchCards();
        fetchUserAccounts();
        setTimeout(() => setProfileExpanded(true), 500);
    }, []);

    const fetchCards = async () => {
        const userId = localStorage.getItem('userId');
        if (!userId) return;
        try {
            const res = await api.get(`/api/cards/by-user/${userId}`);
            if (res.data && res.data.length > 0) {
                setCards(res.data);
                const current = res.data[selectedIndex] || res.data[0];
                setSelectedCard(current);
                setTxLimit(current.transactionLimit || '5000');
            }
        } catch (e) {
            console.error("Failed to fetch cards", e);
        }
    };

    const fetchUserAccounts = async () => {
        const uid = localStorage.getItem('userId');
        if(uid) {
            try {
                const res = await api.get(`/api/accounts/by-user/${uid}`);
                // Filter for source accounts (Savings/Checking) to pay bills from
                const sources = res.data.filter(a => a.accountType !== 'CREDIT');
                setUserAccounts(sources);
                if(sources.length > 0) setPaySourceId(sources[0].id);
            } catch (e) { console.warn(e); }
        }
    };

    // Update 'cardAccount' whenever selected card changes (to calculate debt)
    useEffect(() => {
        if(selectedCard && localStorage.getItem('userId')) {
            api.get(`/api/accounts/by-user/${localStorage.getItem('userId')}`)
               .then(res => {
                   const acc = res.data.find(a => a.id === selectedCard.accountId);
                   setCardAccount(acc);
               }).catch(e => console.warn(e));
        }
    }, [selectedCard]);

    const handleSwitchCard = (index) => {
        setSelectedIndex(index);
        const card = cards[index];
        setSelectedCard(card);
        setTxLimit(card.transactionLimit || '');
        setMsg({ type: '', text: '' });
        setNewPin('');
    };

    const handleUpdate = async (e) => {
        e.preventDefault();
        if(!selectedCard) return;
        setIsSubmitting(true);
        setMsg({ type: '', text: '' });

        try {
            const payload = {};
            if (newPin.length === 4) payload.pin = newPin;
            if (txLimit) payload.limit = txLimit;

            await api.put(`/api/cards/${selectedCard.id}/settings`, payload);
            setMsg({ type: 'success', text: 'Settings Updated Successfully' });
            setNewPin(''); 
            fetchCards();
        } catch (error) {
            setMsg({ type: 'error', text: error.response?.data || 'Update Failed' });
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleApplyCredit = async () => {
        if(!window.confirm("Apply for a Gringotts Platinum Credit Card?")) return;
        
        setIsSubmitting(true);
        try {
            const userId = localStorage.getItem('userId');
            await api.post('/api/cards/credit', { userId });
            alert("Congratulations! Your Credit Card has been approved.");
            fetchCards();
        } catch (error) {
            alert(error.response?.data || "Application Failed");
        } finally {
            setIsSubmitting(false);
        }
    };

    const handlePayBill = async (e) => {
        e.preventDefault();
        setIsSubmitting(true);
        try {
            await api.post('/api/cards/pay-bill', {
                userId: localStorage.getItem('userId'),
                cardId: selectedCard.id,
                creditAccountId: selectedCard.accountId,
                amount: billAmount,
                sourceAccountId: paySourceId
            });
            alert("Payment Successful!");
            setBillAmount('');
            // Refresh to update balances
            fetchCards(); 
            fetchUserAccounts(); // Refresh source account balance too
            
            // Also refresh the account balance used for calculation
            const uid = localStorage.getItem('userId');
            const res = await api.get(`/api/accounts/by-user/${uid}`);
            const acc = res.data.find(a => a.id === selectedCard.accountId);
            setCardAccount(acc);
            
        } catch (err) {
            alert(err.response?.data || "Payment Failed");
        } finally {
            setIsSubmitting(false);
        }
    };

    // Helper to chunk card number
    const formatCardNumber = (num) => num ? num.replace(/(.{4})/g, '$1 ').trim() : '•••• •••• •••• ••••';

    const isCredit = selectedCard?.cardType === 'CREDIT';
    
    // Calculate Debt: For credit accounts, negative balance = debt.
    const currentDebt = cardAccount ? Math.abs(Math.min(0, cardAccount.balance)) : 0;
    const availableCredit = selectedCard?.creditLimit ? selectedCard.creditLimit - currentDebt : 0;

    // Find the currently selected source account object
    const selectedSourceAccount = userAccounts.find(a => String(a.id) === String(paySourceId));

    return (
        <section id="U-Home">
            
             {/* Right Navbar */}
             <div className="right-navbar show">
                <div className="nav-icon" title="Dashboard" onClick={() => navigate('/dashboard')}>
                    <img src="/img/Home.ico" alt="Home" />
                </div>
                <div className="nav-icon" role="button" title="Settings" onClick={() => navigate('/settings')}>
                    <img src="/img/Settings.ico" alt="Settings"  />
                </div>
                <div className="nav-icon" title="Pay" onClick={() => navigate('/pay')}>
                    <img src="/img/Pay.ico" alt="Pay" /> 
                </div>
                <div className="nav-icon" title="Transactions" onClick={() => navigate('/transactions')}>
                    <img src="/img/Transaction_History.ico" alt="History" />
                </div>
                <div className="nav-icon logout-btn" title="Logout" onClick={() => { localStorage.clear(); navigate('/'); }}>
                    <img src="/img/Logout.ico" alt="Logout" />
                </div>
            </div>

            {/* Profile Pill (Top Left) */}
            <div className={`profile-pill ${profileExpanded ? 'expanded' : ''}`} style={{top: 20, left: 20}}>
                <img src={profileUrl || '/img/profile.png'} alt="profile" />
                <div className="welcome-text">
                    {/* ✅ CONDITIONAL DISPLAY: Show Balance if in Credit Mode, else Name */}
                    {isCredit && selectedSourceAccount ? (
                         <>
                            <div className="greet" style={{color: '#90ee90', opacity: 1}}>Avail. Bal:</div>
                            <div className="name rubik-number" style={{fontSize: 15}}>
                                ${selectedSourceAccount.balance.toLocaleString()}
                            </div>
                         </>
                    ) : (
                        <>
                            <div className="greet">Hello,</div>
                            <div className="name">{userInfo.firstName}</div>
                        </>
                    )}
                </div>
            </div>

            <div className="cards-page-container">
                
                {/* --- LEFT SIDE: 3D VIRTUAL CARD --- */}
                <div className="card-visual-section">
                    
                    {/* CARD SWITCHER ICONS */}
                    {cards.length > 0 && (
                        <div className="card-switcher">
                            {cards.map((card, idx) => (
                                <div 
                                    key={card.id} 
                                    className={`mini-card ${idx === selectedIndex ? 'active' : ''}`}
                                    onClick={() => handleSwitchCard(idx)}
                                    title={`${card.cardType} - ${card.cardNumber.slice(-4)}`}
                                >
                                    <span style={{fontSize: 10}}>{card.cardType === 'CREDIT' ? 'CR' : 'DB'}</span>
                                    <span style={{fontSize: 8}}>{card.cardNumber.slice(-4)}</span>
                                </div>
                            ))}
                        </div>
                    )}

                    <div className="glass-card-wrapper">
                        <div className="glass-card">
                            
                            {/* FRONT SIDE */}
                            <div className={`card-face card-front ${isCredit ? 'credit-skin' : 'debit-skin'}`}>
                                <div className="card-border-glow"></div>
                                <div className="card-content">
                                    <div className="card-top">
                                        <span className="card-chip">
                                            <img src="/img/chip.png" alt="chip" onError={(e) => e.target.style.display='none'} />
                                        </span>
                                        <div style={{textAlign: 'right'}}>
                                            <span className="bank-logo">Gringotts</span>
                                            <div className="card-type-label">{isCredit ? 'PLATINUM CREDIT' : 'DEBIT'}</div>
                                        </div>
                                    </div>
                                    <div className="card-number">
                                        {formatCardNumber(selectedCard?.cardNumber)}
                                    </div>
                                    <div className="card-details">
                                        <div className="holder">
                                            <span>Card Holder</span>
                                            <strong>{localStorage.getItem('firstName') || 'USER'}</strong>
                                        </div>
                                        <div className="expiry">
                                            <span>Expires</span>
                                            <strong>{selectedCard?.expiry || 'MM/YY'}</strong>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            {/* BACK SIDE */}
                            <div className="card-face card-back">
                                <div className="magnetic-strip"></div>
                                <div className="signature-strip">Authorized Signature</div>
                                <div className="cvv-box">
                                    <span>CVV</span>
                                    <div className="cvv-text">{selectedCard?.cvv || '•••'}</div>
                                </div>
                                <div className="back-text">
                                    This card is property of Gringotts Wizarding Bank. 
                                    Use subject to terms and goblin laws.
                                </div>
                            </div>

                        </div>
                    </div>
                    <p className="hint-text">Hover card to flip</p>

                    {/* APPLY FOR CREDIT CARD BUTTON */}
                    {!cards.some(c => c.cardType === 'CREDIT') && (
                        <div style={{textAlign: 'center', marginTop: 30}}>
                            <button className="apply-cc-btn" onClick={handleApplyCredit} disabled={isSubmitting}>
                                ✨ Apply for Platinum Credit Card
                            </button>
                        </div>
                    )}
                </div>

                {/* --- RIGHT SIDE: SETTINGS FORM --- */}
                <div className="card-settings-box">
                    <h2>{isCredit ? 'Credit Management' : 'Debit Settings'}</h2>
                    
                    {isCredit ? (
                        <>
                            {/* CREDIT CARD SPECIFIC UI */}
                            <div className="credit-dashboard">
                                <div className="cd-row">
                                    <div>
                                        <span>Total Due</span>
                                        <div className="rubik-number big-limit" style={{color: '#ff6b6b'}}>
                                            ${currentDebt.toLocaleString()}
                                        </div>
                                    </div>
                                    <div style={{textAlign: 'right'}}>
                                        <span>Available</span>
                                        <div className="rubik-number big-limit" style={{color: '#90ee90'}}>
                                            ${availableCredit.toLocaleString()}
                                        </div>
                                    </div>
                                </div>
                                
                                <div className="billing-info">
                                    Billing Date: <strong>15th {new Date().toLocaleString('default', { month: 'long' })}</strong>
                                    <br/>
                                    <span style={{fontSize: '0.8em', opacity: 0.7}}>Interest Rate: 3% on overdue amount</span>
                                </div>

                                <div className="pay-bill-section">
                                    <h3>Make a Payment</h3>
                                    <form onSubmit={handlePayBill}>
                                        <div className="input-box" style={{marginBottom: 15}}>
                                            <input 
                                                type="number" 
                                                placeholder=" " 
                                                value={billAmount}
                                                onChange={e => setBillAmount(e.target.value)}
                                                max={currentDebt}
                                            />
                                            <label>Amount ($)</label>
                                        </div>
                                        
                                        {/* ✅ FIXED DROPDOWN: Label overlap fixed via CSS, Balance removed from text */}
                                        <div className="input-box dropdown-box">
                                            <div className="select-wrapper">
                                                <select value={paySourceId} onChange={e => setPaySourceId(e.target.value)}>
                                                    {userAccounts.map(acc => (
                                                        <option key={acc.id} value={acc.id}>
                                                            {acc.accountType} (..{acc.accountNumber.slice(-4)})
                                                        </option>
                                                    ))}
                                                </select>
                                            </div>
                                            <label className="static-label">Pay From</label>
                                        </div>

                                        <button type="submit" className="c-btn" disabled={isSubmitting || currentDebt <= 0}>
                                            {isSubmitting ? 'Processing...' : 'Pay Bill'}
                                        </button>
                                    </form>
                                </div>
                            </div>
                        </>
                    ) : (
                        /* DEBIT CARD SETTINGS (Restored Full) */
                        <form onSubmit={handleUpdate}>
                            
                            <div className="setting-row">
                                <div className="input-box">
                                    <input 
                                        type="text" 
                                        placeholder=" " 
                                        value={newPin}
                                        onChange={(e) => setNewPin(e.target.value.replace(/\D/g,'').slice(0,4))}
                                        maxLength={4}
                                    />
                                    <label>Set New PIN (4 Digits)</label>
                                </div>
                            </div>

                            <div className="setting-row">
                                 <div className="input-box">
                                    <input 
                                        type="number" 
                                        placeholder=" "
                                        value={txLimit}
                                        onChange={(e) => setTxLimit(e.target.value)} 
                                    />
                                    <label>Daily Transaction Limit ($)</label>
                                </div>
                            </div>

                            <div className="limit-info">
                                Current Daily Limit: <strong>${selectedCard?.transactionLimit || '5000'}</strong>
                            </div>

                            {msg.text && (
                                <div className={`msg-text ${msg.type}`} style={{marginBottom: 15}}>
                                    {msg.text}
                                </div>
                            )}

                            <button type="submit" disabled={isSubmitting} className="c-btn">
                                {isSubmitting ? 'Updating...' : 'Save Settings'}
                            </button>
                        </form>
                    )}
                </div>

            </div>
        </section>
    );
};

export default Cards;