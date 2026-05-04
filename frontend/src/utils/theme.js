const THEME_KEY = 'asset_theme_mode'

export const APPEARANCE_PRESETS = [
  { label: '雾隐墨白', value: 'mist-mono',  subtitle: '默认装扮，黑白极简', preview: ['#e8ecf1', '#5b7a9a', '#f8f9fb'] },
  { label: '青岚晨光', value: 'mint-fresh', subtitle: '青山薄雾，草木呼吸', preview: ['#c8dfd0', '#7fa998', '#f7faf8'] },
  { label: '樱吹花语', value: 'sakura-blossom', subtitle: '落樱缤纷，温柔甜梦', preview: ['#fce4ec', '#d4788f', '#fef9fa'] },
  { label: '鸢尾暮紫', value: 'iris-dusk',  subtitle: '薰衣草调，柔紫治愈', preview: ['#e0d8f0', '#7c6f9e', '#faf8ff'] },
  { label: '暖金日常', value: 'warm-gold', subtitle: '明亮暖金，轻松亲和', preview: ['#ffe066', '#d3a414', '#fffaf0'] },
  { label: '朱砂映暖', value: 'lucky-red',  subtitle: '朱砂暖调，东方韵味', preview: ['#c9383b', '#ffd4a8', '#fffaf8'] },
  { label: '江雪', value: 'river-snow', subtitle: '孤舟蓑笠，独钓寒江', preview: ['#1a212b', '#5a7a90', '#f2f4f6'] },
  { label: '小镇夜色', value: 'town-night', subtitle: '深蓝夜空，静谧安然', preview: ['#1a2340', '#ffd166', '#f5f6f8'] },
  { label: '深海沉蓝', value: 'deep-ocean', subtitle: '深蓝沉浸，夜幕低垂', preview: ['#0d1b2a', '#4a90d9', '#152238'] },
  { label: '暗金雅韵', value: 'tycoon-gold', subtitle: '暗色为底，金纹点缀', preview: ['#1a1814', '#c8a44e', '#f0ece4'] }
]

const THEMES = {
  'warm-gold': {
    // Page & cards
    appPageBg: '#fffaf0', appCardBg: '#ffffff', appCardBgAlt: '#fffdf7',
    appSoftBg: '#fff8dd', appInputBg: '#fffcee', appBorder: '#f0e3b5',
    // Text
    appText: '#2b2415', appMuted: '#8a7a58', appFaint: '#b9aa83',
    // Accents
    appPrimary: '#d3a414', appPrimaryDark: '#8f6b00', appDanger: '#c0392b',
    // Header
    headerBg: 'linear-gradient(180deg, #ffe066 0%, #ffe87a 44%, #fff7d1 100%)',
    headerPattern: 'radial-gradient(circle at 12% 24%, rgba(255,255,255,0.55) 0 36rpx, transparent 38rpx), radial-gradient(circle at 88% 18%, rgba(255,255,255,0.45) 0 48rpx, transparent 50rpx), linear-gradient(135deg, rgba(255,255,255,0.26) 0 12%, transparent 13% 100%)',
    headerText: '#2b2415', headerSub: 'rgba(43,36,21,0.64)', headerAccent: '#a07810', headerAccentFade: 'rgba(160,120,16,0.30)',
    // Icons & FAB
    iconFill: '#ffe573', iconSoft: '#fff7d1',
    fabBg: '#ffe066', fabText: '#2b2415',
    // Nav/Tab
    navBg: '#fffaf0', navText: '#2b2415',
    tabBg: '#ffffff', tabText: '#b9aa83', tabSelected: '#d3a414'
  },
  'town-night': {
    appPageBg: '#f5f6f8', appCardBg: '#ffffff', appCardBgAlt: '#fbfcfe',
    appSoftBg: '#eff2f8', appInputBg: '#f7f9fc', appBorder: '#e0e5ee',
    appText: '#17202a', appMuted: '#6b7a90', appFaint: '#9aa8b8',
    appPrimary: '#2f5090', appPrimaryDark: '#1a3460', appDanger: '#c0392b',
    headerBg: 'linear-gradient(180deg, #1a2340 0%, #2a3a68 54%, #e8ecf4 100%)',
    headerPattern: 'radial-gradient(circle at 16% 22%, rgba(255,255,255,0.42) 0 3rpx, transparent 4rpx), radial-gradient(circle at 38% 16%, rgba(255,255,255,0.36) 0 2rpx, transparent 3rpx), radial-gradient(circle at 62% 28%, rgba(255,255,255,0.48) 0 4rpx, transparent 5rpx), radial-gradient(circle at 82% 14%, rgba(255,255,255,0.30) 0 3rpx, transparent 4rpx), radial-gradient(circle at 24% 36%, rgba(255,209,102,0.28) 0 5rpx, transparent 6rpx), linear-gradient(165deg, transparent 0 52%, rgba(26,35,64,0.30) 53% 100%)',
    headerText: '#f0f2f8', headerSub: 'rgba(240,242,248,0.66)', headerAccent: '#ffd166', headerAccentFade: 'rgba(255,209,102,0.30)',
    iconFill: '#cfdaf0', iconSoft: '#f1f4fb',
    fabBg: '#ffd166', fabText: '#1a2340',
    navBg: '#ffffff', navText: '#17202a',
    tabBg: '#ffffff', tabText: '#9aa8b8', tabSelected: '#2f5090'
  },
  'lucky-red': {
    appPageBg: '#fffaf8', appCardBg: '#ffffff', appCardBgAlt: '#fffefe',
    appSoftBg: '#fff0ed', appInputBg: '#fff8f7', appBorder: '#f0dcd5',
    appText: '#2d1515', appMuted: '#8a5a55', appFaint: '#b8958a',
    appPrimary: '#c9383b', appPrimaryDark: '#8a2022', appDanger: '#c0392b',
    headerBg: 'linear-gradient(180deg, #c9383b 0%, #d9544a 56%, #fff0e8 100%)',
    headerPattern: 'radial-gradient(circle at 74% 22%, rgba(255,212,168,0.38) 0 48rpx, transparent 50rpx), radial-gradient(circle at 18% 30%, rgba(255,255,255,0.20) 0 28rpx, transparent 30rpx), linear-gradient(135deg, rgba(255,255,255,0.14) 0 16%, transparent 17% 100%)',
    headerText: '#fff9f7', headerSub: 'rgba(255,249,247,0.68)', headerAccent: '#ffd4a8', headerAccentFade: 'rgba(255,212,168,0.30)',
    iconFill: '#ffc8c4', iconSoft: '#fff3f1',
    fabBg: '#c9383b', fabText: '#ffffff',
    navBg: '#fffaf8', navText: '#2d1515',
    tabBg: '#ffffff', tabText: '#b8958a', tabSelected: '#c9383b'
  },
  'tycoon-gold': {
    // Page & cards
    appPageBg: '#1c1b18', appCardBg: '#272522', appCardBgAlt: '#2e2b27',
    appSoftBg: '#33302a', appInputBg: '#2a2722', appBorder: '#3d3930',
    // Text
    appText: '#f0ece4', appMuted: '#a09888', appFaint: '#6b6458',
    // Accents
    appPrimary: '#c8a44e', appPrimaryDark: '#9a7b2e', appDanger: '#d9746a',
    // Header
    headerBg: 'linear-gradient(180deg, #1a1814 0%, #2a2318 56%, #3d3528 100%)',
    headerPattern: 'radial-gradient(circle at 20% 22%, rgba(200,164,78,0.18) 0 3rpx, transparent 4rpx), radial-gradient(circle at 72% 14%, rgba(200,164,78,0.14) 0 3rpx, transparent 4rpx), radial-gradient(circle at 48% 34%, rgba(200,164,78,0.10) 0 4rpx, transparent 5rpx), linear-gradient(165deg, transparent 0 58%, rgba(200,164,78,0.06) 59% 100%)',
    headerText: '#f5efe0', headerSub: 'rgba(245,239,224,0.60)', headerAccent: '#d4b868', headerAccentFade: 'rgba(212,184,104,0.30)',
    // Icons & FAB
    iconFill: '#4a3f2a', iconSoft: '#3d3528',
    fabBg: '#c8a44e', fabText: '#1c1b18',
    // Nav/Tab
    navBg: '#1c1b18', navText: '#f0ece4',
    tabBg: '#272522', tabText: '#6b6458', tabSelected: '#c8a44e'
  },
  'mint-fresh': {
    // Page & cards
    appPageBg: '#f7faf8', appCardBg: '#ffffff', appCardBgAlt: '#fcfefc',
    appSoftBg: '#eef5f0', appInputBg: '#f5faf6', appBorder: '#d8e8dc',
    // Text
    appText: '#2d3a30', appMuted: '#6b8a72', appFaint: '#9ab5a0',
    // Accents
    appPrimary: '#7fa998', appPrimaryDark: '#5a8070', appDanger: '#c97070',
    // Header
    headerBg: 'linear-gradient(180deg, #c8dfd0 0%, #d8e8dc 44%, #eaf2ec 100%)',
    headerPattern: 'radial-gradient(circle at 18% 22%, rgba(255,255,255,0.55) 0 40rpx, transparent 42rpx), radial-gradient(circle at 78% 18%, rgba(255,255,255,0.40) 0 46rpx, transparent 48rpx), linear-gradient(135deg, rgba(255,255,255,0.20) 0 14%, transparent 15% 100%)',
    headerText: '#2d3a30', headerSub: 'rgba(45,58,48,0.58)', headerAccent: '#5a8070', headerAccentFade: 'rgba(90,128,112,0.30)',
    // Icons & FAB
    iconFill: '#b8d5c5', iconSoft: '#eaf2ec',
    fabBg: '#7fa998', fabText: '#ffffff',
    // Nav/Tab
    navBg: '#f7faf8', navText: '#2d3a30',
    tabBg: '#ffffff', tabText: '#9ab5a0', tabSelected: '#7fa998'
  },
  'sakura-blossom': {
    // Page & cards
    appPageBg: '#fef9fa', appCardBg: '#ffffff', appCardBgAlt: '#fffdfe',
    appSoftBg: '#fef0f3', appInputBg: '#fef5f7', appBorder: '#f0dce0',
    // Text
    appText: '#3d2028', appMuted: '#8a6070', appFaint: '#b895a2',
    // Accents
    appPrimary: '#d4788f', appPrimaryDark: '#a85a6e', appDanger: '#c0392b',
    // Header
    headerBg: 'linear-gradient(180deg, #fce4ec 0%, #f8d5e0 44%, #fef9fa 100%)',
    headerPattern: 'radial-gradient(circle at 22% 18%, rgba(255,255,255,0.60) 0 38rpx, transparent 40rpx), radial-gradient(circle at 74% 24%, rgba(255,255,255,0.50) 0 34rpx, transparent 36rpx), radial-gradient(circle at 48% 38%, rgba(212,120,143,0.12) 0 4rpx, transparent 5rpx), linear-gradient(135deg, rgba(255,255,255,0.22) 0 10%, transparent 11% 100%)',
    headerText: '#3d2028', headerSub: 'rgba(61,32,40,0.55)', headerAccent: '#d4788f', headerAccentFade: 'rgba(212,120,143,0.28)',
    // Icons & FAB
    iconFill: '#f0c6d2', iconSoft: '#fef0f3',
    fabBg: '#d4788f', fabText: '#ffffff',
    // Nav/Tab
    navBg: '#fef9fa', navText: '#3d2028',
    tabBg: '#ffffff', tabText: '#b895a2', tabSelected: '#d4788f'
  },
  'river-snow': {
    // Page & cards — snow white with cold gray tint
    appPageBg: '#f2f4f6', appCardBg: '#ffffff', appCardBgAlt: '#fcfdfe',
    appSoftBg: '#edf0f4', appInputBg: '#f5f6f9', appBorder: '#e2e5ea',
    // Text — ink black
    appText: '#1c2028', appMuted: '#6b7484', appFaint: '#9ba2b0',
    // Accents — distant mountain blue-gray
    appPrimary: '#5a7a90', appPrimaryDark: '#3d5668', appDanger: '#c97070',
    // Header — ink wash: dark ink → mist → snow
    headerBg: 'linear-gradient(180deg, #1a212b 0%, #2d3848 50%, #c8d2de 100%)',
    headerPattern: 'radial-gradient(circle at 22% 28%, rgba(160,184,204,0.12) 0 32rpx, transparent 34rpx), radial-gradient(circle at 74% 40%, rgba(160,184,204,0.08) 0 44rpx, transparent 46rpx), linear-gradient(135deg, rgba(160,184,204,0.05) 0 14%, transparent 15% 100%)',
    headerText: '#f4f6f8', headerSub: 'rgba(244,246,248,0.60)', headerAccent: '#a0b8cc', headerAccentFade: 'rgba(160,184,204,0.28)',
    // Icons & FAB
    iconFill: '#d0d8e2', iconSoft: '#eff2f6',
    fabBg: '#5a7a90', fabText: '#ffffff',
    // Nav/Tab
    navBg: '#f2f4f6', navText: '#1c2028',
    tabBg: '#ffffff', tabText: '#9ba2b0', tabSelected: '#5a7a90'
  },
  'mist-mono': {
    // Page & cards
    appPageBg: '#f8f9fb', appCardBg: '#ffffff', appCardBgAlt: '#fcfdfe',
    appSoftBg: '#eff1f5', appInputBg: '#f4f6f9', appBorder: '#e2e6ed',
    // Text
    appText: '#1a1d23', appMuted: '#6e7685', appFaint: '#9ba3b0',
    // Accents
    appPrimary: '#5b7a9a', appPrimaryDark: '#3f5a74', appDanger: '#c97070',
    // Header
    headerBg: 'linear-gradient(180deg, #e8ecf1 0%, #f0f3f7 50%, #f8f9fb 100%)',
    headerPattern: 'radial-gradient(circle at 22% 28%, rgba(91,122,154,0.10) 0 28rpx, transparent 30rpx), radial-gradient(circle at 74% 45%, rgba(91,122,154,0.06) 0 38rpx, transparent 40rpx), linear-gradient(135deg, rgba(91,122,154,0.04) 0 10%, transparent 11% 100%)',
    headerText: '#1a1d23', headerSub: 'rgba(26,29,35,0.52)', headerAccent: '#5b7a9a', headerAccentFade: 'rgba(91,122,154,0.25)',
    // Icons & FAB
    iconFill: '#c8d4e0', iconSoft: '#eef2f6',
    fabBg: '#5b7a9a', fabText: '#ffffff',
    // Nav/Tab
    navBg: '#f8f9fb', navText: '#1a1d23',
    tabBg: '#ffffff', tabText: '#9ba3b0', tabSelected: '#5b7a9a'
  },
  'deep-ocean': {
    // Page & cards
    appPageBg: '#0d1b2a', appCardBg: '#152238', appCardBgAlt: '#1a2a40',
    appSoftBg: '#1e3048', appInputBg: '#172840', appBorder: '#253d58',
    // Text
    appText: '#e8edf2', appMuted: '#8a9bb5', appFaint: '#5c6e88',
    // Accents
    appPrimary: '#4a90d9', appPrimaryDark: '#3570b0', appDanger: '#d9746a',
    // Header
    headerBg: 'linear-gradient(180deg, #0a1625 0%, #132238 50%, #1e3048 100%)',
    headerPattern: 'radial-gradient(circle at 30% 18%, rgba(74,144,217,0.16) 0 3rpx, transparent 4rpx), radial-gradient(circle at 68% 28%, rgba(74,144,217,0.12) 0 2rpx, transparent 3rpx), radial-gradient(circle at 44% 42%, rgba(74,144,217,0.08) 0 4rpx, transparent 5rpx), linear-gradient(165deg, transparent 0 62%, rgba(74,144,217,0.05) 63% 100%)',
    headerText: '#e8edf2', headerSub: 'rgba(232,237,242,0.58)', headerAccent: '#6db3f0', headerAccentFade: 'rgba(109,179,240,0.28)',
    // Icons & FAB
    iconFill: '#1e3048', iconSoft: '#253d58',
    fabBg: '#4a90d9', fabText: '#ffffff',
    // Nav/Tab
    navBg: '#0d1b2a', navText: '#e8edf2',
    tabBg: '#152238', tabText: '#5c6e88', tabSelected: '#4a90d9'
  },
  'iris-dusk': {
    // Page & cards
    appPageBg: '#faf8ff', appCardBg: '#ffffff', appCardBgAlt: '#fefcff',
    appSoftBg: '#f3effa', appInputBg: '#f7f4fc', appBorder: '#e2daee',
    // Text
    appText: '#2a2238', appMuted: '#6e608a', appFaint: '#9b8fb5',
    // Accents
    appPrimary: '#7c6f9e', appPrimaryDark: '#5a4e78', appDanger: '#c97070',
    // Header
    headerBg: 'linear-gradient(180deg, #e0d8f0 0%, #ece4f5 48%, #f6f2fb 100%)',
    headerPattern: 'radial-gradient(circle at 16% 24%, rgba(255,255,255,0.50) 0 42rpx, transparent 44rpx), radial-gradient(circle at 80% 20%, rgba(255,255,255,0.38) 0 38rpx, transparent 40rpx), linear-gradient(135deg, rgba(255,255,255,0.18) 0 12%, transparent 13% 100%)',
    headerText: '#2a2238', headerSub: 'rgba(42,34,56,0.58)', headerAccent: '#8b7ab0', headerAccentFade: 'rgba(139,122,176,0.28)',
    // Icons & FAB
    iconFill: '#cdc0e8', iconSoft: '#f3effa',
    fabBg: '#7c6f9e', fabText: '#ffffff',
    // Nav/Tab
    navBg: '#faf8ff', navText: '#2a2238',
    tabBg: '#ffffff', tabText: '#9b8fb5', tabSelected: '#7c6f9e'
  }
}

// ---- public API (signatures unchanged) ----

export function getThemeOptions() { return APPEARANCE_PRESETS }

export function getThemeMode() {
  const saved = uni.getStorageSync(THEME_KEY)
  if (saved && THEMES[saved]) return saved
  return 'mist-mono'
}

export function saveThemeMode(mode) {
  const next = THEMES[mode] ? mode : 'mist-mono'
  uni.setStorageSync(THEME_KEY, next)
  applyTheme(next)
}

export function getResolvedTheme(mode = getThemeMode()) {
  const t = THEMES[mode] || THEMES['mist-mono']
  return {
    id: mode,
    ...t,
    // aliases for backcompat
    primary: t.appPrimary,
    primaryDark: t.appPrimaryDark,
    danger: t.appDanger,
    pageBg: t.appPageBg,
    cardBg: t.appCardBg,
    softBg: t.appSoftBg,
    inputBg: t.appInputBg,
    border: t.appBorder,
    text: t.appText,
    muted: t.appMuted,
    faint: t.appFaint,
    // derived
    shadow: '0 8rpx 22rpx rgba(15, 23, 42, 0.045)',
    shadowLg: '0 18rpx 40rpx rgba(15, 23, 42, 0.10)',
    radius: '16rpx',
    positiveColor: t.appPrimary,
    liabilityColor: t.appDanger,
    heroGradient: t.headerBg,
    heroText: t.headerText,
    heroSub: t.headerSub,
    heroAccent: t.headerAccent,
    headerAccentFade: t.headerAccentFade,
    heroBadgeBg: `${t.headerAccent}1e`,
    sectionBg: 'rgba(255,255,255,0.08)',
    tabbarBorder: t.appBorder,
    tabSelectedBg: `${t.tabSelected}1a`,
    tabSelectedShadow: `0 8rpx 20rpx ${t.tabSelected}1a`
  }
}

export function getThemeVars(mode = getThemeMode()) {
  const t = getResolvedTheme(mode)
  return {
    // Page & card
    '--app-page-bg': t.appPageBg,
    '--app-card-bg': t.appCardBg,
    '--app-card-bg-alt': t.appCardBgAlt,
    '--app-bg': t.appPageBg,
    '--app-desktop-bg': t.appPageBg,
    '--app-soft-bg': t.appSoftBg,
    '--app-input-bg': t.appInputBg,
    '--app-border': t.appBorder,
    // Text
    '--app-text': t.appText,
    '--app-text-alt': t.appText,
    '--app-muted': t.appMuted,
    '--app-faint': t.appFaint,
    // Accents
    '--app-primary': t.appPrimary,
    '--app-primary-dark': t.appPrimaryDark,
    '--app-accent': t.headerAccent,
    '--app-danger': t.appDanger,
    '--app-positive-color': t.positiveColor,
    '--app-liability-color': t.liabilityColor,
    '--app-section-bg': t.sectionBg,
    // Shadows
    '--app-shadow': t.shadow,
    '--app-shadow-lg': t.shadowLg,
    '--app-radius': t.radius,
    // Hero (backcompat — same as outfit-header)
    '--app-hero-gradient': t.headerBg,
    '--app-hero-text': t.headerText,
    '--app-hero-sub': t.headerSub,
    '--app-hero-accent': t.headerAccent,
    '--app-hero-badge-bg': t.heroBadgeBg,
    // Outfit header (primary)
    '--app-outfit-header-bg': t.headerBg,
    '--app-outfit-header-pattern': t.headerPattern,
    '--app-outfit-header-text': t.headerText,
    '--app-outfit-header-sub': t.headerSub,
    '--app-outfit-header-accent': t.headerAccent,
    '--app-outfit-header-accent-fade': t.headerAccentFade,
    // Icons & FAB
    '--app-outfit-icon-fill': t.iconFill,
    '--app-outfit-icon-soft': t.iconSoft,
    '--app-outfit-fab-bg': t.fabBg,
    '--app-outfit-fab-text': t.fabText,
    // Tab bar
    '--app-tabbar-bg': t.tabBg,
    '--app-tabbar-text': t.tabText,
    '--app-tabbar-selected': t.tabSelected,
    '--app-tabbar-selected-bg': t.tabSelectedBg,
    '--app-tabbar-selected-shadow': t.tabSelectedShadow,
    '--app-tabbar-border': t.tabbarBorder,
    // Status
    '--app-warning': '#f59e0b',
    '--app-warning-bg': '#fff8e6',
    '--app-warning-text': '#8a5a13',
    '--app-warning-border': '#f4dfaa',
    '--app-danger-bg': '#fef2f2',
    '--app-danger-border': '#fecaca',
    '--app-status-good-bg': '#ecfdf5',
    '--app-status-good-text': '#15803d',
    '--app-status-warn-bg': '#fff4e2',
    '--app-status-warn-text': '#b7791f',
    '--app-status-risk-bg': '#fff1f2'
  }
}

function isDarkColor(hex) {
  if (!hex || hex.length < 7) return false
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
  return luminance < 0.5
}

export function applyTheme(mode = getThemeMode()) {
  const t = getResolvedTheme(mode)
  const frontColor = isDarkColor(t.navBg) ? '#ffffff' : '#000000'
  try { uni.setNavigationBarColor({ frontColor, backgroundColor: t.navBg }) } catch (_) {}
  try { uni.setTabBarStyle({ color: t.tabText, selectedColor: t.tabSelected, backgroundColor: t.tabBg, borderStyle: 'white' }) } catch (_) {}
  // #ifdef H5
  if (typeof document !== 'undefined') {
    const r = document.documentElement
    r.setAttribute('data-theme', t.id)
    Object.entries(getThemeVars(t.id)).forEach(([k, v]) => r.style.setProperty(k, v))
  }
  // #endif
}
