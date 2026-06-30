import { useLocation, useNavigate } from 'react-router'
import LoginForm from '../../features/auth/LoginForm'
import styles from './LoginPage.module.css'

export default function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const from = location.state?.from?.pathname ?? '/'

  const handleSuccess = () => {
    navigate(from, { replace: true })
  }

  return (
    <div className={styles.page}>
      <p className={styles.logo}>books-shop</p>
      <LoginForm onSuccess={handleSuccess} />
    </div>
  )
}
