const puppeteer = require('puppeteer-core');
const fs = require('fs');
const path = require('path');

const CHROME_PATH = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const BASE_URL = 'http://localhost:5173';
const SCREENSHOT_DIR = path.resolve(__dirname, 'screenshots');

// Theme definitions synced from theme.js
const THEMES = {
  sunrise: {
    mode: 'light', pageBg: '#f8f4ec', cardBg: '#fffdf8', cardBgAlt: '#faf6ef', softBg: '#f4efe4',
    text: '#2d2416', textAlt: '#2d2416', muted: '#7a6b50', faint: '#a8997a', border: '#e8dcc8',
    primary: '#b8860b', primaryDark: '#8b6914', accent: '#daa520', danger: '#c0392b',
    heroGradient: 'linear-gradient(135deg, #3d2e1a 0%, #5a4328 40%, #7a5c30 100%)',
    heroText: '#faf5e8', heroAccent: '#e8c56d', heroSub: 'rgba(250,245,232,0.70)', heroBadgeBg: 'rgba(218,165,32,0.14)',
    shadow: '0 8rpx 22rpx rgba(45,36,22,0.05)', shadowLg: '0 18rpx 40rpx rgba(45,36,22,0.14)',
    radius: '18rpx', inputBg: '#f6f2e8', netWorthColor: '#e8c56d', liabilityColor: '#e07060',
    positiveColor: '#8b6914', sectionBg: 'rgba(255,255,255,0.08)', tabbarBorder: '#e8dcc8',
    navBg: '#faf7f2', navText: '#000000'
  },
  graphite: {
    mode: 'dark', pageBg: '#1e1e1e', cardBg: '#252525', cardBgAlt: '#2a2a2a', softBg: '#2e2e2e',
    text: '#f0f0f0', textAlt: '#f0f0f0', muted: '#a0a0a0', faint: '#666666', border: '#333333',
    primary: '#6ee7b7', primaryDark: '#34d399', accent: '#6ee7b7', danger: '#f87171',
    heroGradient: 'linear-gradient(135deg, #1a1a1a 0%, #202020 35%, #252525 65%, #1f1f1f 100%)',
    heroText: '#f0f0f0', heroAccent: '#6ee7b7', heroSub: 'rgba(240,240,240,0.60)', heroBadgeBg: 'rgba(110,231,183,0.10)',
    shadow: '0 6rpx 16rpx rgba(0,0,0,0.30)', shadowLg: '0 14rpx 32rpx rgba(0,0,0,0.40)',
    radius: '16rpx', inputBg: '#2e2e2e', netWorthColor: '#6ee7b7', liabilityColor: '#f87171',
    positiveColor: '#6ee7b7', sectionBg: 'rgba(255,255,255,0.04)', tabbarBorder: '#222222',
    navBg: '#161616', navText: '#f0f0f0'
  },
  minimal: {
    mode: 'light', pageBg: '#f5f5f5', cardBg: '#ffffff', cardBgAlt: '#fafafa', softBg: '#f0f0f0',
    text: '#111827', textAlt: '#111827', muted: '#6b7280', faint: '#9ca3af', border: '#e5e7eb',
    primary: '#111827', primaryDark: '#374151', accent: '#6b7280', danger: '#ef4444',
    heroGradient: 'linear-gradient(135deg, #1f2937 0%, #374151 40%, #4b5563 100%)',
    heroText: '#f9fafb', heroAccent: '#d1d5db', heroSub: 'rgba(249,250,251,0.72)', heroBadgeBg: 'rgba(107,114,128,0.10)',
    shadow: '0 1rpx 3rpx rgba(0,0,0,0.06)', shadowLg: '0 4rpx 12rpx rgba(0,0,0,0.08)',
    radius: '12rpx', inputBg: '#f9fafb', netWorthColor: '#374151', liabilityColor: '#ef4444',
    positiveColor: '#111827', sectionBg: 'rgba(0,0,0,0.02)', tabbarBorder: '#e5e7eb',
    navBg: '#ffffff', navText: '#000000'
  },
  mint: {
    mode: 'light', pageBg: '#f0f7f3', cardBg: '#ffffff', cardBgAlt: '#f8fcf9', softBg: '#edf5f0',
    text: '#1a2e24', textAlt: '#1a2e24', muted: '#5a7d6a', faint: '#8aaa9a', border: '#d8ede0',
    primary: '#3cb878', primaryDark: '#2a8a5a', accent: '#64d49c', danger: '#e05555',
    heroGradient: 'linear-gradient(135deg, #1a3a28 0%, #2a5540 40%, #3cb878 100%)',
    heroText: '#eafaf0', heroAccent: '#8ed4b0', heroSub: 'rgba(234,250,240,0.70)', heroBadgeBg: 'rgba(100,212,156,0.12)',
    shadow: '0 4rpx 14rpx rgba(26,46,36,0.04)', shadowLg: '0 12rpx 28rpx rgba(26,46,36,0.10)',
    radius: '16rpx', inputBg: '#f4faf6', netWorthColor: '#64d49c', liabilityColor: '#f08080',
    positiveColor: '#3cb878', sectionBg: 'rgba(255,255,255,0.06)', tabbarBorder: '#d8ede0',
    navBg: '#f8fbf9', navText: '#000000'
  },
  cobalt: {
    mode: 'dark', pageBg: '#f0f4f8', cardBg: '#ffffff', cardBgAlt: '#f5f8fc', softBg: '#eef3f8',
    text: '#0b1e3d', textAlt: '#dae6f5', muted: '#4a6480', faint: '#7d94b0', border: '#dce4f0',
    primary: '#1a56b8', primaryDark: '#0f3d88', accent: '#5eead4', danger: '#e05555',
    heroGradient: 'linear-gradient(135deg, #0b1e3d 0%, #0f2d5c 38%, #133a78 70%, #0f4268 100%)',
    heroText: '#d8eafa', heroAccent: '#5eead4', heroSub: 'rgba(216,234,250,0.66)', heroBadgeBg: 'rgba(94,234,212,0.12)',
    shadow: '0 8rpx 22rpx rgba(11,30,61,0.05)', shadowLg: '0 18rpx 40rpx rgba(11,30,61,0.16)',
    radius: '18rpx', inputBg: '#f2f5fa', netWorthColor: '#5eead4', liabilityColor: '#ff8a8a',
    positiveColor: '#1a56b8', sectionBg: 'rgba(255,255,255,0.06)', tabbarBorder: '#152d52',
    navBg: '#0b1e3d', navText: '#d8eafa'
  },
  forest: {
    mode: 'light', pageBg: '#eef4ef', cardBg: '#ffffff', cardBgAlt: '#f6faf7', softBg: '#eef5f0',
    text: '#0a2318', textAlt: '#0a2318', muted: '#4a6b55', faint: '#7d9e85', border: '#d8e6dc',
    primary: '#1b6b3a', primaryDark: '#124a28', accent: '#52b788', danger: '#c0392b',
    heroGradient: 'linear-gradient(135deg, #0a2318 0%, #143828 45%, #1b5840 100%)',
    heroText: '#eaf5ee', heroAccent: '#74c69d', heroSub: 'rgba(234,245,238,0.72)', heroBadgeBg: 'rgba(82,183,136,0.14)',
    shadow: '0 8rpx 22rpx rgba(10,35,24,0.05)', shadowLg: '0 18rpx 40rpx rgba(10,35,24,0.12)',
    radius: '18rpx', inputBg: '#f2f7f4', netWorthColor: '#74c69d', liabilityColor: '#e07060',
    positiveColor: '#1b6b3a', sectionBg: 'rgba(255,255,255,0.08)', tabbarBorder: '#d8e6dc',
    navBg: '#f4f8f5', navText: '#000000'
  }
};

const PAGES = [
  { name: 'account-form', hash: '/pages/account/form', label: '账户编辑页' },
  { name: 'settings', hash: '/pages/mine/settings', label: '设置页' },
];

function buildCssVars(theme) {
  return {
    '--app-page-bg': theme.pageBg,
    '--app-card-bg': theme.cardBg,
    '--app-card-bg-alt': theme.cardBgAlt,
    '--app-soft-bg': theme.softBg,
    '--app-text': theme.text,
    '--app-text-alt': theme.textAlt,
    '--app-muted': theme.muted,
    '--app-faint': theme.faint,
    '--app-border': theme.border,
    '--app-primary': theme.primary,
    '--app-primary-dark': theme.primaryDark,
    '--app-accent': theme.accent,
    '--app-danger': theme.danger,
    '--app-hero-gradient': theme.heroGradient,
    '--app-hero-text': theme.heroText,
    '--app-hero-accent': theme.heroAccent,
    '--app-hero-sub': theme.heroSub,
    '--app-hero-badge-bg': theme.heroBadgeBg,
    '--app-shadow': theme.shadow,
    '--app-shadow-lg': theme.shadowLg,
    '--app-radius': theme.radius,
    '--app-input-bg': theme.inputBg,
    '--app-net-worth-color': theme.netWorthColor,
    '--app-liability-color': theme.liabilityColor,
    '--app-positive-color': theme.positiveColor,
    '--app-section-bg': theme.sectionBg,
    '--app-tabbar-border': theme.tabbarBorder,
  };
}

(async () => {
  if (!fs.existsSync(SCREENSHOT_DIR)) {
    fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
  }

  console.log('Launching Chrome...');
  const browser = await puppeteer.launch({
    executablePath: CHROME_PATH,
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage'],
  });

  const page = await browser.newPage();
  await page.setViewport({ width: 390, height: 844, deviceScaleFactor: 2 });

  try {
    // Set up session
    await page.goto(`${BASE_URL}/#/pages/mine/settings`, { waitUntil: 'networkidle0', timeout: 15000 });
    await page.evaluate(() => {
      localStorage.setItem('token', 'test-token');
      localStorage.setItem('username', 'demo');
    });

    for (const pageInfo of PAGES) {
      console.log(`\n=== ${pageInfo.label} ===`);

      for (const [themeName, theme] of Object.entries(THEMES)) {
        const url = `${BASE_URL}/#${pageInfo.hash}`;
        console.log(`  ${themeName}...`);

        await page.goto(url, { waitUntil: 'networkidle0', timeout: 15000 });

        // Apply theme CSS variables directly
        const cssVars = buildCssVars(theme);
        const mode = theme.mode;
        await page.evaluate((vars, t, m) => {
          const root = document.documentElement;
          root.setAttribute('data-theme', t);
          root.setAttribute('data-color-mode', m);
          Object.entries(vars).forEach(([key, value]) => {
            root.style.setProperty(key, value);
          });
        }, cssVars, themeName, mode);

        // Wait for styles to apply
        await new Promise(resolve => setTimeout(resolve, 500));

        const filename = `${pageInfo.name}_${themeName}.png`;
        await page.screenshot({
          path: path.join(SCREENSHOT_DIR, filename),
          fullPage: true,
        });
      }
    }

    // List sizes to verify different themes produced different screenshots
    console.log('\n=== Screenshot Sizes ===');
    for (const pageInfo of PAGES) {
      console.log(`\n${pageInfo.label}:`);
      for (const [themeName] of Object.entries(THEMES)) {
        const filename = `${pageInfo.name}_${themeName}.png`;
        const filepath = path.join(SCREENSHOT_DIR, filename);
        if (fs.existsSync(filepath)) {
          const stat = fs.statSync(filepath);
          console.log(`  ${themeName}: ${stat.size} bytes`);
        }
      }
    }

    console.log('\nDone!');
  } catch (err) {
    console.error('Error:', err.message);
  } finally {
    await browser.close();
  }
})();
