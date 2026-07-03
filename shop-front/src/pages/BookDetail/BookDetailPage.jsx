import { useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { useBookDetail } from '../../hooks/queries/useBookDetail'
import { useAddCartItem } from '../../hooks/queries/useCarts'
import { useAuthStore } from '../../stores/useAuthStore'
import ConfirmModal from '../../components/ConfirmModal/ConfirmModal'
import styles from './BookDetailPage.module.css'

function StarRating({ rating }) {
  return (
    <span className={styles.stars}>
      {[1, 2, 3, 4, 5].map((n) => (
        <span key={n} className={n <= rating ? styles.starOn : styles.starOff}>
          ★
        </span>
      ))}
    </span>
  )
}

function ReviewItem({ review }) {
  const avatarChar = review.reviewerName?.charAt(0) ?? '?'
  return (
    <li className={styles.reviewItem}>
      <div className={styles.reviewHeader}>
        <div className={styles.reviewLeft}>
          <div className={styles.avatar}>{avatarChar}</div>
          <div>
            <p className={styles.reviewerName}>{review.reviewerName}</p>
            <p className={styles.reviewDate}>{formatDate(review.reviewDate)}</p>
          </div>
        </div>
        <StarRating rating={review.rating} />
      </div>
      <p className={styles.reviewContent}>{review.content}</p>
    </li>
  )
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.slice(0, 10).replace(/-/g, '.')
}

const getPageRange = (current, total) => {
  const delta = 2
  const start = Math.max(0, current - delta)
  const end = Math.min(total - 1, current + delta)
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
}

function Pagination({ currentPage, totalPages, onPageChange }) {
  if (totalPages <= 1) return null
  const pageRange = getPageRange(currentPage, totalPages)
  return (
    <div className={styles.pagination}>
      {pageRange.map((i) => (
        <button
          key={i}
          className={i === currentPage ? styles.pageActive : styles.pageBtn}
          onClick={() => onPageChange(i)}
        >
          {i + 1}
        </button>
      ))}
    </div>
  )
}

export default function BookDetailPage() {
  const { bookId } = useParams()
  const [currentPage, setCurrentPage] = useState(0)
  const [modal, setModal] = useState(null) // null | 'login' | 'added'
  const { data, isPending, isError } = useBookDetail(bookId, currentPage)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const addCartItem = useAddCartItem()
  const navigate = useNavigate()

  const handleCart = () => {
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

  const handleBuy = () => {
    if (!isAuthenticated) {
      alert('로그인이 필요한 기능입니다.')
      return
    }
  }

  if (isPending) return <div className={styles.status}>로딩 중...</div>
  if (isError) return <div className={styles.status}>도서 정보를 불러올 수 없습니다.</div>

  const {
    title,
    coverImage,
    author,
    publisher,
    publishedDate,
    listPrice,
    salePrice,
    description,
    reviewList = [],
    reviewTotalCount = 0,
  } = data

  const formattedListPrice = listPrice?.toLocaleString('ko-KR') + '원'
  const formattedSalePrice = salePrice?.toLocaleString('ko-KR') + '원'
  const discountRate =
    listPrice && salePrice ? Math.round((1 - salePrice / listPrice) * 100) : null

  return (
    <div className={styles.page}>
      {/* 상단: 책 표지 + 기본 정보 */}
      <section className={styles.hero}>
        <div className={styles.coverWrap}>
          {coverImage ? (
            <img src={coverImage} alt={title} className={styles.coverImage} />
          ) : (
            <div className={styles.coverPlaceholder} />
          )}
        </div>

        <div className={styles.info}>
          <h1 className={styles.title}>{title}</h1>
          <div className={styles.metaList}>
            <div className={styles.metaRow}>
              <span className={styles.metaLabel}>저자</span>
              <span className={styles.metaValue}>{author}</span>
            </div>
            <div className={styles.metaRow}>
              <span className={styles.metaLabel}>출판사</span>
              <span className={styles.metaValue}>{publisher}</span>
            </div>
            <div className={styles.metaRow}>
              <span className={styles.metaLabel}>출간일</span>
              <span className={styles.metaValue}>{publishedDate}</span>
            </div>
          </div>

          <hr className={styles.divider} />

          <div className={styles.priceSection}>
            <p className={styles.listPrice}>정가 {formattedListPrice}</p>
            <div className={styles.salePriceRow}>
              {discountRate !== null && (
                <span className={styles.discountBadge}>{discountRate}% 할인</span>
              )}
              <span className={styles.salePrice}>{formattedSalePrice}</span>
            </div>
          </div>

          <div className={styles.buttonRow}>
            <button className={styles.cartButton} onClick={handleCart}>
              장바구니
            </button>
            <button className={styles.buyButton} onClick={handleBuy}>
              바로구매
            </button>
          </div>
        </div>
      </section>

      {/* 중단: 책 소개 */}
      <section className={styles.descSection}>
        <h2 className={styles.sectionHeading}>책 소개</h2>
        <div className={styles.descBox}>
          <p className={styles.descText}>{description}</p>
        </div>
      </section>

      {/* 하단: 독자 리뷰 */}
      <section className={styles.reviewSection}>
        <div className={styles.reviewHeadingRow}>
          <h2 className={styles.sectionHeading}>독자 리뷰</h2>
          <span className={styles.reviewCount}>총 {reviewTotalCount}개</span>
        </div>
        {reviewList.length === 0 ? (
          <div className={styles.emptyReview}>등록된 리뷰가 없습니다.</div>
        ) : (
          <ul className={styles.reviewList}>
            {reviewList.map((review) => (
              <ReviewItem key={review.id} review={review} />
            ))}
          </ul>
        )}
        <Pagination
          currentPage={currentPage}
          totalPages={Math.ceil(reviewTotalCount / 10)}
          onPageChange={setCurrentPage}
        />
      </section>

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
