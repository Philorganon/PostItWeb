const CONFIG = {
    API_BASE: window.location.origin
};

const STORAGE = {
    LOGGED_IN: "webhookLoggedIn",
    GITHUB_USER: "githubUser",
    GITHUB_REPO: "githubRepo",
    GITHUB_TOKEN: "githubToken",
    FOLDERS: "webhookFolders",
    ATTEMPTS: "loginAttempts",
    LOCKOUT: "lockoutUntil"
};

const MAX_ATTEMPTS = 3;
const LOCKOUT_TIME = 12 * 60 * 60 * 1000;

function showNotification(msg, isError = false) {
    const notif = document.getElementById("notification");
    if (!notif) return;
    notif.textContent = msg;
    notif.classList.toggle("error", isError);
    notif.classList.add("show");
    setTimeout(() => notif.classList.remove("show"), 3000);
}

// --- AUTH LOGIC ---
async function login() {
    const pwd = document.getElementById("passwordInput").value;
    const err = document.getElementById("loginError");
    
    // Check Lockout
    const lockoutUntil = localStorage.getItem(STORAGE.LOCKOUT);
    if (lockoutUntil && Date.now() < parseInt(lockoutUntil)) {
        const hours = Math.ceil((parseInt(lockoutUntil) - Date.now()) / (3600000));
        err.textContent = `Terblokir! Coba lagi dalam ${hours} jam.`;
        return;
    }

    if (!pwd) {
        err.textContent = "Masukkan password!";
        return;
    }

    // Tanya ke Server (Backend) - Password tidak ada di sini!
    try {
        const response = await fetch(`${CONFIG.API_BASE}/api/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ password: pwd })
        });

        if (response.ok) {
            localStorage.setItem(STORAGE.LOGGED_IN, "true");
            localStorage.setItem(STORAGE.ATTEMPTS, "0");
            // Kita simpan password di session sementara (bukan di kode) 
            // hanya untuk keperluan generate link webhook
            sessionStorage.setItem('temp_pass', pwd); 
            showDashboard();
        } else {
            handleFailedAttempt(err);
        }
    } catch (e) {
        err.textContent = "Gagal menyambung ke server.";
    }
}

function handleFailedAttempt(err) {
    let attempts = parseInt(localStorage.getItem(STORAGE.ATTEMPTS) || "0") + 1;
    localStorage.setItem(STORAGE.ATTEMPTS, attempts);
    
    if (attempts >= MAX_ATTEMPTS) {
        localStorage.setItem(STORAGE.LOCKOUT, (Date.now() + LOCKOUT_TIME).toString());
        err.textContent = "Batas percobaan habis! Terblokir 12 jam.";
    } else {
        err.textContent = `Password salah! Sisa percobaan: ${MAX_ATTEMPTS - attempts}`;
    }
    setTimeout(() => err.textContent = "", 3000);
}

async function logout() {
    await fetch(`${CONFIG.API_BASE}/api/logout`, { method: "POST" });
    localStorage.removeItem(STORAGE.LOGGED_IN);
    sessionStorage.removeItem('temp_pass');
    showLogin();
}

function showLogin() {
    window.location.href = 'login.html';
}

function showDashboard() {
    window.location.href = 'index.html';
}

// --- CORE LOGIC ---
async function apiFetch(url, options = {}) {
    const pass = sessionStorage.getItem('temp_pass');
    
    // Inisialisasi headers jika belum ada
    if (!options.headers) options.headers = {};

    // Jika body adalah FormData, JANGAN set Content-Type manual (biarkan browser yang set boundary)
    if (!(options.body instanceof FormData)) {
        if (!options.headers["Content-Type"]) {
            options.headers["Content-Type"] = "application/json";
        }
    }

    // Selalu tambahkan password keamanan
    options.headers["X-Password"] = pass;

    try {
        const response = await fetch(url, options);
        if (response.status === 401 || response.status === 403) {
            // JIKA SERVER MENOLAK (HACK DETECTED) -> AUTO KICK
            logout();
            return null;
        }
        return await response.json();
    } catch (e) {
        showNotification("Gagal menyambung ke server.", true);
        return null;
    }
}

async function createFolder() {
    const nameInput = document.getElementById("folderName");
    const name = nameInput.value.trim().toLowerCase();
    
    if (!name) {
        showNotification("Nama folder wajib diisi!", true);
        return;
    }

    try {
        showNotification("Sedang membuat folder di GitHub...");
        const res = await apiFetch(`${CONFIG.API_BASE}/api/create_folder/${name}`, {
            method: "POST"
        });

        if (res && res.success) {
            showNotification(`Folder "${name}" berhasil dibuat di GitHub!`);
            nameInput.value = "";
            loadFolders();
        } else {
            showNotification("Gagal membuat folder.", true);
        }
    } catch (e) {
        showNotification("Error koneksi.", true);
    }
}

async function deleteFolder(name) {
    if (!confirm(`Hapus folder "${name}" secara PERMANEN dari GitHub?`)) return;
    
    try {
        showNotification(`Menghapus folder "${name}" dari GitHub...`);
        const res = await apiFetch(`${CONFIG.API_BASE}/api/delete_folder/${name}`, {
            method: "DELETE"
        });

        if (res && res.success) {
            showNotification(`Folder "${name}" berhasil dihapus!`);
            loadFolders();
        } else {
            showNotification("Gagal menghapus folder di GitHub.", true);
        }
    } catch (e) {
        showNotification("Error koneksi.", true);
    }
}

async function loadFolders() {
    const container = document.getElementById("foldersList");
    const select = document.getElementById("uploadFolder");
    
    const data = await apiFetch(`${CONFIG.API_BASE}/api/folders`);
    if (!data || !data.success) {
        container.innerHTML = '<p style="color:#718096;grid-column:1/-1;text-align:center">Gagal memuat folder.</p>';
        return;
    }

    const folders = data.folders.map(name => ({
        name: name,
        webhookUrl: `${CONFIG.API_BASE}/hook/${name}`
    }));

    if (!folders.length) {
        container.innerHTML = '<p style="color:#718096;grid-column:1/-1;text-align:center">Belum ada folder di GitHub.</p>';
        return;
    }

    container.innerHTML = folders.map(f => `
        <div class="folder-card">
            <h4>📁 ${f.name}</h4>
            <div class="btn-group">
                <button onclick="copyFolderLink('${f.name}')">Salin Link Aman</button>
                <button class="delete-btn" onclick="deleteFolder('${f.name}')">Hapus</button>
            </div>
        </div>
    `).join("");

    select.innerHTML = '<option value="">Pilih folder...</option>' + folders.map(f => `<option value="${f.name}">${f.name}</option>`).join("");
}

async function copyFolderLink(name) {
    const data = await apiFetch(`${CONFIG.API_BASE}/api/get_link/${name}`);
    if (data && data.success) {
        navigator.clipboard.writeText(data.link).then(() => showNotification("Link Aman disalin!"));
    }
}

// --- UPLOAD LOGIC ---
async function manualUpload() {
    const folder = document.getElementById("uploadFolder").value;
    const text = document.getElementById("textContent").value.trim();
    const fileInput = document.getElementById("fileInput");
    const files = fileInput.files;

    if (!folder) { showNotification("Pilih folder dulu!", true); return; }
    if (!text && !files.length) { showNotification("Tidak ada konten!", true); return; }

    try {
        showNotification("Sedang mengirim...");
        
        let result;
        if (files.length > 0) {
            // Jika ada file, gunakan FormData
            const formData = new FormData();
            formData.append("folder", folder);
            if (text) formData.append("text", text);
            for (let i = 0; i < files.length; i++) {
                formData.append(`file${i}`, files[i]);
            }

            result = await apiFetch(`${CONFIG.API_BASE}/api/manual_upload`, {
                method: "POST",
                body: formData // apiFetch perlu diupdate untuk tidak set Content-Type jika body adalah FormData
            });
        } else {
            // Jika hanya teks, gunakan JSON
            result = await apiFetch(`${CONFIG.API_BASE}/api/manual_upload`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ folder, text })
            });
        }

        if (result && result.success) {
            showNotification("Berhasil diunggah ke GitHub!");
            document.getElementById("textContent").value = "";
            fileInput.value = "";
            if (typeof updateFileName === 'function') updateFileName();
        } else {
            showNotification("Gagal mengunggah.", true);
        }
    } catch (e) {
        showNotification("Terjadi kesalahan sistem.", true);
    }
}

// Initial Load
document.addEventListener("DOMContentLoaded", async () => {
    const isDashboard = window.location.pathname.endsWith('index.html') || window.location.pathname === '/';

    if (isDashboard) {
        // --- SECURITY CHECKER ---
        try {
            const response = await fetch(`${CONFIG.API_BASE}/api/check_auth`);
            const data = await response.json();

            if (data && data.authenticated) {
                // RENDER DASHBOARD
                document.getElementById("loadingScreen").style.display = "none";
                document.getElementById("appContent").style.display = "block";
                loadFolders();
            } else {
                window.location.href = 'login.html';
            }
        } catch (e) {
            window.location.href = 'login.html';
        }
    }
});

function updateFileName() {
    const input = document.getElementById('fileInput');
    const display = document.getElementById('file-name-display');
    if (input.files.length > 0) {
        display.innerHTML = `📁 Terpilih: <b>${input.files.length} file</b>`;
    } else {
        display.innerHTML = '';
    }
}
