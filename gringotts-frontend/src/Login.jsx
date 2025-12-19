import { useState } from 'react';
import api from './api';
import { useNavigate, Link } from 'react-router-dom';

const Login = () => {
    const [formData, setFormData] = useState({ username: '', password: '' });
    const [errors, setErrors] = useState({}); // 🔴 State for errors
    const [showPassword, setShowPassword] = useState(false);
    const navigate = useNavigate();

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
        if (errors[e.target.name]) setErrors({ ...errors, [e.target.name]: '' });
    };

    const validateInput = () => {
        const input = formData.username;
        let newErrors = {};
        let isValid = true;

        if (input.includes('@')) {
            // Email Regex
            if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(input)) {
                newErrors.username = "Invalid Email Format";
                isValid = false;
            }
        } else {
            // Username Checks
            if (input.includes(' ')) {
                newErrors.username = "Username cannot contain spaces";
                isValid = false;
            }
        }
        setErrors(newErrors);
        return isValid;
    };

    const handleLogin = async (e) => {
        e.preventDefault();
        setErrors({});

        if (!validateInput()) return;

        try {
            // Use shared api client
            const response = await api.post('/api/auth/login', formData);
            localStorage.setItem('token', response.data.token);
            localStorage.setItem('userId', response.data.userId);
            localStorage.setItem('firstName', response.data.firstName || 'User');
            // Persist whether user still needs to complete profile
            const needsProfile = response.data.needsProfile === true;
            localStorage.setItem('needsProfile', needsProfile ? '1' : '0');
            // Route based on needsProfile
            if (needsProfile) {
                navigate('/details');
            } else {
                navigate('/dashboard');
            }
        } catch (error) {
            // Handle specific backend errors
            if (error.response && error.response.status === 401) {
                // We show this error under the Password field usually, or general
                setErrors({ password: "Invalid Username/Email or Password" });
            } else {
                setErrors({ password: "Server Error. Try again later." });
            }
        }
    };

    return (
        <section>
            <div className="login-box">
                <form onSubmit={handleLogin}>
                    <h2>Gringotts</h2>
                    
                    <div className="input-box">
                        <input type="text" name="username" required placeholder=" " onChange={handleChange} />
                        <label>Username or Email</label>
                    </div>
                    {errors.username && <span className="error-text">{errors.username}</span>}
                    
                    <div className="input-box password-box">
                        <input type={showPassword ? "text" : "password"} name="password" required placeholder=" " onChange={handleChange} />
                        <label>Password</label>
                        <span className="password-toggle" onClick={() => setShowPassword(!showPassword)}>
                            <img src={showPassword ? "/img/Eye-Icon-open.svg" : "/img/Eye-Icon-close.svg"} alt="Toggle" />
                        </span>
                    </div>
                    {errors.password && <span className="error-text">{errors.password}</span>}

                    <div className="remember-forgot">
                        <label><input type="checkbox" />Remember me</label>
                        <a href="#">Forgot Password?</a>
                    </div>

                    <button type="submit" className="btn">Login</button>

                    <div className="register-link">
                        <p>Don't have an account? <Link to="/signup">Register</Link></p>
                    </div>
                </form>
            </div>
        </section>
    );
};

export default Login;