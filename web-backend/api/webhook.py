from flask import Flask, request, jsonify, send_from_directory, session, redirect, url_for
from flask_cors import CORS
import base64
import requests
import os
import json
from datetime import datetime

app = Flask(__name__)
CORS(app)
app.secret_key = os.getenv("SECRET_KEY", "postit-secret-2026")

GITHUB_USER = os.getenv("GITHUB_USER")
GITHUB_REPO = os.getenv("GITHUB_REPO")
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")
APP_PASSWORD = os.getenv("APP_PASSWORD")

import hashlib
import hmac

def generate_secure_token(folder_id):
    return hmac.new(APP_PASSWORD.encode(), folder_id.encode(), hashlib.sha256).hexdigest()[:16]

# --- KEAMANAN SERVER (ANTI-HACK) ---
FAILED_ATTEMPTS = {} # Mencatat salah password per IP
LOCKOUTS = {}        # Mencatat waktu blokir per IP

@app.route('/api/login', methods=['POST'])
def login_verify():
    ip = request.remote_addr
    now = datetime.now().timestamp()

    # Cek apakah IP sedang diblokir
    if ip in LOCKOUTS:
        if now < LOCKOUTS[ip]:
            remaining = int((LOCKOUTS[ip] - now) / 3600)
            return jsonify({"success": False, "message": f"Terblokir! Coba lagi dalam {remaining} jam."}), 403
        else:
            del LOCKOUTS[ip] # Blokir sudah habis

    data = request.get_json()
    if data.get("password") == APP_PASSWORD:
        FAILED_ATTEMPTS[ip] = 0
        session['logged_in'] = True
        return jsonify({"success": True, "message": "Login Berhasil"}), 200
    else:
        # Catat kegagalan
        FAILED_ATTEMPTS[ip] = FAILED_ATTEMPTS.get(ip, 0) + 1
        if FAILED_ATTEMPTS[ip] >= 3:
            LOCKOUTS[ip] = now + (12 * 3600) # Blokir 12 jam
            return jsonify({"success": False, "message": "Batas percobaan habis! Terblokir 12 jam."}), 403
        
        return jsonify({"success": False, "message": f"Password Salah! Sisa: {3 - FAILED_ATTEMPTS[ip]}"}), 401

@app.route('/api/get_link/<folder_id>', methods=['GET'])
def get_secure_link(folder_id):
    if request.headers.get("X-Password") != APP_PASSWORD:
        return jsonify({"success": False}), 401
    token = generate_secure_token(folder_id)
    return jsonify({"success": True, "link": f"{request.host_url}hook/{folder_id}/{token}"})

@app.route('/api/create_folder/<name>', methods=['POST'])
def create_new_folder(name):
    if request.headers.get("X-Password") != APP_PASSWORD:
        return jsonify({"success": False}), 401
    success = upload_to_github(name, ".gitkeep", "Folder Created")
    return jsonify({"success": success}), 201 if success else 500

@app.route('/api/delete_folder/<name>', methods=['DELETE'])
def delete_github_folder(name):
    if request.headers.get("X-Password") != APP_PASSWORD:
        return jsonify({"success": False}), 401
    
    try:
        # 1. Ambil daftar file di folder tersebut
        url = f"https://api.github.com/repos/{GITHUB_USER}/{GITHUB_REPO}/contents/{name}"
        headers = {"Authorization": f"token {GITHUB_TOKEN}"}
        res = requests.get(url, headers=headers)
        
        if res.status_code == 200:
            files = res.json()
            # 2. Hapus tiap file satu per satu
            for f in files:
                del_url = f"https://api.github.com/repos/{GITHUB_USER}/{GITHUB_REPO}/contents/{f['path']}"
                del_data = {"message": f"Delete folder {name}", "sha": f['sha']}
                requests.delete(del_url, headers=headers, json=del_data)
            
            return jsonify({"success": True}), 200
        return jsonify({"success": False, "error": "Folder tidak ditemukan"}), 404
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

def upload_to_github(folder, filename, content, is_base64=False):
    path = f"{folder}/{filename}"
    url = f"https://api.github.com/repos/{GITHUB_USER}/{GITHUB_REPO}/contents/{path}"
    headers = {"Authorization": f"token {GITHUB_TOKEN}", "Content-Type": "application/json"}
    existing = requests.get(url, headers=headers)
    data = {"message": f"Webhook upload {filename}", "content": content if is_base64 else base64.b64encode(content.encode()).decode()}
    if existing.status_code == 200:
        data["sha"] = existing.json()["sha"]
    response = requests.put(url, headers=headers, json=data)
    return response.status_code in [200, 201]

@app.route("/hook/<folder>/<token>", methods=["GET", "POST"])
def webhook_handler(folder, token):
    if token != generate_secure_token(folder):
        return jsonify({"success": False, "error": "Invalid Token"}), 403
    
    # CEK APAKAH FOLDER MASIH ADA DI GITHUB
    headers = {"Authorization": f"token {GITHUB_TOKEN}"}
    check_url = f"https://api.github.com/repos/{GITHUB_USER}/{GITHUB_REPO}/contents/{folder}"
    res = requests.get(check_url, headers=headers)
    
    print(f"DEBUG: Checking folder '{folder}' on GitHub. Status: {res.status_code}")

    if res.status_code != 200:
        print(f"DEBUG: Folder '{folder}' NOT FOUND. Returning 404.")
        return f"<div style='font-family:sans-serif; text-align:center; padding:50px;'><h2>Webhook Inactive</h2><p>Folder <b>{folder}</b> tidak ditemukan.</p></div>", 404
    
    # Jika dibuka lewat browser (GET)
    if request.method == "GET":
        return f"<div style='font-family:sans-serif; text-align:center; padding:50px;'><h2> Webhook Active</h2><p>Folder: <b>{folder}</b></p><p></p></div>", 200

    try:
        data = request.get_json() if request.is_json else {}
        files = request.files
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        uploaded = []

        # JIKA PENGIRIMAN JSON
        if data:
            # Jika ada kunci 'content' atau 'message', ambil itu saja
            # Jika tidak ada, simpan seluruh JSON-nya
            text_to_save = data.get("content") or data.get("message")
            
            if text_to_save:
                filename = f"msg_{timestamp}.txt"
                if upload_to_github(folder, filename, str(text_to_save)):
                    uploaded.append(filename)
            else:
                # Simpan seluruh JSON body
                filename = f"data_{timestamp}.json"
                if upload_to_github(folder, filename, json.dumps(data, indent=2)):
                    uploaded.append(filename)

        # JIKA PENGIRIMAN FILE
        for key in files:
            file = files[key]
            content_b64 = base64.b64encode(file.read()).decode()
            filename = file.filename or f"file_{timestamp}"
            if upload_to_github(folder, filename, content_b64, is_base64=True):
                uploaded.append(filename)

        return jsonify({"success": True, "folder": folder, "uploaded": uploaded}), 200
    except Exception as e:
        print(f"ERROR: {str(e)}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/folders", methods=["GET"])
def list_folders():
    if request.headers.get("X-Password") != APP_PASSWORD:
        return jsonify({"success": False}), 401
    try:
        # ...
        url = f"https://api.github.com/repos/{GITHUB_USER}/{GITHUB_REPO}/contents/"
        headers = {"Authorization": f"token {GITHUB_TOKEN}"}
        response = requests.get(url, headers=headers)
        if response.status_code == 200:
            folders = [item["name"] for item in response.json() if item["type"] == "dir"]
            return jsonify({"success": True, "folders": folders}), 200
        return jsonify({"success": False}), 500
    except: return jsonify({"success": False}), 500

@app.route("/api/files/<folder>", methods=["GET"])
def list_files(folder):
    if request.headers.get("X-Password") != APP_PASSWORD:
        return jsonify({"success": False}), 401
    try:
        url = f"https://api.github.com/repos/{GITHUB_USER}/{GITHUB_REPO}/contents/{folder}"
        headers = {"Authorization": f"token {GITHUB_TOKEN}"}
        response = requests.get(url, headers=headers)
        if response.status_code == 200:
            files = [{"name": item["name"], "path": item["path"], "download_url": item["download_url"], "sha": item["sha"]} for item in response.json() if item["type"] == "file"]
            return jsonify({"success": True, "files": files}), 200
        return jsonify({"success": False}), 404
    except: return jsonify({"success": False}), 500

@app.route("/api/delete/<path:filepath>", methods=["DELETE"])
def delete_file(filepath):
    if request.headers.get("X-Password") != APP_PASSWORD:
        return jsonify({"success": False}), 401
    try:
        url = f"https://api.github.com/repos/{GITHUB_USER}/{GITHUB_REPO}/contents/{filepath}"
        headers = {"Authorization": f"token {GITHUB_TOKEN}"}
        file_info = requests.get(url, headers=headers)
        if file_info.status_code != 200:
            return jsonify({"success": False}), 404
        sha = file_info.json()["sha"]
        delete_data = {"message": f"Delete {filepath}", "sha": sha}
        response = requests.delete(url, headers=headers, json=delete_data)
        return jsonify({"success": response.status_code == 200}), response.status_code
    except: return jsonify({"success": False}), 500

@app.route('/api/manual_upload', methods=['POST'])
def manual_upload():
    if request.headers.get("X-Password") != APP_PASSWORD:
        return jsonify({"success": False}), 401
    
    try:
        # Ambil data dari JSON atau Form-Data
        if request.is_json:
            data = request.get_json()
        else:
            data = request.form
            
        folder = data.get("folder")
        text = data.get("text")
        
        if not folder:
            return jsonify({"success": False, "error": "Folder name is required"}), 400
            
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        uploaded = []
        
        if text:
            filename = f"manual_{timestamp}.txt"
            if upload_to_github(folder, filename, text):
                uploaded.append(filename)
                
        # Handle files jika ada (dari form-data)
        if request.files:
            for key in request.files:
                file = request.files[key]
                content_b64 = base64.b64encode(file.read()).decode()
                filename = file.filename or f"file_{timestamp}"
                if upload_to_github(folder, filename, content_b64, is_base64=True):
                    uploaded.append(filename)
                    
        return jsonify({"success": True, "uploaded": uploaded}), 200
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/check_auth', methods=['GET'])
def check_auth():
    return jsonify({"authenticated": session.get('logged_in', False)}), 200

# --- SERVING FRONTEND ---
BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
FRONTEND_DIR = os.path.join(BASE_DIR, 'web-frontend')

@app.route('/')
def serve_index():
    if not session.get('logged_in'):
        return redirect('/login.html') # Kick ke login di sisi SERVER
    return send_from_directory(FRONTEND_DIR, 'index.html')

@app.route('/api/logout', methods=['POST'])
def api_logout():
    session.clear()
    return jsonify({"success": True}), 200

@app.route('/login.html')
def serve_login():
    if session.get('logged_in'):
        return redirect('/')
    return send_from_directory(FRONTEND_DIR, 'login.html')

@app.route('/<path:path>')
def serve_static(path):
    return send_from_directory(FRONTEND_DIR, path)

# Vercel entry point
def handler(event, context):
    return app(event, context)

if __name__ == "__main__":
    app.run(debug=True, host="0.0.0.0", port=5000)
