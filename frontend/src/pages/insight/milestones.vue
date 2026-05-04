<template>
  <view class="container" :style="themeVars">
    <view class="add-card">
      <input class="note-input" v-model="note" placeholder="例如：还清车贷" maxlength="200" />
      <button class="add-btn" @click="createMilestone">添加</button>
    </view>

    <view class="timeline-card">
      <view v-for="item in milestones" :key="item.id" class="timeline-item">
        <view class="timeline-dot" :class="{ custom: item.milestoneType === 'CUSTOM' }"></view>

        <!-- Auto milestone: rich card -->
        <view class="timeline-content" v-if="item.milestoneType !== 'CUSTOM' && tierOf(item)">
          <view class="tier-card" :style="{ borderColor: tierOf(item).color }">
            <view class="tier-badge" :style="{ background: tierOf(item).color }">
              <text class="tier-icon">{{ tierOf(item).icon }}</text>
              <text class="tier-name">{{ tierOf(item).name }}</text>
            </view>
            <text class="tier-note">{{ item.note }}</text>
            <text class="tier-quote">"{{ tierOf(item).quote }}"</text>
            <text class="timeline-date">{{ item.achievedAt }}</text>
          </view>
        </view>

        <!-- Custom milestone: simple line -->
        <view class="timeline-content" v-else>
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
import { getThemeVars } from '../../utils/theme'

const TIERS = [
  { threshold: 10000000, icon: '⭐', name: '财务自由', color: '#c8a44e', quote: '这不是终点，而是真正的起点' },
  { threshold: 5000000,  icon: '👑', name: '人生底气', color: '#7c6f9e', quote: '你已经拥有了说不的自由' },
  { threshold: 2000000,  icon: '💎', name: '财富进阶', color: '#4a90d9', quote: '复利的魔力，才刚刚开始显现' },
  { threshold: 1000000,  icon: '🏆', name: '百万跨越', color: '#d3a414', quote: '第一个百万最难，后面的路会越来越宽' },
  { threshold: 500000,   icon: '🌿', name: '初具规模', color: '#7fa998', quote: '你已经跑赢了大多数人的起点' },
  { threshold: 100000,   icon: '🌱', name: '崭露头角', color: '#5b7a9a', quote: '每一份积累，都是自由的种子' }
]

export default {
  data() {
    return {
      loading: false,
      note: '',
      milestones: [],
      themeVars: getThemeVars()
    }
  },
  onShow() {
    this.themeVars = getThemeVars()
    this.fetchMilestones()
  },
  methods: {
    tierOf(item) {
      const t = Number(item.threshold || 0)
      return TIERS.find(tier => t >= tier.threshold) || null
    },
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
  background: var(--app-page-bg, #f8f9fb);
}

.add-card {
  margin-bottom: 18rpx;
  padding: 24rpx;
  border-radius: 20rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
  background: var(--app-card-bg, #ffffff);
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(15, 23, 42, 0.045));
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
  width: 140rpx;
  margin: 0;
  height: 64rpx;
  line-height: 64rpx;
  border-radius: 999rpx;
  background: var(--app-primary, #d3a414);
  color: #ffffff;
  font-size: 26rpx;
  font-weight: 600;
}

.timeline-card {
  padding: 10rpx 0;
  border-radius: 20rpx;
  border: 1rpx solid var(--app-border, #edf1f4);
  background: var(--app-card-bg, #ffffff);
  box-shadow: var(--app-shadow, 0 8rpx 22rpx rgba(15, 23, 42, 0.045));
}

/* ========== Timeline Item ========== */
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
  background: var(--app-primary, #d3a414);
  box-shadow: 0 0 0 10rpx var(--app-soft-bg, #eff1f5);
  flex-shrink: 0;
}

.timeline-dot.custom {
  background: var(--app-muted, #9ca3af);
  box-shadow: 0 0 0 10rpx var(--app-soft-bg, #eff1f5);
}

/* ========== Content ========== */
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
  background: var(--app-soft-bg, #eff1f5);
  color: var(--app-muted, #7b8798);
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

/* ========== Tier Card ========== */
.tier-card {
  border-radius: 18rpx;
  border: 2rpx solid;
  padding: 22rpx;
  background: var(--app-card-bg-alt, #fcfdfe);
}

.tier-badge {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  margin-bottom: 14rpx;
}

.tier-icon {
  font-size: 28rpx;
}

.tier-name {
  color: #ffffff;
  font-size: 24rpx;
  font-weight: 800;
}

.tier-note {
  display: block;
  color: var(--app-text, #17202a);
  font-size: 28rpx;
  font-weight: 800;
  line-height: 38rpx;
}

.tier-quote {
  display: block;
  margin-top: 8rpx;
  color: var(--app-muted, #7b8798);
  font-size: 24rpx;
  line-height: 34rpx;
  font-style: italic;
}

.tier-card .timeline-date {
  margin-top: 14rpx;
}

.empty {
  padding: 48rpx 34rpx;
  color: var(--app-faint, #94a3b8);
  text-align: center;
  font-size: 26rpx;
  line-height: 38rpx;
}
</style>
