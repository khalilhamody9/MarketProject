from surprise import SVD, Dataset, Reader, dump
from pymongo import MongoClient
import pandas as pd
from collections import Counter

# Connect to MongoDB
client = MongoClient("mongodb://localhost:27017/")
db = client["market_db"]
history = db["histories"]
items = db["items"]

# Count ratings using (username, barcode)
ratings_counter = Counter()

for doc in history.find():
    item_doc = items.find_one({"name": doc["itemName"]})
    if not item_doc or not item_doc.get("barcode"):
        continue
    barcode = str(item_doc["barcode"])
    key = (doc["username"], barcode)
    ratings_counter[key] += 1

# Build data rows from the counter
data_rows = [(user, barcode, count) for (user, barcode), count in ratings_counter.items()]

if not data_rows:
    print("❌ No history data found to train on.")
    exit()

# Prepare DataFrame and train model
df = pd.DataFrame(data_rows, columns=["user", "item", "rating"])
reader = Reader(rating_scale=(1, 10))
data = Dataset.load_from_df(df, reader)

trainset = data.build_full_trainset()
model = SVD()
model.fit(trainset)

# Debug output
print("מספר משתמשים:", len(set([u for u, _, _ in data_rows])))
print("מספר פריטים:", len(set([i for _, i, _ in data_rows])))
print("דירוגים לדוגמה:")
for row in data_rows[:10]:
    print(row)

# Save model
dump.dump("ml/svd_model", algo=model)
print("✅ Model trained and saved as 'ml/svd_model'")
