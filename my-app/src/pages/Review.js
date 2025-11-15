import React, { useState, useEffect, useRef, useContext } from "react";
import { useParams, useNavigate } from "react-router-dom";
import axios from "axios";
import { AuthContext } from "../contexts/AuthContext";

function ReviewForm() {
  const { userRole } = useContext(AuthContext);
  const { productId } = useParams();
  const navigate = useNavigate();
  const hasRedirected = useRef(false);

  const [rating, setRating] = useState(0);
  const [content, setContent] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!hasRedirected.current) {
      hasRedirected.current = true;
      if (userRole === "ANONYMOUS") {
        alert("로그인이 필요합니다.");
        navigate("/login", { replace: true });
        return;
      }

      const validateReviewEligibility = async () => {
        try {
          // 1. 상품 존재 확인
          await axios.get(`http://localhost:8080/products/${productId}`, { withCredentials: true });
        } catch (err) {
          alert("해당 상품의 상품평을 작성할 수 없습니다.");
          navigate(-1);
          return;
        }

        try {
          // 2. 유저의 주문 목록에서 상품 구매 여부 확인
          const res = await axios.get(`http://localhost:8080/auth/orders/all`, { withCredentials: true });
          const orders = res.data;

          let hasPurchased = false;
          let hasWrittenReview = false;

          for (const order of orders) {
            if (order.status !== "PAID") continue;
            for (const item of order.orderItems) {
              if (item.productId === Number(productId)) {
                hasPurchased = true;
                if (item.reviewId !== null) {
                  hasWrittenReview = true;
                }
              }
            }
          }

          if (!hasPurchased) {
            alert("해당 상품의 상품평을 작성할 수 없습니다.");
            navigate(-1);
            return;
          }

          if (hasWrittenReview) {
            alert("이미 해당 상품의 리뷰를 작성하셨습니다. 리뷰를 수정하려면 마이페이지에서 수정해주세요.");
            navigate("/mypage");
            return;
          }

          setLoading(false);
        } catch (err) {
          console.error("리뷰 작성 조건 확인 중 오류:", err);
          setLoading(false);
        }
      };

      validateReviewEligibility();
    }
  }, [userRole, productId, navigate]);

  if (userRole === "ANONYMOUS" || loading) {
    return null;
  }

  const handleStarClick = (value) => setRating(value);

  const handleSubmit = async () => {
    if (rating === 0) {
      alert("별점을 선택해주세요!");
      return;
    }
    if (content.trim().length === 0) {
      alert("리뷰 내용을 입력해주세요!");
      return;
    }

    try {
      await axios.post(
        `http://localhost:8080/products/${productId}/reviews`,
        {
          rating,
          reviewText: content,
        },
        { withCredentials: true }
      );
      alert("리뷰가 등록되었습니다.");
      navigate(`/products/${productId}`);
    } catch (err) {
      console.error(err);
      alert("리뷰 등록 실패");
    }
  };

  const Star = ({ value }) => (
    <span
      onClick={() => handleStarClick(value)}
      style={{
        fontSize: "2rem",
        cursor: "pointer",
        color: value <= rating ? "#FFD700" : "#ccc",
      }}
    >
      ★
    </span>
  );

  return (
    <div style={{ padding: "20px", maxWidth: "600px", margin: "auto" }}>
      <h2>리뷰 작성</h2>

      <div style={{ margin: "1rem 0" }}>
        {[1, 2, 3, 4, 5].map((v) => (
          <Star key={v} value={v} />
        ))}
      </div>

      <textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        rows="5"
        placeholder="리뷰 내용을 입력하세요 (최대 500자)"
        style={{
          width: "100%",
          padding: "8px",
          resize: "vertical",
          border: "1px solid #ccc",
          borderRadius: "4px",
        }}
        maxLength={500}
      />

      <button
        onClick={handleSubmit}
        style={{
          marginTop: "1rem",
          backgroundColor: "#4CAF50",
          color: "#fff",
          border: "none",
          padding: "0.5rem 1rem",
          cursor: "pointer",
          borderRadius: "4px",
        }}
      >
        등록
      </button>
    </div>
  );
}

export default ReviewForm;
