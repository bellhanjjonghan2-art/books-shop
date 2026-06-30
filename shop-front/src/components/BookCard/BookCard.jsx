import { useNavigate } from 'react-router'
import styles from './BookCard.module.css'

export default function BookCard({ id, title, author, salePrice, coverImage }) {
  const navigate = useNavigate()
  const formattedPrice = salePrice?.toLocaleString('ko-KR') + '원'

  return (
    <div className={styles.card} onClick={() => navigate(`/books/${id}`)}>
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
