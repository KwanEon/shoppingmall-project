import React, { useContext } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { FaShoppingCart } from "react-icons/fa";
import axios from "axios";
import { AuthContext } from "../contexts/AuthContext";
import "../styles/Layout.css";

function Layout({ children }) {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, setUser, setUserRole } = useContext(AuthContext);

  const handleLogout = () => {
    if (!window.confirm("로그아웃 하시겠습니까?")) return;
    axios
      .post(`http://localhost:8080/auth/logout`, {}, { withCredentials: true })
      .then(() => {
        setUser(null);
        setUserRole("ANONYMOUS");
        localStorage.removeItem("token");
        alert("로그아웃 되었습니다.");
        navigate("/");
      })
      .catch((err) => {
        console.error("로그아웃 중 오류 발생:", err);
        alert("로그아웃 중 오류가 발생했습니다.");
      });
  };

  const handleLogin = () => navigate("/login", { state: { from: location } });
  const handleRegister = () => navigate("/register");
  const handleTitleClick = () => navigate("/");
  const handleMyPage = () => navigate("/mypage");
  const handleCart = () => navigate("/cart");

  return (
    <div>
      <header className="layout-header">
        <div className="header-buttons">
          {user ? (
            <>
              <button onClick={handleCart}>
                <FaShoppingCart size={24} />
              </button>
              <p className="user-name" onClick={handleMyPage}>
                {user.name}님
              </p>
              <button onClick={handleLogout}>로그아웃</button>
            </>
          ) : (
            <>
              <button onClick={handleLogin}>로그인</button>
              <button onClick={handleRegister}>회원가입</button>
            </>
          )}
        </div>
        <h1 onClick={handleTitleClick}>쇼핑몰 프로젝트</h1>
      </header>

      <main className="layout-main">{children}</main>
    </div>
  );
}

export default Layout;
