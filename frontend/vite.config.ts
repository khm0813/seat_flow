import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

export default defineConfig({
	plugins: [sveltekit()],
	server: {
		host: '0.0.0.0',
		port: 5173,
		watch: {
			usePolling: true, // Enable polling for file changes in Docker
		},
		hmr: {
			port: 5173,
		}
	}
});
