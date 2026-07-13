import { createBrowserRouter } from 'react-router'
import MainLayout from './layouts/MainLayout/MainLayout'
import HomePage from './pages/Home/HomePage'
import LoginPage from './pages/LoginPage/LoginPage'
import ITBooksPage from './pages/ITBooks/ITBooksPage'
import NovelPage from './pages/Novel/NovelPage'
import SelfDevPage from './pages/SelfDev/SelfDevPage'
import BookDetailPage from './pages/BookDetail/BookDetailPage'
import CartPage from './pages/Cart/CartPage'
import OrderPage from './pages/Order/OrderPage'
import PaymentResultPage from './pages/PaymentResult/PaymentResultPage'
import OrderListPage from './pages/OrderList/OrderListPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'it-books', element: <ITBooksPage /> },
      { path: 'novel', element: <NovelPage /> },
      { path: 'self-dev', element: <SelfDevPage /> },
      { path: 'books/:bookId', element: <BookDetailPage /> },
      { path: 'cart', element: <CartPage /> },
      { path: 'order', element: <OrderPage /> },
      { path: 'payments/result', element: <PaymentResultPage /> },
      { path: 'orders', element: <OrderListPage /> },
    ],
  },
  {
    path: '/login',
    element: <LoginPage />,
  },
])
