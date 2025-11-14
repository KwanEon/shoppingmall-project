import React, { useEffect, useState, useContext } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import { AuthContext } from "../contexts/AuthContext";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faStar as fullStar, faStarHalfAlt as halfStar } from "@fortawesome/free-solid-svg-icons";
import { faStar as emptyStar } from "@fortawesome/free-regular-svg-icons";
import "../styles/ProductList.css";

const categoryOptions = [
  { value: "", label: "카테고리 선택" },
  { value: "ELECTRONICS", label: "전자제품" },
  { value: "CLOTHING", label: "의류" },
  { value: "FOOD", label: "식품" },
  { value: "FURNITURE", label: "가구" },
  { value: "TOYS", label: "장난감" },
];

function Products() {
  const { userRole } = useContext(AuthContext);
  const navigate = useNavigate();
  const [products, setProducts] = useState([]);
  const [category, setCategory] = useState("");
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const renderStars = (rating) => {
    if (rating == null) rating = 0;
    return [...Array(5)].map((_, i) => {
      const starValue = i + 1;
      return (
        <FontAwesomeIcon
          key={i}
          icon={rating >= starValue ? fullStar : rating >= starValue - 0.5 ? halfStar : emptyStar}
          className="star"
        />
      );
    });
  };

  const fetchProducts = async (currentPage = 0) => {
    setLoading(true);
    setError("");
    try {
      const params = { page: currentPage };
      if (category) params.category = category;
      if (keyword) params.keyword = keyword;

      const res = await axios.get("http://localhost:8080/products", {
        params,
        withCredentials: true,
      });

      const data = res.data;
      setProducts(data.content || []);
      setPage(data.number);
      setTotalPages(data.totalPages);
    } catch (e) {
      setError("상품 목록을 불러오는 중 오류가 발생했습니다.");
      setProducts([]);
    }
    setLoading(false);
  };

  useEffect(() => {
    fetchProducts(page);
  }, []);

  const handleAddProduct = () => navigate("/add-product");
  const handleEditProduct = (productId) => navigate(`/edit-product/${productId}`);
  const handleDeleteProduct = async (productId) => {
    if (!window.confirm("정말로 이 상품을 삭제하시겠습니까?")) return;
    try {
      await axios.delete(`http://localhost:8080/products/${productId}`, { withCredentials: true });
      fetchProducts();
      alert("상품이 삭제되었습니다.");
    } catch {
      alert("상품 삭제 중 오류가 발생했습니다.");
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    fetchProducts();
  };

  const handlePageChange = (newPage) => fetchProducts(newPage);

  return (
    <section className="main-section">
      <h1>상품 목록</h1>

      <form className="search-form" onSubmit={handleSearch}>
        <select value={category} onChange={(e) => setCategory(e.target.value)}>
          {categoryOptions.map(({ value, label }) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>

        <input
          type="text"
          placeholder="검색어 입력"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />

        <button type="submit" className="btn">
          검색
        </button>
      </form>

      {loading && <p>로딩 중...</p>}
      {error && <p className="error-message">{error}</p>}

      {userRole === "ROLE_ADMIN" && (
        <div className="admin-actions">
          <button className="btn" onClick={handleAddProduct}>상품 추가</button>
        </div>
      )}

      <div className="product-grid">
        {products.length === 0 ? (
          <p>상품이 없습니다.</p>
        ) : (
          products.map((product) => (
            <div key={product.id} className="product-card">
              <img
                onClick={() => navigate(`/products/${product.id}`)}
                src={
                  product.imageUrl?.startsWith("http")
                    ? product.imageUrl
                    : `http://localhost:8080${product.imageUrl}`
                }
                onError={(e) => { e.target.src = "http://localhost:8080/static/images/noimage.jpg"; }}
                alt={product.name}
              />
              <h4 onClick={() => navigate(`/products/${product.id}`)}>{product.name}</h4>
              <p>{product.category}</p>
              <p>{product.price.toLocaleString()}원</p>
              <div className="rating">
                {renderStars(product.averageRating)}
                <span>{product.averageRating?.toFixed(1) || 0}</span>
              </div>
              <small>({product.reviewCount})</small>

              {userRole === "ROLE_ADMIN" && (
                <div className="admin-actions">
                  <button className="btn" onClick={() => handleEditProduct(product.id)}>수정</button>
                  <button className="btn btn-danger" onClick={() => handleDeleteProduct(product.id)}>삭제</button>
                </div>
              )}
            </div>
          ))
        )}
      </div>

      {totalPages > 1 && (
        <div className="pagination">
          <button className="btn" onClick={() => handlePageChange(0)} disabled={page === 0}>
            First
          </button>
          <button className="btn" onClick={() => handlePageChange(page - 1)} disabled={page === 0}>
            Prev
          </button>

          {[...Array(totalPages)].map((_, idx) => {
            if (idx < page - 2 || idx > page + 2) return null;
            return (
              <button
                key={idx}
                onClick={() => handlePageChange(idx)}
                className={idx === page ? "btn active" : "btn"}
              >
                {idx + 1}
              </button>
            );
          })}

          <button className="btn" onClick={() => handlePageChange(page + 1)} disabled={page >= totalPages - 1}>
            Next
          </button>
          <button className="btn" onClick={() => handlePageChange(totalPages - 1)} disabled={page >= totalPages - 1}>
            Last
          </button>
        </div>
      )}
    </section>
  );
}

export default Products;
