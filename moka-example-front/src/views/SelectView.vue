<template>
  <div class="select-file">
    <h1>This is a file selection page</h1>
    <button @click="selectFile">Select File</button>
    <p>File selected: {{ fileSelected }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { invoke } from '@/utils/moka'

let fileSelected = ref('')

function selectFile() {
  invoke('app.selectFile', { path: '.' }).then((result) => {
    console.log('called!', result)
    fileSelected.value = result.path
  })
}
</script>

<style>
@media (min-width: 1024px) {
  .select-file {
    min-height: 100vh;
    display: flex;
    align-items: center;
  }
}
</style>
