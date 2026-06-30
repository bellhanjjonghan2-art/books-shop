import styles from './BookCard.module.css'

export default function BookCard({ title, author, salePrice, coverImage }) {
  const formattedPrice = salePrice?.toLocaleString('ko-KR') + '원'

  return (
    <div className={styles.card}>
      <div className={styles.cover}>
        {coverImage ? (
          <img src={coverImage} alt={title} className={styles.image} />
        ) : (
          <div className={styles.placeholder} />
        )}
      </div>
      <div className={styles.info}>
        <p className={styles.title}>{title}</p>
        <p className={styles.author}>{author}</p>
        <p className={styles.price}>{formattedPrice}</p>
      </div>
    </div>
  )
}
