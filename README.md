# Marketplace Helper

Веб-приложение для помощи в управлении товарами на маркетплейсе Wildberries.

## Технологический стек

### Backend
- **Java 21** - современная версия Java с новыми возможностями
- **Spring Boot 3.2.0** - фреймворк для создания веб-приложений
- **Spring Data JPA** - для работы с базой данных
- **Hibernate** - ORM для работы с базой данных
- **H2 Database** - встроенная база данных для разработки
- **Maven** - система сборки

### Frontend (планируется)
- **React 18** - библиотека для создания пользовательского интерфейса
- **TypeScript** - типизированный JavaScript
- **Vite** - быстрый инструмент сборки

## Требования

- Java 19 или выше
- Maven 3.6 или выше
- API токен Wildberries (см. [WB_API_SETUP.md](WB_API_SETUP.md))

## Запуск приложения

1. Клонируйте репозиторий:
```bash
git clone <repository-url>
cd marketplace-helper
```

2. Соберите проект:
```bash
mvn clean compile
```

3. Запустите приложение:
```bash
mvn spring-boot:run
```

4. Приложение будет доступно по адресу: http://localhost:8080

## API Endpoints

### Health Check
- `GET /api/health` - проверка состояния приложения
- `GET /api/hello` - приветственное сообщение

### Товары
- `GET /api/products` - получить все товары
- `GET /api/products/{id}` - получить товар по ID
- `GET /api/products/article/{wbArticle}` - получить товар по артикулу WB
- `POST /api/products` - создать новый товар
- `PUT /api/products/{id}` - обновить товар
- `DELETE /api/products/{id}` - удалить товар
- `GET /api/products/category/{category}` - получить товары по категории
- `GET /api/products/brand/{brand}` - получить товары по бренду
- `GET /api/products/search?name={name}` - поиск товаров по названию
- `GET /api/products/low-stock?threshold={number}` - получить товары с низким остатком

### Товары Wildberries (реальная интеграция с WB API)
- `GET /api/v2/list/goods/filter` - **основной эндпоинт** для получения товаров с ценами из реального WB API
  - Поддерживает фильтрацию: `name`, `vendor`, `brand`, `category`, `subject`, `minPrice`, `maxPrice`, `minDiscount`, `lowStockThreshold`
  - Параметр `useLocalData=true` - использовать локальные данные из базы
- `GET /api/v2/wb-api/ping` - проверка подключения к WB API
- `GET /api/v2/wb-api/seller-info` - получение информации о продавце
- `GET /api/v2/wb-api/goods` - получение товаров напрямую из WB API
- `POST /api/v2/wb-api/sync` - синхронизация товаров из WB API в локальную базу
- `GET /api/v2/wb-products` - получить все товары WB
- `GET /api/v2/wb-products/{id}` - получить товар WB по ID
- `GET /api/v2/wb-products/nm/{nmId}` - получить товар WB по артикулу WB
- `POST /api/v2/wb-products` - создать новый товар WB
- `PUT /api/v2/wb-products/{id}` - обновить товар WB
- `DELETE /api/v2/wb-products/{id}` - удалить товар WB
- `GET /api/v2/wb-products/vendor/{vendor}` - получить товары WB по поставщику
- `GET /api/v2/wb-products/brand/{brand}` - получить товары WB по бренду
- `GET /api/v2/wb-products/category/{category}` - получить товары WB по категории
- `GET /api/v2/wb-products/subject/{subject}` - получить товары WB по предмету
- `GET /api/v2/wb-products/search?name={name}` - поиск товаров WB по названию
- `GET /api/v2/wb-products/low-stock?threshold={number}` - получить товары WB с низким остатком
- `GET /api/v2/wb-products/price-range?minPrice={min}&maxPrice={max}` - получить товары WB по диапазону цен
- `GET /api/v2/wb-products/discount?minDiscount={discount}` - получить товары WB со скидкой

## H2 Console

Для просмотра базы данных в браузере:
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: `password`

## Структура проекта

```
src/
├── main/
│   ├── java/com/marketplacehelper/
│   │   ├── MarketplaceHelperApplication.java
│   │   ├── controller/
│   │   │   ├── HealthController.java
│   │   │   └── ProductController.java
│   │   ├── model/
│   │   │   └── Product.java
│   │   ├── repository/
│   │   │   └── ProductRepository.java
│   │   └── service/
│   │       └── ProductService.java
│   └── resources/
│       └── application.yml
└── test/
    └── java/com/marketplacehelper/
        └── MarketplaceHelperApplicationTests.java
```

## Примеры использования API

### Создание товара
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Тестовый товар",
    "wbArticle": "123456789",
    "price": 1500.00,
    "stockQuantity": 100,
    "category": "Электроника",
    "brand": "TestBrand"
  }'
```

### Получение всех товаров
```bash
curl http://localhost:8080/api/products
```

### Поиск товаров
```bash
curl "http://localhost:8080/api/products/search?name=тест"
```

### Работа с товарами Wildberries

#### Создание товара WB
```bash
curl -X POST http://localhost:8080/api/v2/wb-products \
  -H "Content-Type: application/json" \
  -d '{
    "nmId": 123456789,
    "name": "Смартфон TestBrand",
    "vendor": "TestVendor",
    "vendorCode": "TV001",
    "price": 2500.00,
    "discount": 15,
    "priceWithDiscount": 2125.00,
    "salePrice": 2000.00,
    "sale": 20,
    "totalQuantity": 50,
    "category": "Электроника",
    "subject": "Смартфоны",
    "brand": "TestBrand",
    "colors": "Черный,Белый",
    "sizes": "64GB,128GB"
  }'
```

#### Работа с реальным WB API

**Настройка токена:**
```bash
# Установите переменную окружения с вашим API токеном
export WB_API_TOKEN="ваш_токен_wildberries"
```

**Проверка подключения:**
```bash
# Проверка ping
curl "http://localhost:8080/api/v2/wb-api/ping"

# Информация о продавце
curl "http://localhost:8080/api/v2/wb-api/seller-info"
```

**Получение товаров из WB API:**
```bash
# Все товары из реального WB API
curl "http://localhost:8080/api/v2/wb-api/goods"

# Товары с фильтрацией (реальные данные из WB)
curl "http://localhost:8080/api/v2/list/goods/filter?brand=TestBrand"

# Локальные данные (из базы данных)
curl "http://localhost:8080/api/v2/list/goods/filter?useLocalData=true"
```

**Синхронизация данных:**
```bash
# Синхронизация товаров из WB API в локальную базу
curl -X POST "http://localhost:8080/api/v2/wb-api/sync"
```

## Планы развития

- [x] Интеграция с API Wildberries
- [ ] Система аналитики продаж
- [ ] Управление остатками
- [ ] Отчеты по прибыльности
- [ ] React фронтенд
- [ ] Система уведомлений
- [ ] Многопользовательский режим
- [ ] Автоматическая синхронизация данных
- [ ] Интеграция с другими маркетплейсами
