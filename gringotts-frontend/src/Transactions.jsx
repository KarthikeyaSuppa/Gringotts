import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from './api';
import './App.css'; 
import TransactionButton from './TransactionButton'; // ✅ Imported
import PaymentStatus from './PaymentStatus'; 

const Transactions = () => {
    const navigate = useNavigate();
    
    // Data
    const [transactions, setTransactions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [myAccountId, setMyAccountId] = useState(null); // This needs to be set!

    // Filters
    const [filterStart, setFilterStart] = useState('');
    const [filterEnd, setFilterEnd] = useState('');
    const [filterFlow, setFilterFlow] = useState(''); 
    const [filterType, setFilterType] = useState(''); 

    // Pagination State
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const PAGE_SIZE = 8; 

    // Modal
    const [selectedTx, setSelectedTx] = useState(null);

    useEffect(() => {
        loadData();
    }, [currentPage]); 

    const loadData = async () => {
        const userId = localStorage.getItem('userId');
        if (!userId) return;
        setLoading(true);
        try {
            // 1. Fetch Transactions
            const res = await api.get(`/api/transactions/history/${userId}?page=${currentPage}&size=${PAGE_SIZE}`);
            setTransactions(res.data.content);
            setTotalPages(res.data.totalPages);
            
            // 2. ✅ FETCH ACCOUNT ID (CRITICAL FIX)
            // We need this to know if a transaction is Sent or Received
            if (!myAccountId) {
                const accRes = await api.get(`/api/accounts/by-user/${userId}`);
                if (accRes.data && accRes.data.length > 0) {
                    // Assuming the first account is the primary one for calculations
                    setMyAccountId(accRes.data[0].id);
                }
            }

        } catch (error) {
            console.error("Failed to load data", error);
        } finally {
            setLoading(false);
        }
    };

    const handlePageChange = (newPage) => {
        if (newPage >= 0 && newPage < totalPages) {
            setCurrentPage(newPage);
        }
    };

    const fetchTransactions = async (accId) => {
        try {
            const params = {
                accountId: accId, // This endpoint typically requires accountId, ensuring we use the one we fetched
                userId: localStorage.getItem('userId'), // Or userId depending on your backend update
                startDate: filterStart,
                endDate: filterEnd,
                flow: filterFlow,
                type: filterType
            };
            Object.keys(params).forEach(key => !params[key] && delete params[key]);

            const res = await api.get('/api/transactions/search', { params });
            const sorted = res.data.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
            setTransactions(sorted);
        } catch (e) {
            console.error(e);
        }
    };

    const handleApplyFilters = (e) => {
        e.preventDefault();
        // Use myAccountId which we now definitely have
        if(myAccountId) fetchTransactions(myAccountId);
    };

    const handleDownload = () => {
        const userId = localStorage.getItem('userId');
        if(!userId) return;
        
        let url = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8050'}/api/transactions/download?userId=${userId}`;
        if(filterStart) url += `&startDate=${filterStart}`;
        if(filterEnd) url += `&endDate=${filterEnd}`;
        if(filterFlow) url += `&flow=${filterFlow}`;
        if(filterType) url += `&type=${filterType}`;
        
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

    // ✅ FIX: Compare IDs safely (toString ensures no type mismatch issues)
    const isDebit = (tx) => {
        if (!myAccountId || !tx.account) return false;
        return String(tx.account.id) === String(myAccountId);
    };

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

    const formatFullDate = (isoString) => {
        const d = new Date(isoString);
        const datePart = d.toLocaleDateString('en-GB').replace(/\//g, '-'); 
        const timePart = d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
        return `${datePart}, ${timePart}`;
    };

    return (
        <section id="U-Home">
            <div className="right-navbar show">
                <div className="nav-icon" title="Dashboard" onClick={() => navigate('/dashboard')}>
                    <img src="/img/Home.ico" alt="Home" />
                </div>
                <div className="nav-icon" title="Pay" onClick={() => navigate('/pay')}>
                    <img src="/img/Pay.ico" alt="Pay" style={{filter: 'invert(1)'}} /> 
                </div>
                <div className="nav-icon" role="button" title="Cards" onClick={() => navigate('/cards')}>
                    <img src="/img/Card.ico" alt="Cards" />
                </div>
                <div className="nav-icon" title="Transactions" onClick={() => navigate('/transactions')}>
                    <img src="/img/Transaction_History.ico" alt="History" />
                </div>
                <div className="nav-icon logout-btn" title="Logout" onClick={() => { localStorage.clear(); navigate('/'); }}>
                    <img src="/img/Logout.ico" alt="Logout" />
                </div>
            </div>

            <div className="UserDetails-box tx-container">
                <div className="tx-header">
                    <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                        <h2>Transactions</h2>
                        {/* ✅ ADDED BUTTON BACK */}
                        <div style={{transform: 'scale(0.8)', marginRight: '-20px'}}>
                            <TransactionButton />
                        </div>
                    </div>

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

                <div className="tx-table-header">
                    <div className="col">Ref ID</div>
                    <div className="col">Type</div>
                    <div className="col">Date</div>
                    <div className="col">Amount</div>
                    <div className="col">Tag</div>
                </div>

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
                    {transactions.length === 0 && !loading && <p style={{textAlign:'center', marginTop: 20}}>No transactions found.</p>}
                </div>

                <div className="pagination-footer">
                    <button 
                        className="page-btn" 
                        disabled={currentPage === 0}
                        onClick={() => handlePageChange(currentPage - 1)}
                    >
                        &lt; Prev
                    </button>
                    
                    <span className="page-info">
                        Page {currentPage + 1} of {totalPages === 0 ? 1 : totalPages}
                    </span>
                    
                    <button 
                        className="page-btn" 
                        disabled={currentPage >= totalPages - 1}
                        onClick={() => handlePageChange(currentPage + 1)}
                    >
                        Next &gt;
                    </button>
                </div>
                
            </div>

            {selectedTx && (
                <div className="tx-modal-overlay" onClick={() => setSelectedTx(null)}>
                    <div className="modal tx-modal" onClick={e => e.stopPropagation()}>
                        
                        {/* Transaction Details */}
                        <div className="tx-details-grid" style={{ marginTop: '10px' }}>
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
                                ${ (isDebit(selectedTx) ? selectedTx.sourceBalanceAfter : selectedTx.targetBalanceAfter)?.toFixed(2) || '---' }
                            </span>
                        </div>

                        {/* ✅ ANIMATION (Showing Amount as Title) */}
                        <div style={{ marginBottom: 20, transform: 'scale(0.9)' }}>
                            <PaymentStatus 
                                status="success" 
                                theme={isDebit(selectedTx) ? 'red' : 'green'} 
                                title={`${isDebit(selectedTx) ? '-' : '+'}$${selectedTx.amount.toFixed(2)}`}
                            />
                        </div>

                        <button className="c-btn" onClick={() => setSelectedTx(null)}>Close</button>
                    </div>
                </div>
            )}
        </section>
    );
};

export default Transactions;