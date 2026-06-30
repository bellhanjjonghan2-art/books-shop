import styles from './ITBooksPage.module.css'

export default function ITBooksPage() {
  return (
    <div className={styles.container}>
      <h1 className={styles.title}>IT 서적</h1>
      <p className={styles.description}>프로그래밍, 알고리즘, 클라우드 등 IT 전문 도서를 만나보세요.</p>
    </div>
  )
}
