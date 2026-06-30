import { createBrowserRouter } from 'react-router'
import MainLayout from './layouts/MainLayout/MainLayout'
import HomePage from './pages/Home/HomePage'
import LoginPage from './pages/LoginPage/LoginPage'
import ITBooksPage from './pages/ITBooks/ITBooksPage'
import NovelPage from './pages/Novel/NovelPage'
import SelfDevPage from './pages/SelfDev/SelfDevPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'it-books', element: <ITBooksPage /> },
      { path: 'novel', element: <NovelPage /> },
      { path: 'self-dev', element: <SelfDevPage /> },
    ],
  },
  {
    path: '/login',
    element: <LoginPage />,
  },
])
