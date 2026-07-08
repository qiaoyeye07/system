import { reactive } from 'vue'

const state = reactive({
  user: JSON.parse(localStorage.getItem('user') || 'null'),
  token: localStorage.getItem('token') || ''
})

export function useUserStore() {
  const login = (token, user) => {
    state.token = token
    state.user = user
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(user))
  }

  const logout = () => {
    state.token = ''
    state.user = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  const isLoggedIn = () => !!state.token
  const isAdmin = () => state.user?.role === 'ADMIN'
  const getCurrentUser = () => state.user

  return { state, login, logout, isLoggedIn, isAdmin, getCurrentUser }
}
