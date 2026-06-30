import { Routes, Route } from 'react-router'
import MainLayout from './layouts/MainLayout/MainLayout'
import HealthPage from './pages/HealthPage'
import LoginPage from './pages/LoginPage/LoginPage'

function App() {
  return (
    <Routes>
      <Route path="/" element={<MainLayout />}>
        <Route index element={<HealthPage />} />
        <Route path="login" element={<LoginPage />} />
      </Route>
    </Routes>
  )
}

export default App
