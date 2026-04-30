<template>
  <view class="container">
    <view class="hero-card">
      <text class="hero-title">{{ milestones.length ? milestones.length + ' 个节点' : '暂无记录' }}</text>
      <text class="hero-copy">自动记录净资产突破节点与自定义财务事件</text>
    </view>

    <view class="add-card">
      <input class="note-input" v-model="note" placeholder="例如：还清车贷" maxlength="200" />
      <button class="add-btn" @click="createMilestone">添加里程碑</button>
    </view>

    <view class="timeline-card">
      <view v-for="item in milestones" :key="item.id" class="timeline-item">
        <view class="timeline-dot" :class="{ custom: item.milestoneType === 'CUSTOM' }"></view>
        <view class="timeline-content">
          <view class="timeline-head">
            <text class="timeline-title">{{ item.note }}</text>
            <text class="timeline-type">{{ typeText(item) }}</text>
          </view>
          <text class="timeline-date">{{ item.achievedAt }}</text>
        </view>
      </view>
      <view class="empty" v-if="!milestones.length && !loading">
        <text>暂无里程碑。继续记录资产后，系统会自动识别关键节点。</text>
      </view>
    </view>
  </view>
</template>

<script>
import { useInsightStore } from '../../store/insight'

export default {
  data() {
    return {
      loading: false,
      note: '',
      milestones: []
    }
  },
  onShow() {
    this.fetchMilestones()
  },
  methods: {
    async fetchMilestones() {
      this.loading = true
      try {
        const store = useInsightStore()
        await store.fetchMilestones()
        this.milestones = Array.isArray(store.milestones) ? [...store.milestones] : []
      } catch (error) {
        uni.showToast({ title: error && error.message ? error.message : '加载失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    async createMilestone() {
      const text = this.note.trim()
      if (!text) {
        uni.showToast({ title: '请输入里程碑内容', icon: 'none' })
        return
      }
      try {
        await useInsightStore().createMilestone({ note: text })
        this.note = ''
        await this.fetchMilestones()
        uni.showToast({ title: '已添加', icon: 'success' })
      } catch (error) {
        uni.showToast({ title: error && error.message ? error.message : '添加失败', icon: 'none' })
      }
    },
    typeText(item) {
      if (item.milestoneType === 'CUSTOM') return '自定义'
      return '净资产'
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

.hero-card,
.add-card,
.timeline-card {
  margin-bottom: 18rpx;
  border-radius: 18rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
  background: var(--app-card-bg, #ffffff);
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(26, 42, 58, 0.045));
}

.hero-card {
  padding: 34rpx;
  color: var(--app-hero-text, #ffffff);
  background: var(--app-hero-gradient, linear-gradient(135deg, #14202d 0%, #174a43 100%));
}

.hero-label,
.hero-copy {
  display: block;
  color: var(--app-hero-sub, rgba(255, 255, 255, 0.72));
  font-size: 24rpx;
  line-height: 34rpx;
}

.hero-title {
  display: block;
  margin: 8rpx 0 10rpx;
  color: var(--app-hero-accent, #ffd166);
  font-size: 42rpx;
  line-height: 52rpx;
  font-weight: 900;
}

.add-card {
  padding: 24rpx;
  display: flex;
  gap: 14rpx;
  align-items: center;
}

.note-input {
  flex: 1;
  min-width: 0;
  height: 76rpx;
  padding: 0 24rpx;
  border-radius: 14rpx;
  background: var(--app-input-bg, #f6f8fb);
  border: 1rpx solid var(--app-border, #edf1f4);
  color: var(--app-text, #17202a);
  font-size: 28rpx;
}

.add-btn {
  width: 180rpx;
  margin: 0;
  height: 76rpx;
  line-height: 76rpx;
  border-radius: 14rpx;
  background: var(--app-primary, #e8c56d);
  color: #ffffff;
  font-size: 27rpx;
  font-weight: 850;
}

.timeline-card {
  padding: 10rpx 0;
}

.timeline-item {
  position: relative;
  display: flex;
  gap: 20rpx;
  padding: 24rpx 28rpx;
}

.timeline-item:not(:last-child)::after {
  content: '';
  position: absolute;
  left: 38rpx;
  top: 62rpx;
  bottom: -12rpx;
  width: 2rpx;
  background: var(--app-border, #e5edf1);
}

.timeline-dot {
  position: relative;
  z-index: 1;
  width: 22rpx;
  height: 22rpx;
  margin-top: 12rpx;
  border-radius: 50%;
  background: var(--app-accent, #f4c95d);
  box-shadow: 0 0 0 10rpx var(--app-soft-bg, #fff8e1);
  flex-shrink: 0;
}

.timeline-dot.custom {
  background: var(--app-primary, #2ebd85);
  box-shadow: 0 0 0 10rpx var(--app-soft-bg, #ecfdf5);
}

.timeline-content {
  flex: 1;
  min-width: 0;
}

.timeline-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14rpx;
}

.timeline-title {
  color: var(--app-text, #17202a);
  font-size: 29rpx;
  line-height: 40rpx;
  font-weight: 850;
}

.timeline-type {
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  background: var(--app-soft-bg, #eef8f4);
  color: var(--app-primary-dark, #226f63);
  font-size: 22rpx;
  flex-shrink: 0;
}

.timeline-date {
  display: block;
  margin-top: 8rpx;
  color: var(--app-muted, #7b8798);
  font-size: 24rpx;
  line-height: 34rpx;
}

.empty {
  padding: 48rpx 34rpx;
  color: var(--app-faint, #94a3b8);
  text-align: center;
  font-size: 26rpx;
  line-height: 38rpx;
}
</style>
