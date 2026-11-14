"""Lädt eine Datei in einen Google-Drive-Ordner (ersetzt eine gleichnamige Datei dort).

Zugang über ein Google-Dienstkonto (JSON-Schlüssel in GDRIVE_SA_JSON).
Ist GDRIVE_IMPERSONATE gesetzt (Google Workspace mit domainweiter Delegierung), lädt das Dienstkonto
im Namen dieser Person hoch – die Datei gehört dann ihr und landet in ihrer „Meine Ablage“.
Ohne Delegierung muss der Ordner in einer geteilten Ablage liegen (Dienstkonten haben keinen eigenen Speicher).

Aufruf: python drive_upload.py <datei> <ordner-id>
"""
import json, os, sys
import requests
from google.oauth2 import service_account
from google.auth.transport.requests import Request

path, folder = sys.argv[1], sys.argv[2]
name = os.path.basename(path)
creds = service_account.Credentials.from_service_account_info(
    json.loads(os.environ["GDRIVE_SA_JSON"]), scopes=["https://www.googleapis.com/auth/drive"])
if os.environ.get("GDRIVE_IMPERSONATE"):
    creds = creds.with_subject(os.environ["GDRIVE_IMPERSONATE"])
creds.refresh(Request())
h = {"Authorization": f"Bearer {creds.token}"}
common = {"supportsAllDrives": "true"}

q = f"name = '{name}' and '{folder}' in parents and trashed = false"
found = requests.get("https://www.googleapis.com/drive/v3/files", headers=h,
                     params={**common, "q": q, "includeItemsFromAllDrives": "true", "fields": "files(id)"})
found.raise_for_status()
existing = found.json()["files"]

data = open(path, "rb").read()
mime = "application/vnd.android.package-archive"
if existing:
    r = requests.patch(f"https://www.googleapis.com/upload/drive/v3/files/{existing[0]['id']}",
                       headers={**h, "Content-Type": mime}, params={**common, "uploadType": "media"}, data=data)
else:
    meta = json.dumps({"name": name, "parents": [folder]})
    files = {"metadata": ("metadata", meta, "application/json; charset=UTF-8"), "file": (name, data, mime)}
    r = requests.post("https://www.googleapis.com/upload/drive/v3/files", headers=h,
                      params={**common, "uploadType": "multipart", "fields": "id,webViewLink"}, files=files)
if not r.ok:
    print(f"::error::Drive-Upload fehlgeschlagen ({r.status_code}): {r.text[:500]}")
    if "storageQuota" in r.text:
        print("::error::Dienstkonten haben keinen eigenen Speicher – Ordner in eine geteilte Ablage legen "
              "oder GDRIVE_IMPERSONATE mit domainweiter Delegierung nutzen (siehe Workflow-Datei).")
    sys.exit(1)
print(f"✅ {name} in Drive {'aktualisiert' if existing else 'hochgeladen'}")
