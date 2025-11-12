import React, { useState, useEffect, useContext } from "react";
import { useNavigate, Link } from "react-router-dom";
import axios from "axios";
import { AuthContext } from "../contexts/AuthContext";
import "../styles/MyPage.css"; // 방금 만든 CSS 파일

const MyPage = () => {
  const { user, userRole, loading } = useContext(AuthContext);
  const [activeTab, setActiveTab] = useState("orders");
  const [orders, setOrders] = useState([]);
  const [accountInfo, setAccountInfo] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loadingOrders, setLoadingOrders] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    if (loading) return;

    if (userRole === "ANONYMOUS") {
      alert("로그인이 필요합니다.");
      navigate("/login", { replace: true });
      return;
    }

    // 계정 정보 가져오기
    const fetchAccountInfo = async () => {
      try {
        const res = await axios.get("http://localhost:8080/auth/user", { withCredentials: true });
        setAccountInfo(res.data);
      } catch (err) {
        console.error(err);
      }
    };

    fetchAccountInfo();
  }, [userRole, navigate, loading]);

  useEffect(() => {
    // 주문 목록 가져오기
    const fetchOrders = async () => {
      if (userRole === "ANONYMOUS") return;

      setLoadingOrders(true);
      try {
        const res = await axios.get("http://localhost:8080/auth/orders", {
          withCredentials: true,
          params: { page },
        });

        const data = res.data;
        if (data && Array.isArray(data.content)) {
          const paidOrders = data.content.filter(order => order.status === "PAID");
          setOrders(paidOrders);
          setTotalPages(data.totalPages || 1);
        } else {
          const paidOrders = Array.isArray(data) ? data.filter(o => o.status === "PAID") : [];
          setOrders(paidOrders);
          setTotalPages(1);
        }
      } catch (err) {
        console.error(err);
        setOrders([]);
        setTotalPages(0);
      } finally {
        setLoadingOrders(false);
      }
    };

    fetchOrders();
  }, [userRole, page]);

  if (userRole === "ANONYMOUS") return null;

  const goToPage = (p) => {
    if (p < 0 || p >= totalPages) return;
    setPage(p);
  };

  return (
    <section className="mypage-section">
      <h1>My Page</h1>

      {/* 탭 버튼 */}
      <div className="tab-container">
        <button
          className={`tab-btn ${activeTab === "orders" ? "active" : ""}`}
          onClick={() => setActiveTab("orders")}
        >
          주문 목록
        </button>
        <button
          className={`tab-btn ${activeTab === "account" ? "active" : ""}`}
          onClick={() => setActiveTab("account")}
        >
          계정 정보
        </button>
      </div>

      {/* 주문 목록 */}
      {activeTab === "orders" && (
        <>
          {loadingOrders ? (
            <p>주문을 불러오는 중...</p>
          ) : orders.length === 0 ? (
            <p>주문 내역이 없습니다.</p>
          ) : (
            <ul className="order-list">
              {orders.map((order) => (
                <li key={order.id} className="order-card">
                  <div className="order-header">
                    <strong>주문번호:</strong> {order.id}{" "}
                    <span style={{ marginLeft: "12px" }}>
                      <strong>총 가격:</strong> {order.totalPrice.toLocaleString()}원
                    </span>
                    <span style={{ marginLeft: "12px" }}>
                      <strong>주문 날짜:</strong> {new Date(order.orderDate).toLocaleDateString()}
                    </span>
                  </div>

                  <ul className="order-items">
                    {order.orderItems.map((item) => (
                      <li key={item.id} className="order-item-card">
                        <img
                          onClick={() => navigate(`/products/${item.productId}`)}
                          className="order-item-img"
                          src={
                            item.imageUrl?.startsWith("http")
                              ? item.imageUrl
                              : `http://localhost:8080${item.imageUrl}`
                          }
                          onError={(e) => { e.target.src = "http://localhost:8080/static/images/noimage.jpg"; }}
                          alt={item.productName}
                        />
                        <div className="order-item-info">
                          <Link
                            to={`/products/${item.productId}`}
                            className="product-link"
                          >
                            {item.productName || "상품명"}
                          </Link>
                          <span>
                            ({item.quantity}개): {item.price.toLocaleString()}원
                          </span>

                          {item.reviewId ? (
                            <Link
                              to={`/reviews/${item.reviewId}/edit`}
                              className="btn-review-edit small"
                            >
                              리뷰 수정
                            </Link>
                          ) : (
                            <Link
                              to={`/products/${item.productId}/review`}
                              className="btn-review-create small"
                            >
                              리뷰 작성
                            </Link>
                          )}
                        </div>
                      </li>
                    ))}
                  </ul>
                </li>
              ))}
            </ul>
          )}

          {/* 페이지네이션 */}
          {totalPages > 1 && (
            <div className="pagination">
              <button className="btn" onClick={() => goToPage(0)} disabled={page === 0}>
                First
              </button>
              <button className="btn" onClick={() => goToPage(page - 1)} disabled={page === 0}>
                Prev
              </button>

              {[...Array(totalPages)].map((_, idx) => {
                if (idx < page - 2 || idx > page + 2) return null;
                return (
                  <button
                    key={idx}
                    className={`btn ${idx === page ? "active" : ""}`}
                    onClick={() => goToPage(idx)}
                  >
                    {idx + 1}
                  </button>
                );
              })}

              <button className="btn" onClick={() => goToPage(page + 1)} disabled={page >= totalPages - 1}>
                Next
              </button>
              <button className="btn" onClick={() => goToPage(totalPages - 1)} disabled={page >= totalPages - 1}>
                Last
              </button>
            </div>
          )}
        </>
      )}

      {/* 계정 정보 */}
      {activeTab === "account" && (
        <div>
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
    </section>
  );
};

export default MyPage;
