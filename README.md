# 洗車系統 Carwash System

線上洗車預約系統。會員可瀏覽服務、登記車輛、預約並線上付款、使用優惠券與留下評價;後台人員可管理服務項目、員工排班、預約、優惠券、停車區與公休日,管理員另可查看營收報表。

## 功能

**會員端**
- 註冊 / 登入、個人資料與大頭照、Email 驗證碼修改密碼
- 瀏覽洗車服務項目(免登入)
- 車輛管理
- 建立 / 查詢 / 取消預約(依員工排班與停車區名額判斷可預約時段)
- 付款:藍新金流(NewebPay)或現金
- 優惠券領取與兌換
- 服務評價(可附圖)

**後台管理端**(`/carwash/admin/login`)
- 總覽、預約管理(確認 / 完成)、服務項目管理
- 員工管理、員工排班與請假
- 優惠券建立與批次發放(發放時寄 Email 通知)
- 停車區管理、公休日管理
- 營收報表,可匯出 Excel(僅 `ADMIN`)

**角色**:`MEMBER`(會員)、`STAFF`(後台人員)、`ADMIN`(管理員,含財務報表權限)。

## 技術棧

| 層級 | 技術 |
| --- | --- |
| 前端 | Vue 3、Vite 5、Vue Router、Pinia、Element Plus、Axios、ExcelJS |
| 後端 | Java 21、Spring Boot 4、Spring Security(JWT)、Spring Data JPA、Flyway、springdoc(Swagger) |
| 資料庫 | MySQL 8.4(schema 由 Flyway 管理) |
| 快取 | Redis 7(服務清單 / 排班快取、登入失敗鎖定、驗證碼) |
| 檔案儲存 | MinIO(S3 相容,存放服務圖片、評價附圖、大頭照) |
| 金流 | 藍新金流 NewebPay(預設測試環境) |
| 寄信 | Gmail SMTP |
| 部署 | Docker Compose、Nginx、GitHub Actions → AWS EC2 |

## 專案結構

```
carwash-system/
├── backend/                 Spring Boot 後端
│   ├── src/main/java/com/carwash/
│   │   ├── controller/      REST API
│   │   ├── service/         商業邏輯
│   │   ├── repository/      JPA Repository
│   │   ├── entity/          資料表實體
│   │   ├── security/        JWT 驗證與權限
│   │   └── config/          Security、Redis、MinIO、藍新金流等設定
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/    Flyway 資料表腳本(V001 ~ V021)
│   ├── Dockerfile
│   └── .env.example         後端環境變數範本
├── frontend/                Vue 3 前端
│   ├── src/views/           會員端與 admin/ 後台頁面
│   ├── src/api/             API 呼叫
│   ├── nginx/default.conf   容器內 Nginx 設定
│   └── Dockerfile
├── docker-compose.yml       MySQL + Redis + MinIO + 後端 + 前端
├── .env.example             docker-compose 用的環境變數範本
└── .github/workflows/deploy.yml   測試與部署到 EC2
```

## 快速開始(Docker Compose,建議)

需要:Docker 與 Docker Compose。

```bash
# 1. 複製環境變數範本
cp .env.example .env                    # docker-compose 用(資料庫 / Redis / MinIO 密碼、埠號)
cp backend/.env.example backend/.env    # 後端用(JWT、Gmail、藍新金流)

# 2. 編輯兩個 .env
#    - .env:把 change_me_* 改成自己的密碼
#    - backend/.env:至少設定 JWT_SECRET(32 bytes 以上),例如 openssl rand -base64 32

# 3. 啟動
docker compose up -d --build
```

啟動後:

| 項目 | 網址 |
| --- | --- |
| 網站(會員端) | http://localhost:8080/carwash/ |
| 後台登入 | http://localhost:8080/carwash/admin/login |
| MinIO 管理介面 | http://localhost:9001 |
| MySQL(外部工具連線) | `localhost:3307` |

說明:
- 容器內由 `docker-compose.yml` 覆蓋 `backend/.env` 的資料庫、Redis、MinIO 連線位址,不需要自己改。
- 後端不對外開埠,前端 Nginx 會把 `/api/` 轉發到後端、`/files/` 轉發到 MinIO。
- 資料庫資料表在後端首次啟動時由 Flyway 自動建立。

## 本機開發

需要:JDK 21、Node.js 20、MySQL 8、Redis、MinIO。

```bash
# 後端(預設 http://localhost:8081)
cd backend
cp .env.example .env     # 填入 DB_PASSWORD、JWT_SECRET 等
./mvnw spring-boot:run

# 前端(http://localhost:5300/carwash/,/api 會代理到 localhost:8081)
cd frontend
npm install
npm run dev
```

先在 MySQL 建立資料庫 `carwash_db`(可參考 `backend/create-database.sql`),資料表會由 Flyway 建立。

## 環境變數

**根目錄 `.env`**(docker-compose 使用)

| 變數 | 說明 |
| --- | --- |
| `MYSQL_ROOT_PASSWORD` | MySQL root 密碼(必填) |
| `REDIS_PASSWORD` | Redis 密碼(必填) |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | MinIO 帳密(必填,密碼至少 8 字元) |
| `WEB_PORT` | 網站對外埠號,預設 `8080` |
| `APP_BASE_URL` | 前端網址(含 `/carwash`),優惠券通知信的連結會用到 |
| `CORS_ALLOWED_ORIGINS` | 允許呼叫 API 的前端網域,逗號分隔 |
| `SWAGGER_ENABLED` | 是否開啟 Swagger,預設 `false` |

**`backend/.env`**(後端使用)

| 變數 | 說明 |
| --- | --- |
| `JWT_SECRET` | JWT 簽章密鑰(必填,至少 32 bytes) |
| `JWT_EXPIRATION_MS` / `JWT_ADMIN_EXPIRATION_MS` | 會員 / 後台 token 有效期,預設 24 小時 / 8 小時 |
| `DB_URL` `DB_USERNAME` `DB_PASSWORD` | MySQL 連線(本機開發用) |
| `REDIS_HOST` `REDIS_PORT` `REDIS_PASSWORD` | Redis 連線(本機開發用) |
| `MINIO_ENDPOINT` `MINIO_ACCESS_KEY` `MINIO_SECRET_KEY` | MinIO 連線(本機開發用) |
| `GMAIL_USERNAME` / `GMAIL_PASSWORD` | 寄信用 Gmail 帳號與應用程式密碼 |
| `NEWEBPAY_MERCHANT_ID` `NEWEBPAY_HASH_KEY` `NEWEBPAY_HASH_IV` | 藍新金流商店資料 |
| `NEWEBPAY_RETURN_URL` / `NEWEBPAY_NOTIFY_URL` | 藍新回呼網址,必須是公開網址(本機可用 ngrok) |
| `SWAGGER_ENABLED` | 本機開發設 `true` 可開啟 Swagger |

> `.env` 含密碼與金鑰,已列入 `.gitignore`,請勿提交。

## 預設後台帳號

資料庫初始化時會建立一組管理員帳號:`admin` / `admin`。
**首次登入後請立刻修改密碼,正式環境務必不要沿用。**

## API 文件

設定 `SWAGGER_ENABLED=true` 後,開啟 `http://localhost:8081/swagger-ui.html`(Docker 部署需先把 `docker-compose.yml` 中 backend 的 `ports` 註解打開)。

主要 API 路徑:

| 路徑 | 說明 |
| --- | --- |
| `/api/members/**` | 會員註冊、登入、資料、車輛 |
| `/api/services/**` | 服務項目(GET 公開) |
| `/api/reservations/**` | 預約 |
| `/api/payments/**` | 付款與藍新金流回呼 |
| `/api/coupons/**` | 優惠券 |
| `/api/reviews/**` | 評價 |
| `/api/employees/**` | 員工與排班(後台) |
| `/api/zones/**` | 停車區名額 |
| `/api/admin/**` | 後台登入、報表、公休日、停車區管理 |
| `/api/files/**` | 圖片上傳 |

## 測試

```bash
cd backend
./mvnw clean package   # 會一併執行單元測試,需要可連線的 MySQL 與 Redis
```

## 部署

推送到 `main` 分支會觸發 GitHub Actions(`.github/workflows/deploy.yml`):

1. 測試後端、建置前端
2. 將 JAR 與前端靜態檔上傳到 EC2
3. 重啟後端並做健康檢查,失敗時自動回滾到上一版 JAR

需要在 GitHub 設定:
- Secrets:`EC2_SSH_KEY`、`EC2_KNOWN_HOSTS`(選填)
- Variables:`EC2_HOST`、`EC2_USER`
- Environment:`production`

EC2 上的 `backend/.env` 需自行放置,不會由部署流程上傳。
