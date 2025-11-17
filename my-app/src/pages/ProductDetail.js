import React, { useEffect, useState } from "react";
import axios from "axios";
import { useParams, useNavigate } from "react-router-dom";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faStar as fullStar, faStarHalfAlt as halfStar } from "@fortawesome/free-solid-svg-icons";
import { faStar as emptyStar } from "@fortawesome/free-regular-svg-icons";
import "../styles/ProductDetail.css";

function ProductDetail() {
  const { productId } = useParams();
  const navigate = useNavigate();

  const [product, setProduct] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [page, setPage] = useState(0); // 리뷰 페이지
  const [size] = useState(5); // 페이지당 리뷰 수
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    fetchProduct(page, size);
  }, [productId, page, size]);

  const fetchProduct = async (pageToFetch = 0, pageSize = size) => {
    setLoading(true);
    setError("");
    try {
      const res = await axios.get(`http://localhost:8080/products/${productId}`, {
        params: { page: pageToFetch, size: pageSize },
        withCredentials: true,
      });
      setProduct(res.data);
    } catch (e) {
      console.error(e);
      setError("상품 정보를 불러오는 중 오류가 발생했습니다.");
    }
    setLoading(false);
  };

  const renderStars = (rating) => {
    if (rating == null) rating = 0;
    return [...Array(5)].map((_, i) => {
      const starValue = i + 1;
      return (
        <FontAwesomeIcon
          key={i}
          icon={rating >= starValue ? fullStar : rating >= starValue - 0.5 ? halfStar : emptyStar}
          style={{ color: "#FFD700" }}
        />
      );
    });
  };

  const handleQuantityChange = (operation) => {
    setQuantity((prev) => {
      const newQty = operation === "increase" ? prev + 1 : prev - 1;
      if (newQty < 1) newQty = 1;
      if (newQty > 10) newQty = 10;
      return newQty;
    });
  };

  const handleAddToCart = () => {
    axios
      .post(`http://localhost:8080/cartitem/${productId}?quantity=${quantity}`, {}, { withCredentials: true })
      .then(() => alert("장바구니에 추가되었습니다."))
      .catch((err) => {
        if (err.response) {
          if (err.response.status === 400) {
            alert("장바구니 수량 제한: 최대 10개입니다.");
            return;
          }
          if (err.response.status === 401) {
            alert("로그인이 필요합니다.");
            navigate("/login");
            return;
          }
        }
        console.error(err);
        alert("장바구니 추가 중 오류가 발생했습니다.");
      });
  };

  const handleOrder = () => {
    navigate(`/order/${productId}?quantity=${quantity}`);
  };

  const goToPage = (p) => {
    if (p < 0) return;
    setPage(p);
  };

  return (
    <div className="product-detail-container">
      {loading && <p>로딩 중...</p>}
      {error && <p style={{ color: "red" }}>{error}</p>}

      {product ? (
        <>
          <h2 className="product-title">{product.name}</h2>

          <div className="product-image-container">
            <img
              className="product-image"
              src={product.imageUrl?.startsWith("http") ? product.imageUrl : `http://localhost:8080${product.imageUrl}`}
              onError={(e) => (e.target.src = "http://localhost:8080/images/noimage.jpg")}
              alt={product.name}
            />
          </div>

          <p className="product-description">{product.description}</p>
          <p className="product-price">{product.price.toLocaleString()}원</p>

          <div className="quantity-control">
            <button onClick={() => handleQuantityChange("decrease")} disabled={quantity <= 1}>-</button>
            <input
              type="number"
              value={quantity}
              min="1"
              max="10"
              onChange={(e) => {
                let val = parseInt(e.target.value) || 1;
                if (val < 1) val = 1;
                if (val > 10) val = 10;
                setQuantity(val);
              }}
            />
            <button onClick={() => handleQuantityChange("increase")} disabled={quantity >= 10}>+</button>
            <p className="product-all-price">{(product.price * quantity).toLocaleString()}원</p>
          </div>

          <div className="action-buttons">
            <button className="add-to-cart" onClick={handleAddToCart}>
              장바구니에 추가
            </button>
            <button className="order-now" onClick={handleOrder}>
              주문하기
            </button>
          </div>

          <div className="review-section">
            <h3>리뷰</h3>

            {product.reviews?.content?.length > 0 ? (
              <>
                <ul className="review-list">
                  {product.reviews.content.slice().reverse().map((review) => (
                    <li className="review-item" key={review.id}>
                      <strong>{review.reviewer}</strong>{" "}
                      <span className="review-date">
                        ({new Date(review.reviewDate).toLocaleDateString("ko-KR")})
                      </span>
                      <div>
                        {renderStars(review.rating)} <span>{review.rating}</span>
                      </div>
                      <p>{review.reviewText}</p>
                    </li>
                  ))}
                </ul>

                {product.reviews.totalPages > 1 && (
                  <div className="pagination">
                    <button
                      className="btn"
                      onClick={() => goToPage(0)}
                      disabled={page === 0}
                    >
                      First
                    </button>
                    <button
                      className="btn"
                      onClick={() => goToPage(page - 1)}
                      disabled={page === 0}
                    >
                      Prev
                    </button>

                    {[...Array(product.reviews.totalPages)].map((_, idx) => {
                      if (idx < page - 2 || idx > page + 2) return null;
                      return (
                        <button
                          key={idx}
                          onClick={() => goToPage(idx)}
                          className={idx === page ? "btn active" : "btn"}
                        >
                          {idx + 1}
                        </button>
                      );
                    })}

                    <button
                      className="btn"
                      onClick={() => goToPage(page + 1)}
                      disabled={page >= product.reviews.totalPages - 1}
                    >
                      Next
                    </button>
                    <button
                      className="btn"
                      onClick={() => goToPage(product.reviews.totalPages - 1)}
                      disabled={page >= product.reviews.totalPages - 1}
                    >
                      Last
                    </button>
                  </div>
                )}
              </>
            ) : (
              <p>아직 등록된 리뷰가 없습니다.</p>
            )}
          </div>
        </>
      ) : (
        !loading && <p>상품 정보를 찾을 수 없습니다.</p>
      )}
    </div>
  );
}

export default ProductDetail;
