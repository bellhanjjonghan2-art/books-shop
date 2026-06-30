import BookCard from '../BookCard/BookCard'
import styles from './BookSection.module.css'

export default function BookSection({ title, books = [] }) {
  return (
    <section className={styles.section}>
      <h2 className={styles.heading}>
        <span className={styles.bar} />
        {title}
      </h2>
      <div className={styles.list}>
        {books.map((book) => (
          <BookCard
            key={book.id}
            id={book.id}
            title={book.title}
            author={book.author}
            salePrice={book.salePrice}
            coverImage={book.coverImage}
          />
        ))}
      </div>
    </section>
  )
}
