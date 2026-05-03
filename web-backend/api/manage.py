import os
import requests
from flask import Flask, jsonify, request

app = Flask(__name__)

# --- CONFIG ---
GITHUB_TOKEN = os.environ.get("GITHUB_TOKEN")
GITHUB_OWNER = os.environ.get("GITHUB_OWNER")
GITHUB_REPO = os.environ.get("GITHUB_REPO")
APP_PASSWORD = os.environ.get("APP_PASSWORD")

@app.route('/api/manage', methods=['GET'])
def list_folders():
    # Cek Password
    client_pass = request.headers.get("X-Password")
    if client_pass != APP_PASSWORD:
        return jsonify({"status": "error", "message": "Unauthorized"}), 401

    try:
        url = f"https://api.github.com/repos/{GITHUB_OWNER}/{GITHUB_REPO}/contents/"
        headers = {
            "Authorization": f"token {GITHUB_TOKEN}",
            "Accept": "application/vnd.github.v3+json"
        }
        response = requests.get(url, headers=headers)
        items = response.json()
        
        # Ambil hanya yang bertipe 'dir' (folder)
        folders = [item['name'] for item in items if item['type'] == 'dir']
        
        return jsonify({"status": "success", "folders": folders})
    except Exception as e:
        return jsonify({"status": "success", "folders": []}) # Return empty if new repo

# Vercel handler
def handler(event, context):
    return app(event, context)
