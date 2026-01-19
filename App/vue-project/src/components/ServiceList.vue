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
                    <input type="file" id="fileInput" @change="handleChangeFile"/>
                    <label for="fileInput">
                        <div v-if="isDragging">Release to drop files here.</div>
                        <div class="text-xs" v-else>Drop files here or <u>click here</u> to upload.</div>
                    </label>
                </div>

                <div class="flex flex-col gap-1">

                </div>
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
                        <tr v-for="item in [ {id: 1, name: 'Item 1', price: 10}, {id: 2, name: 'Item 2', price: 20} ]" :key="item.id" @click="selectId(item.id)">
                            <td>{{ item.id }}</td>
                            <td>{{ item.name }}</td>
                            <td>{{ item.price }}</td>
                            <td><button @click="handleAddToCart">Add to cart</button></td>
                            <td><button @click="handleRemoveFromCart">Remove from cart</button></td>
                            <td><button @click="handleBuy">Buy</button></td>
                        </tr>
                    </tbody>
                </table>
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
import { ref } from 'vue';
// import { serviceApi } from '@/api'
import { useMinioUpload } from '@/api/minio-upload/use-minio-upload';

const isDragging = ref(false);
const {mutateAsync: upload} = useMinioUpload();

// function handleClick() {
//     alert('Service button clicked!');
//     serviceApi.oracle()
// }

function handleDragOver(event: DragEvent) {
    event.preventDefault();
    isDragging.value = true;
}

function handleDragLeave(event: DragEvent) {
    event.preventDefault();
    isDragging.value = false;
}

function handleChangeFile(event: Event) {
    const target = event.target as HTMLInputElement;
    if (target.files && target.files.length > 0) {
        const file = target.files[0];
        if (!file) return;
        const formData = new FormData();
        formData.append('file', file);
        upload(formData);
    }
}

function handleRemoveFromCart() {

}

function handleAddToCart() {

}

function handleBuy() {

}

function selectId(id: number) {

}

</script>  