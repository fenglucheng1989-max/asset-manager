export function formatMoney(value, symbol = '¥') {
  if (value === null || value === undefined || value === '') return symbol + '0.00'
  const num = Number(value)
  if (Number.isNaN(num)) return symbol + '0.00'
  return symbol + num.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

export function getCurrencySymbol(currency = 'CNY') {
  const symbolMap = {
    CNY: '¥',
    USD: '$',
    HKD: 'HK$',
    EUR: '€',
    JPY: '¥',
    GBP: '£'
  }
  return symbolMap[String(currency || 'CNY').toUpperCase()] || `${currency} `
}

export function toBaseAmount(account) {
  const balance = Number(account && account.currentBalance ? account.currentBalance : 0)
  const rate = Number(account && account.exchangeRateToCny ? account.exchangeRateToCny : 1)
  if (Number.isNaN(balance)) return 0
  if (Number.isNaN(rate) || rate <= 0) return balance
  return balance * rate
}

export function formatCompactMoney(value, symbol = '¥') {
  const num = Math.abs(Number(value || 0))
  if (Number.isNaN(num)) return symbol + '0'
  if (num >= 100000000) return (num / 100000000).toFixed(2).replace(/\.?0+$/, '') + '亿'
  if (num >= 10000) return (num / 10000).toFixed(2).replace(/\.?0+$/, '') + '万'
  return formatMoney(value, symbol)
}

export function getAccountTypeName(type) {
  const typeMap = {
    CASH: '现金',
    BANK: '银行卡',
    CREDIT_CARD: '信用卡',
    E_WALLET: '电子钱包',
    INVESTMENT: '投资理财',
    LOAN: '贷款',
    REAL_ESTATE: '房产',
    OTHER_ASSET: '其他资产',
    OTHER_LIABILITY: '其他负债'
  }
  return typeMap[type] || type
}
