# 🛡️ PostIt - Secure GitHub Webhook Storage

A self-hosted webhook management system that uses GitHub as a data storage backend. Designed with high-level security, a premium Glassmorphism UI, and anti-brute force protection.

## ✨ Key Features
- **Zero Exposure**: GitHub credentials (Token, Repo) are stored securely on the Server Backend and are never leaked to the browser/client.
- **Triple-Layer Security**:
  1. **IP-Based Lockout**: Automatically blocks IPs for 12 hours after 3 failed login attempts.
  2. **Server-Side Auth**: The dashboard is protected by encrypted session cookies. Without logging in, the dashboard's HTML structure is never sent to the browser.
  3. **HMAC Webhook Tokens**: Webhook URLs use dynamic HMAC SHA-256 tokens validated on the backend.
- **Premium UI**: Modern, responsive, and elegant Dark Mode Glassmorphism design using the *Outfit* font.
- **Full File Management**: Create folders, delete folders, copy secure links, and perform manual uploads directly from a single dashboard.

## 🚀 Setup & Installation

### 1. Prerequisites
- Python 3.8+
- GitHub Account & Personal Access Token (PAT) with `repo` permissions.

### 2. Setup Environment Variables
Create a `.env` file in the root folder or set them in your Vercel dashboard:
```env
GITHUB_USER=your_github_username
GITHUB_REPO=your_storage_repo_name
GITHUB_TOKEN=your_pat_token
APP_PASSWORD=your_dashboard_admin_password
SECRET_KEY=random_string_for_sessions
```

### 3. Local Installation
```txt
Clone the repository
Install dependencies
Run the server
```
Access the dashboard at: `http://localhost:5000`

## 📡 Webhook Usage Guide
For detailed instructions on how to send data and files, please refer to the [Posting Guide](README_POST.md).

### URL Format
```
https://postit.devt.isweb.fun/hook/[folder_name]/[secure_token]
```

## 🛠️ Project Structure
- `/web-backend`: Flask Backend (Python) handling API and Security.
- `/web-frontend`: Static Frontend (HTML/CSS/JS) with Glassmorphism design.
- `/android-app`: Companion app for background synchronization.

## 🔒 Security Notes
- Never share the full webhook link with untrusted parties.
- Ensure your GitHub storage repository is set to **Private**.
- Use a strong `APP_PASSWORD`.

---
Developed by **nsn**
