import styles from './NovelPage.module.css'

export default function NovelPage() {
  return (
    <div className={styles.container}>
      <h1 className={styles.title}>소설</h1>
      <p className={styles.description}>국내외 다양한 소설 작품을 만나보세요.</p>
    </div>
  )
}
