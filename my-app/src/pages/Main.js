import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

function Main() {
  const navigate = useNavigate();
  const [popularProducts, setPopularProducts] = useState([]);

  const goToProducts = () => {
    navigate('/products');
  };

  useEffect(() => {
    axios
      .get('http://localhost:8080/products/popular', { withCredentials: true })
      .then((res) => setPopularProducts(res.data))
      .catch((err) => console.error('인기상품 불러오기 실패:', err));
  }, []);

  return (
    <section style={{ padding: '2rem' }}>
      <h2>쇼핑몰 프로젝트 메인</h2>

      <button 
        onClick={goToProducts} 
        style={{ margin: '1rem 0', padding: '0.5rem 1rem', cursor: 'pointer' }}
      >
        상품 목록 보기
      </button>

      <h3>🔥 인기상품 TOP 3</h3>
      <div style={{ display: 'flex', gap: '1rem', marginTop: '1rem' }}>
        {popularProducts.map((product) => (
          <div
            key={product.id}
            style={{
              border: '1px solid #ddd',
              borderRadius: '8px',
              padding: '1rem',
              width: '200px',
              textAlign: 'center',
            }}
          >
            <img
              src={product.thumbnailUrl.startsWith('http') ? product.thumbnailUrl : `http://localhost:8080${product.thumbnailUrl}`}
              alt={product.name}
              style={{ width: '100%', height: '150px', objectFit: 'cover', borderRadius: '8px' }}
            />
            <h4 onClick={() => navigate(`products/${product.id}`)} style={{ cursor: "pointer", color: "blue", textDecoration: "underline" }}>{product.name}</h4>
            <p style={{ color: '#555' }}>{product.price.toLocaleString()}원</p>
          </div>
        ))}
      </div>
    </section>
  );
}

export default Main;
