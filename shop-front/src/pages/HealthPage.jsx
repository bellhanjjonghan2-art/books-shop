import { useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { fetchHealth } from '../api/health'
import { useHealthStore } from '../store/useHealthStore'
import styles from './HealthPage.module.css'

export default function HealthPage() {
  const { data } = useQuery({ queryKey: ['health'], queryFn: fetchHealth })
  const setLastStatus = useHealthStore((state) => state.setLastStatus)

  useEffect(() => {
    if (data?.status) {
      setLastStatus(data.status)
    }
  }, [data, setLastStatus])

  return (
    <div className={styles.container}>
      <h1>books-shop</h1>
      <p className={styles.status}>status: {data?.status ?? 'loading...'}</p>
    </div>
  )
}
