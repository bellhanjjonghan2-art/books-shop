import { useState } from 'react'
import { Link } from 'react-router'
import { useLogin } from '../../hooks/queries/useLogin'
import styles from './LoginForm.module.css'

export default function LoginForm({ onSuccess }) {
  const [userId, setUserId] = useState('')
  const [passwd, setPasswd] = useState('')
  const [validationError, setValidationError] = useState('')

  const { mutate, isPending, isError, error } = useLogin()

  const handleSubmit = (e) => {
    e.preventDefault()
    if (!userId.trim() || !passwd.trim()) {
      setValidationError('아이디와 비밀번호를 모두 입력해주세요.')
      return
    }
    setValidationError('')
    mutate({ userId, passwd }, { onSuccess: () => onSuccess?.() })
  }

  const serverErrorMessage = error?.response?.data?.message

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <h1 className={styles.title}>로그인</h1>

      <div className={styles.field}>
        <label htmlFor="userId" className={styles.label}>아이디</label>
        <input
          id="userId"
          type="text"
          value={userId}
          onChange={(e) => setUserId(e.target.value)}
          className={styles.input}
          autoComplete="username"
        />
      </div>

      <div className={styles.field}>
        <label htmlFor="passwd" className={styles.label}>비밀번호</label>
        <input
          id="passwd"
          type="password"
          value={passwd}
          onChange={(e) => setPasswd(e.target.value)}
          className={styles.input}
          autoComplete="current-password"
        />
      </div>

      {(validationError || (isError && serverErrorMessage)) && (
        <p className={styles.error}>{validationError || serverErrorMessage}</p>
      )}

      <button type="submit" className={styles.button} disabled={isPending}>
        {isPending ? '로그인 중...' : '로그인'}
      </button>

      <p className={styles.signupLink}>
        계정이 없으신가요? <Link to="/signup">회원가입</Link>
      </p>
    </form>
  )
}
