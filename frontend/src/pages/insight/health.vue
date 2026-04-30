<template>
  <view class="container">
    <view class="hero-card">
      <view>
        <text class="hero-title">{{ heroTitle }}</text>
        <text class="hero-copy">{{ health.summary || '基于已记录数据生成，不构成投资建议。' }}</text>
      </view>
      <view class="score-ring">
        <text class="score-value">{{ scoreText }}</text>
        <text class="score-label">{{ health.dataInsufficient ? '待评估' : '分' }}</text>
      </view>
    </view>

    <view class="notice-card warning" v-if="health.dataNotice">
      <text>{{ health.dataNotice }}</text>
    </view>

    <view class="notice-card">
      <text>以下结果仅基于你已录入的账户、流水、预算和快照数据生成，不提供具体投资品种或买卖建议。</text>
    </view>

    <view class="metric-card" v-for="metric in metrics" :key="metric.key">
      <view class="metric-head">
        <view>
          <text class="metric-title">{{ metric.name }}</text>
          <text class="metric-subtitle">{{ metric.conclusion }}</text>
        </view>
        <text class="metric-status" :class="metric.level">{{ levelText(metric.level) }}</text>
      </view>
      <view class="metric-value-row">
        <text class="metric-value">{{ formatMetric(metric) }}</text>
        <text class="metric-unit">{{ metric.unit }}</text>
      </view>
      <view class="metric-advice">
        <text>{{ metric.suggestion }}</text>
      </view>
    </view>

    <view class="empty-card" v-if="!metrics.length && !loading">
      <text>暂无足够数据，继续维护账户和流水后会生成更完整的健康评级。</text>
    </view>
  </view>
</template>

<script>
import { useInsightStore } from '../../store/insight'

export default {
  data() {
    return {
      loading: false,
      health: {}
    }
  },
  computed: {
    metrics() {
      return Array.isArray(this.health.metrics) ? this.health.metrics : []
    },
    heroTitle() {
      const grade = this.health.grade
      if (this.health.dataInsufficient || !grade || grade === 'NA') return '待完善'
      return `${grade} 级`
    },
    scoreText() {
      if (this.health.dataInsufficient || this.health.grade === 'NA' || this.health.score === undefined) return '--'
      return this.health.score
    }
  },
  onShow() {
    this.fetchHealth()
  },
  methods: {
    async fetchHealth() {
      this.loading = true
      try {
        const store = useInsightStore()
        await store.fetchHealthScore()
        this.health = store.healthScore || {}
      } catch (error) {
        uni.showToast({ title: error && error.message ? error.message : '加载失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    levelText(level) {
      if (level === 'GOOD') return '良好'
      if (level === 'WARN') return '关注'
      return '风险'
    },
    formatMetric(metric) {
      const value = Number(metric.value || 0)
      if (metric.unit === '%') return Math.round(value * 100)
      if (metric.unit === '元') return value.toFixed(2)
      return value.toFixed(2)
    }
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  padding: 24rpx 22rpx calc(48rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 26rpx;
  padding: 34rpx;
  margin-bottom: 18rpx;
  border-radius: 20rpx;
  color: var(--app-hero-text, #ffffff);
  background: var(--app-hero-gradient, linear-gradient(135deg, #14202d 0%, #174a43 100%));
  box-shadow: var(--app-shadow-lg, 0 14rpx 34rpx rgba(17, 32, 45, 0.14));
}

.hero-label,
.hero-copy,
.score-label {
  color: var(--app-hero-sub, rgba(255, 255, 255, 0.72));
  font-size: 24rpx;
  line-height: 34rpx;
}

.hero-title {
  display: block;
  margin: 8rpx 0 12rpx;
  color: var(--app-hero-accent, #ffd166);
  font-size: 46rpx;
  line-height: 56rpx;
  font-weight: 900;
}

.hero-copy {
  display: block;
}

.score-ring {
  width: 126rpx;
  height: 126rpx;
  border-radius: 50%;
  background: var(--app-section-bg, rgba(255, 255, 255, 0.12));
  border: 1rpx solid var(--app-hero-badge-bg, rgba(255, 255, 255, 0.16));
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.score-value {
  color: var(--app-hero-text, #ffffff);
  font-size: 42rpx;
  line-height: 48rpx;
  font-weight: 900;
}

.notice-card,
.metric-card,
.empty-card {
  margin-bottom: 18rpx;
  padding: 26rpx 28rpx;
  border-radius: 18rpx;
  background: var(--app-card-bg, #ffffff);
  border: 1rpx solid var(--app-border, #edf1f4);
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(26, 42, 58, 0.045));
}

.notice-card,
.empty-card {
  color: var(--app-muted, #64748b);
  font-size: 24rpx;
  line-height: 36rpx;
}

.notice-card.warning {
  color: var(--app-warning-text, #8a5a13);
  background: var(--app-warning-bg, #fff8e6);
  border-color: var(--app-warning-border, #f4dfaa);
}

.metric-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}

.metric-title {
  display: block;
  color: var(--app-text, #17202a);
  font-size: 31rpx;
  line-height: 40rpx;
  font-weight: 850;
}

.metric-subtitle {
  display: block;
  margin-top: 8rpx;
  color: var(--app-muted, #64748b);
  font-size: 24rpx;
  line-height: 34rpx;
}

.metric-status {
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 800;
  background: var(--app-status-warn-bg, #fff4e2);
  color: var(--app-status-warn-text, #b7791f);
}

.metric-status.GOOD {
  background: var(--app-status-good-bg, #ecfdf5);
  color: var(--app-status-good-text, #15803d);
}

.metric-status.RISK {
  background: var(--app-status-risk-bg, #fff1f2);
  color: var(--app-danger, #d94a62);
}

.metric-value-row {
  display: flex;
  align-items: baseline;
  margin-top: 22rpx;
  gap: 8rpx;
}

.metric-value {
  color: var(--app-text, #17202a);
  font-size: 42rpx;
  line-height: 50rpx;
  font-weight: 900;
}

.metric-unit {
  color: var(--app-faint, #94a3b8);
  font-size: 24rpx;
}

.metric-advice {
  margin-top: 18rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid var(--app-border, #edf1f4);
  color: var(--app-muted, #526173);
  font-size: 25rpx;
  line-height: 36rpx;
}
</style>
