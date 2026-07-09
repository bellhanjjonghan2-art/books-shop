import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useCarts, useDeleteCartItems, useUpdateCartQuantity } from '../../hooks/queries/useCarts'
import styles from './CartPage.module.css'

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

export default function CartPage() {
  const navigate = useNavigate()
  const { data, isPending, isError } = useCarts()
  const updateQuantityMutation = useUpdateCartQuantity()
  const deleteMutation = useDeleteCartItems()

  // 전체 선택 초기값: 목록 최초 로드 시 모든 아이템을 checked=true로 시작한다.
  const [checkedIds, setCheckedIds] = useState(null)
  const [hasInitializedSelection, setHasInitializedSelection] = useState(false)

  if (data && !hasInitializedSelection) {
    setHasInitializedSelection(true)
    setCheckedIds(new Set(data.map((item) => item.itemId)))
  }

  const items = data ?? []
  const checked = checkedIds ?? new Set()
  const allChecked = items.length > 0 && items.every((item) => checked.has(item.itemId))
  const selectedItems = items.filter((item) => checked.has(item.itemId))
  const subtotal = selectedItems.reduce((sum, item) => sum + item.salePrice * item.quantity, 0)

  const toggleAll = () => {
    setCheckedIds(allChecked ? new Set() : new Set(items.map((item) => item.itemId)))
  }

  const toggleItem = (itemId) => {
    setCheckedIds((prev) => {
      const next = new Set(prev ?? [])
      if (next.has(itemId)) {
        next.delete(itemId)
      } else {
        next.add(itemId)
      }
      return next
    })
  }

  const handleQuantityChange = (item, delta) => {
    const nextQuantity = item.quantity + delta
    if (nextQuantity < 1) return
    updateQuantityMutation.mutate({ itemId: item.itemId, quantity: nextQuantity })
  }

  const handleDelete = (itemId) => {
    if (!window.confirm('정말 삭제하시겠습니까?')) return
    deleteMutation.mutate([itemId])
  }

  const handleBuySelected = () => {
    if (selectedItems.length === 0) {
      alert('선택된 상품이 없습니다.')
      return
    }
    navigate('/order', {
      state: { items: selectedItems.map((item) => ({ bookId: item.bookId, quantity: item.quantity })) },
    })
  }

  return (
    <div className={styles.page}>
      <div className={styles.breadcrumb}>
        <Link to="/" className={styles.breadcrumbHome}>
          홈
        </Link>
        <span className={styles.breadcrumbSep}>{'>'}</span>
        <span className={styles.breadcrumbCurrent}>장바구니</span>
      </div>

      <div className={styles.titleArea}>
        <h1 className={styles.pageTitle}>장바구니</h1>
        <p className={styles.pageSubtitle}>선택한 상품을 확인하고 주문을 진행하세요.</p>
      </div>

      <div className={styles.body}>
        <section className={styles.leftColumn}>
          <div className={styles.cartHeader}>
            <div className={styles.cartHeaderLeft}>
              <span className={styles.cartHeaderTitle}>장바구니</span>
              <span className={styles.countBadge}>{items.length}</span>
            </div>
            {items.length > 0 && (
              <button type="button" className={styles.selectAllWrap} onClick={toggleAll}>
                <span className={`${styles.checkbox} ${allChecked ? styles.checkboxChecked : ''}`}>
                  {allChecked && '✓'}
                </span>
                <span className={styles.selectAllLabel}>전체 선택</span>
              </button>
            )}
          </div>

          {isPending ? (
            <div className={styles.empty}>
              <p className={styles.emptyText}>불러오는 중...</p>
            </div>
          ) : isError ? (
            <div className={styles.empty}>
              <p className={styles.emptyText}>장바구니가 비어있습니다.</p>
              <button type="button" className={styles.continueButton} onClick={() => navigate('/')}>
                ← 쇼핑 계속하기
              </button>
            </div>
          ) : items.length === 0 ? (
            <div className={styles.empty}>
              <p className={styles.emptyText}>장바구니가 비어 있습니다.</p>
            </div>
          ) : (
            <ul className={styles.itemList}>
              {items.map((item) => {
                const isChecked = checked.has(item.itemId)
                return (
                  <li
                    key={item.itemId}
                    className={`${styles.itemCard} ${!isChecked ? styles.itemCardUnchecked : ''}`}
                  >
                    <button
                      type="button"
                      className={`${styles.checkbox} ${isChecked ? styles.checkboxChecked : ''}`}
                      onClick={() => toggleItem(item.itemId)}
                    >
                      {isChecked && '✓'}
                    </button>

                    <BookCover coverImage={item.coverImage} title={item.title} />

                    <div className={styles.info}>
                      <p className={styles.itemTitle}>{item.title}</p>
                      <p className={styles.itemMeta}>
                        {item.author} · {item.publisher}
                      </p>
                      <p className={styles.itemPrice}>{item.salePrice.toLocaleString('ko-KR')}원</p>
                      {!isChecked && <p className={styles.deselectedLabel}>선택 해제됨</p>}
                    </div>

                    <div className={styles.actions}>
                      <button
                        type="button"
                        className={styles.deleteBtn}
                        onClick={() => handleDelete(item.itemId)}
                      >
                        🗑 삭제
                      </button>
                      <div className={styles.stepper}>
                        <button
                          type="button"
                          className={styles.stepperBtn}
                          disabled={item.quantity <= 1}
                          onClick={() => handleQuantityChange(item, -1)}
                        >
                          −
                        </button>
                        <span className={styles.stepperValue}>{item.quantity}</span>
                        <button
                          type="button"
                          className={styles.stepperBtn}
                          onClick={() => handleQuantityChange(item, 1)}
                        >
                          +
                        </button>
                      </div>
                    </div>
                  </li>
                )
              })}
            </ul>
          )}
        </section>

        <aside className={styles.sidePanel}>
          <div className={styles.summaryHeader}>
            <p className={styles.summaryTitle}>주문 요약</p>
            {selectedItems.map((item) => (
              <div key={item.itemId} className={styles.summaryRow}>
                <span className={styles.summaryName}>
                  {item.title} × {item.quantity}
                </span>
                <span className={styles.summaryPrice}>
                  {(item.salePrice * item.quantity).toLocaleString('ko-KR')}원
                </span>
              </div>
            ))}
          </div>

          <div className={styles.divider} />

          <div className={styles.totalsArea}>
            <div className={styles.summaryRow}>
              <span className={styles.shippingLabel}>배송비</span>
              <span className={styles.shippingFree}>무료</span>
            </div>
            <div className={styles.totalRow}>
              <span className={styles.totalLabel}>총 결제금액</span>
              <span className={styles.totalPrice}>{subtotal.toLocaleString('ko-KR')}원</span>
            </div>
          </div>

          <div className={styles.buttonArea}>
            <button type="button" className={styles.buyButton} onClick={handleBuySelected}>
              ⚡ 선택 상품 구매하기
            </button>
            <button type="button" className={styles.continueButton} onClick={() => navigate('/')}>
              ← 쇼핑 계속하기
            </button>
          </div>

          <div className={styles.noticeArea}>
            <span>ℹ</span>
            <span>3만원 이상 구매 시 무료 배송</span>
          </div>
        </aside>
      </div>
    </div>
  )
}
