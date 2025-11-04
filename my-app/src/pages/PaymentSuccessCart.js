import React, { useEffect, useRef } from "react";
import { useSearchParams } from "react-router-dom";
import axios from "axios";

function PaymentSuccessCart() {
  const [searchParams] = useSearchParams();
  const orderId = searchParams.get("orderId");
  const pgToken = searchParams.get("pg_token");
  const hasAlerted = useRef(false);

  useEffect(() => {
    if (!orderId || !pgToken) {
      alert("잘못된 접근입니다.");
      window.close();
      return;
    }

    if (hasAlerted.current) {
      return;
    }

    hasAlerted.current = true;

    axios
      .post(`http://localhost:8080/payment/success/cart?orderId=${orderId}&pg_token=${pgToken}`,
        {},
        { withCredentials: true }
      )
      .then((res) => {
        console.log("결제 승인 성공:", res.data);
        window.close();
      })
      .catch((err) => {
        console.error("결제 승인 실패:", err);
        alert("결제 승인 중 오류가 발생했습니다.");
        window.close();
      });
  }, [orderId, pgToken]);

  return <div>결제 처리 중...</div>;
}

export default PaymentSuccessCart;