<template>
  <div class="chat-page">
    <div class="chat-layout">
      <div class="contact-panel">
        <h3>联系人</h3>
        <LoadingState v-if="loadingContacts" />
        <EmptyState v-else-if="contacts.length === 0" text="暂无聊天记录" />
        <div v-else class="contact-list">
          <div v-for="c in contacts" :key="c.contactId" class="contact-item"
            :class="{ active: activeContact === c.contactId }"
            @click="openChat(c.contactId, c.productId)">
            <div class="contact-info">
              <strong>{{ c.contactName }}</strong>
              <p class="last-msg">{{ c.lastMessage }}</p>
            </div>
            <div class="contact-meta">
              <span class="time">{{ formatTime(c.lastMessageTime) }}</span>
              <span v-if="c.unreadCount" class="badge">{{ c.unreadCount }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="chat-panel">
        <template v-if="!activeContact">
          <EmptyState text="选择联系人开始聊天" />
        </template>
        <template v-else>
          <div class="chat-header">
            <strong>{{ activeContactName }}</strong>
            <span v-if="activeProductTitle" class="product-tag">{{ activeProductTitle }}</span>
          </div>
          <div class="message-list" ref="msgList">
            <LoadingState v-if="loadingMessages" />
            <div v-for="msg in messages" :key="msg.id" class="message" :class="{ mine: msg.senderId === myId }">
              <div class="msg-content">{{ msg.content }}</div>
              <div class="msg-meta">
                <span class="msg-time">{{ msg.createdAt?.slice(11, 16) }}</span>
                <button v-if="msg.senderId !== myId" class="btn-report-msg" @click="reportMessage(msg)">举报</button>
              </div>
            </div>
          </div>
          <div class="chat-input">
            <input v-model="newMsg" type="text" placeholder="输入消息..." @keyup.enter="sendMessage" />
            <button @click="sendMessage" :disabled="!newMsg.trim()">发送</button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { chatAPI, reportAPI } from '../api/modules.js'
import LoadingState from '../components/common/LoadingState.vue'
import EmptyState from '../components/common/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const contacts = ref([])
const messages = ref([])
const activeContact = ref(null)
const activeContactName = ref('')
const activeProductId = ref(null)
const activeProductTitle = ref('')
const newMsg = ref('')
const loadingContacts = ref(false)
const loadingMessages = ref(false)
const msgList = ref(null)

const userStr = localStorage.getItem('user')
const user = userStr ? JSON.parse(userStr) : null
const myId = user?.id

let stompClient = null

const fetchContacts = async () => {
  loadingContacts.value = true
  try {
    const res = await chatAPI.getContacts()
    contacts.value = res.data || []
    // Auto-open contact from route
    const contactId = route.query.contactId || route.params.contactId
    if (contactId) {
      const c = contacts.value.find(c => c.contactId == contactId)
      if (c) openChat(c.contactId, c.productId)
    }
  } catch (e) {} finally { loadingContacts.value = false }
}

const fetchMessages = async () => {
  if (!activeContact.value) return
  loadingMessages.value = true
  try {
    const params = {}
    if (activeProductId.value) params.productId = activeProductId.value
    const res = await chatAPI.getMessages(activeContact.value, params)
    messages.value = (res.data?.content || []).reverse()
    scrollToBottom()
  } catch (e) {} finally { loadingMessages.value = false }
}

const openChat = (contactId, productId) => {
  activeContact.value = contactId
  activeProductId.value = productId || null
  const c = contacts.value.find(c => c.contactId === contactId)
  activeContactName.value = c?.contactName || ''
  activeProductTitle.value = c?.productTitle || ''
  fetchMessages()
}

const sendMessage = () => {
  if (!newMsg.value.trim() || !activeContact.value) return
  // Simulated send via REST since STOMP requires full backend
  // In production, use STOMP: stompClient.publish(...)
  const msg = {
    id: Date.now(),
    senderId: myId,
    content: newMsg.value.trim(),
    createdAt: new Date().toISOString()
  }
  messages.value.push(msg)
  newMsg.value = ''
  nextTick(scrollToBottom)
}

const scrollToBottom = () => {
  nextTick(() => {
    if (msgList.value) msgList.value.scrollTop = msgList.value.scrollHeight
  })
}

const reportMessage = (msg) => {
  const reason = prompt('举报原因（骚扰/辱骂/诱导线下交易/广告/其他违规）：')
  if (!reason) return
  reportAPI.submit({ targetType: 'MESSAGE', targetId: msg.id, reason: 'OTHER', description: reason })
    .then(() => alert('举报已提交'))
    .catch(e => alert(e?.message || '提交失败'))
}

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  if (d.toDateString() === now.toDateString()) return d.toTimeString().slice(0, 5)
  return `${d.getMonth() + 1}-${d.getDate()}`
}

onMounted(fetchContacts)
</script>

<style scoped>
.chat-page { height: calc(100vh - 160px); }
.chat-layout { display: grid; grid-template-columns: 280px 1fr; height: 100%; gap: 16px; }
.contact-panel { background: #fff; border-radius: 8px; padding: 16px; overflow-y: auto; }
.contact-panel h3 { margin-bottom: 12px; font-size: 16px; }
.contact-item { display: flex; justify-content: space-between; padding: 12px; border-radius: 6px; cursor: pointer; margin-bottom: 4px; }
.contact-item:hover, .contact-item.active { background: #e6f7ff; }
.contact-info strong { font-size: 14px; }
.last-msg { font-size: 12px; color: #999; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 160px; margin-top: 4px; }
.contact-meta { text-align: right; }
.time { font-size: 11px; color: #999; }
.badge { display: inline-block; background: #ff4d4f; color: #fff; font-size: 11px; padding: 1px 6px; border-radius: 10px; margin-top: 4px; }
.chat-panel { background: #fff; border-radius: 8px; display: flex; flex-direction: column; }
.chat-header { padding: 12px 16px; border-bottom: 1px solid #f0f0f0; display: flex; gap: 8px; align-items: center; }
.product-tag { font-size: 12px; color: #1890ff; background: #e6f7ff; padding: 2px 8px; border-radius: 4px; }
.message-list { flex: 1; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.message { max-width: 70%; align-self: flex-start; }
.message.mine { align-self: flex-end; }
.msg-content { padding: 10px 14px; border-radius: 12px; font-size: 14px; word-break: break-word; }
.message:not(.mine) .msg-content { background: #f0f0f0; }
.message.mine .msg-content { background: #1890ff; color: #fff; }
.msg-meta { display: flex; gap: 8px; align-items: center; margin-top: 4px; font-size: 11px; color: #999; }
.btn-report-msg { background: none; border: none; color: #999; font-size: 11px; cursor: pointer; padding: 0; }
.btn-report-msg:hover { color: #ff4d4f; }
.chat-input { display: flex; gap: 12px; padding: 12px 16px; border-top: 1px solid #f0f0f0; }
.chat-input input { flex: 1; padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; }
.chat-input button { padding: 8px 20px; background: #1890ff; color: #fff; border: none; border-radius: 4px; }
.chat-input button:disabled { background: #91d5ff; }
</style>
