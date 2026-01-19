<template>
  <div class="grid grid-cols-5 divide-x-4 space-x-3">
    <div class="flex flex-col gap-1">
      <h3>Minio</h3>
      <div class="functions">
        <div class="text-xl">Upload</div>
        <div
          class="border border-dashed border-2 p-4"
          @dragover="handleDragOver"
          @dragleave="handleDragLeave"
        >
          <input type="file" id="fileInput" @change="handleChangeFile" />
          <label for="fileInput">
            <div v-if="isDragging">Release to drop files here.</div>
            <div class="text-xs" v-else>Drop files here or <u>click here</u> to upload.</div>
          </label>
        </div>

        <div class="flex flex-col gap-1"></div>
      </div>
    </div>
    <div class="flex flex-col gap-1">
      <h3>KurrentDB</h3>

      <button>Buy</button>

      <div class="">
        <table class="grid">
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Price</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="item in [
                { id: 1, name: 'Item 1', price: 10 },
                { id: 2, name: 'Item 2', price: 20 },
                { id: 3, name: 'Item 3', price: 30 },
              ]"
              :key="item.id"
            >
              <td>{{ item.id }}</td>
              <td>{{ item.name }}</td>
              <td>{{ item.price }}</td>
              <td><button @click="handleAddToCart(item)">Add to cart</button></td>
              <td><button @click="handleRemoveFromCart(item)">Remove from cart</button></td>
              <td><button @click="handleBuy(item)">Buy</button></td>
            </tr>
          </tbody>
        </table>
        <button @click="handleBuyAll">Buy from cart</button>
      </div>
    </div>

    <div class="">
      <h3>Oracle</h3>
    </div>

    <div class="">
      <h3>Keycloak</h3>
    </div>

    <div class="">
      <h3>MongoDB</h3>
    </div>
  </div>
  <!-- <div class="service-list">
        <button class="service-button" @click="handleClick()">Oracle</button>
    </div> -->
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
// import { serviceApi } from '@/api'
import { useMinioUpload } from '@/api/minio-upload/use-minio-upload'
import { useKurrentdbAddToCart } from '@/api/kurrentdb-add-to-cart/use-kurrentdb-add-to-cart'
import { useKurrentdbRemoveFromCart } from '@/api/kurrentdb-remove-from-cart/use-kurrentdb-remove-from-cart'
import { useKurrentdbBuyItem, useKurrentdbBuyAllItems } from '@/api/kurrentdb-buy/use-kurrentdb-buy'

const isDragging = ref(false)

const cart = reactive([] as Array<{ id: number; price: number; name: string }>)
const { mutateAsync: upload } = useMinioUpload()
const { mutateAsync: addToCart } = useKurrentdbAddToCart()
const { mutateAsync: removeFromCart } = useKurrentdbRemoveFromCart()
const { mutateAsync: buyItem } = useKurrentdbBuyItem()
const { mutateAsync: buyAllItems } = useKurrentdbBuyAllItems()

// function handleClick() {
//     alert('Service button clicked!');
//     serviceApi.oracle()
// }

function handleDragOver(event: DragEvent) {
  event.preventDefault()
  isDragging.value = true
}

function handleDragLeave(event: DragEvent) {
  event.preventDefault()
  isDragging.value = false
}

function handleChangeFile(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    const file = target.files[0]
    if (!file) return
    const formData = new FormData()
    formData.append('file', file)
    upload(formData)
  }
}

function handleAddToCart(item: { id: number; price: number; name: string }) {
  cart.push(item)
  addToCart({
    ...item,
    timestamp: Date.now(),
  })
}

function handleRemoveFromCart(item: { id: number }) {
  const index = cart.findIndex((cartItem) => cartItem.id === item.id)
  if (index !== -1) {
    cart.splice(index, 1)
  }

  removeFromCart({
    id: item.id,
    timestamp: Date.now(),
  })
}

function handleBuy(item: { id: number; price: number; name: string }) {
  buyItem({
    ...item,
    timestamp: Date.now(),
  })
}

function handleBuyAll() {
  buyAllItems({
    items: cart,
    timestamp: Date.now(),
  })
}
</script>
