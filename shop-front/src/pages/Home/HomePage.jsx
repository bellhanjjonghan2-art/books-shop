import { useBooks } from '../../hooks/queries/useBooks'
import BookSection from '../../components/BookSection/BookSection'
import styles from './HomePage.module.css'

const SECTIONS = [
  { key: 'bestTopN', label: '베스트 셀러 Top 5' },
  { key: 'newTopN', label: '신간 Top 5' },
  { key: 'ItTopN', label: 'IT 서적 Top 5' },
  { key: 'novelTopN', label: '소설 Top 5' },
  { key: 'selfTopN', label: '자기계발서 Top 5' },
]

export default function HomePage() {
  const { data, isLoading, isError } = useBooks()

  if (isLoading) {
    return (
      <div className={styles.container}>
        <p className={styles.loading}>도서 목록을 불러오는 중입니다...</p>
      </div>
    )
  }

  if (isError) {
    return (
      <div className={styles.container}>
        <p className={styles.error}>도서 목록을 불러오는 데 실패했습니다.</p>
      </div>
    )
  }

  return (
    <div className={styles.container}>
      {SECTIONS.map(({ key, label }) => (
        <BookSection key={key} title={label} books={data?.[key] ?? []} />
      ))}
    </div>
  )
}
