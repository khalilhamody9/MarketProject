# -*- coding: utf-8 -*-
import json
import sys
import io
from surprise import dump
from pymongo import MongoClient
from datetime import datetime

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

if len(sys.argv) != 2:
    print("❌ Usage: python predict_model.py <username>")
    exit()

username = sys.argv[1]

# Load model
_, model = dump.load("ml/svd_model")
if username not in model.trainset._raw2inner_id_users:
    print(f"⚠️ המשתמש '{username}' לא קיים ב־trainset", file=sys.stderr)

# MongoDB setup
client = MongoClient("mongodb://localhost:27017/")
db = client["market_db"]
history = db["histories"]
items_collection = db["items"]

# Load all items with barcodes
with open("data/Data.json", encoding="utf-8") as f:
    all_items_data = json.load(f)
    all_items_data = [item for item in all_items_data if item.get("barcode")]

# Build barcode set of purchased items
bought_barcodes = set()
last_purchase = {}

for h in history.find({"username": username, "action": "bought"}):
    item_name = h["itemName"]
    item = items_collection.find_one({"name": item_name})
    if item and item.get("barcode"):
        barcode = str(item["barcode"])
        bought_barcodes.add(barcode)
        date = h.get("date") or h.get("timestamp")
        if isinstance(date, str):
            date = datetime.fromisoformat(date)
        elif isinstance(date, dict) and "$date" in date:
            date = datetime.fromisoformat(date["$date"])
        if barcode not in last_purchase or date > last_purchase[barcode]:
            last_purchase[barcode] = date

now = datetime.now()
DAYS_LIMIT = 7  # ⏱️ after how many days an item can be suggested again

# Favorites (already bought)
favorite_items = []
for barcode, date in last_purchase.items():
    days_since = (now - date).days
    item_data = next((d for d in all_items_data if str(d.get("barcode")) == barcode), None)
    if item_data:
        score = 9.0
        if days_since > 10:
            score += 5.0
        favorite_items.append({
            "itemName": item_data["name"],
            "score": round(score, 2),
            "imageUrl": item_data.get("img", ""),
            "category": item_data.get("category", ""),
            "barcode": barcode,
            "reason": f"מוצר שנקנה בעבר (לפני {days_since} ימים)"
        })

# Predictions (only if not bought in last 7 days)
predicted_items = []
for item in all_items_data:
    barcode = str(item.get("barcode"))
    last_date = last_purchase.get(barcode)

    if last_date and (now - last_date).days < DAYS_LIMIT:
        continue  # ⛔ עדיין מוקדם מדי להמליץ עליו

    pred = model.predict(username, item["name"])
    if pred.est >= 6.0:
        item_doc = items_collection.find_one({"name": item["name"]})
        db_score = item_doc.get("score", 0) if item_doc else 0

        norm_pred = (pred.est - 1) / 9
        norm_db = min(db_score / 10, 1)
        combined_score = 1 + (0.85 * norm_pred + 0.15 * norm_db) * 9

        predicted_items.append({
            "itemName": item["name"],
            "score": round(combined_score, 2),
            "imageUrl": item.get("img", ""),
            "category": item.get("category", ""),
            "barcode": barcode,
            "reason": "המלצה חכמה – טרם נקנה או עבר זמן מאז הקנייה"
        })

# Combine and return top 10
combined = sorted(favorite_items, key=lambda x: -x["score"])[:5] + \
           sorted(predicted_items, key=lambda x: -x["score"])[:5]

combined = sorted(combined, key=lambda x: -x["score"])

try:
    json.dump(combined, sys.stdout, ensure_ascii=False)
except Exception as e:
    print("⚠️ Failed to dump JSON:", e, file=sys.stderr)
