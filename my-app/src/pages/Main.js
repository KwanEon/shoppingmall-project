import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faStar as fullStar, faStarHalfAlt as halfStar } from "@fortawesome/free-solid-svg-icons";
import { faStar as emptyStar } from "@fortawesome/free-regular-svg-icons";

function Main() {
  const navigate = useNavigate();
  const [popularProducts, setPopularProducts] = useState([]);

  const goToProducts = () => {
    navigate("/products");
  };

  useEffect(() => {
    axios
      .get("http://localhost:8080/products/popular", { withCredentials: true })
      .then((res) => setPopularProducts(res.data))
      .catch((err) => console.error("인기상품 불러오기 실패:", err));
  }, []);

  const renderStars = (rating) => {
    return [...Array(5)].map((_, i) => {
      const starValue = i + 1;
      return (
        <FontAwesomeIcon
          key={i}
          icon={
            rating >= starValue
              ? fullStar
              : rating >= starValue - 0.5
              ? halfStar
              : emptyStar
          }
          style={{ color: "#FFD700" }}
        />
      );
    });
  };

  return (
    <section style={{ padding: "2rem" }}>
      <h2>쇼핑몰 프로젝트 메인</h2>

      <button
        onClick={goToProducts}
        style={{
          margin: "1rem 0",
          padding: "0.5rem 1rem",
          cursor: "pointer",
        }}
      >
        상품 목록 보기
      </button>

      <h3>🔥 인기상품 TOP 3</h3>
      <div style={{ display: "flex", gap: "1rem", marginTop: "1rem" }}>
        {popularProducts.map((product) => (
          <div
            key={product.id}
            style={{
              border: "1px solid #ddd",
              borderRadius: "8px",
              padding: "1rem",
              width: "200px",
              textAlign: "center",
            }}
          >
            <img
              onClick={() => navigate(`products/${product.id}`)}
              src={
                product.imageUrl.startsWith("http")
                  ? product.imageUrl
                  : `http://localhost:8080${product.imageUrl}`
              }
              alt={product.name}
              style={{
                width: "100%",
                height: "150px",
                objectFit: "cover",
                borderRadius: "8px",
                cursor: "pointer",
              }}
            />
            <h4
              onClick={() => navigate(`products/${product.id}`)}
              style={{
                cursor: "pointer",
                color: "blue",
              }}
            >
              {product.name}
            </h4>
            <p>{product.category}</p> <p>{product.price.toLocaleString()}</p>

            {/* ⭐ 평균 별점 + 숫자 표시 */}
            <div
              style={{
                display: "flex",
                justifyContent: "center",
                alignItems: "center",
                gap: "0.3rem",
              }}
            >
              {renderStars(product.averageRating)}
              <span style={{ color: "#333", marginLeft: "0.3rem" }}>
                {product.averageRating.toFixed(1)}
              </span>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}

export default Main;
