// pages/index/index.js
const app = getApp()

Page({
  data: {
    previewImage: '',
    fileInfo: '',
    selectedEmotion: 'happy',
    canGenerate: false,
    loading: false,
    error: '',
    resultImageUrl: '',
    resultEmotion: '',
    emotions: [
      { englishName: 'happy', chineseName: '高兴', icon: '😊' },
      { englishName: 'sad', chineseName: '伤心', icon: '😢' },
      { englishName: 'angry', chineseName: '生气', icon: '😠' },
      { englishName: 'surprised', chineseName: '惊讶', icon: '😲' },
      { englishName: 'confused', chineseName: '困惑', icon: '😕' },
      { englishName: 'excited', chineseName: '兴奋', icon: '🤩' },
      { englishName: 'calm', chineseName: '平静', icon: '😌' },
      { englishName: 'shy', chineseName: '害羞', icon: '😳' }
    ]
  },

  onLoad() {
    console.log('页面加载');
  },

  // 选择图片
  chooseImage() {
    const that = this;
    wx.chooseImage({
      count: 1,
      sizeType: ['original', 'compressed'],
      sourceType: ['album', 'camera'],
      success(res) {
        const tempFilePath = res.tempFilePaths[0];
        
        // 获取文件信息
        wx.getFileInfo({
          filePath: tempFilePath,
          success(fileInfo) {
            // 验证文件大小（10MB）
            if (fileInfo.size > 10 * 1024 * 1024) {
              that.setData({
                error: '图片大小不能超过 10MB'
              });
              return;
            }
            
            that.setData({
              previewImage: tempFilePath,
              fileInfo: `文件名: ${tempFilePath.split('/').pop()} | 大小: ${(fileInfo.size / 1024).toFixed(2)} KB`,
              canGenerate: true,
              error: '',
              resultImageUrl: '',
              resultEmotion: ''
            });
          },
          fail(err) {
            that.setData({
              error: '获取文件信息失败: ' + err.errMsg
            });
          }
        });
      },
      fail(err) {
        console.error('选择图片失败:', err);
        that.setData({
          error: '选择图片失败: ' + err.errMsg
        });
      }
    });
  },

  // 选择情绪
  selectEmotion(e) {
    const emotion = e.currentTarget.dataset.emotion;
    this.setData({
      selectedEmotion: emotion
    });
  },

  // 生成表情包
  generateMeme() {
    if (!this.data.previewImage) {
      wx.showToast({
        title: '请先选择图片',
        icon: 'none'
      });
      return;
    }

    this.setData({
      loading: true,
      error: '',
      resultImageUrl: '',
      resultEmotion: ''
    });

    const that = this;
    const apiUrl = app.globalData.apiBaseUrl + '/generate';

    // 上传图片
    wx.uploadFile({
      url: apiUrl,
      filePath: this.data.previewImage,
      name: 'image',
      formData: {
        'emotion': this.data.selectedEmotion
      },
      success(res) {
        try {
          const data = JSON.parse(res.data);
          
          if (data.success) {
            that.setData({
              resultImageUrl: data.imageUrl,
              resultEmotion: data.emotion || '表情',
              loading: false
            });
            
            wx.showToast({
              title: '生成成功！',
              icon: 'success'
            });
          } else {
            that.setData({
              error: data.message || '生成失败，请重试',
              loading: false
            });
          }
        } catch (e) {
          that.setData({
            error: '解析响应失败: ' + e.message,
            loading: false
          });
        }
      },
      fail(err) {
        console.error('上传失败:', err);
        that.setData({
          error: '网络错误: ' + err.errMsg,
          loading: false
        });
      }
    });
  },

  // 预览图片
  previewImage() {
    if (this.data.resultImageUrl) {
      wx.previewImage({
        urls: [this.data.resultImageUrl],
        current: this.data.resultImageUrl
      });
    }
  },

  // 保存图片到相册
  saveImage() {
    if (!this.data.resultImageUrl) {
      return;
    }

    const that = this;
    
    // 如果是 OSS URL，需要先下载
    if (this.data.resultImageUrl.startsWith('http://') || this.data.resultImageUrl.startsWith('https://')) {
      wx.showLoading({
        title: '下载中...'
      });
      
      wx.downloadFile({
        url: this.data.resultImageUrl,
        success(res) {
          if (res.statusCode === 200) {
            wx.saveImageToPhotosAlbum({
              filePath: res.tempFilePath,
              success() {
                wx.hideLoading();
                wx.showToast({
                  title: '保存成功',
                  icon: 'success'
                });
              },
              fail(err) {
                wx.hideLoading();
                if (err.errMsg.includes('auth deny')) {
                  wx.showModal({
                    title: '提示',
                    content: '需要授权保存图片到相册',
                    showCancel: false
                  });
                } else {
                  wx.showToast({
                    title: '保存失败: ' + err.errMsg,
                    icon: 'none'
                  });
                }
              }
            });
          } else {
            wx.hideLoading();
            wx.showToast({
              title: '下载失败',
              icon: 'none'
            });
          }
        },
        fail(err) {
          wx.hideLoading();
          wx.showToast({
            title: '下载失败: ' + err.errMsg,
            icon: 'none'
          });
        }
      });
    } else {
      // 如果是本地路径，直接保存
      wx.saveImageToPhotosAlbum({
        filePath: this.data.resultImageUrl,
        success() {
          wx.showToast({
            title: '保存成功',
            icon: 'success'
          });
        },
        fail(err) {
          wx.showToast({
            title: '保存失败: ' + err.errMsg,
            icon: 'none'
          });
        }
      });
    }
  },

  // 重置
  reset() {
    this.setData({
      previewImage: '',
      fileInfo: '',
      selectedEmotion: 'happy',
      canGenerate: false,
      error: '',
      resultImageUrl: '',
      resultEmotion: ''
    });
  }
})

