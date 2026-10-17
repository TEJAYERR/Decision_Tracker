import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // Forward API calls to Spring Boot so there are no CORS issues in dev
    proxy: {
      "/documents": "http://localhost:8080",
      "/ask": "http://localhost:8080",
    },
  },
});
