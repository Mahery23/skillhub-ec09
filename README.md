# SkillHub

Plateforme web de formations en ligne. Les formateurs publient des cours, les apprenants s'y inscrivent.

---

## Architecture

| Service | Technologie | Port |
|---|---|---|
| Auth Service | Spring Boot (Java 21) | 8080 |
| API | Laravel (PHP 8.3) | 8000 |
| Frontend | React | 5173 |

---

## Prérequis

- Java 21, Maven
- PHP 8.3, Composer
- Node.js 20+
- XAMPP (MySQL + Apache)

---

## Installation

**1. Cloner le projet**
```bash
git clone https://github.com/Mahery23/skillhub-ec09.git
cd skillhub-ec09
```

**2. Base de données**

Lancer XAMPP puis créer deux bases dans phpMyAdmin : `authdb` et `skillhub_db`

```bash
cd backend
cp .env.example .env
php artisan migrate
```

**3. Dépendances**
```bash
# Backend
cd backend && composer install

# Frontend
cd frontend && npm install
```

---

## Lancer le projet

Ouvrir **3 terminaux** :

```bash
# Terminal 1 — Auth Service
cd auth-service
set APP_MASTER_KEY=skillhub_master_key_aes_gcm_2026_ec09_32chars
set JWT_SECRET=skillhub_secret_jwt_2026_examen_ec09_32chars
mvn spring-boot:run

# Terminal 2 — Laravel
cd backend && php artisan serve

# Terminal 3 — React
cd frontend && npm run dev
```

Ouvrir **http://localhost:5173**

---

## Tests

```bash
cd backend && php artisan test      # Laravel (PHPUnit)
cd frontend && npm run test         # React (Jest)
cd auth-service && mvn test         # Spring Boot
```

---

## Variables d'environnement

| Variable | Où | Description |
|---|---|---|
| `APP_MASTER_KEY` | auth-service | Clé AES-256 (min. 32 caractères) |
| `JWT_SECRET` | auth-service + backend/.env | Clé de signature JWT |
| `AUTH_SERVICE_URL` | backend/.env | URL du auth-service (http://localhost:8080) |

---

## CI/CD

Pipeline GitHub Actions sur chaque push → tests + analyse SonarCloud.

Voir les résultats : **https://sonarcloud.io** → projet `Mahery23_skillhub-ec09`
