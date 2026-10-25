import request from './request'

export function createPayment(reservationId, method) {
  return request.post('/payments', null, { params: { reservationId, method } })
}

export function listPaymentsByReservation(reservationId) {
  return request.get(`/payments/reservation/${reservationId}`)
}

/** 建立藍新信用卡付款單，回傳 MPG 表單欄位 { gatewayUrl, merchantId, version, tradeInfo, tradeSha } */
export function createNewebPayForm(reservationId) {
  return request.post(`/payments/reservations/${reservationId}/newebpay/form`)
}

/** 補查會員逾時未付款的藍新交易（沒收到 NotifyURL 時的備援） */
export function queryPendingNewebPayTrades() {
  return request.get('/payments/newebpay/query-pending')
}

/** 建立並送出隱藏表單，整頁跳轉到藍新刷卡頁 */
export async function payByNewebPay(reservationId) {
  const f = await createNewebPayForm(reservationId)
  const form = document.createElement('form')
  form.method = 'POST'
  form.action = f.gatewayUrl
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
}
