import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faStar as fullStar, faStarHalfAlt as halfStar } from "@fortawesome/free-solid-svg-icons";
import { faStar as emptyStar } from "@fortawesome/free-regular-svg-icons";
import "../styles/Main.css";

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
    <section className="main-section">
      <h2 className="main-title">쇼핑몰 프로젝트 메인</h2>

      <button className="btn" onClick={goToProducts}>
        상품 목록 보기
      </button>
      <br></br><br></br>
      <h3 className="section-title">🔥 인기상품 TOP 3</h3>
      <div className="popular-products">
        {popularProducts.map((product) => (
          <div key={product.id} className="product-card">
            <img
              onClick={() => navigate(`products/${product.id}`)}
              src={
                product.imageUrl?.startsWith("http")
                  ? product.imageUrl
                  : `http://localhost:8080${product.imageUrl}`
              }
              onError={(e) => {
                e.target.src = "http://localhost:8080/static/images/noimage.jpg";
              }}
              alt={product.name}
            />
            <h4 onClick={() => navigate(`products/${product.id}`)}>
              {product.name}
            </h4>
            <p>{product.category}</p>
            <p>{product.price.toLocaleString()}원</p>
            <div className="rating">
              {renderStars(product.averageRating)}
              <span>{product.averageRating.toFixed(1)}</span>
            </div>
            <small>({product.reviewCount})</small>
          </div>
        ))}
      </div>
    </section>
  );
}

export default Main;
