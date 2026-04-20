export function formatMoney(value, symbol = '¥') {
  if (value === null || value === undefined || value === '') return symbol + '0.00'
  const num = Number(value)
  if (Number.isNaN(num)) return symbol + '0.00'
  return symbol + num.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
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
