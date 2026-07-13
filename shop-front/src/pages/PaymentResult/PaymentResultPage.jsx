import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { useConfirmPayment } from '../../hooks/queries/usePayments'
import { useOrderResult } from '../../hooks/queries/useOrders'
import styles from './PaymentResultPage.module.css'

const DELIVERY_STATUS_LABEL = {
  PREPARING: '결제 확인 중',
}

export default function PaymentResultPage() {
  const [params] = useSearchParams()
  const paymentKey = params.get('paymentKey')
  const orderIdParam = params.get('orderId')
  const amountParam = params.get('amount')
  const code = params.get('code')
  const message = params.get('message')

  const [status, setStatus] = useState('PENDING')
  const confirmRequested = useRef(false)

  const { mutate: confirmPayment } = useConfirmPayment()

  useEffect(() => {
    if (confirmRequested.current) return
    confirmRequested.current = true

    if (paymentKey) {
      // 성공(인증) 분기 — 서버 승인 요청 (마운트 시 1회만)
      confirmPayment(
        { paymentKey, orderId: orderIdParam, amount: Number(amountParam) },
        {
          onSuccess: () => setStatus('SUCCESS'),
          onError: (error) => {
            console.error(error)
            setStatus('FAILED')
          },
        },
      )
    } else if (code) {
      // 실패(인증) 분기 — 승인 API 호출하지 않음
      setStatus('FAILED')
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const {
    data: order,
    isPending: isOrderPending,
    isError: isOrderError,
  } = useOrderResult(status === 'SUCCESS' ? orderIdParam : undefined)

  if (status === 'PENDING') {
    return (
      <div className={styles.page}>
        <div className={styles.pendingBox}>
          <p className={styles.pendingText}>결제 승인을 처리하고 있습니다...</p>
        </div>
      </div>
    )
  }

  if (status === 'FAILED') {
    return (
      <div className={styles.page}>
        <div className={styles.resultCard}>
          <div className={styles.failIcon}>✕</div>
          <h1 className={styles.failTitle}>결제에 실패했습니다</h1>
          {message && <p className={styles.failMessage}>{message}</p>}
          <Link to="/" className={styles.homeButton}>
            홈으로 돌아가기
          </Link>
        </div>
      </div>
    )
  }

  // SUCCESS — 주문 결과 조회 중
  if (isOrderPending) {
    return (
      <div className={styles.page}>
        <div className={styles.pendingBox}>
          <p className={styles.pendingText}>주문 정보를 불러오는 중...</p>
        </div>
      </div>
    )
  }

  if (isOrderError || !order) {
    return (
      <div className={styles.page}>
        <div className={styles.resultCard}>
          <div className={styles.failIcon}>✕</div>
          <h1 className={styles.failTitle}>주문 정보를 불러올 수 없습니다</h1>
          <Link to="/" className={styles.homeButton}>
            홈으로 돌아가기
          </Link>
        </div>
      </div>
    )
  }

  const totalCount = order.items.reduce((sum, item) => sum + item.quantity, 0)
  const deliveryStatusLabel = DELIVERY_STATUS_LABEL[order.delivery.status] ?? order.delivery.status

  return (
    <div className={styles.page}>
      <div className={styles.completeHeader}>
        <div className={styles.checkIcon}>✓</div>
        <h1 className={styles.completeTitle}>주문이 완료되었습니다</h1>
        <div className={styles.orderIdBadge}>
          <span>주문번호</span>
          <strong>{order.orderId}</strong>
        </div>
        <p className={styles.noticeLine}>주문하신 상품은 결제 확인 후 배송이 시작됩니다.</p>
        <p className={styles.noticeLine}>배송 현황은 마이페이지에서 확인하실 수 있습니다.</p>
      </div>

      <div className={styles.card}>
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionTitle}>주문 상품</h2>
          <span className={styles.itemCountLabel}>
            총 {order.items.length}종 {totalCount}권
          </span>
        </div>
        <ul className={styles.itemList}>
          {order.items.map((item) => (
            <li key={item.bookId} className={styles.itemRow}>
              {item.coverImage ? (
                <img src={item.coverImage} alt={item.title} className={styles.cover} />
              ) : (
                <div className={styles.cover} />
              )}
              <div className={styles.itemInfo}>
                <p className={styles.itemTitle}>{item.title}</p>
                <p className={styles.itemMeta}>
                  {item.author} · {item.publisher}
                </p>
                <p className={styles.itemQuantity}>{item.quantity}권</p>
              </div>
              <p className={styles.itemAmount}>{item.amount.toLocaleString('ko-KR')}원</p>
            </li>
          ))}
        </ul>
      </div>

      <div className={styles.card}>
        <div className={styles.amountSummary}>
          <div className={styles.amountBlock}>
            <span className={styles.amountLabel}>상품 금액</span>
            <span className={styles.amountValue}>{order.productAmount.toLocaleString('ko-KR')}원</span>
          </div>
          <span className={styles.amountOperator}>+</span>
          <div className={styles.amountBlock}>
            <span className={styles.amountLabel}>배송비</span>
            <span className={styles.amountValueFree}>
              {order.deliveryFee === 0 ? '무료' : `${order.deliveryFee.toLocaleString('ko-KR')}원`}
            </span>
          </div>
          <span className={styles.amountOperator}>=</span>
          <div className={styles.amountBlock}>
            <span className={styles.amountLabel}>최종 결제금액</span>
            <span className={styles.amountValueTotal}>{order.totalAmount.toLocaleString('ko-KR')}원</span>
          </div>
        </div>
      </div>

      <div className={styles.infoGrid}>
        <div className={styles.card}>
          <h2 className={styles.sectionTitle}>주문자 정보</h2>
          <dl className={styles.infoList}>
            <div className={styles.infoRow}>
              <dt>이름</dt>
              <dd>{order.orderer.name}</dd>
            </div>
            <div className={styles.infoRow}>
              <dt>연락처</dt>
              <dd>{order.orderer.phone}</dd>
            </div>
            <div className={styles.infoRow}>
              <dt>이메일</dt>
              <dd>{order.orderer.email}</dd>
            </div>
            <div className={styles.infoRow}>
              <dt>결제수단</dt>
              <dd>{order.orderer.method}</dd>
            </div>
            <div className={styles.infoRow}>
              <dt>주문일</dt>
              <dd>{order.createdAt}</dd>
            </div>
          </dl>
        </div>

        <div className={styles.card}>
          <h2 className={styles.sectionTitle}>배송 정보</h2>
          <dl className={styles.infoList}>
            <div className={styles.infoRow}>
              <dt>수령인</dt>
              <dd>{order.delivery.receiverName}</dd>
            </div>
            <div className={styles.infoRow}>
              <dt>연락처</dt>
              <dd>{order.delivery.receiverPhone}</dd>
            </div>
            <div className={styles.infoRow}>
              <dt>주소</dt>
              <dd>
                [{order.delivery.postCode}] {order.delivery.address} {order.delivery.addrDetail}
              </dd>
            </div>
            <div className={styles.infoRow}>
              <dt>요청사항</dt>
              <dd>{order.delivery.deliveryMemo || '-'}</dd>
            </div>
            <div className={styles.infoRow}>
              <dt>배송 상태</dt>
              <dd>
                <span className={styles.deliveryBadge}>{deliveryStatusLabel}</span>
              </dd>
            </div>
          </dl>
        </div>
      </div>

      <div className={styles.buttonArea}>
        <Link to="/" className={styles.homeButton}>
          홈으로 돌아가기
        </Link>
        <Link to="/orders" className={styles.historyButton}>
          주문 내역 확인
        </Link>
      </div>
    </div>
  )
}
