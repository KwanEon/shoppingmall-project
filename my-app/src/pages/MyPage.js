import React, { useState, useEffect, useRef, useContext } from "react";
import { useNavigate, Link } from "react-router-dom";
import axios from "axios";
import { AuthContext } from "../contexts/AuthContext";

const MyPage = () => {
  const { user, userRole, loading } = useContext(AuthContext);
  const [activeTab, setActiveTab] = useState("orders");
  const [orders, setOrders] = useState([]);
  const [accountInfo, setAccountInfo] = useState(null);
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

    // 주문 목록 가져오기
    const fetchOrders = async () => {
      if (user !== null && userRole !== "ANONYMOUS") {
        try {
            const response = await axios.get(`http://localhost:8080/auth/orders`, { withCredentials: true });
            const paidOrders = response.data.filter(order => order.status === "PAID");
            setOrders(paidOrders);
        } catch (error) {
            console.error("Failed to fetch orders:", error);
        }
      }
    };

    // 계정 정보 가져오기
    const fetchAccountInfo = async () => {
      if (user !== null && userRole !== "ANONYMOUS") {
        try {
            const response = await axios.get(`http://localhost:8080/auth/user`, { withCredentials: true });
            setAccountInfo(response.data);
        } catch (error) {
            console.error("Failed to fetch account info:", error);
        }
      }
    };

    fetchOrders();
    fetchAccountInfo();
  }, [user, userRole, navigate, loading]);

  if (userRole === "ANONYMOUS") {
    return null;
  }

  return (
    <div style={{ padding: "20px", maxWidth: "600px", margin: "auto" }}>
      <h1>My Page</h1>
      <div style={{ display: "flex", justifyContent: "space-around", marginBottom: "20px" }}>
        <button onClick={() => setActiveTab("orders")} style={{ padding: "10px 20px", cursor: "pointer" }}>
          주문 목록
        </button>
        <button onClick={() => setActiveTab("account")} style={{ padding: "10px 20px", cursor: "pointer" }}>
          계정 정보
        </button>
      </div>

      <div>
        {activeTab === "orders" && (
          <div>
            <h2>주문 목록</h2>
            {orders.length > 0 ? (
              <ul>
                {orders.map((order) => (
                  <li key={order.id}>
                    주문번호: {order.id}, 총 가격: ₩{order.totalPrice}, 주문 날짜: {new Date(order.orderDate).toLocaleDateString()}
                    <ul>
                      {order.orderItems.map((item) => (
                        <li key={item.id}>
                          - <Link to = {`/products/${item.productId}`} style = {{color: "blue", textDecoration: "none"}}>
                          {item.productName} 
                          </Link>{" "}
                          ({item.quantity}개): ₩{item.price}
                          {item.reviewId ? (
                            <Link to={`/reviews/${item.reviewId}/edit`}
                              style={{
                                marginLeft: "8px",
                                padding: "4px 8px",
                                backgroundColor: "#4CAF50",
                                color: "#fff",
                                borderRadius: "4px",
                                textDecoration: "none",
                                fontSize: "0.9rem"
                              }}
                            >
                              리뷰 수정
                            </Link>
                          ) : (
                            <Link to={`/products/${item.productId}/review`}
                              style={{
                                marginLeft: "8px",
                                padding: "4px 8px",
                                backgroundColor: "#4CAF50",
                                color: "#fff",
                                borderRadius: "4px",
                                textDecoration: "none",
                                fontSize: "0.9rem"
                              }}
                            >
                              리뷰 작성
                            </Link>
                          )}
                        </li>
                    ))}
                    </ul>
                  </li>
                ))}
              </ul>
            ) : (
              <p>주문 내역이 없습니다.</p>
            )}
          </div>
        )}

        {activeTab === "account" && (
          <div>
            <h2>계정 정보</h2>
            {accountInfo ? (
              <div>
                <p>이름: {user.name}</p>
                <p>이메일: {user.email}</p>
                <p>휴대전화: {user.phoneNumber}</p>
              </div>
            ) : (
              <p>계정 정보를 불러오는 중입니다...</p>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default MyPage;
