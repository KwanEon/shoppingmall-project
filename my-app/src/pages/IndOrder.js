import React, { useEffect, useState, useRef, useContext } from "react";
import axios from "axios";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { AuthContext } from "../contexts/AuthContext";
import "../styles/Order.css";

function IndOrder() {
  const { user, userRole, loading } = useContext(AuthContext);
  const { productId } = useParams();
  const [searchParams] = useSearchParams();
  const rawQuantity = parseInt(searchParams.get("quantity"), 10);
  const quantity = isNaN(rawQuantity) || rawQuantity <= 0 ? 1 : rawQuantity;
  const [product, setProduct] = useState(null);
  const [address, setAddress] = useState("");
  const [phone, setPhone] = useState("");
  const navigate = useNavigate();
  const hasAlerted = useRef(false);

  useEffect(() => {
    if (loading) return;

    if (!hasAlerted.current) {
      hasAlerted.current = true;
      if (userRole === "ANONYMOUS") {
        alert("로그인이 필요합니다.");
        navigate("/login", { replace: true });
        return;
      }

      if (user) {
        setAddress(user.address || "");
        setPhone(user.phoneNumber || "");
      }

      axios
        .get(`http://localhost:8080/products/${productId}`, {
          withCredentials: true,
        })
        .then((res) => {
          setProduct(res.data);
        })
        .catch((err) => {
          console.error(err);
          alert("해당 상품은 주문할 수 없습니다.");
          navigate("/products");
        });
    }
  }, [user, userRole, navigate, productId, loading]);

  if (userRole === "ANONYMOUS" || loading || !product) {
    return null;
  }

  const totalPrice = product.price * quantity;

  const handlePayment = () => {
    axios
      .post(
        `http://localhost:8080/order/${productId}?quantity=${quantity}`,
        {},
        { withCredentials: true }
      )
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
    <div className="order-container">
      <h2 className="order-title">개별 상품 주문</h2>

      <div className="input-group">
        <label>배송지: {user.address}</label>
      </div>

      <div className="input-group">
        <label>연락처: {user.phoneNumber}</label>
      </div>

      <h3>주문 상품</h3>
      <ul className="order-item-list">
        <li>
          {product.name} ({quantity}개) - {totalPrice.toLocaleString()}원
        </li>
      </ul>

      <p className="total-price">총 결제 금액: {totalPrice.toLocaleString()}원</p>

      <button className="order-button" onClick={handlePayment}>
        결제하기
      </button>
    </div>
  );
}

export default IndOrder;
