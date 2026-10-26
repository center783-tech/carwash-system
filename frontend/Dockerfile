# =========================================================
# 前端：Vue 3 + Vite
# 第一階段 npm run build 產生靜態檔，第二階段用 Nginx 提供網頁並轉發 /api 給後端
# =========================================================

# ---------- 1. 建置階段 ----------
FROM node:20-alpine AS build
WORKDIR /app

# 先複製套件清單安裝相依，package-lock.json 沒變時這層走快取
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# 會套用 .env.production（VITE_API_BASE_URL=/api）與 vite.config.js 的 base: '/carwash/'
RUN npm run build

# ---------- 2. 執行階段 ----------
FROM nginx:1.27-alpine
COPY nginx/default.conf /etc/nginx/conf.d/default.conf
# vite base 是 /carwash/，所以靜態檔放在 /carwash 子目錄
COPY --from=build /app/dist /usr/share/nginx/html/carwash
EXPOSE 80
