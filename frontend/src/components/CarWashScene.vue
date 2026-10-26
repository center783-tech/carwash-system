<template>
  <svg class="scene" viewBox="0 0 480 310" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <defs>
      <linearGradient :id="ids.body" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="#ffffff" />
        <stop offset="1" stop-color="#cfe0e3" />
      </linearGradient>
      <linearGradient :id="ids.glass" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#6fc4d1" />
        <stop offset="1" stop-color="#1c7c8c" />
      </linearGradient>
      <clipPath :id="ids.clip">
        <path :d="bodyPath" />
      </clipPath>
    </defs>

    <!-- 洗車門架與噴水橫桿 -->
    <rect x="52" y="20" width="14" height="248" rx="7" class="frame" />
    <rect x="414" y="20" width="14" height="248" rx="7" class="frame" />
    <rect x="52" y="14" width="376" height="16" rx="8" class="frame" />
    <g class="nozzle">
      <rect v-for="d in drops" :key="'n' + d.id" :x="d.x - 4" y="30" width="8" height="6" rx="2" />
    </g>

    <!-- 水柱 -->
    <g class="spray">
      <line
        v-for="d in drops"
        :key="d.id"
        class="drop"
        :x1="d.x"
        y1="40"
        :x2="d.x"
        y2="54"
        :style="{ animationDelay: d.delay + 's', animationDuration: d.dur + 's' }"
      />
    </g>

    <!-- 地面與倒影 -->
    <line x1="20" y1="274" x2="460" y2="274" class="ground" />
    <ellipse cx="240" cy="280" rx="190" ry="8" class="shadow" />

    <g class="car">
      <!-- 車身 -->
      <path :d="bodyPath" :fill="`url(#${ids.body})`" class="body" />
      <!-- 車窗 -->
      <path d="M152 168 Q178 130 230 128 L264 128 L264 168 Z" :fill="`url(#${ids.glass})`" />
      <path d="M274 128 L302 128 Q336 130 360 168 L274 168 Z" :fill="`url(#${ids.glass})`" />
      <!-- 車門線與門把 -->
      <path d="M268 128 L268 228" class="line" />
      <rect x="244" y="176" width="16" height="4" rx="2" class="handle" />
      <rect x="286" y="176" width="16" height="4" rx="2" class="handle" />
      <!-- 燈 -->
      <path d="M426 186 Q440 188 442 200 L424 200 Z" class="headlight" />
      <rect x="42" y="186" width="12" height="14" rx="4" class="taillight" />

      <!-- 打蠟亮光掃過 -->
      <g :clip-path="`url(#${ids.clip})`">
        <g class="shine">
          <rect x="-80" y="100" width="46" height="150" transform="skewX(-22)" />
        </g>
      </g>

      <!-- 輪拱與輪胎 -->
      <circle cx="124" cy="232" r="40" class="arch" />
      <circle cx="372" cy="232" r="40" class="arch" />
      <g v-for="cx in [124, 372]" :key="cx">
        <circle :cx="cx" cy="232" r="32" class="tire" />
        <g class="wheel" :style="{ transformOrigin: cx + 'px 232px' }">
          <circle :cx="cx" cy="232" r="19" class="rim" />
          <path
            :d="`M${cx} 215 V249 M${cx - 17} 232 H${cx + 17} M${cx - 12} 220 L${cx + 12} 244 M${cx + 12} 220 L${cx - 12} 244`"
            class="spoke"
          />
        </g>
        <circle :cx="cx" cy="232" r="5" class="hub" />
      </g>
    </g>

    <!-- 泡泡 -->
    <g class="bubbles">
      <circle
        v-for="b in bubbles"
        :key="b.id"
        class="bubble"
        :cx="b.x"
        :cy="b.y"
        :r="b.r"
        :style="{ animationDelay: b.delay + 's', animationDuration: b.dur + 's' }"
      />
    </g>

    <!-- 閃亮星芒 -->
    <g class="sparkles">
      <path
        v-for="s in sparkles"
        :key="s.id"
        class="sparkle"
        :d="`M${s.x} ${s.y - s.r} L${s.x + s.r * 0.3} ${s.y - s.r * 0.3} L${s.x + s.r} ${s.y} L${s.x + s.r * 0.3} ${s.y + s.r * 0.3} L${s.x} ${s.y + s.r} L${s.x - s.r * 0.3} ${s.y + s.r * 0.3} L${s.x - s.r} ${s.y} L${s.x - s.r * 0.3} ${s.y - s.r * 0.3} Z`"
        :style="{ animationDelay: s.delay + 's' }"
      />
    </g>
  </svg>
</template>

<script>
// 同一頁可能出現多個場景，gradient / clipPath 的 id 不能重複
let uid = 0
</script>

<script setup>
const n = ++uid
const ids = {
  body: `cws-body-${n}`,
  glass: `cws-glass-${n}`,
  clip: `cws-clip-${n}`
}

const bodyPath =
  'M44 208 Q44 182 74 176 L134 168 Q166 122 230 118 L302 118 Q348 120 374 164 L412 172 Q444 178 444 208 L444 226 Q444 236 434 236 L54 236 Q44 236 44 226 Z'

const drops = Array.from({ length: 8 }, (_, i) => ({
  id: i,
  x: 96 + i * 44,
  delay: (i % 4) * 0.22,
  dur: 1.1 + (i % 3) * 0.2
}))

const bubbles = [
  { id: 0, x: 76, y: 262, r: 7, delay: 0, dur: 4.2 },
  { id: 1, x: 150, y: 266, r: 4, delay: 1.1, dur: 3.6 },
  { id: 2, x: 232, y: 268, r: 6, delay: 2.0, dur: 4.8 },
  { id: 3, x: 318, y: 266, r: 4, delay: 0.6, dur: 3.9 },
  { id: 4, x: 396, y: 262, r: 8, delay: 1.6, dur: 4.4 },
  { id: 5, x: 440, y: 268, r: 5, delay: 2.6, dur: 3.8 }
]

const sparkles = [
  { id: 0, x: 200, y: 150, r: 9, delay: 0 },
  { id: 1, x: 340, y: 190, r: 7, delay: 0.9 },
  { id: 2, x: 120, y: 196, r: 6, delay: 1.7 }
]
</script>

<style scoped>
.scene {
  width: 100%;
  height: auto;
  display: block;
  overflow: visible;
}

.frame {
  fill: #2a4350;
}
.nozzle rect {
  fill: #4a6676;
}
.ground {
  stroke: #2a4350;
  stroke-width: 3;
  stroke-linecap: round;
}
.shadow {
  fill: rgba(0, 0, 0, 0.28);
}

.drop {
  stroke: #3fb3c2;
  stroke-width: 3;
  stroke-linecap: round;
  opacity: 0;
  animation: fall linear infinite;
}
@keyframes fall {
  0% {
    transform: translateY(0);
    opacity: 0;
  }
  15% {
    opacity: 0.95;
  }
  100% {
    transform: translateY(150px);
    opacity: 0;
  }
}

.car {
  animation: bob 3.2s ease-in-out infinite;
}
@keyframes bob {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-2px);
  }
}
.body {
  stroke: #1c7c8c;
  stroke-width: 4;
  stroke-linejoin: round;
}
.line {
  stroke: #1c7c8c;
  stroke-width: 2.5;
  fill: none;
  opacity: 0.55;
}
.handle {
  fill: #1c7c8c;
  opacity: 0.7;
}
.headlight {
  fill: var(--wax, #e8a33d);
}
.taillight {
  fill: #c0493f;
}
.arch {
  fill: var(--ink, #14202b);
}
.tire {
  fill: #22333f;
  stroke: #0f1a22;
  stroke-width: 3;
}
.rim {
  fill: #dfe8ea;
  stroke: #8aa3ab;
  stroke-width: 2;
}
.spoke {
  stroke: #8aa3ab;
  stroke-width: 3;
  stroke-linecap: round;
  fill: none;
}
.hub {
  fill: #2a4350;
}
.wheel {
  animation: spin 1.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.shine {
  animation: sweep 4.5s ease-in-out infinite;
}
.shine rect {
  fill: rgba(255, 255, 255, 0.75);
}
@keyframes sweep {
  0% {
    transform: translateX(0);
  }
  55%,
  100% {
    transform: translateX(640px);
  }
}

.bubble {
  fill: rgba(63, 179, 194, 0.18);
  stroke: #7fd0da;
  stroke-width: 1.5;
  opacity: 0;
  animation: rise ease-in infinite;
}
@keyframes rise {
  0% {
    transform: translateY(0) scale(0.6);
    opacity: 0;
  }
  15% {
    opacity: 0.9;
  }
  100% {
    transform: translateY(-110px) scale(1.15);
    opacity: 0;
  }
}

.sparkle {
  fill: var(--wax, #e8a33d);
  opacity: 0;
  transform-box: fill-box;
  transform-origin: center;
  animation: twinkle 2.6s ease-in-out infinite;
}
@keyframes twinkle {
  0%,
  100% {
    opacity: 0;
    transform: scale(0.3) rotate(0deg);
  }
  50% {
    opacity: 1;
    transform: scale(1) rotate(45deg);
  }
}

/* 使用者設定「減少動態效果」時，停止所有動畫，只留靜態畫面 */
@media (prefers-reduced-motion: reduce) {
  .drop,
  .car,
  .wheel,
  .shine,
  .bubble,
  .sparkle {
    animation: none;
  }
  .drop {
    opacity: 0.8;
  }
  .bubble {
    opacity: 0.8;
  }
  .sparkle {
    opacity: 1;
    transform: none;
  }
  .shine {
    display: none;
  }
}
</style>
