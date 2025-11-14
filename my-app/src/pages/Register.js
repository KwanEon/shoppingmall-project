import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import "../styles/Register.css";

function Register() {
  const [formData, setFormData] = useState({
    username: "",
    name: "",
    password: "",
    email: "",
    phoneNumber: "",
    address: ""
  });

  const [validationErrors, setValidationErrors] = useState({});
  const [error, setError] = useState("");

  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    setValidationErrors(prev => ({ ...prev, [name]: "" }));
  };

  const handleSignup = async (e) => {
    e.preventDefault();
    try {
      await axios.post(`http://localhost:8080/register`, formData);
      alert("회원가입이 성공적으로 완료되었습니다! 이메일 인증을 완료해야 로그인할 수 있습니다.");
      navigate("/login");
    } catch (error) {
      const res = error.response;
      if (!res) {
        alert("서버에 연결할 수 없습니다.");
        return;
      }
      if (res.data?.status === "validation_error" && res.data.errors) {
        setValidationErrors(res.data.errors);
        setError("");
        return;
      }
      const message = res.data?.message || res.data;
      setError(typeof message === "string" ? message : "회원가입 중 알 수 없는 오류가 발생했습니다.");
    }
  };

  return (
    <div className="signup-container">
      <div className="signup-box">
        <h1 className="signup-title">회원가입</h1>
        <form onSubmit={handleSignup} noValidate>
          {["username","name","password","email","phoneNumber","address"].map((field) => (
            <div key={field} style={{ marginBottom: "1rem", textAlign: "left" }}>
              <label style={{ fontWeight: "bold" }}>
                {field === "username" ? "아이디" :
                 field === "name" ? "이름" :
                 field === "password" ? "비밀번호" :
                 field === "email" ? "이메일" :
                 field === "phoneNumber" ? "휴대전화" :
                 "주소"}
              </label>
              <input
                type={field === "password" ? "password" : field === "email" ? "email" : "text"}
                name={field}
                placeholder={
                  field === "username" ? "아이디" :
                  field === "name" ? "이름" :
                  field === "password" ? "비밀번호" :
                  field === "email" ? "이메일" :
                  field === "phoneNumber" ? "휴대전화" :
                  "주소"
                }
                value={formData[field]}
                onChange={handleChange}
              />
              {validationErrors[field] && (
                <p className="error-message">{validationErrors[field]}</p>
              )}
            </div>
          ))}

          <button type="submit">회원가입</button>
          {error && <p className="error-message">{error}</p>}
        </form>
      </div>
    </div>
  );
}

export default Register;
