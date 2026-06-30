import { useState, useMemo } from 'react'
import { useNavigate } from 'react-router'
import { useAuthStore } from '../../stores/useAuthStore'
import styles from './BookListPage.module.css'

const SORTS = {
  popular:   (a, b) => b.reviewCount - a.reviewCount,
  newest:    (a, b) => b.publishedDate.localeCompare(a.publishedDate),
  priceLow:  (a, b) => a.salePrice - b.salePrice,
  priceHigh: (a, b) => b.salePrice - a.salePrice,
  reviews:   (a, b) => b.reviewCount - a.reviewCount,
}

const sortBooks = (books, key) => [...books].sort(SORTS[key] || SORTS.popular)

const getPageRange = (current, total) => {
  const delta = 2
  const start = Math.max(0, current - delta)
  const end = Math.min(total - 1, current + delta)
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
}

const discountRate = (listPrice, salePrice) =>
  Math.round(((listPrice - salePrice) / listPrice) * 100)

const SORT_OPTIONS = [
  { key: 'popular',   label: '인기순' },
  { key: 'newest',    label: '최신순' },
  { key: 'priceLow',  label: '낮은가격순' },
  { key: 'priceHigh', label: '높은가격순' },
  { key: 'reviews',   label: '리뷰많은순' },
]

const PAGE_SIZE = 10

function StarRating({ rating }) {
  const filledCount = Math.round(rating / 2)
  return (
    <span className={styles.stars}>
      {Array.from({ length: 5 }, (_, i) => (
        <span key={i} className={i < filledCount ? styles.starOn : styles.starOff}>
          ★
        </span>
      ))}
    </span>
  )
}

export default function BookListPage({ categoryTitle, books }) {
  const [sortKey, setSortKey] = useState('popular')
  const [currentPage, setCurrentPage] = useState(0)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const navigate = useNavigate()

  const sorted = useMemo(() => sortBooks(books, sortKey), [books, sortKey])
  const totalPages = Math.ceil(sorted.length / PAGE_SIZE)
  const paged = sorted.slice(currentPage * PAGE_SIZE, (currentPage + 1) * PAGE_SIZE)

  const handleSort = (key) => {
    setSortKey(key)
    setCurrentPage(0)
  }

  const handleAuthAction = (e) => {
    e.stopPropagation()
    if (!isAuthenticated) {
      alert('로그인이 필요한 기능입니다.')
    }
  }

  return (
    <div className={styles.page}>
      {/* 헤더: 제목 + 정렬 */}
      <div className={styles.header}>
        <div className={styles.titleArea}>
          <h1 className={styles.categoryTitle}>{categoryTitle}</h1>
          <span className={styles.totalBadge}>총 {books.length}권</span>
        </div>
        <div className={styles.sortButtons}>
          {SORT_OPTIONS.map((opt) => (
            <button
              key={opt.key}
              className={sortKey === opt.key ? styles.sortBtnActive : styles.sortBtn}
              onClick={() => handleSort(opt.key)}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>

      {/* 도서 목록 */}
      <ul className={styles.list}>
        {paged.map((book) => (
          <li
            key={book.id}
            className={styles.item}
            onClick={() => navigate('/books/' + book.id)}
          >
            {/* 표지 플레이스홀더 — coverColor는 데이터에서 오는 동적 값이므로 CSS 변수로 전달 */}
            <div
              className={styles.cover}
              style={{ '--cover-color': book.coverColor }}
            />

            {/* 도서 정보 */}
            <div className={styles.itemBody}>
              {book.badge && (
                <span className={book.badge === 'best' ? styles.badgeBest : styles.badgeNew}>
                  {book.badge === 'best' ? '베스트' : '신간'}
                </span>
              )}
              <p className={styles.itemTitle}>{book.title}</p>
              {book.subtitle && (
                <p className={styles.itemSubtitle}>{book.subtitle}</p>
              )}
              <p className={styles.itemMeta}>
                {book.author} | {book.publisher} | {book.publishedDate}
              </p>
              <div className={styles.ratingRow}>
                <StarRating rating={book.rating} />
                <span className={styles.ratingScore}>{book.rating}</span>
                <span className={styles.reviewCount}>
                  (리뷰 {book.reviewCount.toLocaleString()}건)
                </span>
              </div>
              <p className={styles.listPriceRow}>
                정가 {book.listPrice.toLocaleString()}원
              </p>
              <div className={styles.salePriceRow}>
                <span className={styles.discountBadge}>
                  {discountRate(book.listPrice, book.salePrice)}% 할인
                </span>
                <span className={styles.salePrice}>
                  {book.salePrice.toLocaleString()}원
                </span>
              </div>
            </div>

            {/* 액션 버튼 */}
            <div className={styles.actionCol}>
              <button className={styles.cartBtn} onClick={handleAuthAction}>
                장바구니
              </button>
              <button className={styles.buyBtn} onClick={handleAuthAction}>
                바로구매
              </button>
              <button className={styles.wishlistBtn} onClick={handleAuthAction}>
                리스트에 담기
              </button>
            </div>
          </li>
        ))}
      </ul>

      {/* 페이지네이션 */}
      {totalPages > 1 && (
        <div className={styles.pagination}>
          <button
            className={styles.pageBtn}
            onClick={() => setCurrentPage(0)}
            disabled={currentPage === 0}
          >
            ◀◀
          </button>
          <button
            className={styles.pageBtn}
            onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
            disabled={currentPage === 0}
          >
            ◀
          </button>
          {getPageRange(currentPage, totalPages).map((i) => (
            <button
              key={i}
              className={i === currentPage ? styles.pageActive : styles.pageBtn}
              onClick={() => setCurrentPage(i)}
            >
              {i + 1}
            </button>
          ))}
          <button
            className={styles.pageBtn}
            onClick={() => setCurrentPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={currentPage === totalPages - 1}
          >
            ▶
          </button>
          <button
            className={styles.pageBtn}
            onClick={() => setCurrentPage(totalPages - 1)}
            disabled={currentPage === totalPages - 1}
          >
            ▶▶
          </button>
        </div>
      )}
    </div>
  )
}
