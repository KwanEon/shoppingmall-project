import React, { useEffect, useState, useRef, useContext } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import { AuthContext } from "../contexts/AuthContext";
import "../styles/Cart.css";

function Cart() {
  const { userRole, loading } = useContext(AuthContext);
  const [cartItems, setCartItems] = useState([]);
  const [error, setError] = useState("");
  const navigate = useNavigate();
  const hasRedirected = useRef(false);

  useEffect(() => {
    if (loading) return;

    if (!hasRedirected.current) {
      hasRedirected.current = true;
      if (userRole === "ANONYMOUS") {
        alert("로그인이 필요합니다.");
        navigate("/login", { replace: true });
        return;
      }
    }

    if (userRole !== "ANONYMOUS") {
      axios
        .get(`http://localhost:8080/cartitem`, { withCredentials: true })
        .then((res) => setCartItems(res.data))
        .catch(() => setError("장바구니를 불러오는 중 오류가 발생했습니다."));
    }
  }, [userRole, navigate, loading]);

  if (userRole === "ANONYMOUS") return null;

  const handleQuantityChange = (cartId, operation) => {
    axios
      .patch(`http://localhost:8080/cartitem/${cartId}?operation=${operation}`, {}, { withCredentials: true })
      .then(() => {
        setCartItems((prev) =>
          prev.map((item) =>
            item.id === cartId
              ? { ...item, quantity: operation === "increase" ? item.quantity + 1 : item.quantity - 1 }
              : item
          )
        );
      })
      .catch(() => setError("수량 변경 중 오류가 발생했습니다."));
  };

  const handleRemoveItem = (productId) => {
    axios
      .delete(`http://localhost:8080/cartitem/${productId}`, { withCredentials: true })
      .then(() => setCartItems((prev) => prev.filter((item) => item.productId !== productId)))
      .catch(() => setError("상품 삭제 중 오류가 발생했습니다."));
  };

  const handleClearCart = () => {
    axios
      .delete(`http://localhost:8080/cartitem`, { withCredentials: true })
      .then(() => setCartItems([]))
      .catch(() => setError("장바구니를 비우는 중 오류가 발생했습니다."));
  };

  const handleCheckout = () => navigate("/order/cartitem");

  return (
    <section className="cart-container">
      <h2>장바구니</h2>

      {cartItems.length === 0 ? (
        <p className="cart-empty">장바구니가 비어 있습니다.</p>
      ) : (
        <div className="cart-grid">
          {cartItems.map((item) => (
            <div key={item.id} className="cart-item">
              <img
                src={item.imageUrl?.startsWith("http") ? item.imageUrl : `http://localhost:8080${item.imageUrl}`}
                onError={(e) => { e.target.src = "http://localhost:8080/static/images/noimage.jpg"; }}
                alt={item.productName}
                onClick={() => navigate(`/products/${item.productId}`)}
              />

              <h4 onClick={() => navigate(`/products/${item.productId}`)}>{item.productName}</h4>
              <p>{item.productPrice.toLocaleString()}원</p>

              <div className="quantity-controls">
                <button onClick={() => handleQuantityChange(item.id, "decrease")} disabled={item.quantity <= 1}>-</button>
                <span>{item.quantity}</span>
                <button onClick={() => handleQuantityChange(item.id, "increase")}>+</button>
              </div>

              <button className="remove-btn" onClick={() => handleRemoveItem(item.productId)}>삭제</button>
            </div>
          ))}
        </div>
      )}

      {cartItems.length > 0 && (
        <div className="cart-actions">
          <button className="clear-btn" onClick={handleClearCart}>장바구니 비우기</button>
          <button className="checkout-btn" onClick={handleCheckout}>주문하기</button>
        </div>
      )}

      {error && <p className="error-message">{error}</p>}
    </section>
  );
}

export default Cart;
