import { useState } from 'react';
import api from './api';
import { Link, useNavigate } from 'react-router-dom';

const Signup = () => {
    const navigate = useNavigate();
    
    const [user, setUser] = useState({
        username: '', email: '', password: '', confirmPassword: ''
    });

    const [errors, setErrors] = useState({});
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);

    // --- 🔐 REAL-TIME VALIDATION LOGIC ---
    const validateField = (name, value, currentUserState) => {
        let errorMsg = "";

        if (name === 'password') {
            if (value.length < 8) {
                errorMsg = "Minimum 8 characters required";
            } else if (!/[A-Z]/.test(value)) {
                errorMsg = "Must include 1 Uppercase letter";
            } else if (!/[a-z]/.test(value)) {
                errorMsg = "Must include 1 Lowercase letter";
            } else if (!/\d/.test(value)) {
                errorMsg = "Must include 1 Number";
            } else if (!/[@$!%*?&]/.test(value)) {
                errorMsg = "Must include 1 Special Char (@$!%*?&)";
            }
        } 
        
        if (name === 'confirmPassword') {
            if (currentUserState.password && value !== currentUserState.password) {
                errorMsg = "Passwords do not match";
            }
        }

        return errorMsg;
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        
        // 1. Update State
        const updatedUser = { ...user, [name]: value };
        setUser(updatedUser);

        // 2. Validate Immediately
        const error = validateField(name, value, updatedUser);
        
        // 3. Update Errors State
        setErrors(prev => {
            const newErrors = { ...prev };
            if (error) {
                newErrors[name] = error;
            } else {
                delete newErrors[name];
            }
            // Edge case: If typing in password, re-validate confirmPassword match
            if (name === 'password' && updatedUser.confirmPassword) {
                if (value !== updatedUser.confirmPassword) {
                    newErrors.confirmPassword = "Passwords do not match";
                } else {
                    delete newErrors.confirmPassword;
                }
            }
            return newErrors;
        });
    };

    const handleSignup = async (e) => {
        e.preventDefault();
        
        // Final check before submit
        if (Object.keys(errors).length > 0 || !user.password || !user.email) {
            alert("Please fix the errors before submitting.");
            return;
        }

        try {
            const payload = {
                username: user.username,
                email: user.email,
                password: user.password
            };
            // Use shared api client
            await api.post('/api/users/register', payload);
            alert("Registration Successful! Please Login.");
            navigate('/');
        } catch (error) {
            const errorMsg = error.response?.data || "Registration Failed";
            if (typeof errorMsg === 'string' && errorMsg.includes("Username")) {
                setErrors(prev => ({ ...prev, username: "Username already taken" }));
            } else if (typeof errorMsg === 'string' && errorMsg.includes("Email")) {
                setErrors(prev => ({ ...prev, email: "Email already registered" }));
            } else {
                alert(errorMsg);
            }
        }
    };

    return (
        <section>
            <div className="signup-box">
                <form onSubmit={handleSignup}>
                    <h2>Gringotts</h2>
                    
                    <div className="input-box">
                        <input 
                            type="text" 
                            name="username" 
                            required 
                            placeholder=" " 
                            onChange={handleChange} 
                        />
                        <label>Username</label>
                        {errors.username && <span className="error-text">{errors.username}</span>}
                    </div>
                    
                    <div className="input-box">
                        <input 
                            type="email" 
                            name="email" 
                            required 
                            placeholder=" " 
                            onChange={handleChange} 
                        />
                        <label>Email</label>
                        {errors.email && <span className="error-text">{errors.email}</span>}
                    </div>
                    
                    <div className="input-box password-box">
                        <input 
                            type={showPassword ? "text" : "password"} 
                            name="password" 
                            required 
                            placeholder=" " 
                            onChange={handleChange} 
                        />
                        <label>Password</label>
                        <span className="password-toggle" onClick={() => setShowPassword(!showPassword)}>
                            <img src={showPassword ? "/img/Eye-Icon-open.svg" : "/img/Eye-Icon-close.svg"} alt="Toggle" />
                        </span>
                        {errors.password && <span className="error-text">{errors.password}</span>}
                    </div>
                    
                    <div className="input-box password-box">
                        <input 
                            type={showConfirmPassword ? "text" : "password"} 
                            name="confirmPassword" 
                            required 
                            placeholder=" " 
                            onChange={handleChange}
                        />
                        <label>Confirm Password</label>
                        <span className="password-toggle" onClick={() => setShowConfirmPassword(!showConfirmPassword)}>
                            <img src={showConfirmPassword ? "/img/Eye-Icon-open.svg" : "/img/Eye-Icon-close.svg"} alt="Toggle" />
                        </span>
                        {errors.confirmPassword && <span className="error-text">{errors.confirmPassword}</span>}
                    </div>

                    <button type="submit" className="btn">Signup</button>

                    <div className="register-link">
                        <p>Do you have an account? <Link to="/">Signin</Link></p>
                    </div>
                </form>
            </div>
        </section>
    );
};

export default Signup;