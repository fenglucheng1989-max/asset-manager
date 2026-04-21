export function formatMoney(value) {
  const number = Number(value || 0)
  return `¥${number.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })}`
}

export function accountTypeName(type) {
  const map = {
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
  return map[type] || type
}

export const accountTypeOptions = [
  { label: '现金', value: 'CASH' },
  { label: '银行卡', value: 'BANK' },
  { label: '信用卡', value: 'CREDIT_CARD' },
  { label: '电子钱包', value: 'E_WALLET' },
  { label: '投资理财', value: 'INVESTMENT' },
  { label: '贷款', value: 'LOAN' },
  { label: '房产', value: 'REAL_ESTATE' },
  { label: '其他资产', value: 'OTHER_ASSET' },
  { label: '其他负债', value: 'OTHER_LIABILITY' }
]
