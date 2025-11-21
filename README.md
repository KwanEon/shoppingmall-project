# 쇼핑몰 프로젝트
Spring Boot + React 기반으로 개발한 쇼핑몰 사이트입니다.  
회원가입, 로그인, 상품 목록 조회, 장바구니, 리뷰, 카카오페이 결제 기능을 제공합니다.

---

## 🛠 기술 스택

| 영역 | 기술 |
|------|------|
| IDE | VS Code |
| Backend | Java 21, Spring Boot 3.4.11, Spring Security, JPA, Gradle |
| Frontend | React, JavaScript, HTML, CSS |
| DB | MySQL |
| 사용한 AI | ChatGPT, GitHub Copilot, Gemini |

---

## ✨ 주요 기능

### 🔹 사용자 기능
- 회원가입 / 로그인
- 마이페이지

### 🔹 상품
- 상품 목록, 상세조회
- 검색 / 필터

### 🔹 장바구니
- 장바구니 추가 / 수정 / 삭제
- 장바구니 기반 주문

### 🔹 주문 & 결제
- 단건 주문
- 카카오페이 결제 연동

### 🔹 리뷰
- 작성 / 수정

### 🔹 관리자
- 상품 등록 / 수정 / 삭제

---

## 🎥 시연 영상
[유튜브로 보기](https://youtu.be/V01i80B_UPc)

---

## 🏗 DB 구조
<img width="1426" height="1095" alt="ER Diagram" src="https://github.com/user-attachments/assets/b97ae574-d464-45a8-af26-c6adc07ac6f3" />

---

## 🚀 실행 방법

### Backend
project/src/main/resources/application-dev.properties 작성 (application-dev.properties.example 참고)
```bash
cd project
./gradlew bootRun
```

### Frontend
```bash
cd my-app
npm install
npm start
```
