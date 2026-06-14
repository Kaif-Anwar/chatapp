# 🚀 Deployment Guide — DigitalOcean (GitHub Student Pack)

## Architecture
```
Internet → Port 80 → nginx (frontend container)
                         ├── serves React SPA
                         ├── /api/* → Spring Boot (internal)
                         ├── /ws/*  → Spring Boot WebSocket (internal)
                         └── /uploads/* → Spring Boot (internal)
```

---

## Step 1 — Redeem DigitalOcean Credit

1. Go to **[education.github.com/pack](https://education.github.com/pack)** → "Get your pack"
2. Click **DigitalOcean** → copy your promo code
3. Sign up at **digitalocean.com** → add the promo code in Billing → Credits

---

## Step 2 — Create a Droplet (Virtual Server)

In the DigitalOcean dashboard:

1. Click **Create → Droplets**
2. Choose:
   - **Region**: closest to your users (e.g., Singapore for Pakistan)
   - **OS**: Ubuntu 22.04 LTS
   - **Plan**: Basic → Regular → **$6/month** (1 vCPU, 1 GB RAM, 25 GB SSD)
     > ⚠️ Spring Boot + MySQL + Redis + nginx needs ~900 MB RAM. The $6 plan works.
     > Your $200 credit covers this for **33+ months**.
3. **Authentication**: SSH Key → add your public key (see below)
4. Click **Create Droplet**

### Generate SSH Key (if you don't have one):
```bash
ssh-keygen -t ed25519 -C "your-email@example.com"
# Press Enter for all prompts
# Your public key is in: C:\Users\KAIF\.ssh\id_ed25519.pub
```
Copy the contents of `id_ed25519.pub` and paste it into DigitalOcean.

---

## Step 3 — Push Code to GitHub

In your project folder on Windows (PowerShell):

```powershell
git init
git add .
git commit -m "feat: production-ready deployment"
```

Go to **github.com → New Repository** → create `chatapp` (public or private).

```powershell
git remote add origin https://github.com/YOUR_USERNAME/chatapp.git
git branch -M main
git push -u origin main
```

---

## Step 4 — Set Up the Server

SSH into your Droplet (replace IP with yours):
```bash
ssh root@YOUR_DROPLET_IP
```

Install Docker:
```bash
apt update && apt upgrade -y
apt install -y docker.io docker-compose-plugin git
systemctl enable docker
systemctl start docker
```

Open the firewall for HTTP:
```bash
ufw allow 22/tcp    # SSH
ufw allow 80/tcp    # HTTP
ufw enable
```

---

## Step 5 — Clone & Configure

```bash
git clone https://github.com/YOUR_USERNAME/chatapp.git
cd chatapp

# Create .env from the template
cp .env.example .env
nano .env
```

In nano, fill in real values:
```
COMPOSE_PROJECT_NAME=antigravity-chatapp
JWT_SECRET=         ← paste output of: openssl rand -hex 64
MYSQL_ROOT_PASSWORD=← choose a strong password
```

Generate the JWT secret right now:
```bash
openssl rand -hex 64
```
Copy the output and paste it as the `JWT_SECRET` value.

Save with **Ctrl+O**, exit with **Ctrl+X**.

---

## Step 6 — Launch 🚀

```bash
docker compose up -d --build
```

This builds and starts 4 containers:
- `antigravity-chatapp-mysql` — database
- `antigravity-chatapp-redis` — cache
- `antigravity-chatapp-backend` — Spring Boot API
- `antigravity-chatapp-frontend` — React + nginx

Check everything is running:
```bash
docker compose ps
docker compose logs -f    # watch live logs, Ctrl+C to stop
```

**Visit `http://YOUR_DROPLET_IP` in your browser — the app is live!**

---

## Step 7 — Auto-Deploy on Git Push (Optional)

Every time you push a fix to GitHub, run this on the server to update:
```bash
cd ~/chatapp && git pull && docker compose up -d --build
```

For full automation, add a **GitHub Actions** workflow that SSHes into the server on push.

---

## Step 8 — Add a Free Domain + HTTPS (Optional but recommended)

1. Get a free `.me` domain from the GitHub Student Pack (via Namecheap)
2. Point the domain's DNS A record to your Droplet IP
3. Install Certbot for free SSL:
```bash
apt install -y certbot python3-certbot-nginx
certbot --nginx -d yourdomain.me
```

---

## Useful Commands

```bash
# View running containers
docker compose ps

# See live backend logs
docker compose logs -f app

# Restart everything
docker compose restart

# Update after git pull
docker compose up -d --build

# Shell into backend container
docker exec -it antigravity-chatapp-backend sh
```
