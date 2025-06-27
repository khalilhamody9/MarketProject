# ml/train_model.py

from surprise import SVD, Dataset, Reader, dump
from pymongo import MongoClient
import pandas as pd
from collections import Counter

# Connect to MongoDB
client = MongoClient("mongodb://localhost:27017/")
db = client["market_db"]
history = db["histories"]

# Count ratings
ratings_counter = Counter()
for doc in history.find():
    key = (doc["username"], doc["itemName"])
    ratings_counter[key] += 1

# Build data rows from the counter
data_rows = [(user, item, count) for (user, item), count in ratings_counter.items()]

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

# Save model
dump.dump("ml/svd_model", algo=model)
print("✅ Model trained and saved as 'ml/svd_model'")
