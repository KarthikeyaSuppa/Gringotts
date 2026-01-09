import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from './api';
import './App.css'; 

const Transactions = () => {
    const navigate = useNavigate();
    
    // Data
    const [transactions, setTransactions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [myAccountId, setMyAccountId] = useState(null);

    // Filters
    const [filterStart, setFilterStart] = useState('');
    const [filterEnd, setFilterEnd] = useState('');
    const [filterFlow, setFilterFlow] = useState(''); // 'SENT' or 'RECEIVED'
    const [filterType, setFilterType] = useState(''); // 'TRANSFER', 'WITHDRAWAL', etc.

    // Modal
    const [selectedTx, setSelectedTx] = useState(null);

    useEffect(() => {
        loadData();
    }, []);

    const loadData = async () => {
        setLoading(true);
        try {
            const userId = localStorage.getItem('userId');
            // 1. Get Account ID first (needed for queries)
            const accRes = await api.get(`/api/accounts/by-user/${userId}`);
            const accId = accRes.data[0]?.id; // Assuming primary account
            setMyAccountId(accId);

            if(accId) {
                // Initial Load: Fetch all or search with empty filters
                fetchTransactions(accId);
            }
        } catch (e) {
            console.error("Load failed", e);
        } finally {
            setLoading(false);
        }
    };

    const fetchTransactions = async (accId) => {
        try {
            const params = {
                accountId: accId,
                startDate: filterStart,
                endDate: filterEnd,
                flow: filterFlow,
                type: filterType
            };
            // Clean empty params
            Object.keys(params).forEach(key => !params[key] && delete params[key]);

            const res = await api.get('/api/transactions/search', { params });
            
            // Sort by Date Descending (Newest first)
            const sorted = res.data.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
            setTransactions(sorted);
        } catch (e) {
            console.error(e);
        }
    };

    const handleApplyFilters = (e) => {
        e.preventDefault();
        if(myAccountId) fetchTransactions(myAccountId);
    };

    const handleDownload = () => {
        if(!myAccountId) return;
        // Construct query string manually for window.open
        let url = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8050'}/api/transactions/download?accountId=${myAccountId}`;
        if(filterStart) url += `&startDate=${filterStart}`;
        if(filterEnd) url += `&endDate=${filterEnd}`;
        if(filterFlow) url += `&flow=${filterFlow}`;
        if(filterType) url += `&type=${filterType}`;
        
        // Need to pass token? Browser window.open won't pass header. 
        // FIX: For security, usually we use axios with blob. For this demo, let's assume we use axios.
        
        api.get(url, { responseType: 'blob' })
           .then((response) => {
               const href = window.URL.createObjectURL(response.data);
               const link = document.createElement('a');
               link.href = href;
               link.setAttribute('download', 'transactions.csv');
               document.body.appendChild(link);
               link.click();
               document.body.removeChild(link);
           })
           .catch(err => console.error("Download failed", err));
    };

    // --- Helper Logic ---

    // Is the money leaving or entering MY account?
    const isDebit = (tx) => tx.account.id === myAccountId;

    // Group by Month (e.g., "January 2026")
    const groupedTransactions = transactions.reduce((groups, tx) => {
        const date = new Date(tx.timestamp);
        const monthYear = date.toLocaleString('default', { month: 'long', year: 'numeric' });
        if (!groups[monthYear]) {
            groups[monthYear] = [];
        }
        groups[monthYear].push(tx);
        return groups;
    }, {});

    const copyToClipboard = (text) => {
        navigator.clipboard.writeText(text);
        alert("Reference ID Copied!");
    };

    // Format Date for Modal: "26-06-2026, 07:55 PM"
    const formatFullDate = (isoString) => {
        const d = new Date(isoString);
        const datePart = d.toLocaleDateString('en-GB').replace(/\//g, '-'); // 26-06-2026
        const timePart = d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
        return `${datePart}, ${timePart}`;
    };

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
                <div className="nav-icon" role="button" title="Cards" onClick={() => navigate('/cards')}>
                    <img src="/img/Card.ico" alt="Cards"  />
                </div>
                <div className="nav-icon" title="Pay" onClick={() => navigate('/pay')}>
                    <img src="/img/Pay.ico" alt="Pay" /> 
                </div>
                <div className="nav-icon logout-btn" title="Logout" onClick={() => { localStorage.clear(); navigate('/'); }}>
                    <img src="/img/Logout.ico" alt="Logout" />
                </div>
            </div>

            <div className="UserDetails-box tx-container">
                <div className="tx-header">
                    <h2>Transactions</h2>
                    
                    {/* FILTERS BAR */}
                    <div className="filter-bar">
                        <input type="date" value={filterStart} onChange={e => setFilterStart(e.target.value)} />
                        <span style={{color:'white'}}>-</span>
                        <input type="date" value={filterEnd} onChange={e => setFilterEnd(e.target.value)} />
                        
                        <select value={filterFlow} onChange={e => setFilterFlow(e.target.value)}>
                            <option value="">All Flows</option>
                            <option value="SENT">Sent (Dr)</option>
                            <option value="RECEIVED">Received (Cr)</option>
                        </select>

                        <select value={filterType} onChange={e => setFilterType(e.target.value)}>
                            <option value="">All Types</option>
                            <option value="TRANSFER">Transfer</option>
                            <option value="WITHDRAWAL">Withdrawal</option>
                            <option value="DEPOSIT">Deposit</option>
                        </select>

                        <button onClick={handleApplyFilters} className="filter-btn">Filter</button>
                        <button onClick={handleDownload} className="filter-btn download-btn">⬇ CSV</button>
                    </div>
                </div>

                {/* TABLE HEADER */}
                <div className="tx-table-header">
                    <div className="col">Ref ID</div>
                    <div className="col">Type</div>
                    <div className="col">Date</div>
                    <div className="col">Amount</div>
                    <div className="col">Tag</div>
                </div>

                {/* SCROLLABLE LIST */}
                <div className="tx-list">
                    {Object.keys(groupedTransactions).map(month => (
                        <div key={month} className="month-group">
                            <div className="month-label">{month}</div>
                            {groupedTransactions[month].map(tx => {
                                const debit = isDebit(tx);
                                return (
                                    <div key={tx.id} className="tx-row" onClick={() => setSelectedTx(tx)}>
                                        <div className="col ref-col">{tx.referenceId.substring(0, 8)}...</div>
                                        <div className="col">
                                            <span className={`badge ${debit ? 'badge-sent' : 'badge-received'}`}>
                                                {debit ? 'Sent' : 'Received'}
                                            </span>
                                        </div>
                                        <div className="col" style={{fontSize: '0.85em'}}>
                                            {new Date(tx.timestamp).toLocaleDateString()}
                                        </div>
                                        <div className={`col amount-col ${debit ? 'red-text' : 'green-text'}`}>
                                            {debit ? '-' : '+'}${tx.amount.toFixed(2)}
                                        </div>
                                        <div className="col" style={{fontSize: '0.8em', opacity: 0.7}}>
                                            {tx.type}
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    ))}
                    {transactions.length === 0 && <p style={{textAlign:'center', marginTop: 20}}>No transactions found.</p>}
                </div>
            </div>

            {/* DETAIL MODAL */}
            {selectedTx && (
                <div className="tx-modal-overlay" onClick={() => setSelectedTx(null)}>
                    <div className="modal tx-modal" onClick={e => e.stopPropagation()}>
                        <h3 className={isDebit(selectedTx) ? 'red-text' : 'green-text'}>
                            {isDebit(selectedTx) ? 'Money Sent' : 'Money Received'}
                        </h3>
                        
                        <div className={`rubik-number big-amount ${isDebit(selectedTx) ? 'red-text' : 'green-text'}`}>
                            {isDebit(selectedTx) ? '-' : '+'}${selectedTx.amount.toFixed(2)}
                        </div>

                        <div className="tx-details-grid">
                            <label>Description:</label>
                            <span>{selectedTx.description}</span>

                            <label>Ref ID:</label>
                            <span className="copy-row">
                                {selectedTx.referenceId} 
                                <button onClick={() => copyToClipboard(selectedTx.referenceId)}>📋</button>
                            </span>

                            <label>Date & Time:</label>
                            <span>{formatFullDate(selectedTx.timestamp)}</span>

                            <label>{isDebit(selectedTx) ? 'To Account:' : 'From Account:'}</label>
                            <span className="rubik-number">
                                {isDebit(selectedTx) 
                                    ? (selectedTx.targetAccount?.accountNumber || 'External') 
                                    : (selectedTx.account?.accountNumber || 'System')}
                            </span>
                            
                            <label>Balance After:</label>
                            <span className="rubik-number" style={{color: 'white', fontWeight: 'bold'}}>
                                {/* Display correct running balance based on perspective */}
                                ${ (isDebit(selectedTx) ? selectedTx.sourceBalanceAfter : selectedTx.targetBalanceAfter)?.toFixed(2) || '---' }
                            </span>
                        </div>

                        <button className="c-btn" onClick={() => setSelectedTx(null)}>Close</button>
                    </div>
                </div>
            )}
        </section>
    );
};

export default Transactions;