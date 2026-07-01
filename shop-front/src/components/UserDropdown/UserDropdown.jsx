import { useState, useRef, useEffect } from 'react'
import { useNavigate } from 'react-router'
import { useAuthStore } from '../../stores/useAuthStore'
import styles from './UserDropdown.module.css'

export default function UserDropdown({ user }) {
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()
  const logout = useAuthStore((state) => state.logout)
  const ref = useRef(null)

  useEffect(() => {
    const handler = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  const handleLogout = () => {
    logout()
    navigate('/')
    setOpen(false)
  }

  const handleProfile = () => {
    navigate('/profile')
    setOpen(false)
  }

  const handleCart = () => {
    navigate('/cart')
    setOpen(false)
  }

  return (
    <div className={styles.wrapper} ref={ref}>
      <button className={styles.trigger} onClick={() => setOpen((v) => !v)}>
        <span>{user?.names}님</span>
        <span className={`${styles.chevron} ${open ? styles.chevronUp : ''}`}>▾</span>
      </button>

      {open && (
        <div className={styles.dropdown}>
          <button className={styles.item} onClick={handleProfile}>
            프로필
          </button>
          <button className={styles.item} onClick={handleCart}>
            장바구니
          </button>
          <button className={`${styles.item} ${styles.itemLogout}`} onClick={handleLogout}>
            로그아웃
          </button>
        </div>
      )}
    </div>
  )
}
