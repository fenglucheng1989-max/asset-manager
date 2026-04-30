const puppeteer = require('puppeteer-core');
const fs = require('fs');
const path = require('path');

const CHROME_PATH = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const BASE_URL = 'http://localhost:5173';

// Full theme definitions synced with theme.js (2026-04-29)
const THEMES = {
  sunrise: {
    mode:'light', pageBg:'#f8f4ec', cardBg:'#fffdf8', cardBgAlt:'#faf6ef', softBg:'#f4efe4',
    text:'#2d2416', textAlt:'#2d2416', muted:'#7a6b50', faint:'#a8997a', border:'#e8dcc8',
    primary:'#b8860b', primaryDark:'#8b6914', accent:'#daa520', danger:'#c0392b',
    inputBg:'#f6f2e8', netWorthColor:'#e8c56d', liabilityColor:'#e07060', positiveColor:'#8b6914',
    heroGradient:'linear-gradient(135deg, #3d2e1a 0%, #5a4328 40%, #7a5c30 100%)',
    heroText:'#faf5e8', heroAccent:'#e8c56d', heroSub:'rgba(250,245,232,0.70)', heroBadgeBg:'rgba(218,165,32,0.14)',
    shadow:'0 8rpx 22rpx rgba(45,36,22,0.05)', shadowLg:'0 18rpx 40rpx rgba(45,36,22,0.14)',
    radius:'18rpx', sectionBg:'rgba(255,255,255,0.08)', tabbarBorder:'#e8dcc8',
    navBg:'#faf7f2', navText:'#000000', tabBg:'#faf7f2', tabText:'#a8997a', tabSelected:'#b8860b'
  },
  cobalt: {
    mode:'dark', pageBg:'#f0f4f8', cardBg:'#ffffff', cardBgAlt:'#f5f8fc', softBg:'#eef3f8',
    text:'#0b1e3d', textAlt:'#dae6f5', muted:'#4a6480', faint:'#7d94b0', border:'#dce4f0',
    primary:'#1a56b8', primaryDark:'#0f3d88', accent:'#5eead4', danger:'#e05555',
    inputBg:'#f2f5fa', netWorthColor:'#5eead4', liabilityColor:'#ff8a8a', positiveColor:'#1a56b8',
    heroGradient:'linear-gradient(135deg, #0b1e3d 0%, #0f2d5c 38%, #133a78 70%, #0f4268 100%)',
    heroText:'#d8eafa', heroAccent:'#5eead4', heroSub:'rgba(216,234,250,0.66)', heroBadgeBg:'rgba(94,234,212,0.12)',
    shadow:'0 8rpx 22rpx rgba(11,30,61,0.05)', shadowLg:'0 18rpx 40rpx rgba(11,30,61,0.16)',
    radius:'18rpx', sectionBg:'rgba(255,255,255,0.06)', tabbarBorder:'#152d52',
    navBg:'#0b1e3d', navText:'#d8eafa', tabBg:'#0b1e3d', tabText:'#5a80a8', tabSelected:'#5eead4'
  },
  forest: {
    mode:'light', pageBg:'#eef4ef', cardBg:'#ffffff', cardBgAlt:'#f6faf7', softBg:'#eef5f0',
    text:'#0a2318', textAlt:'#0a2318', muted:'#4a6b55', faint:'#7d9e85', border:'#d8e6dc',
    primary:'#1b6b3a', primaryDark:'#124a28', accent:'#52b788', danger:'#c0392b',
    inputBg:'#f2f7f4', netWorthColor:'#74c69d', liabilityColor:'#e07060', positiveColor:'#1b6b3a',
    heroGradient:'linear-gradient(135deg, #0a2318 0%, #143828 45%, #1b5840 100%)',
    heroText:'#eaf5ee', heroAccent:'#74c69d', heroSub:'rgba(234,245,238,0.72)', heroBadgeBg:'rgba(82,183,136,0.14)',
    shadow:'0 8rpx 22rpx rgba(10,35,24,0.05)', shadowLg:'0 18rpx 40rpx rgba(10,35,24,0.12)',
    radius:'18rpx', sectionBg:'rgba(255,255,255,0.08)', tabbarBorder:'#d8e6dc',
    navBg:'#f4f8f5', navText:'#000000', tabBg:'#f4f8f5', tabText:'#6b8a75', tabSelected:'#1b6b3a'
  },
  twilight: {
    mode:'dark', pageBg:'#f4f0f8', cardBg:'#ffffff', cardBgAlt:'#f8f5fc', softBg:'#f2edf8',
    text:'#1a0f2e', textAlt:'#e4daf6', muted:'#5a4a78', faint:'#8c7da8', border:'#e4daf0',
    primary:'#7c3aed', primaryDark:'#5b21b6', accent:'#c4a0f0', danger:'#e05565',
    inputBg:'#f4f0f8', netWorthColor:'#d4a5f0', liabilityColor:'#ff8a98', positiveColor:'#7c3aed',
    heroGradient:'linear-gradient(135deg, #1a0f2e 0%, #281a48 38%, #3a1e68 68%, #2e1658 100%)',
    heroText:'#e8ddf8', heroAccent:'#d4a5f0', heroSub:'rgba(232,221,248,0.66)', heroBadgeBg:'rgba(196,160,240,0.14)',
    shadow:'0 8rpx 22rpx rgba(26,15,46,0.06)', shadowLg:'0 18rpx 40rpx rgba(26,15,46,0.16)',
    radius:'18rpx', sectionBg:'rgba(255,255,255,0.06)', tabbarBorder:'#2a1850',
    navBg:'#1a0f2e', navText:'#e8ddf8', tabBg:'#1a0f2e', tabText:'#7a65a0', tabSelected:'#c4a0f0'
  },
  graphite: {
    mode:'dark', pageBg:'#1e1e1e', cardBg:'#252525', cardBgAlt:'#2a2a2a', softBg:'#2e2e2e',
    text:'#f0f0f0', textAlt:'#f0f0f0', muted:'#a0a0a0', faint:'#8a8a8a', border:'#333333',
    primary:'#6ee7b7', primaryDark:'#34d399', accent:'#6ee7b7', danger:'#f87171',
    inputBg:'#2e2e2e', netWorthColor:'#6ee7b7', liabilityColor:'#f87171', positiveColor:'#6ee7b7',
    heroGradient:'linear-gradient(135deg, #1a1a1a 0%, #202020 35%, #252525 65%, #1f1f1f 100%)',
    heroText:'#f0f0f0', heroAccent:'#6ee7b7', heroSub:'rgba(240,240,240,0.60)', heroBadgeBg:'rgba(110,231,183,0.10)',
    shadow:'0 6rpx 16rpx rgba(0,0,0,0.30)', shadowLg:'0 14rpx 32rpx rgba(0,0,0,0.40)',
    radius:'16rpx', sectionBg:'rgba(255,255,255,0.04)', tabbarBorder:'#222222',
    navBg:'#161616', navText:'#f0f0f0', tabBg:'#161616', tabText:'#5c5c5c', tabSelected:'#6ee7b7'
  },
  sunset: {
    mode:'dark', pageBg:'#faf4f0', cardBg:'#ffffff', cardBgAlt:'#fdf8f5', softBg:'#f6f0ea',
    text:'#2d1613', textAlt:'#fdf0e8', muted:'#7a5245', faint:'#a8806a', border:'#ecdccd',
    primary:'#e85d2c', primaryDark:'#c04014', accent:'#ff9f5c', danger:'#d94444',
    inputBg:'#f6f0ea', netWorthColor:'#ff9f5c', liabilityColor:'#ff7b7b', positiveColor:'#e85d2c',
    heroGradient:'linear-gradient(135deg, #1a0f0e 0%, #2d1512 38%, #5a2010 70%, #4a1a0e 100%)',
    heroText:'#fce4d6', heroAccent:'#ff9f5c', heroSub:'rgba(252,228,214,0.66)', heroBadgeBg:'rgba(255,159,92,0.12)',
    shadow:'0 8rpx 22rpx rgba(45,22,19,0.05)', shadowLg:'0 18rpx 40rpx rgba(45,22,19,0.14)',
    radius:'18rpx', sectionBg:'rgba(255,255,255,0.06)', tabbarBorder:'#2a1815',
    navBg:'#1a0f0e', navText:'#fce4d6', tabBg:'#1a0f0e', tabText:'#8a6058', tabSelected:'#ff8c42'
  },
  minimal: {
    mode:'light', pageBg:'#f5f5f5', cardBg:'#ffffff', cardBgAlt:'#fafafa', softBg:'#f0f0f0',
    text:'#111827', textAlt:'#111827', muted:'#6b7280', faint:'#9ca3af', border:'#e5e7eb',
    primary:'#111827', primaryDark:'#374151', accent:'#6b7280', danger:'#ef4444',
    inputBg:'#f9fafb', netWorthColor:'#374151', liabilityColor:'#ef4444', positiveColor:'#111827',
    heroGradient:'linear-gradient(135deg, #1f2937 0%, #374151 40%, #4b5563 100%)',
    heroText:'#f9fafb', heroAccent:'#d1d5db', heroSub:'rgba(249,250,251,0.72)', heroBadgeBg:'rgba(107,114,128,0.10)',
    shadow:'0 1rpx 3rpx rgba(0,0,0,0.06)', shadowLg:'0 4rpx 12rpx rgba(0,0,0,0.08)',
    radius:'12rpx', sectionBg:'rgba(0,0,0,0.02)', tabbarBorder:'#e5e7eb',
    navBg:'#ffffff', navText:'#000000', tabBg:'#ffffff', tabText:'#9ca3af', tabSelected:'#111827'
  },
  mint: {
    mode:'light', pageBg:'#f0f7f3', cardBg:'#ffffff', cardBgAlt:'#f8fcf9', softBg:'#edf5f0',
    text:'#1a2e24', textAlt:'#1a2e24', muted:'#5a7d6a', faint:'#8aaa9a', border:'#d8ede0',
    primary:'#3cb878', primaryDark:'#1e6b46', accent:'#64d49c', danger:'#e05555',
    inputBg:'#f4faf6', netWorthColor:'#64d49c', liabilityColor:'#f08080', positiveColor:'#3cb878',
    heroGradient:'linear-gradient(135deg, #1a3a28 0%, #2a5540 40%, #3cb878 100%)',
    heroText:'#eafaf0', heroAccent:'#8ed4b0', heroSub:'rgba(234,250,240,0.70)', heroBadgeBg:'rgba(100,212,156,0.12)',
    shadow:'0 4rpx 14rpx rgba(26,46,36,0.04)', shadowLg:'0 12rpx 28rpx rgba(26,46,36,0.10)',
    radius:'16rpx', sectionBg:'rgba(255,255,255,0.06)', tabbarBorder:'#d8ede0',
    navBg:'#f8fbf9', navText:'#000000', tabBg:'#f8fbf9', tabText:'#8aaa9a', tabSelected:'#3cb878'
  },
  arctic: {
    mode:'light', pageBg:'#f4f7fa', cardBg:'#ffffff', cardBgAlt:'#f8fafc', softBg:'#f0f4f8',
    text:'#0f1a2e', textAlt:'#0f1a2e', muted:'#5a6d80', faint:'#8ca0b0', border:'#e2e8f0',
    primary:'#0d7eb8', primaryDark:'#095d88', accent:'#38bdf8', danger:'#e05555',
    inputBg:'#f6f9fb', netWorthColor:'#7dd3fc', liabilityColor:'#fca5a5', positiveColor:'#0d7eb8',
    heroGradient:'linear-gradient(135deg, #0f1a2e 0%, #152840 40%, #0d4a68 100%)',
    heroText:'#e8f4fb', heroAccent:'#7dd3fc', heroSub:'rgba(232,244,251,0.68)', heroBadgeBg:'rgba(56,189,248,0.10)',
    shadow:'0 6rpx 18rpx rgba(15,26,46,0.04)', shadowLg:'0 14rpx 32rpx rgba(15,26,46,0.10)',
    radius:'16rpx', sectionBg:'rgba(255,255,255,0.06)', tabbarBorder:'#e2e8f0',
    navBg:'#ffffff', navText:'#000000', tabBg:'#ffffff', tabText:'#8ca0b0', tabSelected:'#0d7eb8'
  },
  sakura: {
    mode:'light', pageBg:'#faf5f6', cardBg:'#ffffff', cardBgAlt:'#fdf9fa', softBg:'#f8f1f3',
    text:'#2d1a22', textAlt:'#2d1a22', muted:'#7a5a63', faint:'#b8959a', border:'#f0dfe3',
    primary:'#c97d88', primaryDark:'#a85d68', accent:'#e8b4b8', danger:'#d94455',
    inputBg:'#f7f0f2', netWorthColor:'#f5d0d6', liabilityColor:'#f08090', positiveColor:'#c97d88',
    heroGradient:'linear-gradient(135deg, #2d1a22 0%, #3d2430 40%, #5a3040 100%)',
    heroText:'#fdf0f2', heroAccent:'#f5d0d6', heroSub:'rgba(253,240,242,0.68)', heroBadgeBg:'rgba(232,180,184,0.12)',
    shadow:'0 8rpx 22rpx rgba(45,26,34,0.05)', shadowLg:'0 18rpx 40rpx rgba(45,26,34,0.12)',
    radius:'18rpx', sectionBg:'rgba(255,255,255,0.08)', tabbarBorder:'#f0dfe3',
    navBg:'#fdf8f9', navText:'#000000', tabBg:'#fdf8f9', tabText:'#b8959a', tabSelected:'#c97d88'
  }
};

// CSS vars to apply for each theme
function themeVars(t) {
  return {
    '--app-page-bg':t.pageBg,'--app-card-bg':t.cardBg,'--app-card-bg-alt':t.cardBgAlt,
    '--app-soft-bg':t.softBg,'--app-text':t.text,'--app-text-alt':t.textAlt,
    '--app-muted':t.muted,'--app-faint':t.faint,'--app-border':t.border,
    '--app-primary':t.primary,'--app-primary-dark':t.primaryDark,'--app-accent':t.accent,
    '--app-danger':t.danger,'--app-input-bg':t.inputBg,
    '--app-liability-color':t.liabilityColor,'--app-positive-color':t.positiveColor,
    '--app-net-worth-color':t.netWorthColor,'--app-shadow':t.shadow,'--app-shadow-lg':t.shadowLg,
    '--app-radius':t.radius,'--app-section-bg':t.sectionBg,'--app-tabbar-border':t.tabbarBorder,
  };
}

// Calculate WCAG contrast ratio
function contrastRatio(hex1, hex2) {
  const lum = (hex) => {
    const r = parseInt(hex.slice(1,3),16)/255;
    const g = parseInt(hex.slice(3,5),16)/255;
    const b = parseInt(hex.slice(5,7),16)/255;
    const linear = (c) => c <= 0.03928 ? c/12.92 : Math.pow((c+0.055)/1.055, 2.4);
    return 0.2126*linear(r) + 0.7152*linear(g) + 0.0722*linear(b);
  };
  const l1 = lum(hex1), l2 = lum(hex2);
  const lighter = Math.max(l1,l2), darker = Math.min(l1,l2);
  return ((lighter+0.05)/(darker+0.05)).toFixed(1);
}

// Parse hex from any CSS color, walking up DOM for transparent backgrounds
async function getEffectiveColors(page, selector) {
  return await page.evaluate((sel) => {
    const el = document.querySelector(sel);
    if (!el) return { found: false };

    const style = window.getComputedStyle(el);
    let bgColor = style.backgroundColor;

    // Walk up parent tree if background is transparent
    let current = el.parentElement;
    const MAX_DEPTH = 10;
    let depth = 0;
    while (
      current &&
      depth < MAX_DEPTH &&
      (!bgColor || bgColor === 'rgba(0, 0, 0, 0)' || bgColor === 'transparent')
    ) {
      const parentStyle = window.getComputedStyle(current);
      bgColor = parentStyle.backgroundColor;
      current = current.parentElement;
      depth++;
    }

    // Fallback for body/html
    if (!bgColor || bgColor === 'rgba(0, 0, 0, 0)' || bgColor === 'transparent') {
      bgColor = 'rgb(255, 255, 255)'; // assume white as ultimate fallback
    }

    let borderColor = style.borderColor;
    if (!borderColor || borderColor === 'rgba(0, 0, 0, 0)' || borderColor === 'transparent') {
      // Try border-top-color for elements with mixed borders
      borderColor = style.borderTopColor || style.borderBottomColor;
    }

    return {
      found: true,
      text: el.textContent?.trim().substring(0, 60),
      backgroundColor: bgColor,
      color: style.color,
      borderColor: borderColor,
      borderWidth: style.borderWidth,
      borderStyle: style.borderStyle,
      fontSize: style.fontSize,
      display: style.display,
      visibility: style.visibility,
      opacity: style.opacity,
      fontWeight: style.fontWeight,
    };
  }, selector);
}

function parseHex(color) {
  if (!color) return '#000000';
  const m = color.match(/rgb\((\d+),\s*(\d+),\s*(\d+)\)/);
  if (m) return '#' + [m[1],m[2],m[3]].map(x => parseInt(x).toString(16).padStart(2,'0')).join('');
  const ma = color.match(/rgba\((\d+),\s*(\d+),\s*(\d+)/);
  if (ma) return '#' + [ma[1],ma[2],ma[3]].map(x => parseInt(x).toString(16).padStart(2,'0')).join('');
  if (color.startsWith('#')) return color;
  return color;
}

function statusIcon(ratio, threshold = 4.5) {
  if (ratio >= threshold) return '\x1b[32m✓\x1b[0m'; // green
  if (ratio >= 3) return '\x1b[33m⚠\x1b[0m'; // yellow
  return '\x1b[31m✗\x1b[0m'; // red
}

// Map 3.0-4.4 ratios as "warn" instead of "fail" for large text (>=18px or bold >=14px)
function evaluateRatio(ratio, fontSize, fontWeight) {
  const sizeNum = parseFloat(fontSize);
  const isBold = fontWeight >= 600 || fontWeight === 'bold';
  // Large text: >=18px or bold >=14px → need 3:1
  const isLargeText = sizeNum >= 18 || (sizeNum >= 14 && isBold);
  const threshold = isLargeText ? 3 : 4.5;
  if (ratio >= threshold) return 'pass';
  if (ratio >= 3) return 'warn';
  return 'fail';
}

async function auditPage(page, pageName, hash, selectors, themeName, theme) {
  const vars = themeVars(theme);
  const mode = theme.mode;

  // Navigate and apply theme
  await page.goto(`${BASE_URL}/#${hash}`, { waitUntil: 'networkidle0', timeout: 15000 });
  await page.evaluate((v, t, m) => {
    const root = document.documentElement;
    root.setAttribute('data-theme', t);
    root.setAttribute('data-color-mode', m);
    Object.entries(v).forEach(([k,val]) => root.style.setProperty(k, val));
  }, vars, themeName, mode);
  await new Promise(r => setTimeout(r, 400));

  console.log(`\n  ┌─ ${pageName} ─ ${themeName} (${mode}) ─┐`);

  const results = [];
  for (const [name, selector] of Object.entries(selectors)) {
    if (name.startsWith('_')) continue; // skip metadata keys
    const info = await getEffectiveColors(page, selector);
    if (!info.found) {
      console.log(`  │ ✗ ${name.padEnd(32)} NOT FOUND`);
      results.push({ name, found: false });
      continue;
    }
    const bg = parseHex(info.backgroundColor);
    const fg = parseHex(info.color);
    const border = parseHex(info.borderColor);
    const ratio = parseFloat(contrastRatio(bg, fg));
    const eval_ = evaluateRatio(ratio, info.fontSize, info.fontWeight);
    const icon = eval_ === 'pass' ? statusIcon(ratio) : eval_ === 'warn' ? statusIcon(ratio, 3) : '\x1b[31m✗\x1b[0m';
    const label = eval_ === 'pass' ? '' : eval_ === 'warn' ? ' [WARN]' : ' [FAIL]';
    console.log(`  │ ${icon} ${name.padEnd(32)} bg=${bg} fg=${fg} ratio=${ratio}:1${label}`);
    if (info.borderWidth && info.borderWidth !== '0px' && info.borderStyle !== 'none') {
      console.log(`  │   border: ${info.borderWidth} ${border} style=${info.borderStyle}`);
    }
    if (info.text && info.text.length > 0) {
      console.log(`  │   text: "${info.text.substring(0,50)}"`);
    }
    results.push({ name, bg, fg, border, ratio, eval: eval_, found: true, text: info.text });
  }
  return results;
}

(async () => {
  const browser = await puppeteer.launch({
    executablePath: CHROME_PATH,
    headless: 'new',
    args: ['--no-sandbox'],
  });

  const page = await browser.newPage();
  await page.setViewport({ width: 390, height: 844 });

  // Set up session
  await page.goto(`${BASE_URL}/#/pages/mine/settings`, { waitUntil: 'networkidle0', timeout: 15000 });
  await page.evaluate(() => {
    localStorage.setItem('token', 'test-token');
    localStorage.setItem('username', 'demo');
  });

  // ========== PAGE DEFINITIONS ==========

  const pages = [
    {
      name: '账户编辑页',
      hash: '/pages/account/form',
      selectors: {
        'Page Title': '.page-title',
        'Kind Tab (active)': '.kind-tab.active',
        'Kind Tab (inactive)': '.kind-tab:not(.active)',
        'Kind Tabs Container': '.account-kind-tabs',
        'Form Card': '.form-card',
        'Group Title': '.group-title',
        'Form Label (name)': '.form-item:nth-child(2) .form-label',
        'Form Input (name)': '.form-item:nth-child(2) .form-input',
        'Field Hint': '.field-hint',
        'Form Picker': '.form-picker',
        'Picker Arrow': '.picker-arrow',
        'Save Button': '.btn-save',
        'Delete Button': '.btn-delete',
        'Switch Row': '.form-item:last-child',
      }
    },
    {
      name: '设置页',
      hash: '/pages/mine/settings',
      selectors: {
        'Section Group': '.section-group',
        'Section Item': '.section-item',
        'Item Title (视觉装扮)': '.section-item:first-child .item-title',
        'Item Desc': '.section-item:first-child .item-desc',
        'Item Arrow': '.section-item:first-child .item-arrow',
        'Inline Picker (Currency)': '.inline-picker',
        'Theme Mini Preview': '.theme-mini-preview',
      }
    },
  ];

  // ========== RUN AUDITS ==========

  const allFailures = [];
  const allWarnings = [];

  for (const pageDef of pages) {
    console.log(`\n${'═'.repeat(60)}`);
    console.log(`  ${pageDef.name.toUpperCase()}`);
    console.log(`${'═'.repeat(60)}`);

    for (const [themeName, theme] of Object.entries(THEMES)) {
      const results = await auditPage(page, pageDef.name, pageDef.hash, pageDef.selectors, themeName, theme);
      for (const r of results) {
        if (!r.found) continue;
        if (r.eval === 'fail') {
          allFailures.push(`${pageDef.name} / ${themeName}: ${r.name} (${r.bg}→${r.fg} ${r.ratio}:1)`);
        } else if (r.eval === 'warn') {
          allWarnings.push(`${pageDef.name} / ${themeName}: ${r.name} (${r.bg}→${r.fg} ${r.ratio}:1)`);
        }
      }
    }
  }

  // ========== INPUT BORDER SPECIAL AUDIT ==========
  console.log(`\n\n${'═'.repeat(60)}`);
  console.log('  INPUT / BORDER SPECIAL AUDIT');
  console.log(`${'═'.repeat(60)}`);

  // Account form page has the most input elements
  for (const [themeName, theme] of Object.entries(THEMES)) {
    const vars = themeVars(theme);
    const mode = theme.mode;
    await page.goto(`${BASE_URL}/#/pages/account/form`, { waitUntil: 'networkidle0', timeout: 15000 });
    await page.evaluate((v, t, m) => {
      const root = document.documentElement;
      root.setAttribute('data-theme', t);
      root.setAttribute('data-color-mode', m);
      Object.entries(v).forEach(([k,val]) => root.style.setProperty(k, val));
    }, vars, themeName, mode);
    await new Promise(r => setTimeout(r, 400));

    // Audit input borders specifically
    const inputInfo = await page.evaluate(() => {
      const inputs = document.querySelectorAll('input, .form-input, .form-card, .account-kind-tabs, .adjust-input-wrap, .field-picker, .section-group');
      const results = [];
      inputs.forEach((el, i) => {
        const style = window.getComputedStyle(el);
        const tagName = el.tagName;
        const className = el.className?.toString?.() || '';
        const shortClass = className.split(' ').slice(0, 3).join(' ');
        results.push({
          tag: tagName,
          class: shortClass,
          borderWidth: style.borderWidth,
          borderColor: style.borderColor,
          borderStyle: style.borderStyle,
          backgroundColor: style.backgroundColor,
          color: style.color,
        });
      });
      return results;
    });

    console.log(`\n  --- ${themeName} (${theme.mode}) ---`);
    console.log(`  pageBg=${theme.pageBg}  cardBg=${theme.cardBg}  border=${theme.border}`);
    for (const inp of inputInfo) {
      if (inp.borderWidth === '0px' && inp.borderStyle === 'none') continue; // skip borderless
      const borderHex = parseHex(inp.borderColor);
      const bgHex = parseHex(inp.backgroundColor);
      const borderVis = contrastRatio(bgHex, borderHex);
      const visIcon = borderVis >= 1.5 ? 'visible' : '\x1b[31mfaint\x1b[0m';
      console.log(`    ${inp.tag.padEnd(8)} ${inp.class.padEnd(30)} border=${inp.borderWidth} ${borderHex} (vs bg ${bgHex} Δ${borderVis}:1 ${visIcon})`);
    }
  }

  // ========== SUMMARY ==========
  console.log(`\n\n${'═'.repeat(60)}`);
  console.log('  SUMMARY');
  console.log(`${'═'.repeat(60)}`);

  if (allFailures.length > 0) {
    console.log(`\n  ❌ FAILURES (${allFailures.length}):`);
    for (const f of allFailures) {
      console.log(`     ${f}`);
    }
  } else {
    console.log('\n  ✅ No WCAG failures found.');
  }

  if (allWarnings.length > 0) {
    console.log(`\n  ⚠️  WARNINGS (${allWarnings.length}):`);
    for (const w of allWarnings) {
      console.log(`     ${w}`);
    }
  }

  await browser.close();
  console.log('\n✓ Audit complete.');
})();
