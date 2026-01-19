import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { initializeKeycloak, keycloak } from './keycloak'
import { VueQueryPlugin } from '@tanstack/vue-query'

const app = createApp(App)

app.config.globalProperties.$keycloak = keycloak

app.use(createPinia())

app.use(initializeKeycloak)
app.use(VueQueryPlugin)

//Because initializing the adapter can cause mutations to the URL of the page, make sure that the adapter is always initialized before initializing a router of your client-side framework.
app.use(router)

app.mount('#app')
