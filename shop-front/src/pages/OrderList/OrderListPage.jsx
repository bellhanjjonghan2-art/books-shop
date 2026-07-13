import { useState } from 'react'
import { Link } from 'react-router'
import { useOrderList } from '../../hooks/queries/useOrders'
import styles from './OrderListPage.module.css'

const PERIOD_OPTIONS = [
  { key: '1m', label: '1개월' },
  { key: '3m', label: '3개월' },
  { key: '6m', label: '6개월' },
  { key: 'all', label: '전체' },
]

const DELIVERY_STATUS_MAP = {
  PREPARING: { label: '배송준비중', className: 'badgePreparing' },
  SHIPPING: { label: '배송중', className: 'badgeShipping' },
  DELIVERED: { label: '배송완료', className: 'badgeDelivered' },
  CANCELED: { label: '취소', className: 'badgeCanceled' },
}

const PAGE_SIZE = 5

const formatOrderDate = (isoDate) => (isoDate ? isoDate.replaceAll('-', '.') : '')

const getPageRange = (current, total) => {
  const delta = 2
  const start = Math.max(0, current - delta)
  const end = Math.min(total - 1, current + delta)
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
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

export default function OrderListPage() {
  const [period, setPeriod] = useState('1m')
  const [currentPage, setCurrentPage] = useState(0)

  const { data, isPending, isError } = useOrderList({
    period,
    page: currentPage,
    size: PAGE_SIZE,
  })

  const items = data?.items ?? []
  const totalPages = data?.totalPages ?? 0

  const handlePeriodChange = (key) => {
    setPeriod(key)
    setCurrentPage(0)
  }

  return (
    <div className={styles.page}>
      <div className={styles.breadcrumb}>
        <Link to="/" className={styles.breadcrumbHome}>
          홈
        </Link>
        <span className={styles.breadcrumbSep}>{'>'}</span>
        <span className={styles.breadcrumbCurrent}>주문/배송 목록</span>
      </div>

      <div className={styles.titleArea}>
        <div className={styles.titleTexts}>
          <h1 className={styles.pageTitle}>주문/배송 목록</h1>
          <p className={styles.pageSubtitle}>최근 주문 내역을 확인하세요</p>
        </div>
        <div className={styles.periodButtons}>
          {PERIOD_OPTIONS.map((opt) => (
            <button
              key={opt.key}
              className={period === opt.key ? styles.periodBtnActive : styles.periodBtn}
              onClick={() => handlePeriodChange(opt.key)}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>

      <div className={styles.body}>
        {isPending && <p className={styles.stateMessage}>불러오는 중...</p>}
        {isError && <p className={styles.stateMessage}>주문 내역을 불러오지 못했습니다.</p>}
        {!isPending && !isError && items.length === 0 && (
          <p className={styles.stateMessage}>주문 내역이 없습니다.</p>
        )}

        {!isPending && !isError && items.length > 0 && (
          <ul className={styles.orderList}>
            {items.map((order) => {
              const statusInfo = DELIVERY_STATUS_MAP[order.deliveryStatus] ?? {
                label: order.deliveryStatus,
                className: 'badgeCanceled',
              }
              return (
                <li key={order.orderId} className={styles.orderCard}>
                  <div className={styles.cardHeader}>
                    <div className={styles.cardHeaderLeft}>
                      <span className={styles.orderId}>{order.orderId}</span>
                      <span className={styles.headerSep}>|</span>
                      <span className={styles.orderDate}>{formatOrderDate(order.orderDate)}</span>
                    </div>
                    <span className={`${styles.statusBadge} ${styles[statusInfo.className]}`}>
                      {statusInfo.label}
                    </span>
                  </div>

                  <ul className={styles.itemList}>
                    {order.items.map((item) => (
                      <li key={item.bookId} className={styles.itemRow}>
                        <BookCover coverImage={item.coverImage} title={item.title} />
                        <div className={styles.itemInfo}>
                          <p className={styles.itemTitle}>{item.title}</p>
                          <p className={styles.itemQuantity}>수량: {item.quantity}권</p>
                        </div>
                        <p className={styles.itemAmount}>
                          {item.amount.toLocaleString('ko-KR')}원
                        </p>
                      </li>
                    ))}
                  </ul>

                  <div className={styles.cardFooter}>
                    <span className={styles.footerLabel}>합계</span>
                    <span className={styles.footerAmount}>
                      {order.totalAmount.toLocaleString('ko-KR')}원
                    </span>
                  </div>
                </li>
              )
            })}
          </ul>
        )}

        {totalPages > 1 && (
          <div className={styles.pagination}>
            <button
              className={styles.pageBtn}
              onClick={() => setCurrentPage(0)}
              disabled={currentPage === 0}
            >
              «
            </button>
            <button
              className={styles.pageBtn}
              onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
              disabled={currentPage === 0}
            >
              ‹
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
              ›
            </button>
            <button
              className={styles.pageBtn}
              onClick={() => setCurrentPage(totalPages - 1)}
              disabled={currentPage === totalPages - 1}
            >
              »
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
