import React, { useEffect, useState, useRef, useContext } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import { AuthContext } from "../contexts/AuthContext";

function CartOrder() {
  const { user, userRole, loading } = useContext(AuthContext);
  const [cartItems, setCartItems] = useState([]);
  const [address, setAddress] = useState("");
  const [phone, setPhone] = useState("");
  const navigate = useNavigate();
  const hasRedirected = useRef(false);

  useEffect(() => {
    if (loading) return;

    if (!hasRedirected.current) {
      hasRedirected.current = true;
      if (userRole === "ANONYMOUS") {
        alert("로그인이 필요합니다.");
        navigate("/login");
        return;
      }

      axios
        .get(`http://localhost:8080/cartitem`, { withCredentials: true })
        .then((response) => setCartItems(response.data))
        .catch((err) => {
          console.error("장바구니를 불러오는 중 오류 발생:", err);
        });

      setAddress(user.address || "");
      setPhone(user.phoneNumber || "");
    }
  }, [user, userRole, navigate, loading]);

  if (userRole === "ANONYMOUS") return null;

  const totalPrice = cartItems.reduce(
    (sum, item) => sum + (item.productPrice ?? 0) * item.quantity,
    0
  );

  const handlePayment = () => {
    axios
      .post(`http://localhost:8080/order/cartitem`, {}, { withCredentials: true })
      .then((res) => {
        const kakaoPayResponse = res.data.kakaoPayResponse;
        const popup = window.open(kakaoPayResponse.next_redirect_pc_url, "kakaoPay", "width=500,height=600,scrollbars=yes");
        if (!popup) {
          alert("팝업이 차단되었습니다. 팝업 차단을 해제해주세요.");
          return;
        }

        const orderId = res.data.orderId;

        const pollTimer = window.setInterval(() => {
          if (popup.closed) {
            window.clearInterval(pollTimer);
            // 결제 상태 확인
            axios.get(`http://localhost:8080/order/status/${orderId}`, { withCredentials: true })
              .then((statusRes) => {
                if (statusRes.data.status === "PAID") {
                  alert("주문이 정상적으로 완료되었습니다.");
                  navigate("/mypage");
                } else {
                  axios.post(`http://localhost:8080/payment/cancel?orderId=${orderId}`, {}, { withCredentials: true });
                  alert("결제가 취소되었거나 실패했습니다.");
                }
              })
              .catch(() => {
                alert("상태 확인 중 오류 발생.");
              });
          }
        }, 500);
      })
      .catch((err) => {
        console.error("주문 처리 중 오류", err);
        alert("주문 처리에 실패했습니다. 다시 시도해주세요.");
      });
  };

  return (
    <div style={{ padding: "2rem", maxWidth: "600px", margin: "0 auto" }}>
      <h2>결제 페이지</h2>

      <div style={{ marginBottom: "1rem" }}>
        <label>배송지: </label>
        <input
          type="text"
          value={address}
          onChange={(e) => setAddress(e.target.value)}
          style={{ width: "100%", padding: "0.5rem", marginTop: "0.5rem" }}
        />
      </div>

      <div style={{ marginBottom: "1rem" }}>
        <label>연락처: </label>
        <input
          type="text"
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
          style={{ width: "100%", padding: "0.5rem", marginTop: "0.5rem" }}
        />
      </div>

      <h3>주문 상품</h3>
      <ul>
        {cartItems.map((item) => (
          <li key={item.id}>
            {item.productName} - {item.quantity}개 - ₩
            {(item.productPrice ?? 0) * item.quantity}
          </li>
        ))}
      </ul>

      <p style={{ fontWeight: "bold", marginTop: "1rem" }}>
        총 결제 금액: ₩{totalPrice.toLocaleString()}
      </p>

      <button
        onClick={handlePayment}
        style={{
          marginTop: "1rem",
          backgroundColor: "#4CAF50",
          color: "#fff",
          border: "none",
          padding: "0.7rem 2rem",
          cursor: "pointer",
          borderRadius: "4px",
        }}
      >
        결제하기
      </button>
    </div>
  );
}

export default CartOrder;
