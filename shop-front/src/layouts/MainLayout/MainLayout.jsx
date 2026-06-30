import { NavLink, Outlet, useLocation, Link } from 'react-router'
import { useAuthStore } from '../../stores/useAuthStore'
import UserDropdown from '../../components/UserDropdown/UserDropdown'
import styles from './MainLayout.module.css'

const navClass = ({ isActive }) =>
  `${styles.link} ${isActive ? styles.active : ''}`

export default function MainLayout() {
  const location = useLocation()
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated)
  const user = useAuthStore((state) => state.user)

  return (
    <div className={styles.layout}>
      <header className={styles.header}>
        <nav className={styles.nav}>
          <NavLink to="/" end className={navClass}>Home</NavLink>
          <NavLink to="/it-books" className={navClass}>IT 서적</NavLink>
          <NavLink to="/novel" className={navClass}>소설</NavLink>
          <NavLink to="/self-dev" className={navClass}>자기개발서</NavLink>
        </nav>

        <div className={styles.authArea}>
          {isAuthenticated ? (
            <UserDropdown user={user} />
          ) : (
            <Link to="/login" state={{ from: location }} className={styles.authButton}>
              로그인
            </Link>
          )}
        </div>
      </header>

      <main className={styles.content}>
        <Outlet />
      </main>
    </div>
  )
}
