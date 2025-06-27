# -*- coding: utf-8 -*-
import json
import sys
import io
from surprise import dump
from pymongo import MongoClient
from datetime import datetime

# ✅ תמיכה מלאה בעברית
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

if len(sys.argv) != 2:
    print("❌ Usage: python predict_model.py <username>")
    exit()

username = sys.argv[1]
# טעינת המודל
_, model = dump.load("ml/svd_model")

# חיבור ל-MongoDB
client = MongoClient("mongodb://localhost:27017/")
db = client["market_db"]
history = db["histories"]

# קריאת כל המוצרים
with open("data/Data.json", encoding="utf-8") as f:
    all_items_data = json.load(f)
    all_items = [item["name"] for item in all_items_data]

# בניית מילון של פריטים עם זמן הרכישה האחרון
last_purchase = {}
for h in history.find({"username": username, "action": "bought"}):
    item = h["itemName"]
    date = h.get("date") or h.get("timestamp")
    if isinstance(date, str):
        date = datetime.fromisoformat(date)
    elif isinstance(date, dict) and "$date" in date:
        date = datetime.fromisoformat(date["$date"])
    if item not in last_purchase or date > last_purchase[item]:
        last_purchase[item] = date

now = datetime.now()
user_items = set(last_purchase.keys())

# === חלק 1: מוצרים שהמשתמש אהב ===
favorite_items = []
for item, date in last_purchase.items():
    days_since = (now - date).days
    item_data = next((d for d in all_items_data if d["name"] == item), None)
    if item_data:
        score = 9.0
        if days_since > 14:
            score += 1.0  # יותר חשוב להמליץ אם עבר זמן
        favorite_items.append({
            "itemName": item,
            "score": round(score, 2),
            "imageUrl": item_data.get("img", ""),
            "category": item_data.get("category", ""),
            "reason": f"מוצר שנקנה בעבר (לפני {days_since} ימים)"
        })
# === חלק 2: המלצות חדשות לפי SVD ===
predicted_items = []
for item in all_items:
    if item not in user_items:
        pred = model.predict(username, item)
        if pred.est >= 6.0:
            item_data = next((d for d in all_items_data if d["name"] == item), None)
            if item_data:
                predicted_items.append({
                    "itemName": item,
                    "score": round(pred.est, 2),
                    "imageUrl": item_data.get("img", ""),
                    "category": item_data.get("category", ""),
                    "reason": "המלצה חכמה – טרם נקנה"
                })

# מיזוג 5 ישנים + 5 חדשים
combined = sorted(favorite_items, key=lambda x: -x["score"])[:5] + \
           sorted(predicted_items, key=lambda x: -x["score"])[:5]

# מיון סופי
combined = sorted(combined, key=lambda x: -x["score"])

# פלט
try:
    json.dump(combined, sys.stdout, ensure_ascii=False)
except Exception as e:
    print("⚠️ Failed to dump JSON:", e, file=sys.stderr)
