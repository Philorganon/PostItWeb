import os
import base64
import time
import requests
from flask import Flask, request, jsonify, send_from_directory
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

# --- KONFIGURASI GITHUB (DIAMBIL DARI ENV VERCEL) ---
GITHUB_TOKEN = os.environ.get("GITHUB_TOKEN")
GITHUB_OWNER = os.environ.get("GITHUB_OWNER")
GITHUB_REPO = os.environ.get("GITHUB_REPO")
APP_PASSWORD = os.environ.get("APP_PASSWORD")

@app.route('/api/login', methods=['POST'])
def login_verify():
    data = request.get_json()
    client_pass = data.get("password")
    if client_pass == APP_PASSWORD:
        return jsonify({"success": True, "message": "Login Berhasil"}), 200
    return jsonify({"success": False, "message": "Password Salah"}), 401

def upload_to_github(folder, filename, content_bytes):
    url = f"https://api.github.com/repos/{GITHUB_OWNER}/{GITHUB_REPO}/contents/{folder}/{filename}"
    headers = {
        "Authorization": f"token {GITHUB_TOKEN}",
        "Accept": "application/vnd.github.v3+json"
    }
    
    encoded_content = base64.b64encode(content_bytes).decode('utf-8')
    data = {
        "message": f"Webhook push: {filename}",
        "content": encoded_content
    }
    
    response = requests.put(url, headers=headers, json=data)
    return response.json(), response.status_code

import hashlib
import hmac

def generate_secure_token(folder_id):
    # Membuat token unik berdasarkan folder_id dan password admin
    return hmac.new(APP_PASSWORD.encode(), folder_id.encode(), hashlib.sha256).hexdigest()[:16]

@app.route('/hook/<folder_id>/<token>', methods=['POST'])
def webhook(folder_id, token):
    # Verifikasi apakah token benar
    expected_token = generate_secure_token(folder_id)
    if token != expected_token:
        return jsonify({"success": False, "error": "Invalid Token"}), 403

    try:
        if 'file' in request.files:
            file = request.files['file']
            filename = f"{int(time.time())}_{file.filename}"
            res, status = upload_to_github(folder_id, filename, file.read())
            return jsonify({"success": True, "github": res}), status
            
        data = request.get_json(silent=True) or request.form
        content = data.get('content', 'No content')
        filename = data.get('filename', f"msg_{int(time.time())}.txt")
        
        if not filename.endswith('.txt') and '.' not in filename:
            filename += '.txt'
            
        res, status = upload_to_github(folder_id, filename, content.encode('utf-8'))
        return jsonify({"success": True, "github": res}), status
            
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

# Endpoint bantu untuk frontend mendapatkan token folder
@app.route('/api/get_link/<folder_id>', methods=['GET'])
def get_secure_link(folder_id):
    # Cek Auth (Hanya admin yang bisa minta link)
    client_pass = request.headers.get("X-Password")
    if client_pass != APP_PASSWORD:
        return jsonify({"success": False}), 401
    
    token = generate_secure_token(folder_id)
    return jsonify({"success": True, "link": f"{request.host_url}hook/{folder_id}/{token}"})

# --- SERVING FRONTEND LOKAL ---
# Mencari folder public secara dinamis
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FRONTEND_DIR = os.path.join(BASE_DIR, 'public')

@app.route('/')
def serve_index():
    return send_from_directory(FRONTEND_DIR, 'index.html')

@app.route('/<path:path>')
def serve_static(path):
    return send_from_directory(FRONTEND_DIR, path)

# Vercel entry point
def handler(event, context):
    return app(event, context)

if __name__ == "__main__":
    app.run(debug=True, host="0.0.0.0", port=5000)
