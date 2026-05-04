<script>
import { applyTheme } from './utils/theme'

export default {
  onLaunch() {
    applyTheme()
    uni.hideTabBar()
  },
  onShow() {
    applyTheme()
    uni.hideTabBar()
  }
}
</script>

<style>
page {
  background-color: var(--app-page-bg, #f3f6f8);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  color: var(--app-text, #17202a);
}

/* #ifdef H5 */
body {
  margin: 0;
  background: var(--app-desktop-bg, linear-gradient(180deg, #dde7ef 0%, #f6f8fb 42%, #edf1f5 100%));
}

#app {
  width: 100%;
  max-width: 480px;
  min-height: 100vh;
  margin: 0 auto;
  background: var(--app-bg, linear-gradient(180deg, #f7fafc 0%, #eef3f6 100%));
  box-shadow: 0 0 42px rgba(15, 23, 42, 0.12);
}

.container {
  background: transparent;
  color: var(--app-text, #17202a);
}

/* ---- Cards & surfaces ---- */
.profile-card,
.menu-group,
.about-card,
.login-card,
.account-section,
.login-tip,
.error-tip,
.loading-tip,
.insight-entry,
.summary-entry,
.section-card,
.notice-card,
.metric-card,
.empty-card,
.option-group,
.settings-group,
.settings-card,
.data-card,
.security-card,
.snapshot-card,
.milestone-card,
.form-card,
.content-card,
.list-card,
.summary-card,
.theme-card {
  background: var(--app-card-bg, #ffffff) !important;
  border-color: var(--app-border, #edf1f4) !important;
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(26, 42, 58, 0.045)) !important;
}

/* ---- Hero / Dark cards ---- */
.net-card,
.net-worth-card,
.theme-hero,
.settings-hero,
.security-hero,
.data-hero,
.trend-hero {
  background: var(--app-outfit-header-bg, var(--app-hero-gradient, linear-gradient(135deg, #14202d 0%, #174a43 100%))) !important;
  color: var(--app-outfit-header-text, var(--app-hero-text, #ffffff)) !important;
  box-shadow: 0 18rpx 42rpx rgba(15, 23, 42, 0.10) !important;
}

/* ---- Hero inner elements ---- */
.net-card .net-label,
.net-worth-card .card-title,
.trend-hero .hero-label,
.trend-hero .meta-label,
.settings-hero .hero-kicker,
.settings-hero .hero-subtitle,
.security-hero .hero-subtitle,
.data-hero .hero-subtitle,
.theme-hero .hero-subtitle {
  color: var(--app-outfit-header-sub, var(--app-hero-sub, rgba(255,255,255,0.72))) !important;
}

.net-card .net-value,
.net-worth-card .net-worth-amount,
.trend-hero .hero-value {
  color: var(--app-outfit-header-accent, var(--app-hero-accent, #f4c95d)) !important;
}

.net-card .net-arrow,
.trend-hero .meta-value {
  color: var(--app-outfit-header-sub, var(--app-hero-sub, rgba(255,255,255,0.72))) !important;
}

.health-badge {
  background: var(--app-hero-badge-bg, rgba(255,255,255,0.12)) !important;
  color: var(--app-outfit-header-accent, var(--app-hero-accent, #f4c95d)) !important;
}

.detail-item {
  background: var(--app-section-bg, rgba(255,255,255,0.08)) !important;
}

.detail-value.liability,
.trend-hero .meta-value.liability {
  color: var(--app-liability-color, #ff9aa9) !important;
}

/* ---- Text colors ---- */
.username,
.menu-title,
.about-title,
.login-title,
.section-title,
.metric-title,
.option-title,
.settings-title,
.setting-title,
.grid-value,
.row-title,
.hero-kicker {
  color: var(--app-text, #17202a) !important;
}

.settings-hero .hero-title,
.security-hero .hero-title,
.data-hero .hero-title,
.theme-hero .hero-title {
  color: var(--app-hero-text, #ffffff) !important;
}

.profile-email,
.profile-subtitle,
.menu-subtitle,
.feedback,
.section-subtitle,
.metric-subtitle,
.option-subtitle,
.settings-subtitle,
.setting-subtitle,
.notice-card,
.empty-card,
.hero-subtitle {
  color: var(--app-muted, #64748b) !important;
}

.menu-arrow,
.net-arrow,
.summary-arrow,
.settings-arrow {
  color: var(--app-faint, #94a3b8) !important;
}

.inline-picker,
.about-links,
.legal-link,
.switch-text,
.link {
  color: var(--app-primary, #d3a414) !important;
}

/* ---- Soft backgrounds & inputs ---- */
.login-input,
.theme-option.active,
.account-surface,
.empty-state,
.setting-preview,
.budget-track,
.account-rank-track {
  background: var(--app-soft-bg, #f6f8fb) !important;
  border-color: var(--app-border, #edf1f4) !important;
}

input,
.input,
.textarea,
.login-input {
  background: var(--app-input-bg, #f6f8fb) !important;
  border-color: var(--app-border, #edf1f4) !important;
  color: var(--app-text, #17202a) !important;
}

.form-card input,
.form-card .form-input,
.form-card .form-picker {
  background: transparent !important;
  border-color: transparent !important;
}

/* ---- Fixed bottom bars ---- */
.bottom-actions {
  background: var(--app-page-bg, #f3f6f8) !important;
  border-color: var(--app-border, #edf1f4) !important;
}

/* ---- Primary buttons & accents ---- */
.primary-btn,
.submit-btn,
.login-btn {
  background: var(--app-primary, #d3a414) !important;
  color: #ffffff !important;
}

.add-account-btn,
.load-btn,
.insight-score {
  background: var(--app-soft-bg, #f6f8fb) !important;
  color: var(--app-primary, #d3a414) !important;
  border-color: var(--app-border, #edf1f4) !important;
}

/* ---- Danger ---- */
.danger-btn {
  background: var(--app-danger, #d94a62) !important;
  color: #ffffff !important;
}

.danger-card {
  border-color: var(--app-danger, #d94a62) !important;
}

.danger-text {
  color: var(--app-danger, #d94a62) !important;
}

.danger .menu-title,
.danger {
  color: var(--app-danger, #d94a62) !important;
}

/* ---- Positive / accent ---- */
.account-balance {
  color: var(--app-text, #17202a) !important;
}

.account-balance.liability {
  color: var(--app-liability-color, #d94a62) !important;
}

.snapshot-value,
.option-check,
.picker-value {
  color: var(--app-positive-color, #226f63) !important;
}


	uni-tabbar .uni-tabbar {
  left: 50% !important;
  right: auto !important;
  width: 100%;
  max-width: 480px;
  transform: translateX(-50%);
  background: transparent !important;
  border-color: var(--app-tabbar-border, #edf1f4) !important;
}

uni-tabbar .uni-tabbar__bd {
  height: 56px !important;
  padding: 4px 0 env(safe-area-inset-bottom, 0) !important;
}

uni-tabbar .uni-tabbar__item {
  padding-top: 2px !important;
  position: relative !important;
}

uni-tabbar .uni-tabbar__item {
  color: var(--app-tabbar-text, var(--app-faint, #94a3b8));
}

uni-tabbar .uni-tabbar__item::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 8px;
  width: 56px;
  height: 34px;
  border-radius: 999px;
  background: transparent;
  transform: translateX(-50%) scale(0.92);
  opacity: 0;
  transition: opacity 0.18s ease, transform 0.18s ease, background 0.18s ease;
  pointer-events: none;
}

uni-tabbar .uni-tabbar__item::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 5px;
  width: 18px;
  height: 3px;
  border-radius: 999px;
  background: transparent;
  transform: translateX(-50%) scaleX(0.6);
  opacity: 0;
  transition: opacity 0.18s ease, transform 0.18s ease, background 0.18s ease;
  pointer-events: none;
}

uni-tabbar .uni-tabbar__label {
  color: var(--app-tabbar-text, var(--app-faint, #94a3b8)) !important;
  font-size: 12px !important;
  font-weight: 650 !important;
  line-height: 1.3 !important;
  margin-top: 2px !important;
  position: relative !important;
  z-index: 1 !important;
  transition: color 0.18s ease, font-weight 0.18s ease;
}

uni-tabbar .uni-tabbar__label[style*="rgb(232, 197, 109)"],
uni-tabbar .uni-tabbar__label[style*="#e8c56d"] {
  color: var(--app-tabbar-selected, var(--app-primary, #d3a414)) !important;
  font-weight: 750 !important;
}

uni-tabbar .uni-tabbar__icon {
  width: 24px !important;
  height: 24px !important;
}

uni-tabbar .uni-tabbar__item.uni-tabbar__item--active .uni-tabbar__label {
  color: var(--app-tabbar-selected, var(--app-primary, #d3a414)) !important;
  font-weight: 750 !important;
}

uni-tabbar .uni-tabbar__item.uni-tabbar__item--active::before {
  background: var(--app-tabbar-selected-bg, rgba(211, 164, 20, 0.12));
  box-shadow: var(--app-tabbar-selected-shadow, none);
  opacity: 1;
  transform: translateX(-50%) scale(1);
}

uni-tabbar .uni-tabbar__item.uni-tabbar__item--active::after {
  background: var(--app-tabbar-selected, var(--app-primary, #d3a414));
  opacity: 1;
  transform: translateX(-50%) scaleX(1);
}

/* ---- Navigation bar ---- */
uni-page-head,
.uni-page-head,
.uni-page-head__bd,
.uni-page-head__ft {
  background: var(--app-card-bg, #ffffff) !important;
  color: var(--app-text, #17202a) !important;
  border-color: var(--app-border, #edf1f4) !important;
}

.uni-page-head__title,
.uni-page-head__btn,
.uni-page-head__btn i,
.uni-page-head .uni-btn-icon {
  color: var(--app-text, #17202a) !important;
}

/* ---- Menu items ---- */
.menu-item {
  border-color: var(--app-border, #edf1f4) !important;
}

.section-head {
  border-color: var(--app-border, #edf1f4) !important;
}

/* ---- Export / list items ---- */
.export-item {
  border-color: var(--app-border, #edf1f4) !important;
}

.snapshot-row {
  border-color: var(--app-border, #edf1f4) !important;
}


/* ---- Media queries ---- */
@media (max-width: 480px) {
  body {
    background: var(--app-page-bg, #f3f6f8);
  }

  #app {
    max-width: none;
    box-shadow: none;
  }

  uni-tabbar .uni-tabbar,
  uni-tabbar .uni-tabbar__bd {
    left: 0 !important;
    right: 0 !important;
    max-width: none;
    transform: none;
  }
}
/* #endif */
</style>
