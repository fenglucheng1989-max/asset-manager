const DEFAULT_H5_API_BASE_URL = ''
const DEFAULT_APP_API_BASE_URL = 'http://192.168.101.12:8080'

function trimTrailingSlash(value) {
  return value ? value.replace(/\/+$/, '') : ''
}

export function getApiBaseUrl() {
  let baseUrl = import.meta.env.VITE_API_BASE_URL || DEFAULT_H5_API_BASE_URL

  // #ifdef APP-PLUS
  baseUrl = import.meta.env.VITE_APP_API_BASE_URL || import.meta.env.VITE_API_BASE_URL || DEFAULT_APP_API_BASE_URL
  // #endif

  return trimTrailingSlash(baseUrl)
}
