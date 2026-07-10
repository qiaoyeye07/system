<template>
  <div class="chat-page">
    <div class="chat-layout">
      <aside class="contact-panel">
        <h3>消息</h3>
        <div class="user-search">
          <input ref="userSearchInput" v-model="userSearchKeyword" type="text" placeholder="搜索用户..." @keyup.enter="goToUserProfile" />
          <button @click="goToUserProfile" :disabled="searchingUser || !userSearchKeyword.trim()">{{ searchingUser ? '搜索中...' : '搜索' }}</button>
        </div>
        <LoadingState v-if="loadingContacts" />
        <EmptyState v-else-if="contacts.length === 0" text="暂无消息" />
        <div v-else class="contact-list">
          <div
            v-for="c in contacts"
            :key="`${c.contactId}-${c.productId || 'none'}`"
            class="contact-item"
            :class="{ active: isActiveContact(c), muted: isMuted(c) }"
            @click="openChat(c.contactId, c.productId)"
            @contextmenu.prevent="onContextMenu($event, c)"
          >
            <div class="contact-info">
              <strong>{{ c.contactName }}</strong>
              <span v-if="c.productTitle" class="contact-product">{{ c.productTitle }}</span>
              <span v-if="isMuted(c)" class="muted-tag">免打扰</span>
              <p class="last-msg">{{ c.lastMessage }}<span v-if="c.lastMessageIsMine" class="read-tag" :class="{ read: c.lastMessageIsRead }">{{ c.lastMessageIsRead ? '已读' : '未读' }}</span></p>
            </div>
            <div class="contact-meta">
              <span class="time">{{ formatTime(c.lastMessageTime) }}</span>
              <span v-if="c.unreadCount && !isMuted(c)" class="badge">{{ c.unreadCount }}</span>
            </div>
          </div>
        </div>

        <!-- 右键菜单 -->
        <div v-if="contextMenu.visible" class="context-menu" :style="{ top: contextMenu.y + 'px', left: contextMenu.x + 'px' }" @click.stop>
          <div class="context-item" @click="handleMute">免打扰</div>
          <div class="context-item danger" @click="handleDeleteConversation">删除对话</div>
        </div>
        <div v-if="contextMenu.visible" class="context-overlay" @click="contextMenu.visible = false" @contextmenu.prevent="contextMenu.visible = false"></div>
      </aside>

      <section class="chat-panel">
        <template v-if="!activeContact">
          <EmptyState text="选择联系人开始聊天" />
        </template>
        <template v-else>
          <div class="chat-header">
            <strong class="contact-name-link" @click="router.push('/user/' + activeContact)">{{ activeContactName }}</strong>
            <span v-if="activeProductTitle" class="product-tag">{{ activeProductTitle }}</span>
            <button class="btn-refresh" @click="fetchMessages" :disabled="loadingMessages">刷新</button>
          </div>

          <!-- 商品信息条 -->
          <div v-if="productInfo && activeProductId" class="product-bar" @click="goToProduct">
            <img v-if="productInfo.images" :src="'/' + productInfo.images.split(',')[0]" class="product-bar-img" @load="scrollToBottomAfterRender" />
            <div class="product-bar-info">
              <span class="product-bar-title">{{ productInfo.title }}</span>
              <span class="product-bar-price">¥{{ productInfo.price }}</span>
            </div>
            <button v-if="!isProductOwner && canBuyProduct" class="product-bar-btn" @click.stop="handleProductAction">去购买</button>
            <button v-if="!isProductOwner && canSwapProduct" class="product-bar-btn swap" @click.stop="handleSwapProduct">去交换</button>
          </div>

          <div class="message-list" ref="msgList">
            <LoadingState v-if="loadingMessages" />
            <div v-for="msg in messages" :key="msg.id"
              :class="msg.messageType === 'SYSTEM_MSG' ? 'system-msg' : ['message', { mine: Number(msg.senderId) === Number(myId) }]">
              <div v-if="msg.messageType === 'SYSTEM_MSG'" class="system-msg-content">
                <span class="system-msg-icon">⚖</span>
                <p>{{ parseOrderCard(msg).text || '订单纠纷处理中' }}</p>
                <div class="card-actions" v-if="getOrderCardActions(msg).length">
                  <button v-for="act in getOrderCardActions(msg)" :key="act.key"
                    :class="act.cls" @click="act.handler">{{ act.label }}</button>
                </div>
              </div>
              <div v-else class="msg-content">
                <img v-if="msg.messageType === 'IMAGE'" :src="msg.attachmentUrl" class="msg-img" @load="scrollToBottomAfterRender" @click="window.open(msg.attachmentUrl)" />
                <video v-else-if="msg.messageType === 'VIDEO'" :src="msg.attachmentUrl" controls class="msg-video" @loadedmetadata="scrollToBottomAfterRender"></video>
                <div v-else-if="msg.messageType === 'PRODUCT_CARD'" class="msg-card" @click="goToCardProduct(msg)">
                  <span class="card-label">📦 分享了商品</span>
                  <div class="card-row">
                    <img v-if="getCardImage(msg)" :src="getCardImage(msg)" class="card-thumb" @load="scrollToBottomAfterRender" />
                    <strong>{{ msg.content }}</strong>
                  </div>
                </div>
                <div v-else-if="msg.messageType === 'ORDER_CARD'" class="msg-card order-card" @click.stop>
                  <p class="card-label">📋 {{ parseOrderCard(msg).text || '订单消息' }}</p>
                  <!-- 目标商品 -->
                  <div class="card-row">
                    <img v-if="parseOrderCard(msg).productImage" :src="'/' + parseOrderCard(msg).productImage" class="card-thumb" @load="scrollToBottomAfterRender" />
                    <div class="card-info">
                      <strong>{{ parseOrderCard(msg).productTitle }}</strong>
                      <span class="card-price">¥{{ parseOrderCard(msg).productPrice?.toFixed(2) }}</span>
                      <span class="card-status">{{ statusLabel(parseOrderCard(msg).orderStatus) }}</span>
                    </div>
                  </div>
                  <!-- 交换物 -->
                  <div v-if="parseOrderCard(msg).swapProductTitle" class="card-row" style="margin-top:8px;padding-top:8px;border-top:1px dashed var(--border)">
                    <img v-if="parseOrderCard(msg).swapProductImage" :src="'/' + parseOrderCard(msg).swapProductImage" class="card-thumb" @load="scrollToBottomAfterRender" />
                    <div class="card-info">
                      <strong style="color:var(--success)">⇄ {{ parseOrderCard(msg).swapProductTitle }}</strong>
                    </div>
                  </div>
                  <div class="card-actions" v-if="getOrderCardActions(msg).length">
                    <button v-for="act in getOrderCardActions(msg)" :key="act.key"
                      :class="act.cls" @click="act.handler">{{ act.label }}</button>
                  </div>
                </div>
                <template v-else>{{ msg.content }}</template>
              </div>
              <div v-if="msg.messageType !== 'SYSTEM_MSG'" class="msg-meta">
                <span class="msg-time">{{ formatMessageTime(msg.createdAt) }}</span>
                <span v-if="Number(msg.senderId) === Number(myId)" class="msg-status" :class="{ read: msg.isRead }">{{ msg.isRead ? '已读' : '未读' }}</span>
                <button v-if="Number(msg.senderId) !== Number(myId)" class="btn-report-msg" @click="reportMessage(msg)">举报</button>
              </div>
            </div>
          </div>

          <div class="chat-input">
            <input ref="msgInput" v-model="newMsg" type="text" placeholder="输入消息..." :disabled="sending" @keyup.enter="sendMessage" />
            <input type="file" ref="fileInput" accept="image/*,video/*" style="display:none" @change="onFilePicked" />
            <button class="btn-attach" :disabled="sending" @click="fileInput?.click()">📎</button>
            <button class="btn-attach" :disabled="sending || !activeContact" @click="showProductPicker = true">📦</button>
            <button @click="sendMessage" :disabled="!canSend">{{ sending ? '发送中...' : '发送' }}</button>
          </div>
          <!-- 商品选择弹窗 -->
          <div v-if="showProductPicker" class="picker-overlay" @click.self="showProductPicker = false">
            <div class="picker-card">
              <h4>分享商品到聊天</h4>
              <input v-model="pickerKeyword" type="text" placeholder="输入商品ID或关键词搜索..." @keyup.enter="searchProducts" />
              <button class="btn-search" @click="searchProducts">搜索</button>
              <div v-if="pickerProducts.length" class="picker-list">
                <div v-for="p in pickerProducts" :key="p.id" class="picker-item" @click="sendProductCard(p)">
                  <img v-if="p.firstImage" :src="p.firstImage" class="picker-thumb" />
                  <div class="picker-info">
                    <strong>{{ p.title }}</strong>
                    <span>¥{{ p.price?.toFixed(2) }}</span>
                  </div>
                </div>
              </div>
              <EmptyState v-else-if="pickerSearched" text="未找到商品" />
              <button class="btn-cancel" @click="showProductPicker = false">关闭</button>
            </div>
          </div>
          <div v-if="pendingPreview" class="img-preview">
            <img v-if="pendingType === 'IMAGE'" :src="pendingPreview" />
            <video v-else-if="pendingType === 'VIDEO'" :src="pendingPreview" controls class="video-preview" />
            <button @click="pendingPreview='';pendingFile=null;pendingType=''">✕</button>
          </div>

          <!-- 评价弹窗 -->
          <div v-if="showRatingModal" class="picker-overlay" @click.self="showRatingModal = false">
            <div class="picker-card" style="width:400px">
              <h4>评价</h4>
              <StarRating v-model="ratingScore" :showText="true" />
              <div style="margin-top:12px">
                <label style="display:block;margin-bottom:4px;font-size:14px">评价内容</label>
                <textarea v-model="ratingComment" rows="4" maxlength="500" placeholder="写下本次交易体验..." style="width:100%;padding:8px;border:1px solid var(--border);border-radius:4px;resize:vertical"></textarea>
              </div>
              <div style="display:flex;justify-content:flex-end;gap:8px;margin-top:12px">
                <button class="btn-cancel" @click="showRatingModal = false">取消</button>
                <button class="btn-primary" @click="doRating">提交评价</button>
              </div>
            </div>
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onActivated, onMounted, nextTick, watch } from 'vue'
defineOptions({ name: 'ChatView' })
import { useRoute, useRouter } from 'vue-router'
import { chatAPI, reportAPI, productAPI, orderAPI, swapAPI, userAPI, ratingAPI } from '../api/modules.js'
import LoadingState from '../components/common/LoadingState.vue'
import EmptyState from '../components/common/EmptyState.vue'
import StarRating from '../components/common/StarRating.vue'

const route = useRoute()
const router = useRouter()
const contacts = ref([])
const productInfo = ref(null)
const isProductOwner = computed(() => productInfo.value && Number(productInfo.value.sellerId) === Number(myId))
const canBuyProduct = computed(() => productInfo.value?.status === 'ACTIVE' && productInfo.value?.tradeMode !== 'SWAP')
const canSwapProduct = computed(() => productInfo.value?.status === 'ACTIVE' && (productInfo.value?.tradeMode === 'SWAP' || productInfo.value?.tradeMode === 'BOTH'))
const messages = ref([])
const activeContact = ref(null)
const activeContactName = ref('')
const activeProductId = ref(null)
const activeProductTitle = ref('')
const newMsg = ref('')
const loadingContacts = ref(false)
const loadingMessages = ref(false)
const sending = ref(false)
const canSend = computed(() => !sending.value && (newMsg.value.trim() || pendingFile.value))
const searchingUser = ref(false)
const pendingPreview = ref('')
const pendingFile = ref(null)
const pendingType = ref('')
const fileInput = ref(null)
const userSearchInput = ref(null)
const msgInput = ref(null)
const msgList = ref(null)

const userSearchKeyword = ref('')
const focusSearchOnReturn = ref(false)
const routeChatOpened = ref(false)
// Reset auto-open flag when route query changes
watch(() => route.query.contactId, () => { routeChatOpened.value = false })
// Product picker
const showProductPicker = ref(false)
const pickerKeyword = ref('')
const pickerProducts = ref([])
const pickerSearched = ref(false)

// 评价弹窗
const showRatingModal = ref(false)
const ratingScore = ref(5)
const ratingComment = ref('')
const ratingOrderId = ref(null)
const ratingRatedUserId = ref(null)
const ratedOrders = ref(new Set(JSON.parse(localStorage.getItem('ratedOrders') || '[]')))
const contextMenu = ref({ visible: false, x: 0, y: 0, contact: null })
const user = JSON.parse(localStorage.getItem('user') || 'null')
const myId = user?.id

// 免打扰列表存 localStorage：key = `${contactId}_${productId||0}`
const getMuteKey = (contact) => `${contact.contactId}_${contact.productId || 0}`
const getMutedList = () => JSON.parse(localStorage.getItem('mutedContacts') || '[]')
const isMuted = (contact) => getMutedList().includes(getMuteKey(contact))

const sameProduct = (left, right) => {
  if (!left && !right) return true
  return Number(left) === Number(right)
}

const isActiveContact = (contact) =>
  Number(contact.contactId) === Number(activeContact.value) && sameProduct(contact.productId, activeProductId.value)

const fetchContacts = async () => {
  loadingContacts.value = true
  try {
    const res = await chatAPI.getContacts()
    contacts.value = res.data || []
    const contactId = route.query.contactId || route.params.contactId
    if (contactId && !routeChatOpened.value) {
      const targetId = Number(contactId)
      if (!Number.isFinite(targetId) || targetId <= 0) return
      const productId = route.query.productId ? Number(route.query.productId) : null
      let c = contacts.value.find(item => Number(item.contactId) === targetId && sameProduct(item.productId, productId))
      // If no existing conversation, create a placeholder so chat can start immediately
      if (!c) {
        c = {
          contactId: targetId,
          contactName: route.query.contactName || `用户${targetId}`,
          productId: productId || undefined,
          productTitle: '',
          lastMessage: '',
          lastMessageTime: null,
          unreadCount: 0
        }
        contacts.value.unshift(c)
      }
      routeChatOpened.value = true
      await openChat(c.contactId, c.productId, c.contactName)
      // Auto-send product card if came from product detail page
      if (route.query.sendCard === 'true' && route.query.productId) {
        const pid = Number(route.query.productId)
        if (pid > 0) {
          try {
            const prodRes = await productAPI.getDetail(pid)
            const p = prodRes.data
            await chatAPI.send({
              receiverId: targetId,
              productId: pid,
              content: p?.title || `商品 #${pid}`,
              messageType: 'PRODUCT_CARD',
              attachmentUrl: p?.images?.split(',')[0] || ''
            })
            await fetchMessages()
          } catch {}
        }
      }
    }
  } catch (e) {
    contacts.value = []
  } finally {
    loadingContacts.value = false
  }
}

const fetchMessages = async () => {
  if (!activeContact.value) return
  loadingMessages.value = true
  try {
    const params = {}
    if (activeProductId.value) params.productId = activeProductId.value
    const res = await chatAPI.getMessages(activeContact.value, params)
    messages.value = (res.data?.content || []).reverse()
  } catch (e) {
    messages.value = []
  } finally {
    loadingMessages.value = false
    await scrollToBottomAfterRender()
  }
}

const onContextMenu = (event, contact) => {
  contextMenu.value = { visible: true, x: event.clientX, y: event.clientY, contact }
}

const handleMute = () => {
  const c = contextMenu.value.contact
  if (!c) return
  const key = getMuteKey(c)
  const list = getMutedList()
  const idx = list.indexOf(key)
  if (idx === -1) { list.push(key) } else { list.splice(idx, 1) }
  localStorage.setItem('mutedContacts', JSON.stringify(list))
  contextMenu.value.visible = false
}

const handleDeleteConversation = async () => {
  const c = contextMenu.value.contact
  if (!c) return
  contextMenu.value.visible = false
  if (!confirm(`确定删除和 ${c.contactName} 的全部对话记录吗？`)) return
  try {
    const params = {}
    if (c.productId) params.productId = c.productId
    await chatAPI.deleteConversation(c.contactId, params)
    contacts.value = contacts.value.filter(item =>
      !(Number(item.contactId) === Number(c.contactId) && sameProduct(item.productId, c.productId))
    )
    if (isActiveContact(c)) {
      activeContact.value = null
      activeContactName.value = ''
      activeProductId.value = null
      activeProductTitle.value = ''
      messages.value = []
    }
  } catch (e) {
    alert(e?.message || '删除失败')
  }
}

const fetchProductInfo = async (productId) => {
  if (!productId) { productInfo.value = null; return }
  try {
    const res = await productAPI.getDetail(productId)
    productInfo.value = res.data
  } catch {
    productInfo.value = null
  }
}

const goToProduct = () => {
  if (activeProductId.value) router.push(`/product/${activeProductId.value}`)
}

const handleProductAction = async () => {
  if (!activeProductId.value) return
  // 和商品详情页「立即购买」一致：创建订单 → 跳转订单页
  try {
    const res = await orderAPI.create({ productId: activeProductId.value })
    router.push(`/order/${res.data.id}`)
  } catch (e) {
    alert(e?.message || '下单失败')
  }
}

const handleSwapProduct = () => {
  if (activeProductId.value) router.push(`/swap/propose/${activeProductId.value}`)
}

const goToUserProfile = async () => {
  const username = userSearchKeyword.value.trim()
  if (!username) return
  searchingUser.value = true
  try {
    const res = await userAPI.searchByUsername(username)
    focusSearchOnReturn.value = true
    sessionStorage.setItem('chatFocusSearchOnReturn', '1')
    router.push(`/user/${res.data.id}`)
  } catch (e) {
    alert(e?.message || '未找到该用户')
  } finally {
    searchingUser.value = false
  }
}

const openChat = async (contactId, productId, fallbackName = '') => {
  activeContact.value = contactId
  activeProductId.value = productId || null
  const c = contacts.value.find(item => Number(item.contactId) === Number(contactId) && sameProduct(item.productId, productId))
  activeContactName.value = c?.contactName || fallbackName
  activeProductTitle.value = c?.productTitle || ''
  // 立刻清零红点，不需要等 API 返回
  if (c && c.unreadCount > 0) {
    c.unreadCount = 0
    const params = {}; if (productId) params.productId = productId
    chatAPI.markRead(contactId, params).catch(() => {})
    window.dispatchEvent(new CustomEvent('chat-unread-refresh'))
  }
  await fetchMessages()
  fetchProductInfo(activeProductId.value)
}

const sendMessage = async () => {
  if (sending.value || !activeContact.value) return
  if (!newMsg.value.trim() && !pendingFile.value) return
  sending.value = true
  let shouldRefocus = false
  try {
    let attachmentUrl = ''
    let fileType = ''
    if (pendingFile.value) {
      const fd = new FormData()
      fd.append('file', pendingFile.value)
      fd.append('type', pendingType.value)
      fileType = pendingType.value
      const up = await chatAPI.uploadImage(fd)
      attachmentUrl = up.data?.url || ''
      pendingPreview.value = ''
      pendingFile.value = null
      pendingType.value = ''
    }
    const labels = { IMAGE: '[图片]', VIDEO: '[视频]' }
    const content = attachmentUrl ? (labels[fileType] || '[文件]') : newMsg.value.trim()
    if (!content) { sending.value = false; return }
    const res = await chatAPI.send({
      receiverId: activeContact.value,
      productId: activeProductId.value,
      content,
      messageType: attachmentUrl ? (fileType || 'IMAGE') : 'TEXT',
      attachmentUrl: attachmentUrl || undefined
    })
    if (res.data) messages.value.push(res.data)
    newMsg.value = ''
    shouldRefocus = true
    await fetchContacts()
    await scrollToBottomAfterRender()
  } catch (e) {
    alert(e?.message || '发送失败')
  } finally {
    sending.value = false
    if (shouldRefocus) {
      await nextTick()
      msgInput.value?.focus()
    }
  }
}

const onFilePicked = (e) => {
  const f = e.target.files[0]
  if (!f) return
  const isVideo = f.type.startsWith('video/')
  const max = isVideo ? 30 * 1024 * 1024 : 5 * 1024 * 1024
  if (f.size > max) { alert('文件不超过 ' + (max/1024/1024) + 'MB'); return }
  pendingFile.value = f
  pendingType.value = isVideo ? 'VIDEO' : 'IMAGE'
  pendingPreview.value = URL.createObjectURL(f)
}

const scrollToBottom = () => {
  if (!msgList.value) return
  const el = msgList.value
  el.scrollTop = el.scrollHeight
  // 备用：用 scrollIntoView 确保滚到底部
  const children = el.children
  if (children.length > 0) {
    children[children.length - 1].scrollIntoView({ block: 'end' })
  }
}

const scrollToBottomAfterRender = async () => {
  await nextTick()
  scrollToBottom()
  ;[0, 50, 150, 300].forEach(delay => {
    setTimeout(scrollToBottom, delay)
  })
}

const focusUserSearchInput = async () => {
  const shouldFocus = focusSearchOnReturn.value || sessionStorage.getItem('chatFocusSearchOnReturn') === '1'
  if (!shouldFocus) return
  focusSearchOnReturn.value = false
  sessionStorage.removeItem('chatFocusSearchOnReturn')
  await nextTick()
  userSearchInput.value?.focus()
  const valueLength = userSearchInput.value?.value?.length || 0
  userSearchInput.value?.setSelectionRange?.(valueLength, valueLength)
}

const getCardImage = (msg) => {
  const au = msg.attachmentUrl || ''
  return au.split('|pid:')[0]
}

const goToCardProduct = (msg) => {
  const pid = (msg.attachmentUrl || '').split('|pid:')[1] || msg.productId
  if (pid && Number(pid) > 0) router.push('/product/' + Number(pid))
}

// ========= 订单卡片 =========
const parseOrderCard = (msg) => {
  try {
    const data = JSON.parse(msg.content)
    data.text = msg.content // fallback
    // If content is JSON with order data, extract display text separately
    if (typeof data === 'object' && data.orderId) {
      // The display text is determined by order status
      const statusTexts = {
        PENDING_PAY: '买家已拍下，等待付款',
        PAID: '买家已付款，等待发货',
        SHIPPED: '卖家已发货',
        COMPLETED: '交易已完成',
        CANCELLED: '订单已取消',
        DISPUTE: '订单纠纷中'
      }
      data.text = statusTexts[data.orderStatus] || '订单状态更新'
    }
    return data
  } catch (e) {
    return { text: msg.content }
  }
}

const statusLabel = (s) => {
  const map = { PENDING_PAY: '待付款', PAID: '已付款', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消', DISPUTE: '纠纷中' }
  return map[s] || s
}

const getOrderCardActions = (msg) => {
  const card = parseOrderCard(msg)
  if (!card.orderId) return []

  const isBuyer = Number(card.buyerId) === Number(myId)
  const isSeller = Number(card.sellerId) === Number(myId)
  const status = card.orderStatus
  const hasRefund = !!card.refundReason
  const isDispute = status === 'DISPUTE'
  const actions = []

  const btn = (label, key, cls, handler) => ({ label, key, cls, handler })

  const isSwap = card.orderType === 'SWAP'

  // 交换：待确认 → 卖家可同意/拒绝
  if (isSwap && status === 'PENDING_CONFIRM' && isSeller) {
    actions.push(btn('同意交换', 'agreeSwap', 'card-btn-primary', () => handleSwapAction(card.orderId, 'agreeSwap')))
    actions.push(btn('拒绝交换', 'rejectSwap', 'card-btn-danger', () => handleSwapAction(card.orderId, 'rejectSwap')))
    actions.push(btn('查看订单', 'view', 'card-btn-default', () => router.push(`/swap/${card.orderId}`)))
    return actions
  }

  // 交换：已确认 → 双方都可以发货
  if (isSwap && status === 'CONFIRMED' && (isBuyer || isSeller)) {
    actions.push(btn('立即发货', 'ship', 'card-btn-primary', () => showShipInput(card.orderId)))
  }

  // 交换：双方已发货 → 都可以收货
  if (isSwap && status === 'BOTH_SHIPPED' && (isBuyer || isSeller)) {
    actions.push(btn('确认收货', 'receive', 'card-btn-primary', () => handleSwapAction(card.orderId, 'receive')))
  }

  // 交换：完成 → 双方可评价
  if (isSwap && status === 'COMPLETED') {
    if (ratedOrders.value.has(card.orderId)) {
      actions.push(btn('已评价', 'rated', 'card-btn-disabled', null))
    } else {
      actions.push(btn('去评价', 'rate', 'card-btn-primary', () => openRateModal(card.orderId, isBuyer ? card.sellerId : card.buyerId)))
    }
  }

  // 以下为非交换订单逻辑
  if (hasRefund && !isDispute && isSeller && status !== 'CANCELLED' && status !== 'COMPLETED') {
    actions.push(btn('同意退款', 'agreeRefund', 'card-btn-primary', () => handleRefundAction(card.orderId, 'agreeRefund')))
    actions.push(btn('拒绝退款', 'rejectRefund', 'card-btn-danger', () => handleRefundAction(card.orderId, 'rejectRefund')))
    actions.push(btn('查看订单', 'view', 'card-btn-default', () => router.push(`/order/${card.orderId}`)))
    return actions
  }

  // 退款申请中：买家可以取消退款或申请平台介入
  if (hasRefund && !isDispute && isBuyer && status !== 'CANCELLED' && status !== 'COMPLETED') {
    actions.push(btn('取消退款', 'cancelRefund', 'card-btn-default', () => handleRefundAction(card.orderId, 'cancelRefund')))
    actions.push(btn('申请平台介入', 'escalate', 'card-btn-danger', () => handleEscalateAction(card.orderId)))
  }

  // 纠纷中
  if (isDispute) {
    actions.push(btn('查看订单', 'view', 'card-btn-default', () => router.push(`/order/${card.orderId}`)))
    return actions
  }

  if (status === 'PENDING_PAY' && isBuyer) {
    actions.push(btn('立即付款', 'pay', 'card-btn-primary', () => handleOrderAction(card.orderId, 'pay')))
  }
  if (status === 'PAID' && isSeller) {
    actions.push(btn('立即发货', 'ship', 'card-btn-primary', () => showShipInput(card.orderId)))
  }
  if (status === 'SHIPPED' && isBuyer) {
    actions.push(btn('确认收货', 'receive', 'card-btn-primary', () => handleOrderAction(card.orderId, 'receive')))
  }
  if (status === 'COMPLETED' && isBuyer) {
    if (ratedOrders.value.has(card.orderId)) {
      actions.push(btn('已评价', 'rated', 'card-btn-disabled', null))
    } else {
      actions.push(btn('去评价', 'rate', 'card-btn-primary', () => openRateModal(card.orderId, card.sellerId)))
    }
  }
  actions.push(btn('查看订单', 'view', 'card-btn-default', () => router.push(`/order/${card.orderId}`)))

  return actions
}

const handleRefundAction = async (orderId, action) => {
  try {
    if (action === 'agreeRefund') await orderAPI.agreeRefund(orderId)
    else if (action === 'rejectRefund') await orderAPI.rejectRefund(orderId)
    else if (action === 'cancelRefund') await orderAPI.cancelRefund(orderId)
    alert('操作成功')
    fetchMessages()
    fetchContacts()
  } catch (e) { alert(e?.message || '操作失败') }
}

const handleEscalateAction = (orderId) => {
  const reason = prompt('请描述申请平台介入的原因：')
  if (!reason) return
  orderAPI.escalate(orderId, { reason }).then(() => {
    alert('已提交平台介入申请')
    fetchMessages()
    fetchContacts()
  }).catch(e => alert(e?.message || '操作失败'))
}

const handleOrderAction = async (orderId, action) => {
  try {
    if (action === 'pay') {
      await orderAPI.pay(orderId)
    } else if (action === 'receive') {
      await orderAPI.receive(orderId)
    }
    alert('操作成功')
    fetchMessages()
    fetchContacts()
  } catch (e) {
    alert(e?.message || '操作失败')
  }
}

const handleSwapAction = async (orderId, action) => {
  try {
    if (action === 'agreeSwap') await swapAPI.agree(orderId)
    else if (action === 'rejectSwap') await swapAPI.reject(orderId)
    else if (action === 'receive') await swapAPI.receive(orderId)
    alert('操作成功')
    fetchMessages()
    fetchContacts()
  } catch (e) { alert(e?.message || '操作失败') }
}

const showShipInput = (orderId) => {
  const info = prompt('请输入物流信息（快递公司+单号）：')
  if (!info) return
  orderAPI.ship(orderId, { logisticsInfo: info }).then(() => {
    alert('发货成功')
    fetchMessages()
    fetchContacts()
  }).catch(e => alert(e?.message || '操作失败'))
}

const openRateModal = (orderId, ratedUserId) => {
  ratingOrderId.value = orderId
  ratingRatedUserId.value = ratedUserId
  ratingScore.value = 5
  ratingComment.value = ''
  showRatingModal.value = true
}

const doRating = async () => {
  try {
    await ratingAPI.submit({ orderId: ratingOrderId.value, score: ratingScore.value, comment: ratingComment.value.trim() })
    showRatingModal.value = false
    ratedOrders.value.add(ratingOrderId.value)
    localStorage.setItem('ratedOrders', JSON.stringify([...ratedOrders.value]))
    alert('评价成功')
    fetchMessages()
    fetchContacts()
  } catch (e) { alert(e?.message || '评价失败') }
}

const searchProducts = async () => {
  const kw = pickerKeyword.value.trim()
  if (!kw) return
  pickerSearched.value = true
  try {
    // Try as product ID first
    if (/^\d+$/.test(kw)) {
      const res = await productAPI.getDetail(Number(kw))
      if (res.data) { pickerProducts.value = [res.data]; return }
    }
    // Keyword search
    const res = await productAPI.search({ keyword: kw, size: 10 })
    pickerProducts.value = (res.data?.content || []).map(p => ({
      ...p,
      firstImage: p.firstImage || (p.images?.split(',')[0] || '')
    }))
  } catch { pickerProducts.value = [] }
}

const sendProductCard = async (p) => {
  if (!activeContact.value) return
  try {
    await chatAPI.send({
      receiverId: activeContact.value,
      productId: activeProductId.value,
      content: p.title,
      messageType: 'PRODUCT_CARD',
      attachmentUrl: (p.firstImage || p.images?.split(',')[0] || '') + '|pid:' + p.id
    })
    showProductPicker.value = false
    pickerKeyword.value = ''
    pickerProducts.value = []
    pickerSearched.value = false
    await fetchMessages()
  } catch (e) { alert(e?.message || '发送失败') }
}

const reportMessage = (msg) => {
  const reason = prompt('举报原因：')
  if (!reason) return
  reportAPI.submit({ targetType: 'MESSAGE', targetId: msg.id, reason: 'OTHER', description: reason })
    .then(() => alert('举报已提交'))
    .catch(e => alert(e?.message || '提交失败'))
}

const formatTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  const now = new Date()
  if (date.toDateString() === now.toDateString()) return date.toTimeString().slice(0, 5)
  return `${date.getMonth() + 1}-${date.getDate()}`
}

const formatMessageTime = (value) => {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(5, 16)
}

onMounted(() => {
  fetchContacts()
  focusUserSearchInput()
})

onActivated(() => {
  routeChatOpened.value = false
  fetchContacts().then(async () => {
    window.dispatchEvent(new CustomEvent('chat-unread-refresh'))
    // 进入聊天页后，标记所有未读对话为已读
    for (const c of contacts.value) {
      if (c.unreadCount > 0) {
        const params = {}; if (c.productId) params.productId = c.productId
        chatAPI.markRead(c.contactId, params).catch(() => {})
      }
    }
    fetchContacts()
  })
  focusUserSearchInput()
})
</script>

<style scoped>
.chat-page { height: calc(100vh - 160px); overflow: hidden; }
.chat-layout { display: grid; grid-template-columns: 280px 1fr; height: 100%; gap: 16px; }
.contact-panel { background: var(--card-bg); border-radius: 8px; padding: 16px; overflow-y: auto; }
.contact-panel h3 { margin-bottom: 8px; font-size: 16px; }
.user-search { display: flex; gap: 6px; margin-bottom: 12px; }
.user-search input { flex: 1; min-width: 0; padding: 6px 8px; border: 1px solid var(--border); border-radius: 4px; font-size: 12px; }
.user-search button { padding: 4px 10px; background: var(--primary); color: #fff; border: none; border-radius: 4px; font-size: 12px; white-space: nowrap; }
.user-search button:disabled { background: #c5cfc0; }
.contact-item { display: flex; justify-content: space-between; gap: 8px; padding: 12px; border-radius: 6px; cursor: pointer; margin-bottom: 4px; }
.contact-item:hover, .contact-item.active { background: rgba(139,157,131,0.1); }
.contact-item.muted { opacity: 0.6; }
.contact-info { min-width: 0; }
.contact-info strong { display: block; font-size: 14px; }
.contact-product { display: block; max-width: 160px; margin-top: 3px; color: var(--primary); font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.muted-tag { display: inline-block; margin-left: 6px; padding: 0 4px; font-size: 10px; color: #faad14; border: 1px solid #faad14; border-radius: 2px; vertical-align: middle; }
.last-msg { font-size: 12px; color: var(--text-muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 160px; margin-top: 4px; }
.read-tag { font-size: 10px; color: var(--text-muted); margin-left: 4px; }
.read-tag.read { color: var(--success); }
.contact-meta { text-align: right; flex: 0 0 auto; }
.time { font-size: 11px; color: var(--text-muted); }
.badge { display: inline-block; background: var(--danger); color: #fff; font-size: 11px; padding: 1px 6px; border-radius: 10px; margin-top: 4px; }
.chat-panel { background: var(--card-bg); border-radius: 8px; display: flex; flex-direction: column; min-width: 0; overflow: hidden; }
.chat-header { padding: 12px 16px; border-bottom: 1px solid var(--bg); display: flex; gap: 8px; align-items: center; }
.contact-name-link { cursor: pointer; }
.contact-name-link:hover { text-decoration: underline; }
.product-tag { font-size: 12px; color: var(--primary); background: rgba(139,157,131,0.1); padding: 2px 8px; border-radius: 4px; }
.btn-refresh { margin-left: auto; background: none; border: 1px solid var(--border); padding: 2px 12px; border-radius: 4px; font-size: 12px; color: var(--text-secondary); }
.product-bar { display: flex; align-items: center; gap: 12px; padding: 10px 16px; border-bottom: 1px solid var(--bg); cursor: pointer; transition: background 0.2s; }
.product-bar:hover { background: var(--bg); }
.product-bar-img { width: 48px; height: 48px; border-radius: 4px; object-fit: cover; flex-shrink: 0; }
.product-bar-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.product-bar-title { font-size: 14px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-bar-price { font-size: 16px; color: var(--danger); font-weight: bold; }
.product-bar-btn { flex-shrink: 0; padding: 6px 16px; background: var(--primary); color: #fff; border: none; border-radius: 4px; font-size: 13px; cursor: pointer; }
.product-bar-btn:hover { background: var(--primary-hover); }
.product-bar-btn.swap { background: #52c41a; }
.product-bar-btn.swap:hover { background: #73d13d; }
.message-list { flex: 1; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.message { max-width: 70%; align-self: flex-start; }
.message.mine { align-self: flex-end; }
.msg-content { padding: 10px 14px; border-radius: 12px; font-size: 14px; word-break: break-word; }
.message:not(.mine) .msg-content { background: var(--bg); }
.message.mine .msg-content { background: var(--primary); color: #fff; }
.msg-meta { display: flex; gap: 8px; align-items: center; margin-top: 4px; font-size: 11px; color: var(--text-muted); }
.msg-status { font-size: 11px; color: var(--text-muted); }
.msg-status.read { color: var(--success); }
.btn-report-msg { background: none; border: none; color: var(--text-muted); font-size: 11px; cursor: pointer; padding: 0; }
.btn-report-msg:hover { color: var(--danger); }
.chat-input { display: flex; gap: 12px; padding: 12px 16px; border-top: 1px solid var(--bg); }
.chat-input input { flex: 1; min-width: 0; padding: 8px 12px; border: 1px solid var(--border); border-radius: 4px; font-size: 14px; }
.chat-input button { padding: 8px 20px; background: var(--primary); color: #fff; border: none; border-radius: 4px; }
.chat-input button:disabled { background: #c5cfc0; }
.btn-attach { background: var(--bg) !important; color: var(--text) !important; padding: 8px 12px !important; }
.img-preview { padding: 8px 16px; border-top: 1px solid var(--bg); display:flex; align-items:center; gap:8px; }
.img-preview img { max-height: 100px; border-radius: 4px; }
.img-preview button { background: rgba(0,0,0,0.5); color: #fff; border: none; border-radius: 50%; width: 20px; height: 20px; font-size: 12px; cursor: pointer; }
.msg-img { max-width: 200px; max-height: 260px; border-radius: 8px; cursor: pointer; }
.msg-video { max-width: 280px; max-height: 320px; border-radius: 8px; }
.video-preview { max-height: 150px; border-radius: 4px; }
.msg-card { background: var(--card-bg); border: 1px solid var(--border); border-radius: 8px; padding: 10px 14px; cursor: pointer; min-width: 180px; }
.msg-card:hover { border-color: var(--primary); }
.card-label { display: block; font-size: 11px; color: var(--text-muted); margin-bottom: 4px; }
.card-row { display: flex; align-items: center; gap: 8px; }
.card-thumb { width: 48px; height: 48px; border-radius: 4px; object-fit: cover; }
.order-card { cursor: default; min-width: 220px; }
.order-card:hover { border-color: var(--border); }
.card-info { display: flex; flex-direction: column; gap: 2px; }
.card-price { color: var(--danger); font-size: 16px; font-weight: bold; }
.card-status { font-size: 11px; color: var(--primary); }
.card-actions { display: flex; gap: 8px; margin-top: 10px; flex-wrap: wrap; }
.card-actions button { padding: 5px 14px; border-radius: 4px; font-size: 12px; cursor: pointer; border: 1px solid var(--border); background: var(--card-bg); }
.card-btn-primary { background: var(--primary) !important; color: #fff !important; border-color: var(--primary) !important; }
.card-btn-danger { background: var(--card-bg) !important; color: var(--danger) !important; border-color: var(--danger) !important; }
.card-btn-disabled { background: var(--bg) !important; color: var(--text-muted) !important; border-color: var(--border) !important; cursor: not-allowed !important; }
.card-btn-default { color: var(--text); }
.system-msg { display: flex; justify-content: center; margin: 16px 0; }
.system-msg-content { background: rgba(201,169,110,0.1); border: 1px solid rgba(201,169,110,0.3); border-radius: 8px; padding: 10px 20px; text-align: center; max-width: 350px; }
.system-msg-icon { font-size: 20px; display: block; margin-bottom: 4px; }
.system-msg-content p { margin: 0; font-size: 13px; color: #ad6800; }
.system-msg .card-actions { justify-content: center; margin-top: 8px; }
.picker-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 1000; display:flex; align-items:center; justify-content:center; }
.picker-card { background: var(--card-bg); border-radius: 8px; padding: 20px; width: 420px; max-height: 70vh; display:flex; flex-direction:column; gap:12px; }
.picker-card h4 { margin: 0; }
.picker-card input { flex:1; padding: 8px 12px; border: 1px solid var(--border); border-radius: 4px; font-size: 14px; }
.btn-search { padding: 8px 16px; background: var(--primary); color: #fff; border: none; border-radius: 4px; }
.picker-list { overflow-y: auto; max-height: 300px; display:flex; flex-direction:column; gap:8px; }
.picker-item { display:flex; align-items:center; gap:10px; padding: 8px; border: 1px solid var(--bg); border-radius: 6px; cursor:pointer; }
.picker-item:hover { background: var(--bg); }
.picker-thumb { width: 40px; height: 40px; border-radius: 4px; object-fit: cover; background: var(--bg); }
.picker-info { display:flex; flex-direction:column; gap:2px; }
.picker-info strong { font-size: 14px; }
.picker-info span { font-size: 13px; color: var(--danger); }
.btn-cancel { padding: 8px 16px; border: 1px solid var(--border); background: var(--card-bg); border-radius: 4px; }
.context-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; z-index: 999; }
.context-menu { position: fixed; z-index: 1000; background: var(--card-bg); border: 1px solid var(--border); border-radius: 6px; box-shadow: 0 4px 12px rgba(0,0,0,0.12); padding: 4px 0; min-width: 120px; }
.context-item { padding: 8px 16px; font-size: 13px; cursor: pointer; }
.context-item:hover { background: var(--bg); }
.context-item.danger { color: var(--danger); }
.context-item.danger:hover { background: rgba(194,120,120,0.1); }
@media (max-width: 768px) {
  .chat-layout { grid-template-columns: 1fr; }
  .contact-panel { max-height: 220px; }
}
</style>
