<template>
  <div class="flex flex-col gap-3">
    <h3 class="text-lg font-bold">Keycloak</h3>
    
    <div v-if="loading" class="text-sm text-gray-600">
      Đang xử lý...
    </div>

    <div v-else-if="error" class="text-sm text-red-600">
      <p>{{ error }}</p>
      <button 
        @click="clearError" 
        class="mt-2 px-3 py-1 text-sm bg-red-500 text-white rounded"
      >
        Thử lại
      </button>
    </div>

    <div v-else-if="!userInfo" class="flex flex-col gap-2">
      <button 
        @click="handleLogin" 
        class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
      >
        Login với Keycloak
      </button>
    </div>

    <div v-else class="flex flex-col gap-2 text-sm">
      <div class="p-3 bg-green-50 rounded border border-green-200">
        <div class="font-semibold text-green-700 mb-2">Đã đăng nhập</div>
        
        <div class="space-y-1">
          <div><span class="font-medium">User:</span> {{ userInfo.preferred_username }}</div>
          <div><span class="font-medium">Email:</span> {{ userInfo.email }}</div>
          <div v-if="userInfo.name"><span class="font-medium">Name:</span> {{ userInfo.name }}</div>
        </div>
      </div>

      <button 
        @click="handleLogout" 
        class="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
      >
        Logout
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { login } from '@/api/auth/login'
import { exchangeCode } from '@/api/auth/exchange-token'

interface UserInfo {
  sub: string
  email_verified: boolean
  name?: string
  preferred_username: string
  given_name?: string
  family_name?: string
  email: string
}

const userInfo = ref<UserInfo | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

onMounted(async () => {
  // Kiểm tra callback từ Keycloak
  const params = new URLSearchParams(window.location.search)
  const code = params.get('code')
  const state = params.get('state')
  const savedState = sessionStorage.getItem('oauth_state')

  if (code && state && savedState === state) {
    loading.value = true
    try {
      // Exchange code và nhận user info từ backend
        await fetch('http://localhost:9992/api/auth/callback', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ code }),
        })
      
      // Lấy thông tin user từ backend (có cookie)
      const response = await fetch('http://localhost:9992/api/auth/me', {
        credentials: 'include',
      })
      
      if (response.ok) {
        userInfo.value = await response.json()
      }

      // Xóa state và URL params
      sessionStorage.removeItem('oauth_state')
      window.history.replaceState({}, document.title, window.location.pathname)
    } catch (err: any) {
      error.value = err.message || 'Đăng nhập thất bại'
      console.error('Login error:', err)
    } finally {
      loading.value = false
    }
  } else {
    // Kiểm tra xem đã có session chưa
    try {
      const response = await fetch('http://localhost:9992/api/auth/me', {
        credentials: 'include',
      })
      
      if (response.ok) {
        userInfo.value = await response.json()
      }
    } catch (err) {
      console.log('Chưa đăng nhập')
    }
  }
})

function handleLogin() {
  login()
}

async function handleLogout() {
  try {
    await fetch('http://localhost:9992/api/auth/logout', {
      method: 'POST',
      credentials: 'include',
    })
  } catch (err) {
    console.error('Logout error:', err)
  }
  
  sessionStorage.removeItem('oauth_state')
  userInfo.value = null
}

function clearError() {
  error.value = null
}
</script>
