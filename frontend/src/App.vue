<script>
export default {
  onLaunch() {
    console.log('应用启动')
  },
  onShow() {
    console.log('应用显示')
  },
  onHide() {
    console.log('应用隐藏')
  }
}
</script>

<style>
page {
  background-color: #f3f6f8;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  color: #17202a;
}

/* #ifdef H5 */
body {
  margin: 0;
  background: linear-gradient(180deg, #dde7ef 0%, #f6f8fb 42%, #edf1f5 100%);
}

#app {
  width: 100%;
  max-width: 480px;
  min-height: 100vh;
  margin: 0 auto;
  background: linear-gradient(180deg, #f7fafc 0%, #eef3f6 100%);
  box-shadow: 0 0 42px rgba(15, 23, 42, 0.12);
}

uni-tabbar .uni-tabbar,
uni-tabbar .uni-tabbar__bd {
  left: 50% !important;
  right: auto !important;
  width: 100%;
  max-width: 480px;
  transform: translateX(-50%);
}

@media (max-width: 480px) {
  body {
    background: #f3f6f8;
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
