<template>
  <div class="chat-page">
    <div class="chat-layout">
      <aside class="contact-panel">
        <h3>联系人</h3>
        <div class="new-chat">
          <input v-model="newContactId" type="number" placeholder="输入用户ID发起对话" @keyup.enter="startNewChat" />
          <button @click="startNewChat" :disabled="!newContactId">发起</button>
        </div>
        <LoadingState v-if="loadingContacts" />
        <EmptyState v-else-if="contacts.length === 0" text="暂无聊天记录" />
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
            <strong>{{ activeContactName }}</strong>
            <span v-if="activeProductTitle" class="product-tag">{{ activeProductTitle }}</span>
            <button class="btn-refresh" @click="fetchMessages" :disabled="loadingMessages">刷新</button>
          </div>

          <!-- 商品信息条 -->
          <div v-if="productInfo && activeProductId" class="product-bar" @click="goToProduct">
            <img v-if="productInfo.images" :src="'/' + productInfo.images.split(',')[0]" class="product-bar-img" />
            <div class="product-bar-info">
              <span class="product-bar-title">{{ productInfo.title }}</span>
              <span class="product-bar-price">¥{{ productInfo.price }}</span>
            </div>
            <button v-if="!isProductOwner" class="product-bar-btn" @click.stop="handleProductAction">立即购买</button>
          </div>

          <div class="message-list" ref="msgList">
            <LoadingState v-if="loadingMessages" />
            <div v-for="msg in messages" :key="msg.id" class="message" :class="{ mine: Number(msg.senderId) === Number(myId) }">
              <div class="msg-content">
                <img v-if="msg.messageType === 'IMAGE'" :src="msg.attachmentUrl" class="msg-img" @click="window.open(msg.attachmentUrl)" />
                <template v-else>{{ msg.content }}</template>
              </div>
              <div class="msg-meta">
                <span class="msg-time">{{ formatMessageTime(msg.createdAt) }}</span>
                <span v-if="Number(msg.senderId) === Number(myId)" class="msg-status" :class="{ read: msg.isRead }">{{ msg.isRead ? '已读' : '未读' }}</span>
                <button v-if="Number(msg.senderId) !== Number(myId)" class="btn-report-msg" @click="reportMessage(msg)">举报</button>
              </div>
            </div>
          </div>

          <div class="chat-input">
            <input v-model="newMsg" type="text" placeholder="输入消息..." :disabled="sending" @keyup.enter="sendMessage" />
            <input type="file" ref="fileInput" accept="image/*" style="display:none" @change="onFileSelected" />
            <button class="btn-attach" :disabled="sending" @click="$refs.fileInput.click()">🖼</button>
            <button @click="sendMessage" :disabled="sending || (!newMsg.trim() && !pendingFile)">{{ sending ? '发送中...' : '发送' }}</button>
          </div>
          <div v-if="pendingPreview" class="img-preview">
            <img :src="pendingPreview" />
            <button @click="pendingPreview='';pendingFile=null">✕</button>
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { chatAPI, reportAPI, productAPI, orderAPI } from '../api/modules.js'
import LoadingState from '../components/common/LoadingState.vue'
import EmptyState from '../components/common/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const contacts = ref([])
const productInfo = ref(null)
const isProductOwner = computed(() => productInfo.value && Number(productInfo.value.sellerId) === Number(myId))
const messages = ref([])
const activeContact = ref(null)
const activeContactName = ref('')
const activeProductId = ref(null)
const activeProductTitle = ref('')
const newMsg = ref('')
const loadingContacts = ref(false)
const loadingMessages = ref(false)
const sending = ref(false)
const pendingPreview = ref('')
const pendingFile = ref(null)
const msgList = ref(null)

const newContactId = ref('')
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
    if (contactId) {
      const targetId = Number(contactId)
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
      openChat(c.contactId, c.productId)
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
    nextTick(scrollToBottom)
  } catch (e) {
    messages.value = []
  } finally {
    loadingMessages.value = false
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

const startNewChat = () => {
  const id = parseInt(newContactId.value)
  if (!id || id === myId) return
  openChat(id, null, `用户${id}`)
  newContactId.value = ''
}

const openChat = async (contactId, productId, fallbackName = '') => {
  activeContact.value = contactId
  activeProductId.value = productId || null
  const c = contacts.value.find(item => Number(item.contactId) === Number(contactId) && sameProduct(item.productId, productId))
  activeContactName.value = c?.contactName || fallbackName
  activeProductTitle.value = c?.productTitle || ''
  await fetchMessages()
  fetchProductInfo(activeProductId.value)
}

const sendMessage = async () => {
  if (sending.value || !activeContact.value) return
  if (!newMsg.value.trim() && !pendingFile.value) return
  sending.value = true
  try {
    let attachmentUrl = ''
    if (pendingFile.value) {
      const fd = new FormData()
      fd.append('file', pendingFile.value)
      const up = await chatAPI.uploadImage(fd)
      attachmentUrl = up.data?.url || ''
      pendingPreview.value = ''
      pendingFile.value = null
    }
    const content = attachmentUrl ? '[图片]' : newMsg.value.trim()
    if (!content) { sending.value = false; return }
    const res = await chatAPI.send({
      receiverId: activeContact.value,
      productId: activeProductId.value,
      content,
      messageType: attachmentUrl ? 'IMAGE' : 'TEXT',
      attachmentUrl: attachmentUrl || undefined
    })
    if (res.data) messages.value.push(res.data)
    newMsg.value = ''
    nextTick(scrollToBottom)
    await fetchContacts()
  } catch (e) {
    alert(e?.message || '发送失败')
  } finally {
    sending.value = false
  }
}

const onFileSelected = (e) => {
  const f = e.target.files[0]
  if (!f) return
  if (f.size > 5*1024*1024) { alert('图片不超过 5MB'); return }
  pendingFile.value = f
  pendingPreview.value = URL.createObjectURL(f)
  e.target.value = ''
}

const scrollToBottom = () => {
  if (msgList.value) msgList.value.scrollTop = msgList.value.scrollHeight
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

let pollTimer = null

onMounted(() => {
  fetchContacts()
  pollTimer = setInterval(() => {
    if (activeContact.value) fetchMessages()
    fetchContacts()
  }, 300000)
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.chat-page { height: calc(100vh - 160px); }
.chat-layout { display: grid; grid-template-columns: 280px 1fr; height: 100%; gap: 16px; }
.contact-panel { background: #fff; border-radius: 8px; padding: 16px; overflow-y: auto; }
.contact-panel h3 { margin-bottom: 8px; font-size: 16px; }
.new-chat { display: flex; gap: 6px; margin-bottom: 12px; }
.new-chat input { flex: 1; padding: 6px 8px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 12px; }
.new-chat button { padding: 4px 10px; background: #1890ff; color: #fff; border: none; border-radius: 4px; font-size: 12px; white-space: nowrap; }
.new-chat button:disabled { background: #91d5ff; }
.contact-item { display: flex; justify-content: space-between; gap: 8px; padding: 12px; border-radius: 6px; cursor: pointer; margin-bottom: 4px; }
.contact-item:hover, .contact-item.active { background: #e6f7ff; }
.contact-item.muted { opacity: 0.6; }
.contact-info { min-width: 0; }
.contact-info strong { display: block; font-size: 14px; }
.contact-product { display: block; max-width: 160px; margin-top: 3px; color: #1890ff; font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.muted-tag { display: inline-block; margin-left: 6px; padding: 0 4px; font-size: 10px; color: #faad14; border: 1px solid #faad14; border-radius: 2px; vertical-align: middle; }
.last-msg { font-size: 12px; color: #999; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 160px; margin-top: 4px; }
.read-tag { font-size: 10px; color: #bbb; margin-left: 4px; }
.read-tag.read { color: #52c41a; }
.contact-meta { text-align: right; flex: 0 0 auto; }
.time { font-size: 11px; color: #999; }
.badge { display: inline-block; background: #ff4d4f; color: #fff; font-size: 11px; padding: 1px 6px; border-radius: 10px; margin-top: 4px; }
.chat-panel { background: #fff; border-radius: 8px; display: flex; flex-direction: column; min-width: 0; }
.chat-header { padding: 12px 16px; border-bottom: 1px solid #f0f0f0; display: flex; gap: 8px; align-items: center; }
.product-tag { font-size: 12px; color: #1890ff; background: #e6f7ff; padding: 2px 8px; border-radius: 4px; }
.btn-refresh { margin-left: auto; background: none; border: 1px solid #d9d9d9; padding: 2px 12px; border-radius: 4px; font-size: 12px; color: #666; }
.product-bar { display: flex; align-items: center; gap: 12px; padding: 10px 16px; border-bottom: 1px solid #f0f0f0; cursor: pointer; transition: background 0.2s; }
.product-bar:hover { background: #fafafa; }
.product-bar-img { width: 48px; height: 48px; border-radius: 4px; object-fit: cover; flex-shrink: 0; }
.product-bar-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.product-bar-title { font-size: 14px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-bar-price { font-size: 16px; color: #ff4d4f; font-weight: bold; }
.product-bar-btn { flex-shrink: 0; padding: 6px 16px; background: #1890ff; color: #fff; border: none; border-radius: 4px; font-size: 13px; cursor: pointer; }
.product-bar-btn:hover { background: #40a9ff; }
.message-list { flex: 1; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.message { max-width: 70%; align-self: flex-start; }
.message.mine { align-self: flex-end; }
.msg-content { padding: 10px 14px; border-radius: 12px; font-size: 14px; word-break: break-word; }
.message:not(.mine) .msg-content { background: #f0f0f0; }
.message.mine .msg-content { background: #1890ff; color: #fff; }
.msg-meta { display: flex; gap: 8px; align-items: center; margin-top: 4px; font-size: 11px; color: #999; }
.msg-status { font-size: 11px; color: #bbb; }
.msg-status.read { color: #52c41a; }
.btn-report-msg { background: none; border: none; color: #999; font-size: 11px; cursor: pointer; padding: 0; }
.btn-report-msg:hover { color: #ff4d4f; }
.chat-input { display: flex; gap: 12px; padding: 12px 16px; border-top: 1px solid #f0f0f0; }
.chat-input input { flex: 1; min-width: 0; padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; }
.chat-input button { padding: 8px 20px; background: #1890ff; color: #fff; border: none; border-radius: 4px; }
.chat-input button:disabled { background: #91d5ff; }
.btn-attach { background: #f0f0f0 !important; color: #333 !important; padding: 8px 12px !important; }
.img-preview { padding: 8px 16px; border-top: 1px solid #f0f0f0; display:flex; align-items:center; gap:8px; }
.img-preview img { max-height: 100px; border-radius: 4px; }
.img-preview button { background: rgba(0,0,0,0.5); color: #fff; border: none; border-radius: 50%; width: 20px; height: 20px; font-size: 12px; cursor: pointer; }
.msg-img { max-width: 200px; max-height: 260px; border-radius: 8px; cursor: pointer; }
.context-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; z-index: 999; }
.context-menu { position: fixed; z-index: 1000; background: #fff; border: 1px solid #e8e8e8; border-radius: 6px; box-shadow: 0 4px 12px rgba(0,0,0,0.12); padding: 4px 0; min-width: 120px; }
.context-item { padding: 8px 16px; font-size: 13px; cursor: pointer; }
.context-item:hover { background: #f5f5f5; }
.context-item.danger { color: #ff4d4f; }
.context-item.danger:hover { background: #fff2f0; }
@media (max-width: 768px) {
  .chat-layout { grid-template-columns: 1fr; }
  .contact-panel { max-height: 220px; }
}
</style>
