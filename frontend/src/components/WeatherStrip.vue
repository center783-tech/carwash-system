<template>
  <section v-if="days.length" class="weather">
    <div class="weather__now">
      <span class="weather__icon" aria-hidden="true">{{ days[0].icon }}</span>
      <div>
        <div class="weather__place">{{ LOCATION.name }} · 現在</div>
        <div class="weather__temp">{{ currentTemp }}°C <small>{{ days[0].text }}</small></div>
      </div>
    </div>

    <p class="weather__tip" :class="`weather__tip--${tip.level}`">{{ tip.text }}</p>

    <ul class="weather__days">
      <li v-for="(d, i) in days" :key="d.date">
        <span class="weather__day">{{ i === 0 ? '今天' : d.label }}</span>
        <span class="weather__icon-sm" aria-hidden="true">{{ d.icon }}</span>
        <span class="weather__range">{{ d.min }}–{{ d.max }}°</span>
        <span class="weather__rain">☂ {{ d.rain }}%</span>
      </li>
    </ul>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

// 洗車場所在位置（預設台北），要換地點改這裡的經緯度即可
const LOCATION = { name: '台北', latitude: 25.033, longitude: 121.565 }

// WMO weather code -> 圖示與中文說明
function describe(code) {
  if (code === 0) return { icon: '☀️', text: '晴朗' }
  if (code <= 2) return { icon: '🌤️', text: '晴時多雲' }
  if (code === 3) return { icon: '☁️', text: '陰天' }
  if (code === 45 || code === 48) return { icon: '🌫️', text: '有霧' }
  if (code >= 51 && code <= 57) return { icon: '🌦️', text: '毛毛雨' }
  if (code >= 61 && code <= 67) return { icon: '🌧️', text: '下雨' }
  if (code >= 71 && code <= 77) return { icon: '❄️', text: '降雪' }
  if (code >= 80 && code <= 82) return { icon: '🌦️', text: '陣雨' }
  if (code >= 95) return { icon: '⛈️', text: '雷雨' }
  return { icon: '🌡️', text: '—' }
}

const WEEKDAY = ['日', '一', '二', '三', '四', '五', '六']

const currentTemp = ref(null)
const days = ref([])

const tip = computed(() => {
  const rain = days.value[0]?.rain ?? 0
  if (rain >= 60) return { level: 'bad', text: '今天降雨機率偏高，洗完可能馬上又弄髒，建議改天預約。' }
  if (rain >= 30) return { level: 'mid', text: '今天可能有雨，想打蠟或鍍膜的話建議挑晴天。' }
  return { level: 'good', text: '今天降雨機率低，很適合洗車！' }
})

onMounted(async () => {
  try {
    const params = new URLSearchParams({
      latitude: LOCATION.latitude,
      longitude: LOCATION.longitude,
      current: 'temperature_2m,weather_code',
      daily: 'weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max',
      timezone: 'Asia/Taipei',
      forecast_days: 4
    })
    const res = await fetch(`https://api.open-meteo.com/v1/forecast?${params}`)
    if (!res.ok) throw new Error(String(res.status))
    const data = await res.json()

    currentTemp.value = Math.round(data.current.temperature_2m)
    days.value = data.daily.time.map((date, i) => ({
      date,
      label: `週${WEEKDAY[new Date(`${date}T00:00:00`).getDay()]}`,
      ...describe(i === 0 ? data.current.weather_code : data.daily.weather_code[i]),
      min: Math.round(data.daily.temperature_2m_min[i]),
      max: Math.round(data.daily.temperature_2m_max[i]),
      rain: data.daily.precipitation_probability_max[i] ?? 0
    }))
  } catch {
    // 天氣只是輔助資訊，取不到就整塊不顯示，不影響首頁
    days.value = []
  }
})
</script>

<style scoped>
.weather {
  display: flex;
  align-items: center;
  gap: 28px;
  flex-wrap: wrap;
  margin: 0 56px 8px;
  padding: 18px 24px;
  background: #fff;
  border: 1px solid var(--paper-dim);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
}
.weather__now {
  display: flex;
  align-items: center;
  gap: 12px;
}
.weather__icon {
  font-size: 40px;
  line-height: 1;
}
.weather__place {
  font-size: 12px;
  color: var(--ink-text-secondary);
}
.weather__temp {
  font-family: var(--font-display);
  font-size: 26px;
  font-weight: 700;
  color: var(--rinse-deep);
}
.weather__temp small {
  font-size: 14px;
  font-weight: 400;
  color: var(--ink-text-secondary);
}
.weather__tip {
  flex: 1;
  min-width: 220px;
  margin: 0;
  padding: 8px 14px;
  font-size: 14px;
  border-radius: 10px;
}
.weather__tip--good {
  background: #e8f5ee;
  color: #1e6b45;
}
.weather__tip--mid {
  background: #fdf3df;
  color: #8a5a12;
}
.weather__tip--bad {
  background: #fbe9e7;
  color: #a43a2c;
}
.weather__days {
  display: flex;
  gap: 18px;
  list-style: none;
  margin: 0;
  padding: 0;
}
.weather__days li {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  color: var(--ink-text-secondary);
}
.weather__day {
  font-weight: 600;
  color: var(--ink);
}
.weather__icon-sm {
  font-size: 22px;
}
.weather__rain {
  color: var(--rinse-deep);
}

@media (max-width: 860px) {
  .weather {
    margin: 0 24px 8px;
    gap: 16px;
  }
  .weather__days {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
