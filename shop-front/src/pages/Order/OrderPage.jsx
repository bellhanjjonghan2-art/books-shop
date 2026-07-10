import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { loadTossPayments } from '@tosspayments/tosspayments-sdk'
import { useBooksByIds } from '../../hooks/queries/useBooks'
import { useCreateOrder } from '../../hooks/queries/useOrders'
import { useAuthStore } from '../../stores/useAuthStore'
import styles from './OrderPage.module.css'

const DAUM_POSTCODE_SRC = '//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js'

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

export default function OrderPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const items = useMemo(() => location.state?.items ?? [], [location.state])

  const [recipient, setRecipient] = useState('')
  const [phone, setPhone] = useState('')
  const [zonecode, setZonecode] = useState('')
  const [address, setAddress] = useState('')
  const [addressDetail, setAddressDetail] = useState('')
  const [memo, setMemo] = useState('')
  const [agree, setAgree] = useState(false)

  const bookIds = useMemo(() => items.map((item) => item.bookId), [items])
  const { data, isPending, isError } = useBooksByIds(bookIds)
  const { mutateAsync: createOrder } = useCreateOrder()

  useEffect(() => {
    if (items.length === 0) {
      alert('주문할 상품 정보가 없습니다.')
      navigate('/cart', { replace: true })
    }
  }, [items, navigate])

  const orderItems = useMemo(() => {
    const bookMap = new Map((data ?? []).map((book) => [book.bookId, book]))
    return items
      .map((item) => {
        const book = bookMap.get(item.bookId)
        if (!book) return null
        return { ...book, quantity: item.quantity }
      })
      .filter(Boolean)
  }, [data, items])

  const totalCount = orderItems.reduce((sum, item) => sum + item.quantity, 0)
  const subtotal = orderItems.reduce((sum, item) => sum + item.salePrice * item.quantity, 0)
  const shippingFee = 0
  const totalPayment = subtotal + shippingFee

  const handleSearchAddress = () => {
    const openPostcode = () => {
      new window.daum.Postcode({
        oncomplete: (result) => {
          setZonecode(result.zonecode)
          setAddress(result.roadAddress)
        },
      }).open()
    }

    if (window.daum?.Postcode) {
      openPostcode()
      return
    }

    const script = document.createElement('script')
    script.src = DAUM_POSTCODE_SRC
    script.onload = openPostcode
    document.body.appendChild(script)
  }

  const canSubmit =
    recipient.trim() !== '' &&
    phone.trim() !== '' &&
    zonecode.trim() !== '' &&
    address.trim() !== '' &&
    addressDetail.trim() !== '' &&
    agree

  const handlePayment = async () => {
    try {
      // [1] 주문 생성 — 결제창을 띄우기 전에 먼저 주문을 만들어 orderId/amount를 확보한다.
      const { orderId, orderName, amount } = await createOrder({
        items: orderItems.map((item) => ({ bookId: item.bookId, quantity: item.quantity })),
        delivery: {
          receiverName: recipient,
          receiverPhone: phone,
          postCode: zonecode,
          address,
          addrDetail: addressDetail,
          deliveryMemo: memo,
        },
      })

      // [2] 결제창 초기화 (API 개별 연동 — 카드/간편결제 통합결제창)
      const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY
      const tossPayments = await loadTossPayments(clientKey)
      const customerKey = useAuthStore.getState().user.userId
      const payment = tossPayments.payment({ customerKey })

      // [2]~[3] 결제창 호출
      try {
        await payment.requestPayment({
          method: 'CARD',
          amount: { currency: 'KRW', value: amount },
          orderId,
          orderName,
          successUrl: window.location.origin + '/payments/result',
          failUrl: window.location.origin + '/payments/result',
          customerName: recipient,
          customerMobilePhone: phone,
          card: {
            useEscrow: false,
            flowMode: 'DEFAULT',
            useCardPoint: false,
            useAppCardOnly: false,
          },
        })
      } catch (error) {
        console.error(error)
        alert('결제 요청 중 오류가 발생했습니다. 다시 시도해주세요.')
      }
    } catch (error) {
      console.error(error)
      alert('주문 생성 중 오류가 발생했습니다. 다시 시도해주세요.')
    }
  }

  if (items.length === 0) {
    return null
  }

  return (
    <div className={styles.page}>
      <div className={styles.breadcrumb}>
        <Link to="/" className={styles.breadcrumbHome}>
          홈
        </Link>
        <span className={styles.breadcrumbSep}>{'>'}</span>
        <Link to="/cart" className={styles.breadcrumbHome}>
          장바구니
        </Link>
        <span className={styles.breadcrumbSep}>{'>'}</span>
        <span className={styles.breadcrumbCurrent}>주문하기</span>
      </div>

      <div className={styles.titleArea}>
        <h1 className={styles.pageTitle}>주문하기</h1>
        <p className={styles.pageSubtitle}>배송지와 주문 상품을 확인하고 결제를 진행하세요.</p>
      </div>

      <div className={styles.body}>
        <section className={styles.leftColumn}>
          <div className={styles.card}>
            <h2 className={styles.cardTitle}>배송지 정보</h2>
            <div className={styles.formGrid}>
              <label className={styles.formField}>
                <span className={styles.fieldLabel}>수령인</span>
                <input
                  type="text"
                  className={styles.input}
                  value={recipient}
                  onChange={(e) => setRecipient(e.target.value)}
                  placeholder="수령인 이름을 입력하세요"
                />
              </label>

              <label className={styles.formField}>
                <span className={styles.fieldLabel}>연락처</span>
                <input
                  type="text"
                  className={styles.input}
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="010-0000-0000"
                />
              </label>

              <label className={styles.formField}>
                <span className={styles.fieldLabel}>우편번호</span>
                <div className={styles.zonecodeRow}>
                  <input type="text" className={styles.input} value={zonecode} readOnly />
                  <button type="button" className={styles.searchBtn} onClick={handleSearchAddress}>
                    우편번호 검색
                  </button>
                </div>
              </label>

              <label className={styles.formField}>
                <span className={styles.fieldLabel}>주소</span>
                <input type="text" className={styles.input} value={address} readOnly />
              </label>

              <label className={styles.formField}>
                <span className={styles.fieldLabel}>상세주소</span>
                <input
                  type="text"
                  className={styles.input}
                  value={addressDetail}
                  onChange={(e) => setAddressDetail(e.target.value)}
                  placeholder="상세주소를 입력하세요"
                />
              </label>

              <label className={styles.formField}>
                <span className={styles.fieldLabel}>배송 메모</span>
                <textarea
                  className={styles.textarea}
                  value={memo}
                  onChange={(e) => setMemo(e.target.value)}
                  placeholder="배송 시 요청사항을 입력하세요 (선택)"
                />
              </label>
            </div>
          </div>

          <div className={styles.card}>
            <h2 className={styles.cardTitle}>주문 상품 목록</h2>

            {isPending ? (
              <p className={styles.stateMessage}>불러오는 중...</p>
            ) : isError ? (
              <p className={styles.stateMessage}>도서 정보를 불러올 수 없습니다.</p>
            ) : (
              <ul className={styles.itemList}>
                {orderItems.map((item) => (
                  <li key={item.bookId} className={styles.itemCard}>
                    <BookCover coverImage={item.coverImage} title={item.title} />
                    <div className={styles.info}>
                      {item.categoryName && (
                        <span className={styles.categoryBadge}>{item.categoryName}</span>
                      )}
                      <p className={styles.itemTitle}>{item.title}</p>
                      <p className={styles.itemMeta}>{item.author}</p>
                      <p className={styles.itemQuantity}>{item.quantity}권</p>
                    </div>
                    <p className={styles.itemPrice}>
                      {(item.salePrice * item.quantity).toLocaleString('ko-KR')}원
                    </p>
                  </li>
                ))}
              </ul>
            )}

            <div className={styles.itemSummary}>
              <span>총 {totalCount}개 상품</span>
              <span>{subtotal.toLocaleString('ko-KR')}원</span>
            </div>
          </div>
        </section>

        <aside className={styles.sidePanel}>
          <div className={styles.summaryHeader}>
            <p className={styles.summaryTitle}>결제 금액</p>
            <div className={styles.summaryRow}>
              <span className={styles.summaryName}>상품 금액</span>
              <span className={styles.summaryPrice}>{subtotal.toLocaleString('ko-KR')}원</span>
            </div>
            <div className={styles.summaryRow}>
              <span className={styles.shippingLabel}>배송비</span>
              <span className={styles.shippingFree}>무료</span>
            </div>
          </div>

          <div className={styles.divider} />

          <div className={styles.totalsArea}>
            <div className={styles.totalRow}>
              <span className={styles.totalLabel}>총 결제금액</span>
              <span className={styles.totalPrice}>{totalPayment.toLocaleString('ko-KR')}원</span>
            </div>
          </div>

          <div className={styles.buttonArea}>
            <label className={styles.agreeRow}>
              <input
                type="checkbox"
                className={styles.agreeCheckbox}
                checked={agree}
                onChange={(e) => setAgree(e.target.checked)}
              />
              <span>주문 내용을 확인하였으며, 결제에 동의합니다.</span>
            </label>

            <button
              type="button"
              className={styles.payButton}
              disabled={!canSubmit}
              onClick={handlePayment}
            >
              {totalPayment.toLocaleString('ko-KR')}원 결제하기
            </button>
          </div>

          <div className={styles.noticeArea}>
            <span>🔒 SSL 보안 결제</span>
            <span>✔ 환불 보장</span>
            <span>☎ 고객지원</span>
          </div>
        </aside>
      </div>
    </div>
  )
}
