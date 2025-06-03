import sys
import json
from collections import defaultdict

def get_top_5_popular_items(data):
    item_stats = defaultdict(lambda: {"quantity": 0, "category": "", "lastDate": "", "imageUrl": ""})

    for item in data:
        name = item.get("itemName")
        qty = item.get("quantity", 0)
        category = item.get("category", "")
        image = item.get("imageUrl", "")
        timestamp = item.get("timestamp", "")

        if name:
            item_stats[name]["quantity"] += qty
            item_stats[name]["category"] = category
            item_stats[name]["imageUrl"] = image
            item_stats[name]["lastDate"] = timestamp  # Optional: could track latest only if needed

    # Sort by quantity descending
    sorted_items = sorted(item_stats.items(), key=lambda x: x[1]["quantity"], reverse=True)
    top_5 = sorted_items[:5]

    # Format output
    return [
        {
            "itemName": name,
            "quantity": info["quantity"],
            "category": info["category"],
            "imageUrl": info["imageUrl"],
            "lastDate": info["lastDate"]
        }
        for name, info in top_5
    ]

def main():
    try:
        _ = sys.argv[1]  # we ignore groupName for global popularity
        raw_input = sys.stdin.read()
        data = json.loads(raw_input)

        results = get_top_5_popular_items(data)
        print(json.dumps(results))

    except Exception as e:
        import traceback
        print("❌ Python crashed:\n", traceback.format_exc(), file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
