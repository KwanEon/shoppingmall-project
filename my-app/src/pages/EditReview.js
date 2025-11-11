import React, { useEffect, useState, useRef, useContext } from "react";
import { useParams, useNavigate } from "react-router-dom";
import axios from "axios";
import { AuthContext } from "../contexts/AuthContext";

function EditReview() {
  const { userRole } = useContext(AuthContext);
  const { reviewId } = useParams();
  const navigate = useNavigate();
  const hasRedirected = useRef(false);

  const [rating, setRating] = useState(0);
  const [content, setContent] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchReview = async () => {
      if (!hasRedirected.current) {
        if (userRole === "ANONYMOUS") {
          hasRedirected.current = true;
          alert("로그인이 필요합니다.");
          navigate("/login");
          return;
        }
      }

      try {
        const res = await axios.get(
          `http://localhost:8080/products/reviews/${reviewId}`,
          { withCredentials: true }
        );

        setRating(res.data.rating);
        setContent(res.data.reviewText);
      } catch (err) {
        if (!hasRedirected.current) {
          console.error(err);
          hasRedirected.current = true;

          const status = err.response?.status;
          if (status === 403) {
            alert("잘못된 접근입니다.");
          } else {
            alert("리뷰 정보를 불러오지 못했습니다.");
          }

          navigate(-1);
        }
      } finally {
        setLoading(false);
      }
    };

    fetchReview();
  }, [userRole, reviewId, navigate]);


  if (userRole === "ANONYMOUS" || loading) {
    return null;
  }

  const handleSubmit = async () => {
    try {
      await axios.put(
        `http://localhost:8080/products/reviews/${reviewId}`,
        { rating, reviewText: content },
        { withCredentials: true }
      );
      alert("리뷰가 수정되었습니다.");
      navigate(-1);
    } catch (err) {
      console.error(err);
      alert("리뷰 수정 실패");
    }
  };

  const Star = ({ value }) => (
    <span
      onClick={() => setRating(value)}
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
      <h2>리뷰 수정</h2>

      <div style={{ margin: "1rem 0" }}>
        {[1, 2, 3, 4, 5].map((v) => (
          <Star key={v} value={v} />
        ))}
      </div>

      <textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        rows="5"
        style={{ width: "100%", padding: "8px" }}
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
        수정 완료
      </button>
    </div>
  );
}

export default EditReview;
