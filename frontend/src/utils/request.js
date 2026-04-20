const BASE_URL = ''

export function request(options) {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    const header = {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    }

    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header,
      success: (res) => {
        if (res.statusCode === 200) {
          resolve(res.data)
          return
        }

        if (res.statusCode === 401) {
          uni.removeStorageSync('token')
          uni.removeStorageSync('username')
          uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
          setTimeout(() => {
            uni.switchTab({ url: '/pages/mine/mine' })
          }, 1500)
          reject(res.data)
          return
        }

        const message = res.data && res.data.message ? res.data.message : '请求失败'
        uni.showToast({ title: message, icon: 'none' })
        reject(res.data)
      },
      fail: (err) => {
        uni.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      }
    })
  })
}
