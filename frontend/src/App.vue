<script>
export default {
  onLaunch() {
    console.log('App Launch')
  },
  onShow() {
    console.log('App Show')
  },
  onHide() {
    console.log('App Hide')
  }
}
</script>

<style>
page {
  background-color: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* #ifdef H5 */
body {
  background: #e9ecef;
}

#app {
  width: 100%;
  max-width: 480px;
  min-height: 100vh;
  margin: 0 auto;
  background: #f5f5f5;
  box-shadow: 0 0 32px rgba(15, 23, 42, 0.08);
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
    background: #f5f5f5;
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
