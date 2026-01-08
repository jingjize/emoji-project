// pages/index/index.js
const app = getApp()

Page({
  data: {
    previewImage: '',
    fileInfo: '',
    selectedEmotion: 'happy',
    customText: '',
    canGenerate: false,
    loading: false,
    error: '',
    resultImageUrl: '',
    resultEmotion: '',
    // 文字样式相关
    textStyleExpanded: false,
    textPositionIndex: 1, // 默认中间
    positionOptions: [
      { label: '顶部', value: 'top' },
      { label: '中间', value: 'center' },
      { label: '底部', value: 'bottom' }
    ],
    fontSize: 40,
    textColorRgb: '255,255,255',
    strokeColorRgb: '0,0,0',
    strokeWidth: 3,
    // 滤镜相关
    filterExpanded: false,
    selectedFilter: 'none',
    filters: [
      { code: 'none', name: '无滤镜' },
      { code: 'grayscale', name: '黑白' },
      { code: 'vintage', name: '复古' },
      { code: 'bright', name: '明亮' },
      { code: 'dark', name: '暗调' },
      { code: 'warm', name: '暖色' },
      { code: 'cool', name: '冷色' },
      { code: 'sepia', name: '怀旧' },
      { code: 'contrast', name: '高对比' },
      { code: 'saturate', name: '高饱和' }
    ],
    emotions: [
      { englishName: 'happy', chineseName: '高兴', icon: '😊' },
      { englishName: 'sad', chineseName: '伤心', icon: '😢' },
      { englishName: 'angry', chineseName: '生气', icon: '😠' },
      { englishName: 'surprised', chineseName: '惊讶', icon: '😲' },
      { englishName: 'confused', chineseName: '困惑', icon: '😕' },
      { englishName: 'excited', chineseName: '兴奋', icon: '🤩' },
      { englishName: 'calm', chineseName: '平静', icon: '😌' },
      { englishName: 'shy', chineseName: '害羞', icon: '😳' },
      { englishName: 'playful', chineseName: '调皮', icon: '😜' }
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

  // 文字输入
  onTextInput(e) {
    this.setData({
      customText: e.detail.value
    });
  },

  // 切换文字样式面板
  toggleTextStyle() {
    this.setData({
      textStyleExpanded: !this.data.textStyleExpanded
    });
  },

  // 切换滤镜面板
  toggleFilter() {
    this.setData({
      filterExpanded: !this.data.filterExpanded
    });
  },

  // 位置选择
  onPositionChange(e) {
    this.setData({
      textPositionIndex: parseInt(e.detail.value)
    });
  },

  // 字体大小变化
  onFontSizeChange(e) {
    this.setData({
      fontSize: e.detail.value
    });
  },

  // 文字颜色输入
  onTextColorInput(e) {
    const rgb = e.detail.value;
    if (/^\d+,\d+,\d+$/.test(rgb)) {
      this.setData({
        textColorRgb: rgb
      });
    }
  },

  // 描边颜色输入
  onStrokeColorInput(e) {
    const rgb = e.detail.value;
    if (/^\d+,\d+,\d+$/.test(rgb)) {
      this.setData({
        strokeColorRgb: rgb
      });
    }
  },

  // 描边宽度变化
  onStrokeWidthChange(e) {
    this.setData({
      strokeWidth: e.detail.value
    });
  },

  // 显示颜色选择器（使用系统颜色选择器）
  showColorPicker(e) {
    const type = e.currentTarget.dataset.type;
    const that = this;
    
    // 小程序没有原生的颜色选择器，使用输入框提示
    wx.showModal({
      title: '选择颜色',
      content: '请输入RGB值，格式：255,255,255',
      editable: true,
      placeholderText: type === 'text' ? this.data.textColorRgb : this.data.strokeColorRgb,
      success(res) {
        if (res.confirm && res.content) {
          const rgb = res.content.trim();
          if (/^\d+,\d+,\d+$/.test(rgb)) {
            const [r, g, b] = rgb.split(',').map(Number);
            if (r >= 0 && r <= 255 && g >= 0 && g <= 255 && b >= 0 && b <= 255) {
              if (type === 'text') {
                that.setData({
                  textColorRgb: rgb
                });
              } else {
                that.setData({
                  strokeColorRgb: rgb
                });
              }
            } else {
              wx.showToast({
                title: 'RGB值范围0-255',
                icon: 'none'
              });
            }
          } else {
            wx.showToast({
              title: '格式错误，请输入：255,255,255',
              icon: 'none'
            });
          }
        }
      }
    });
  },

  // 选择滤镜
  selectFilter(e) {
    const filter = e.currentTarget.dataset.filter;
    this.setData({
      selectedFilter: filter
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
    const formData = {
      'emotion': this.data.selectedEmotion
    };
    
    // 如果输入了自定义文字，添加到请求中
    if (this.data.customText && this.data.customText.trim()) {
      formData['text'] = this.data.customText.trim();
      
      // 构建文字样式JSON
      const textStyle = {
        textColor: this.data.textColorRgb,
        strokeColor: this.data.strokeColorRgb,
        strokeWidth: this.data.strokeWidth,
        fontSize: this.data.fontSize,
        position: this.data.positionOptions[this.data.textPositionIndex].value,
        fontName: 'SimHei',
        opacity: 1.0,
        rotation: 0,
        enableShadow: false
      };
      formData['textStyle'] = JSON.stringify(textStyle);
    }
    
    // 添加滤镜参数
    if (this.data.selectedFilter && this.data.selectedFilter !== 'none') {
      formData['filter'] = this.data.selectedFilter;
    }
    
    wx.uploadFile({
      url: apiUrl,
      filePath: this.data.previewImage,
      name: 'image',
      formData: formData,
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
      customText: '',
      canGenerate: false,
      error: '',
      resultImageUrl: '',
      resultEmotion: '',
      textStyleExpanded: false,
      textPositionIndex: 1,
      fontSize: 40,
      textColorRgb: '255,255,255',
      strokeColorRgb: '0,0,0',
      strokeWidth: 3,
      filterExpanded: false,
      selectedFilter: 'none'
    });
  }
})

