const THEME_KEY = 'asset_theme_mode'

export const APPEARANCE_PRESETS = [
  { label: '暖金日常', value: 'warm-gold', subtitle: '明亮暖金，轻松亲和', preview: ['#ffe066', '#d3a414', '#fffaf0'] },
  { label: '小镇夜色', value: 'town-night', subtitle: '深蓝夜空，静谧安然', preview: ['#1a2340', '#ffd166', '#f5f6f8'] },
  { label: '好运红',   value: 'lucky-red',  subtitle: '红色好运，喜庆温暖', preview: ['#c9383b', '#ffd4a8', '#fffaf8'] }
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
    headerText: '#2b2415', headerSub: 'rgba(43,36,21,0.64)', headerAccent: '#a07810',
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
    headerText: '#f0f2f8', headerSub: 'rgba(240,242,248,0.66)', headerAccent: '#ffd166',
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
    headerText: '#fff9f7', headerSub: 'rgba(255,249,247,0.68)', headerAccent: '#ffd4a8',
    iconFill: '#ffc8c4', iconSoft: '#fff3f1',
    fabBg: '#c9383b', fabText: '#ffffff',
    navBg: '#fffaf8', navText: '#2d1515',
    tabBg: '#ffffff', tabText: '#b8958a', tabSelected: '#c9383b'
  }
}

// ---- public API (signatures unchanged) ----

export function getThemeOptions() { return APPEARANCE_PRESETS }

export function getThemeMode() {
  const saved = uni.getStorageSync(THEME_KEY)
  if (saved && THEMES[saved]) return saved
  return 'warm-gold'
}

export function saveThemeMode(mode) {
  const next = THEMES[mode] ? mode : 'warm-gold'
  uni.setStorageSync(THEME_KEY, next)
  applyTheme(next)
}

export function getResolvedTheme(mode = getThemeMode()) {
  const t = THEMES[mode] || THEMES['warm-gold']
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
    '--app-warning-bg': '#fff8e6',
    '--app-warning-text': '#8a5a13',
    '--app-warning-border': '#f4dfaa',
    '--app-status-good-bg': '#ecfdf5',
    '--app-status-good-text': '#15803d',
    '--app-status-warn-bg': '#fff4e2',
    '--app-status-warn-text': '#b7791f',
    '--app-status-risk-bg': '#fff1f2'
  }
}

export function applyTheme(mode = getThemeMode()) {
  const t = getResolvedTheme(mode)
  try { uni.setNavigationBarColor({ frontColor: '#000000', backgroundColor: t.navBg }) } catch (_) {}
  try { uni.setTabBarStyle({ color: t.tabText, selectedColor: t.tabSelected, backgroundColor: t.tabBg, borderStyle: 'white' }) } catch (_) {}
  // #ifdef H5
  if (typeof document !== 'undefined') {
    const r = document.documentElement
    r.setAttribute('data-theme', t.id)
    Object.entries(getThemeVars(t.id)).forEach(([k, v]) => r.style.setProperty(k, v))
  }
  // #endif
}
