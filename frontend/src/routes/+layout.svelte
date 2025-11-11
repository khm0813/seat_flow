<script lang="ts">
  import favicon from '$lib/assets/favicon.svg';
  import '$lib/styles/tokens.css';
  import '$lib/styles/global.css';

  let { children } = $props();
</script>

<svelte:head>
  <link rel="icon" href={favicon} />
</svelte:head>

<div class="app-bg">
  <div class="phone">
    <div class="phone-notch" />
    <div class="phone-screen">
      {@render children()}
    </div>
  </div>
  
</div>
