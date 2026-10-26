import request from './request'

export function createPayment(reservationId, method) {
  return request.post('/payments', null, { params: { reservationId, method } })
}

export function listPaymentsByReservation(reservationId) {
  return request.get(`/payments/reservation/${reservationId}`)
}

/** 建立藍新信用卡付款單，回傳 MPG 表單欄位 { gatewayUrl, merchantId, version, tradeInfo, tradeSha } */
export function createNewebPayForm(reservationId, method = 'CREDIT_CARD') {
  return request.post(`/payments/reservations/${reservationId}/newebpay/form`, null, { params: { method } })
}

/** 補查會員逾時未付款的藍新交易（沒收到 NotifyURL 時的備援） */
export function queryPendingNewebPayTrades() {
  return request.get('/payments/newebpay/query-pending')
}

/**
 * 建立並送出隱藏表單到藍新付款頁
 * method：CREDIT_CARD 信用卡（整頁跳轉）/ ATM ATM 轉帳（開新分頁，關掉後原頁面仍在）
 */
export async function payByNewebPay(reservationId, method = 'CREDIT_CARD') {
  // 新分頁要在點擊當下同步開啟，等 await 之後才開會被瀏覽器當成彈出視窗擋掉
  const newTab = method === 'ATM' ? window.open('', '_blank') : null
  let f
  try {
    f = await createNewebPayForm(reservationId, method)
  } catch (e) {
    newTab?.close()
    throw e
  }
  const form = document.createElement('form')
  form.method = 'POST'
  form.action = f.gatewayUrl
  if (newTab) {
    newTab.name = `newebpay-${reservationId}`
    form.target = newTab.name
  }
  const fields = { MerchantID: f.merchantId, TradeInfo: f.tradeInfo, TradeSha: f.tradeSha, Version: f.version }
  Object.entries(fields).forEach(([name, value]) => {
    const input = document.createElement('input')
    input.type = 'hidden'
    input.name = name
    input.value = value
    form.appendChild(input)
  })
  document.body.appendChild(form)
  form.submit()
  if (newTab) {
    document.body.removeChild(form)
  }
}

/** 會員選擇現場付現：建立現金付款單（未收款） */
export function chooseCashPayment(reservationId) {
  return request.post(`/payments/reservations/${reservationId}/cash`)
}

/** 後台現場收到現金：標記已付款並確認預約 */
export function collectCashPayment(reservationId) {
  return request.post(`/payments/reservations/${reservationId}/cash/collect`)
}
