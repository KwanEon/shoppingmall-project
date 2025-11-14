import React, { useState, useContext } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { AuthContext } from "../contexts/AuthContext";
import axios from "axios";
import "../styles/Login.css";

function Login() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const { setUser, setUserRole } = useContext(AuthContext);

  const navigate = useNavigate();
  const location = useLocation();
  let from = location.state?.from?.pathname || "/";

  if (from === "/login" || from === "/signup") {
    from = "/";
  }

  const handleLogin = async (e) => {
    e.preventDefault();

    const params = new URLSearchParams();
    params.append("username", username);
    params.append("password", password);

    try {
      await axios.post(`http://localhost:8080/auth/login`, params, { withCredentials: true });
      const resUser = await axios.get(`http://localhost:8080/auth/user`, { withCredentials: true });
      setUser(resUser.data);
      const resRole = await axios.get(`http://localhost:8080/auth/role`, { withCredentials: true });
      setUserRole(resRole.data);

      alert("로그인 되었습니다.");
      navigate(from === "/login" || from === "/register" ? "/" : from, { replace: true });
    } catch (err) {
      console.error("Login failed:", err);
      setError("아이디와 비밀번호를 확인해주세요.");
    }
  };

  return (
    <div className="login-container">
      <div className="login-box">
        <h1 className="login-title">로그인</h1>
        <form onSubmit={handleLogin} className="login-form">
          <input
            type="text"
            placeholder="아이디"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
          <input
            type="password"
            placeholder="비밀번호"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          <button type="submit">로그인</button>
          {error && <p className="error-message">{error}</p>}
        </form>
      </div>
    </div>
  );
}

export default Login;
