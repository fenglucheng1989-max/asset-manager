<template>
  <view class="container">
    <view class="data-hero">
      <text class="hero-title">数据与同步</text>
      <text class="hero-subtitle">导出表格、备份本机数据，或从备份文件恢复当前账号数据。</text>
    </view>

    <view class="section-card">
      <view class="section-head">
        <text class="section-title">表格导出</text>
        <text class="section-subtitle">CSV 更通用，Excel 文件可直接用表格软件打开。</text>
      </view>
      <view v-for="item in exportTypes" :key="item.type" class="export-item">
        <view class="menu-copy">
          <text class="menu-title">{{ item.name }}</text>
          <text class="menu-subtitle">{{ item.subtitle }}</text>
        </view>
        <view class="action-row">
          <button class="mini-btn ghost" @click.stop="exportCsv(item)">CSV</button>
          <button class="mini-btn" @click.stop="exportExcel(item)">Excel</button>
        </view>
      </view>
    </view>

    <view class="section-card">
      <view class="section-head">
        <text class="section-title">本地备份</text>
        <text class="section-subtitle">备份包含账户、记录、预算、快照和余额历史。恢复会覆盖当前账号业务数据。</text>
      </view>
      <view class="menu-item" @click="exportBackup">
        <view class="menu-copy">
          <text class="menu-title">导出备份 JSON</text>
          <text class="menu-subtitle">用于本机留存或迁移到其他环境</text>
        </view>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="importBackup">
        <view class="menu-copy">
          <text class="menu-title">恢复备份 JSON</text>
          <text class="menu-subtitle">H5 选择文件，App 可从剪贴板读取 JSON</text>
        </view>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <view class="section-card">
      <view class="menu-item danger" @click="confirmClearData">
        <view class="menu-copy">
          <text class="menu-title">清空所有数据</text>
          <text class="menu-subtitle">只清理业务数据，账号和登录密码会保留</text>
        </view>
        <text class="menu-arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script>
import { useUserStore } from '../../store/user'

export default {
  data() {
    return {
      exportTypes: [
        { type: 'accounts', name: '账户', subtitle: '账户余额、币种和备注' },
        { type: 'transactions', name: '记录', subtitle: '收入、支出和转账记录' },
        { type: 'budgets', name: '预算', subtitle: '月度预算和分类预算' },
        { type: 'snapshots', name: '快照', subtitle: '每日资产快照' }
      ]
    }
  },
  methods: {
    async exportCsv(item) {
      await this.exportTable(item, 'csv')
    },
    async exportExcel(item) {
      await this.exportTable(item, 'excel')
    },
    async exportTable(item, format) {
      uni.showLoading({ title: '正在导出' })
      try {
        const csv = await useUserStore().exportCsv(item.type)
        if (typeof csv !== 'string') throw new Error('导出数据格式异常')

        const filename = this.createFilename(item.name, format === 'excel' ? 'xls' : 'csv')
        const payload = format === 'excel' ? this.csvToExcelHtml(csv, item.name) : csv
        const result = await this.saveText(payload, filename, format === 'excel' ? 'application/vnd.ms-excel;charset=utf-8' : 'text/csv;charset=utf-8')
        uni.hideLoading()
        uni.showToast({ title: result === 'copied' ? '内容已复制' : '已导出', icon: 'success' })
      } catch (error) {
        uni.hideLoading()
        this.showError('导出失败', error)
      }
    },
    async exportBackup() {
      uni.showLoading({ title: '正在备份' })
      try {
        const res = await useUserStore().exportBackup()
        const backup = this.unwrapBackupResponse(res)
        const json = JSON.stringify(backup, null, 2)
        const result = await this.saveText(json, this.createFilename('资产管家备份', 'json'), 'application/json;charset=utf-8')
        uni.hideLoading()
        uni.showToast({ title: result === 'copied' ? '备份已复制' : '备份已导出', icon: 'success' })
      } catch (error) {
        uni.hideLoading()
        this.showError('备份失败', error)
      }
    },
    unwrapBackupResponse(res) {
      if (!res) throw new Error('备份接口无响应')
      if (res.code && res.code !== 200) {
        throw new Error(res.message || '备份接口返回异常')
      }
      const backup = res.code === 200 ? res.data : res
      if (!backup || typeof backup !== 'object' || Array.isArray(backup)) {
        throw new Error('备份数据格式异常')
      }
      return backup
    },
    importBackup() {
      uni.showModal({
        title: '恢复备份',
        content: '恢复会先清空当前账号的业务数据，再导入备份内容。账号本身不会被替换。',
        confirmText: '选择备份',
        confirmColor: '#d94a62',
        success: async (result) => {
          if (!result.confirm) return
          try {
            const backup = await this.readBackupPayload()
            await this.restoreBackupPayload(backup)
          } catch (error) {
            this.showError('恢复失败', error)
          }
        }
      })
    },
    async restoreBackupPayload(backup) {
      uni.showLoading({ title: '正在恢复' })
      try {
        const res = await useUserStore().restoreBackup(backup)
        if (res && res.code === 200) {
          uni.hideLoading()
          uni.showToast({ title: '恢复完成', icon: 'success' })
        }
      } finally {
        uni.hideLoading()
      }
    },
    readBackupPayload() {
      // #ifdef H5
      return this.readBackupFromFile()
      // #endif

      // #ifndef H5
      return this.readBackupFromClipboard()
      // #endif
    },
    readBackupFromFile() {
      return new Promise((resolve, reject) => {
        const input = document.createElement('input')
        input.type = 'file'
        input.accept = '.json,application/json'
        input.style.display = 'none'
        input.onchange = () => {
          const file = input.files && input.files[0]
          document.body.removeChild(input)
          if (!file) {
            reject(new Error('未选择备份文件'))
            return
          }
          const reader = new FileReader()
          reader.onload = () => {
            try {
              resolve(JSON.parse(String(reader.result || '{}')))
            } catch (error) {
              reject(new Error('备份文件不是有效 JSON'))
            }
          }
          reader.onerror = () => reject(new Error('读取备份文件失败'))
          reader.readAsText(file, 'utf-8')
        }
        document.body.appendChild(input)
        input.click()
      })
    },
    readBackupFromClipboard() {
      return new Promise((resolve, reject) => {
        uni.getClipboardData({
          success: (res) => {
            try {
              resolve(JSON.parse(res.data || '{}'))
            } catch (error) {
              reject(new Error('剪贴板内容不是有效 JSON'))
            }
          },
          fail: () => reject(new Error('读取剪贴板失败'))
        })
      })
    },
    csvToExcelHtml(csv, title) {
      const rows = this.parseCsv(csv)
      const htmlRows = rows.map(row => `<tr>${row.map(cell => `<td>${this.escapeHtml(cell)}</td>`).join('')}</tr>`).join('')
      return `<!doctype html><html><head><meta charset="utf-8"><title>${this.escapeHtml(title)}</title></head><body><table>${htmlRows}</table></body></html>`
    },
    parseCsv(csv) {
      const rows = []
      let row = []
      let cell = ''
      let quoted = false
      for (let index = 0; index < csv.length; index += 1) {
        const char = csv[index]
        const next = csv[index + 1]
        if (char === '"' && quoted && next === '"') {
          cell += '"'
          index += 1
        } else if (char === '"') {
          quoted = !quoted
        } else if (char === ',' && !quoted) {
          row.push(cell)
          cell = ''
        } else if ((char === '\n' || char === '\r') && !quoted) {
          if (char === '\r' && next === '\n') index += 1
          row.push(cell)
          if (row.some(value => value !== '')) rows.push(row)
          row = []
          cell = ''
        } else {
          cell += char
        }
      }
      if (cell || row.length) {
        row.push(cell)
        rows.push(row)
      }
      return rows
    },
    escapeHtml(value) {
      return String(value || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
    },
    createFilename(name, extension) {
      const date = new Date()
      const stamp = [
        date.getFullYear(),
        String(date.getMonth() + 1).padStart(2, '0'),
        String(date.getDate()).padStart(2, '0')
      ].join('')
      return `${name || '数据'}-${stamp}.${extension}`
    },
    async saveText(content, filename, mimeType) {
      // #ifdef H5
      try {
        this.downloadTextInBrowser(content, filename, mimeType)
        return 'saved'
      } catch (error) {
        await this.copyText(content)
        return 'copied'
      }
      // #endif

      // #ifdef APP-PLUS
      try {
        await this.writeTextInApp(content, filename)
        return 'saved'
      } catch (error) {
        await this.copyText(content)
        return 'copied'
      }
      // #endif

      await this.copyText(content)
      return 'copied'
    },
    downloadTextInBrowser(content, filename, mimeType) {
      if (typeof Blob === 'undefined' || typeof document === 'undefined') {
        throw new Error('当前浏览器不支持文件下载')
      }
      const prefix = filename.endsWith('.json') ? '' : '\ufeff'
      const blob = new Blob([prefix, content], { type: mimeType })
      const urlApi = window.URL || window.webkitURL
      if (!urlApi || !urlApi.createObjectURL) {
        throw new Error('当前浏览器不支持文件下载')
      }
      const url = urlApi.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename
      link.style.display = 'none'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      setTimeout(() => urlApi.revokeObjectURL(url), 0)
    },
    writeTextInApp(content, filename) {
      return new Promise((resolve, reject) => {
        if (typeof plus === 'undefined' || !plus.io) {
          reject(new Error('当前环境不支持文件保存'))
          return
        }
        plus.io.resolveLocalFileSystemURL('_doc/', (directoryEntry) => {
          directoryEntry.getFile(filename, { create: true }, (fileEntry) => {
            fileEntry.createWriter((writer) => {
              writer.onwrite = () => resolve(fileEntry.fullPath || filename)
              writer.onerror = () => reject(new Error('写入文件失败'))
              writer.write(content)
            }, () => reject(new Error('创建文件失败')))
          }, () => reject(new Error('创建文件失败')))
        }, () => reject(new Error('打开应用文档目录失败')))
      })
    },
    copyText(content) {
      return new Promise((resolve, reject) => {
        uni.setClipboardData({
          data: content,
          success: resolve,
          fail: (error) => reject(new Error(error && error.errMsg ? error.errMsg : '复制失败'))
        })
      })
    },
    showError(title, error) {
      const message = error && error.message ? error.message : String(error || '未知错误')
      uni.showModal({ title, content: message, showCancel: false })
    },
    confirmClearData() {
      uni.showModal({
        title: '清空所有数据',
        content: '将删除当前用户的账户、记录、预算和快照，操作不可恢复。',
        confirmText: '继续',
        confirmColor: '#d94a62',
        success: (first) => {
          if (!first.confirm) return
          uni.showModal({
            title: '再次确认',
            content: '确定清空全部业务数据吗？账号仍会保留。',
            confirmText: '清空',
            confirmColor: '#d94a62',
            success: async (second) => {
              if (!second.confirm) return
              try {
                const res = await useUserStore().clearData()
                if (res && res.code === 200) {
                  uni.showToast({ title: '数据已清空', icon: 'success' })
                }
              } catch (error) {
                const message = error && error.message ? error.message : '清空失败'
                uni.showToast({ title: message, icon: 'none' })
              }
            }
          })
        }
      })
    }
  }
}
</script>

<style scoped>
.container {
  padding: 24rpx 22rpx calc(48rpx + env(safe-area-inset-bottom));
  min-height: 100vh;
  box-sizing: border-box;
}

.data-hero {
  padding: 30rpx;
  margin-bottom: 18rpx;
  border-radius: 18rpx;
  background: var(--app-hero-gradient, linear-gradient(135deg, #14202d 0%, #174a43 100%));
  color: var(--app-hero-text, #ffffff);
}

.hero-title {
  display: block;
  font-size: 40rpx;
  line-height: 52rpx;
  font-weight: 850;
}

.hero-subtitle {
  display: block;
  margin-top: 12rpx;
  color: var(--app-hero-sub, rgba(255, 255, 255, 0.72));
  font-size: 25rpx;
  line-height: 38rpx;
}

.section-card {
  margin-bottom: 18rpx;
  overflow: hidden;
  background: var(--app-card-bg, #ffffff);
  border-radius: 18rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(26, 42, 58, 0.045));
}

.section-head {
  padding: 24rpx 28rpx 18rpx;
  border-bottom: 1rpx solid var(--app-border, #edf1f4);
}

.section-title {
  display: block;
  color: var(--app-text, #17202a);
  font-size: 31rpx;
  line-height: 40rpx;
  font-weight: 850;
}

.section-subtitle,
.menu-subtitle {
  display: block;
  color: var(--app-muted, #7b8798);
  font-size: 24rpx;
  line-height: 34rpx;
}

.section-subtitle {
  margin-top: 8rpx;
}

.export-item,
.menu-item {
  min-height: 98rpx;
  padding: 0 28rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
  border-bottom: 1rpx solid var(--app-border, #edf1f4);
}

.export-item:last-child,
.menu-item:last-child {
  border-bottom: none;
}

.menu-copy {
  flex: 1;
  min-width: 0;
  padding: 18rpx 0;
}

.menu-title {
  display: block;
  color: var(--app-text, #17202a);
  font-size: 29rpx;
  line-height: 38rpx;
  font-weight: 760;
}

.action-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.mini-btn {
  margin: 0;
  height: 56rpx;
  line-height: 56rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: var(--app-primary, #e8c56d);
  color: #ffffff;
  font-size: 24rpx;
}

.mini-btn.ghost {
  background: var(--app-soft-bg, #f2f6f4);
  color: var(--app-primary-dark, #226f63);
}

.menu-arrow {
  color: var(--app-faint, #94a3b8);
  font-size: 32rpx;
  flex-shrink: 0;
}

.danger .menu-title {
  color: var(--app-danger, #d94a62);
}
</style>
