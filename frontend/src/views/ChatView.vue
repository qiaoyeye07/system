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
            :class="{ active: isActiveContact(c) }"
            @click="openChat(c.contactId, c.productId)"
          >
            <div class="contact-info">
              <strong>{{ c.contactName }}</strong>
              <span v-if="c.productTitle" class="contact-product">{{ c.productTitle }}</span>
              <p class="last-msg">{{ c.lastMessage }}</p>
            </div>
            <div class="contact-meta">
              <span class="time">{{ formatTime(c.lastMessageTime) }}</span>
              <span v-if="c.unreadCount" class="badge">{{ c.unreadCount }}</span>
            </div>
          </div>
        </div>
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

          <div class="message-list" ref="msgList">
            <LoadingState v-if="loadingMessages" />
            <div v-for="msg in messages" :key="msg.id" class="message" :class="{ mine: Number(msg.senderId) === Number(myId) }">
              <div class="msg-content">{{ msg.content }}</div>
              <div class="msg-meta">
                <span class="msg-time">{{ formatMessageTime(msg.createdAt) }}</span>
                <span v-if="Number(msg.senderId) === Number(myId)" class="msg-status" :class="{ read: msg.isRead }">{{ msg.isRead ? '已读' : '未读' }}</span>
                <button v-if="Number(msg.senderId) !== Number(myId)" class="btn-report-msg" @click="reportMessage(msg)">举报</button>
              </div>
            </div>
          </div>

          <div class="chat-input">
            <input v-model="newMsg" type="text" placeholder="输入消息..." :disabled="sending" @keyup.enter="sendMessage" />
            <button @click="sendMessage" :disabled="sending || !newMsg.trim()">{{ sending ? '发送中...' : '发送' }}</button>
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { chatAPI, reportAPI } from '../api/modules.js'
import LoadingState from '../components/common/LoadingState.vue'
import EmptyState from '../components/common/EmptyState.vue'

const route = useRoute()
const contacts = ref([])
const messages = ref([])
const activeContact = ref(null)
const activeContactName = ref('')
const activeProductId = ref(null)
const activeProductTitle = ref('')
const newMsg = ref('')
const loadingContacts = ref(false)
const loadingMessages = ref(false)
const sending = ref(false)
const msgList = ref(null)

const newContactId = ref('')
const user = JSON.parse(localStorage.getItem('user') || 'null')
const myId = user?.id

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
    if (contactId && contacts.value.length > 0) {
      const productId = route.query.productId ? Number(route.query.productId) : null
      const c = contacts.value.find(item => Number(item.contactId) === Number(contactId) && sameProduct(item.productId, productId))
      if (c) openChat(c.contactId, c.productId)
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

const startNewChat = () => {
  const id = parseInt(newContactId.value)
  if (!id || id === myId) return
  openChat(id, null, `用户${id}`)
  newContactId.value = ''
}

const openChat = (contactId, productId, fallbackName = '') => {
  activeContact.value = contactId
  activeProductId.value = productId || null
  const c = contacts.value.find(item => Number(item.contactId) === Number(contactId) && sameProduct(item.productId, productId))
  activeContactName.value = c?.contactName || fallbackName
  activeProductTitle.value = c?.productTitle || ''
  fetchMessages()
}

const sendMessage = async () => {
  if (sending.value || !newMsg.value.trim() || !activeContact.value) return
  sending.value = true
  try {
    const res = await chatAPI.send({ receiverId: activeContact.value, productId: activeProductId.value, content: newMsg.value.trim() })
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
  }, 5000)
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
.contact-info { min-width: 0; }
.contact-info strong { display: block; font-size: 14px; }
.contact-product { display: block; max-width: 160px; margin-top: 3px; color: #1890ff; font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.last-msg { font-size: 12px; color: #999; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 160px; margin-top: 4px; }
.contact-meta { text-align: right; flex: 0 0 auto; }
.time { font-size: 11px; color: #999; }
.badge { display: inline-block; background: #ff4d4f; color: #fff; font-size: 11px; padding: 1px 6px; border-radius: 10px; margin-top: 4px; }
.chat-panel { background: #fff; border-radius: 8px; display: flex; flex-direction: column; min-width: 0; }
.chat-header { padding: 12px 16px; border-bottom: 1px solid #f0f0f0; display: flex; gap: 8px; align-items: center; }
.product-tag { font-size: 12px; color: #1890ff; background: #e6f7ff; padding: 2px 8px; border-radius: 4px; }
.btn-refresh { margin-left: auto; background: none; border: 1px solid #d9d9d9; padding: 2px 12px; border-radius: 4px; font-size: 12px; color: #666; }
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
@media (max-width: 768px) {
  .chat-layout { grid-template-columns: 1fr; }
  .contact-panel { max-height: 220px; }
}
</style>
