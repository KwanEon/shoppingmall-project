import React, {useContext } from "react";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import Login from "./pages/Login";
import Register from "./pages/Register";
import EmailVerify from "./pages/EmailVerify";
import Home from "./pages/Main";
import { ProductList } from "./pages/Products";
import { ProductDetail } from "./pages/ProductDetail";
import AddProduct from "./pages/AddProduct";
import EditProduct from "./pages/EditProduct";
import MyPage from "./pages/MyPage";
import Cart from "./pages/Cart";
import CartOrder from "./pages/CartOrder";
import IndOrder from "./pages/IndOrder";
import Review from "./pages/Review";
import EditReview from "./pages/EditReview";
import PaymentSuccess from "./pages/PaymentSuccess";
import Layout from "./components/Layout";
import { AuthContext } from "./contexts/AuthContext"

function App() {
  const { loading } = useContext(AuthContext);

  if (loading) {
    return null;
  }

  return (
    <Router>
      <Layout>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/auth/verify" element={<EmailVerify />} />
          <Route path="/" element={<Home />} />
          <Route path="/products" element={<ProductList />} />
          <Route path="/add-product" element={<AddProduct />} />
          <Route path="/products/:productId" element={<ProductDetail />} />
          <Route path="/edit-product/:productId" element={<EditProduct />} />
          <Route path="/mypage" element={<MyPage />} />
          <Route path="/cart" element={<Cart />} />
          <Route path="/order/cartitem" element={<CartOrder />} />
          <Route path="/products/:productId/review" element={<Review />} />
          <Route path="/reviews/:reviewId/edit" element={<EditReview />} />
          <Route path="/order/:productId" element={<IndOrder />} />
          <Route path="/payment/success" element={<PaymentSuccess />} />
        </Routes>
      </Layout>
    </Router>
  );
}

export default App;
