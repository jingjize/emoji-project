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
    // 标签页
    currentTab: 'upload',
    // 图库相关
    gallerySearchQuery: '',
    galleryCurrentCategory: '',
    galleryImages: [],
    galleryLoading: false,
    galleryPage: 1,
    selectedGalleryImage: null,
    // 黑话盒子相关
    slangWord: '',
    slangResult: false,
    slangShortExplanation: '',
    slangDetailedExplanation: '',
    slangLoading: false,
    slangExamples: [],
    showDetailed: false,
    slangRefreshing: false,
    selectedIndustry: '',
    industryExpanded: false,
    industries: [
      { code: '', name: '随机' },
      { code: '政治', name: '政治' },
      { code: '游戏', name: '游戏' },
      { code: '短视频', name: '短视频' },
      { code: '娱乐圈', name: '娱乐圈' },
      { code: '科技', name: '科技' },
      { code: '体育', name: '体育' },
      { code: '教育', name: '教育' },
      { code: '财经', name: '财经' },
      { code: '时尚', name: '时尚', hidden: true },
      { code: '美食', name: '美食', hidden: true },
      { code: '旅游', name: '旅游', hidden: true },
      { code: '汽车', name: '汽车', hidden: true },
      { code: '房产', name: '房产', hidden: true },
      { code: '医疗', name: '医疗', hidden: true },
      { code: '职场', name: '职场', hidden: true },
      { code: '生活', name: '生活', hidden: true }
    ],
    galleryCategories: [
      { code: '', name: '全部' },
      { code: 'beauty', name: '靓女' },
      { code: 'anime', name: '动漫' },
      { code: 'cartoon', name: '卡通' },
      { code: 'kawaii', name: '二次元' },
      { code: 'cute', name: '可爱' },
      { code: 'emotion', name: '表情' },
      { code: 'animals', name: '动物' },
      { code: 'nature', name: '自然' },
      { code: 'people', name: '人物' },
      { code: 'food', name: '食物' },
      { code: 'funny', name: '搞笑' }
    ],
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

  onLoad(options) {
    console.log('页面加载');
    // 加载热门词语
    this.loadHotSlangs();
    
    // 处理分享参数：如果是从分享链接进来的，显示分享的表情包
    if (options.shareImageUrl) {
      const imageUrl = decodeURIComponent(options.shareImageUrl);
      const emotion = options.emotion || 'happy';
      
      this.setData({
        resultImageUrl: imageUrl,
        selectedEmotion: emotion,
        resultEmotion: this.getEmotionChineseName(emotion)
      });
      
      wx.showModal({
        title: '🎉 好友分享的表情包',
        content: '这是好友用AI生成的表情包，你也来试试吧！',
        confirmText: '我也要做',
        cancelText: '先看看',
        success: (res) => {
          if (!res.confirm) {
            // 选择“先看看”，滚动到结果区域
            wx.pageScrollTo({
              selector: '.result-container',
              duration: 300
            });
          }
        }
      });
    }
  },
  
  // 获取情绪的中文名称
  getEmotionChineseName(englishName) {
    const emotion = this.data.emotions.find(e => e.englishName === englishName);
    return emotion ? emotion.chineseName : '表情';
  },

  // 加载热门词语
  loadHotSlangs() {
    wx.request({
      url: `${app.globalData.apiBaseUrl}/slang/hot-words`,
      method: 'GET',
      success: (res) => {
        if (res.data.success && res.data.hotSlangs && res.data.hotSlangs.length > 0) {
          this.setData({
            slangExamples: res.data.hotSlangs
          });
        }
      },
      fail: (err) => {
        console.error('加载热门词语失败:', err);
        // 使用默认词语
        this.setData({
          slangExamples: ['yyds', '破防', '内卷', '社死', 'emo', '摆烂', '躺平', '打工人']
        });
      }
    });
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

  // 生成表情包（统一处理上传图片和图库图片）
  generateMeme() {
    if (!this.data.previewImage) {
      wx.showToast({
        title: '请先选择图片或从图库选择',
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
    
    // 构建请求参数（情绪、文字、样式）
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
    
    // 判断是图库图片还是上传图片
    if (this.data.selectedGalleryImage && this.data.previewImage === this.data.selectedGalleryImage) {
      // 图库图片：使用 generate-from-gallery 接口
      formData['imageUrl'] = this.data.previewImage;
      
      wx.request({
        url: app.globalData.apiBaseUrl + '/generate-from-gallery',
        method: 'POST',
        data: formData,
        header: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        success(res) {
          that.handleGenerateResponse(res);
        },
        fail(err) {
          console.error('生成失败:', err);
          that.setData({
            error: '网络错误: ' + err.errMsg,
            loading: false
          });
        }
      });
    } else {
      // 上传图片：使用 generate 接口
      wx.uploadFile({
        url: app.globalData.apiBaseUrl + '/generate',
        filePath: this.data.previewImage,
        name: 'image',
        formData: formData,
        success(res) {
          that.handleGenerateResponse(res);
        },
        fail(err) {
          console.error('上传失败:', err);
          that.setData({
            error: '网络错误: ' + err.errMsg,
            loading: false
          });
        }
      });
    }
  },
  
  // 统一处理生成响应
  handleGenerateResponse(res) {
    try {
      const data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data;
      
      if (data.success) {
        this.setData({
          resultImageUrl: data.imageUrl,
          resultEmotion: data.emotion || '表情',
          loading: false
        });
        
        wx.showToast({
          title: '生成成功！',
          icon: 'success'
        });
      } else {
        this.setData({
          error: data.message || '生成失败，请重试',
          loading: false
        });
      }
    } catch (e) {
      this.setData({
        error: '解析响应失败: ' + e.message,
        loading: false
      });
    }
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

  // 切换标签页
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({
      currentTab: tab
    });
    
    // 如果切换到图库标签页且还没有加载图片，则加载默认图片
    if (tab === 'gallery' && this.data.galleryImages.length === 0) {
      this.loadGalleryImages('', 1, '');
    }
    // 如果切换到黑话盒子标签页，确保热门词语已加载
    if (tab === 'slang' && this.data.slangExamples.length === 0) {
      this.loadHotSlangs();
    }
  },

  // 黑话输入
  onSlangInput(e) {
    this.setData({
      slangWord: e.detail.value
    });
  },

  // 加载热门词语
  loadHotSlangs() {
    wx.request({
      url: `${app.globalData.apiBaseUrl}/slang/hot-words`,
      method: 'GET',
      success: (res) => {
        if (res.data.success && res.data.hotSlangs) {
          this.setData({
            slangExamples: res.data.hotSlangs
          });
        }
      },
      fail: (err) => {
        console.error('加载热门词语失败:', err);
        // 使用默认词语
        this.setData({
          slangExamples: ['yyds', '破防', '内卷', '社死', 'emo', '摆烂', '躺平', '打工人']
        });
      }
    });
  },

  // 切换行业展开/收起
  toggleIndustryExpand() {
    this.setData({
      industryExpanded: !this.data.industryExpanded
    });
  },

  // 选择行业
  selectIndustry(e) {
    const industry = e.currentTarget.dataset.industry || '';
    this.setData({
      selectedIndustry: industry
    });
    // 重新加载热门词语
    this.loadHotSlangs();
  },

  // 选择热门词语示例
  selectSlangExample(e) {
    const word = e.currentTarget.dataset.word;
    this.setData({
      slangWord: word
    });
    this.explainSlang();
  },

  // 解释黑话
  explainSlang() {
    const word = this.data.slangWord.trim();
    
    if (!word) {
      wx.showToast({
        title: '请输入需要解释的词语',
        icon: 'none'
      });
      return;
    }

    this.setData({
      slangLoading: true,
      slangResult: false,
      showDetailed: false
    });

    wx.request({
      url: `${app.globalData.apiBaseUrl}/slang/explain`,
      method: 'GET',
      data: {
        word: word
      },
      success: (res) => {
        if (res.data.success) {
          this.setData({
            slangWord: res.data.word,
            slangShortExplanation: res.data.shortExplanation || '暂无简短解释',
            slangDetailedExplanation: res.data.detailedExplanation || '',
            slangResult: true,
            slangLoading: false
          });
        } else {
          wx.showToast({
            title: res.data.message || '解释失败',
            icon: 'none'
          });
          this.setData({
            slangLoading: false
          });
        }
      },
      fail: (err) => {
        console.error('解释失败:', err);
        wx.showToast({
          title: '网络错误，请重试',
          icon: 'none'
        });
        this.setData({
          slangLoading: false
        });
      }
    });
  },

  // 切换详细说明显示
  toggleDetailedExplanation() {
    this.setData({
      showDetailed: !this.data.showDetailed
    });
  },

  // 刷新热门词语
  refreshHotSlangs() {
    this.setData({
      slangRefreshing: true
    });

    let url = `${app.globalData.apiBaseUrl}/slang/hot-words/refresh`;
    if (this.data.selectedIndustry) {
      url += `?industry=${encodeURIComponent(this.data.selectedIndustry)}`;
    }

    wx.request({
      url: url,
      method: 'POST',
      success: (res) => {
        if (res.data.success && res.data.hotSlangs && res.data.hotSlangs.length > 0) {
          this.setData({
            slangExamples: res.data.hotSlangs,
            slangRefreshing: false
          });
          wx.showToast({
            title: '刷新成功',
            icon: 'success',
            duration: 1500
          });
        } else {
          wx.showToast({
            title: res.data.message || '刷新失败',
            icon: 'none'
          });
          this.setData({
            slangRefreshing: false
          });
        }
      },
      fail: (err) => {
        console.error('刷新热门词语失败:', err);
        wx.showToast({
          title: '刷新失败，请重试',
          icon: 'none'
        });
        this.setData({
          slangRefreshing: false
        });
      }
    });
  },

  // 图库搜索输入
  onGallerySearchInput(e) {
    this.setData({
      gallerySearchQuery: e.detail.value
    });
  },

  // 搜索图库
  searchGallery() {
    const query = this.data.gallerySearchQuery.trim();
    this.setData({
      galleryCurrentCategory: '',
      galleryPage: 1,
      galleryImages: []
    });
    this.loadGalleryImages(query, 1, '');
  },

  // 选择图库分类
  selectGalleryCategory(e) {
    const category = e.currentTarget.dataset.category;
    this.setData({
      galleryCurrentCategory: category,
      gallerySearchQuery: '',
      galleryPage: 1,
      galleryImages: []
    });
    this.loadGalleryImages('', 1, category);
  },

  // 加载图库图片
  loadGalleryImages(query, page, category) {
    this.setData({
      galleryLoading: true
    });

    const app = getApp();
    let url;
    if (category) {
      url = `${app.globalData.apiBaseUrl}/gallery/category?category=${encodeURIComponent(category)}&page=${page}`;
    } else if (query) {
      url = `${app.globalData.apiBaseUrl}/gallery/search?query=${encodeURIComponent(query)}&page=${page}&perPage=15`;
    } else {
      url = `${app.globalData.apiBaseUrl}/gallery/curated?page=${page}&perPage=15`;
    }

    wx.request({
      url: url,
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200 && res.data.success && res.data.images) {
          const newImages = page === 1 ? res.data.images : this.data.galleryImages.concat(res.data.images);
          this.setData({
            galleryImages: newImages,
            galleryPage: page,
            galleryLoading: false
          });
        } else {
          this.setData({
            galleryLoading: false
          });
          if (page === 1) {
            wx.showToast({
              title: '暂无图片',
              icon: 'none'
            });
          }
        }
      },
      fail: (err) => {
        console.error('加载图库失败:', err);
        this.setData({
          galleryLoading: false
        });
        wx.showToast({
          title: '加载失败',
          icon: 'none'
        });
      }
    });
  },

  // 加载更多图库图片
  loadMoreGallery() {
    if (this.data.galleryLoading) return;
    
    const nextPage = this.data.galleryPage + 1;
    if (this.data.galleryCurrentCategory) {
      this.loadGalleryImages('', nextPage, this.data.galleryCurrentCategory);
    } else if (this.data.gallerySearchQuery) {
      this.loadGalleryImages(this.data.gallerySearchQuery, nextPage, '');
    } else {
      this.loadGalleryImages('', nextPage, '');
    }
  },

  // 选择图库图片
  selectGalleryImage(e) {
    const imageUrl = e.currentTarget.dataset.url; // 修复：使用 data-url 对应的 dataset.url
    if (!imageUrl) {
      wx.showToast({
        title: '图片URL无效',
        icon: 'none'
      });
      return;
    }
    
    this.setData({
      previewImage: imageUrl,
      selectedGalleryImage: imageUrl, // 标记这是图库图片
      fileInfo: `图库图片 | URL: ${imageUrl.length > 30 ? imageUrl.substring(0, 30) + '...' : imageUrl}`,
      canGenerate: true,
      error: '',
      resultImageUrl: '',
      resultEmotion: '',
      currentTab: 'upload' // 自动切换到上传图片标签页，让用户设置情绪、文字、样式、滤镜
    });
    
    wx.showToast({
      title: '图片已选择，请设置参数后生成',
      icon: 'success',
      duration: 2000
    });
  },


  // 重置
  reset() {
    this.setData({
      previewImage: '',
      fileInfo: '',
      selectedGalleryImage: '', // 清除图库选择
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
  },

  // ==================== 分享功能 ====================
  
  /**
   * 分享给好友/群聊（点击右上角转发按钮）
   * 微信小程序原生分享功能
   */
  onShareAppMessage() {
    // 如果生成了表情包，分享表情包结果
    if (this.data.resultImageUrl) {
      return {
        title: `我用AI做了个${this.data.resultEmotion}表情包，超好玩！`,
        path: `/pages/index/index?shareImageUrl=${encodeURIComponent(this.data.resultImageUrl)}&emotion=${this.data.selectedEmotion}`,
        imageUrl: this.data.resultImageUrl.startsWith('http') ? this.data.resultImageUrl : '', // OSS图片可以直接用
        success: () => {
          wx.showToast({
            title: '分享成功',
            icon: 'success'
          });
        }
      };
    }
    // 默认分享（邀请好友使用）
    return {
      title: 'AI表情盒子 - 一键生成个性表情包',
      path: '/pages/index/index',
      imageUrl: '', // 可以设置默认分享图
      success: () => {
        wx.showToast({
          title: '分享成功',
          icon: 'success'
        });
      }
    };
  },

  /**
   * 分享到朋友圈（点击右上角选择朋友圈）
   * 需要在小程序后台配置
   */
  onShareTimeline() {
    // 如果生成了表情包，分享表情包
    if (this.data.resultImageUrl) {
      return {
        title: `我用AI做了个${this.data.resultEmotion}表情包！`,
        query: `shareImageUrl=${encodeURIComponent(this.data.resultImageUrl)}&emotion=${this.data.selectedEmotion}`,
        imageUrl: this.data.resultImageUrl.startsWith('http') ? this.data.resultImageUrl : ''
      };
    }
    // 默认分享
    return {
      title: 'AI表情盒子 - 一键生成个性表情包',
      query: '',
      imageUrl: ''
    };
  },

  /**
   * 主动触发分享（显示分享菜单）
   */
  shareToWeChat() {
    if (!this.data.resultImageUrl) {
      wx.showToast({
        title: '请先生成表情包',
        icon: 'none'
      });
      return;
    }

    wx.showActionSheet({
      itemList: ['保存到相册后分享', '直接转发给好友'],
      success: (res) => {
        if (res.tapIndex === 0) {
          // 保存到相册
          this.saveImageAndShare();
        } else if (res.tapIndex === 1) {
          // 显示转发引导提示
          wx.showModal({
            title: '转发给好友',
            content: '请点击右上角「···」按钮，选择「转发」分享给好友或群聊',
            showCancel: false,
            confirmText: '我知道了'
          });
        }
      }
    });
  },

  /**
   * 保存图片并引导分享
   */
  saveImageAndShare() {
    if (!this.data.resultImageUrl) {
      return;
    }

    const that = this;
    wx.showLoading({
      title: '保存中...'
    });

    // 下载图片
    const downloadAndSave = (imageUrl) => {
      wx.downloadFile({
        url: imageUrl,
        success(res) {
          if (res.statusCode === 200) {
            wx.saveImageToPhotosAlbum({
              filePath: res.tempFilePath,
              success() {
                wx.hideLoading();
                // 显示分享引导
                wx.showModal({
                  title: '✅ 保存成功',
                  content: '图片已保存到相册\n\n快去微信发给好友吧！\n\n📱 打开微信 → 选择聊天 → 点击相册 → 选择刚保存的图片',
                  showCancel: false,
                  confirmText: '好的'
                });
              },
              fail(err) {
                wx.hideLoading();
                if (err.errMsg.includes('auth deny')) {
                  wx.showModal({
                    title: '需要授权',
                    content: '需要您授权保存图片到相册',
                    confirmText: '去授权',
                    success(res) {
                      if (res.confirm) {
                        wx.openSetting();
                      }
                    }
                  });
                } else {
                  wx.showToast({
                    title: '保存失败',
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
            title: '下载失败',
            icon: 'none'
          });
        }
      });
    };

    // 判断是否需要下载
    if (this.data.resultImageUrl.startsWith('http://') || this.data.resultImageUrl.startsWith('https://')) {
      downloadAndSave(this.data.resultImageUrl);
    } else {
      // 本地文件直接保存
      wx.saveImageToPhotosAlbum({
        filePath: this.data.resultImageUrl,
        success() {
          wx.hideLoading();
          wx.showModal({
            title: '✅ 保存成功',
            content: '图片已保存到相册\n\n快去微信发给好友吧！',
            showCancel: false
          });
        },
        fail(err) {
          wx.hideLoading();
          wx.showToast({
            title: '保存失败',
            icon: 'none'
          });
        }
      });
    }
  }
})

