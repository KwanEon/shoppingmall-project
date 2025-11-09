import React, { useEffect, useState, useContext } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import { AuthContext } from "../contexts/AuthContext";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faStar as fullStar, faStarHalfAlt as halfStar } from "@fortawesome/free-solid-svg-icons";
import { faStar as emptyStar } from "@fortawesome/free-regular-svg-icons";

const categoryOptions = [
  { value: "", label: "카테고리 선택" },
  { value: "ELECTRONICS", label: "전자제품" },
  { value: "CLOTHING", label: "의류" },
  { value: "FOOD", label: "식품" },
  { value: "FURNITURE", label: "가구" },
  { value: "TOYS", label: "장난감" },
];

function ProductList() {
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
          style={{ color: "#FFD700" }}
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
    <section style={{ padding: "2rem" }}>
      <h2>상품 목록</h2>

    <form
      onSubmit={handleSearch}
      style={{
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        gap: "0.5rem",
        marginBottom: "1rem",
      }}
    >
      <select
        value={category}
        onChange={(e) => setCategory(e.target.value)}
        style={{ padding: "0.4rem 0.6rem", borderRadius: "6px", border: "1px solid #ccc" }}
      >
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
        style={{ padding: "0.4rem 0.6rem", borderRadius: "6px", border: "1px solid #ccc", minWidth: "200px" }}
      />

      <button
        type="submit"
        style={{
          padding: "0.4rem 0.8rem",
          borderRadius: "6px",
          border: "none",
          backgroundColor: "#111",
          color: "#fff",
          cursor: "pointer",
        }}
      >
        검색
      </button>
    </form>


      {loading && <p>로딩 중...</p>}
      {error && <p style={{ color: "red" }}>{error}</p>}

      {userRole === "ROLE_ADMIN" && (
        <div style={{ marginBottom: "1rem" }}>
          <button onClick={handleAddProduct}>상품 추가</button>
        </div>
      )}

      <div style={{ display: "flex", flexWrap: "wrap", gap: "1rem" }}>
        {products.length === 0 ? (
          <p>상품이 없습니다.</p>
        ) : (
          products.map((product) => (
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
                onClick={() => navigate(`/products/${product.id}`)}
                src={
                  product.imageUrl?.startsWith("http")
                    ? product.imageUrl
                    : `http://localhost:8080${product.imageUrl}`
                }
                alt={product.name}
                style={{
                  width: "100%",
                  height: "150px",
                  objectFit: "cover",
                  borderRadius: "8px",
                  cursor: "pointer" }}
              />
              <h4
                onClick={() => navigate(`/products/${product.id}`)}
                style={{ cursor: "pointer", color: "blue"}}
              >
                {product.name}
              </h4>
              <p>{product.category}</p> <p>{product.price.toLocaleString()}원</p>
              <div style={{
                display: "flex",
                justifyContent: "center",
                alignItems: "center",
                gap: "0.3rem" }}>
                {renderStars(product.averageRating)}
                <span>{product.averageRating?.toFixed(1) || 0}</span>
              </div>

              {userRole === "ROLE_ADMIN" && (
                <div style={{ marginTop: "0.5rem" }}>
                  <button onClick={() => handleEditProduct(product.id)}>수정</button>
                  <button onClick={() => handleDeleteProduct(product.id)}>삭제</button>
                </div>
              )}
            </div>
          ))
        )}
      </div>

      {totalPages > 1 && (
        <div style={{ marginTop: "1rem" }}>
          {[...Array(totalPages)].map((_, idx) => (
            <button
              key={idx}
              onClick={() => handlePageChange(idx)}
              disabled={idx === page}
              style={{
                margin: "0 0.25rem",
                fontWeight: idx === page ? "bold" : "normal",
                padding: "0.3rem 0.6rem",
                border: "1px solid #ccc",
                borderRadius: "4px",
                backgroundColor: idx === page ? "#ddd" : "white",
                cursor: "pointer",
              }}
            >
              {idx + 1}
            </button>
          ))}
        </div>
      )}
    </section>
  );
}

export { ProductList };
