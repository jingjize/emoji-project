// app.js
App({
  onLaunch() {
    // 小程序启动时执行
    console.log('AI 情绪表情生成器启动');
  },
  
  globalData: {
    // 后端 API 地址，需要根据实际情况修改
    apiBaseUrl: 'http://localhost:8080/api/meme',
    // 如果部署到服务器，修改为：https://your-domain.com/api/meme
  }
})

