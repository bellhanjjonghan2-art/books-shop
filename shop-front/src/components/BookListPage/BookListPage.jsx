import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useAuthStore } from '../../stores/useAuthStore'
import { useBooksByCategory } from '../../hooks/queries/useBooks'
import { useAddCartItem } from '../../hooks/queries/useCarts'
import ConfirmModal from '../ConfirmModal/ConfirmModal'
import styles from './BookListPage.module.css'

// 프론트 내부 정렬 키 → API orderType 매핑
const ORDER_TYPE_MAP = {
  newest: 'new',
  priceLow: 'lower',
  priceHigh: 'high',
  reviews: 'reviewCnt',
}

const formatPublishedDate = (isoDate) => (isoDate ? isoDate.slice(0, 7).replace('-', '.') : '')

const getPageRange = (current, total) => {
  const delta = 2
  const start = Math.max(0, current - delta)
  const end = Math.min(total - 1, current + delta)
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
}

const discountRate = (listPrice, salePrice) =>
  Math.round(((listPrice - salePrice) / listPrice) * 100)

const SORT_OPTIONS = [
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

function BookCover({ coverImage, title }) {
  const [failed, setFailed] = useState(false)
  if (!coverImage || failed) {
    return <div className={styles.cover} />
  }
  return (
    <img
      src={coverImage}
      alt={title}
      className={styles.cover}
      onError={() => setFailed(true)}
    />
  )
}

export default function BookListPage({ categoryTitle, categoryType }) {
  const [sortKey, setSortKey] = useState('newest')
  const [currentPage, setCurrentPage] = useState(0)
  const [modal, setModal] = useState(null) // null | 'login' | 'added'
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const navigate = useNavigate()
  const addCartItem = useAddCartItem()

  const { data, isPending, isError } = useBooksByCategory(categoryType, {
    page: currentPage,
    size: PAGE_SIZE,
    orderType: ORDER_TYPE_MAP[sortKey],
  })

  const books = data?.content ?? []
  const totalElements = data?.totalElements ?? 0
  const totalPages = data?.totalPages ?? 0

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

  const handleCartClick = (e, bookId) => {
    e.stopPropagation()
    if (!isAuthenticated) {
      setModal('login')
      return
    }
    addCartItem.mutate(
      { bookId, quantity: 1 },
      {
        onSuccess: (data) => {
          if (data.code === 200) {
            setModal('added')
          }
        },
      },
    )
  }

  return (
    <div className={styles.page}>
      {/* 헤더: 제목 + 정렬 */}
      <div className={styles.header}>
        <div className={styles.titleArea}>
          <h1 className={styles.categoryTitle}>{categoryTitle}</h1>
          <span className={styles.totalBadge}>총 {totalElements}권</span>
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

      {isPending && <p className={styles.stateMessage}>불러오는 중…</p>}
      {isError && <p className={styles.stateMessage}>도서 목록을 불러오지 못했습니다.</p>}
      {!isPending && !isError && books.length === 0 && (
        <p className={styles.stateMessage}>등록된 도서가 없습니다.</p>
      )}

      {/* 도서 목록 */}
      {!isPending && !isError && books.length > 0 && (
        <ul className={styles.list}>
          {books.map((book) => (
            <li
              key={book.bookId}
              className={styles.item}
              onClick={() => navigate('/books/' + book.bookId)}
            >
              {/* 표지 이미지 — 없거나 로드 실패 시 색상 placeholder로 대체 */}
              <BookCover coverImage={book.coverImage} title={book.title} />

              {/* 도서 정보 */}
              <div className={styles.itemBody}>
                {book.bestYn === 'Y' ? (
                  <span className={styles.badgeBest}>베스트</span>
                ) : book.newYn === 'Y' ? (
                  <span className={styles.badgeNew}>신간</span>
                ) : null}
                <p className={styles.itemTitle}>{book.title}</p>
                {book.subtitle && (
                  <p className={styles.itemSubtitle}>{book.subtitle}</p>
                )}
                <p className={styles.itemMeta}>
                  {book.author} | {book.publisher} | {formatPublishedDate(book.publishedDate)}
                </p>
                <div className={styles.ratingRow}>
                  {book.reviewRating == null ? (
                    <span className={styles.reviewCount}>리뷰 없음</span>
                  ) : (
                    <>
                      <StarRating rating={book.reviewRating} />
                      <span className={styles.ratingScore}>{book.reviewRating}</span>
                    </>
                  )}
                  <span className={styles.reviewCount}>
                    (리뷰 {book.totalReviewCnt.toLocaleString()}건)
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
                <button className={styles.cartBtn} onClick={(e) => handleCartClick(e, book.bookId)}>
                  장바구니
                </button>
                <button className={styles.buyBtn} onClick={handleAuthAction}>
                  바로구매
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

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

      <ConfirmModal
        open={modal === 'login'}
        message={'로그인이 필요한 기능입니다.\n로그인 하시겠습니까?'}
        confirmLabel="로그인"
        cancelLabel="취소"
        onConfirm={() => navigate('/login')}
        onCancel={() => setModal(null)}
      />
      <ConfirmModal
        open={modal === 'added'}
        message={'장바구니에 도서를 담았습니다.\n장바구니로 이동하시겠습니까?'}
        confirmLabel="장바구니로 이동"
        cancelLabel="계속 쇼핑"
        onConfirm={() => navigate('/cart')}
        onCancel={() => setModal(null)}
      />
    </div>
  )
}
