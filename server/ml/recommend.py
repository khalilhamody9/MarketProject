import sys, json
from collections import defaultdict
from datetime import datetime
from datetime import timezone

def parse_items(data):
    parsed = []
    for item in data:
        try:
            item['timestamp'] = datetime.fromisoformat(item['timestamp'].replace('Z', '+00:00'))
            parsed.append(item)
        except:
            continue
    return parsed

def recommend(group_name, items):
    now = datetime.now(timezone.utc)
    group_items = [i for i in items if i['groupName'] == group_name]
    other_items = [i for i in items if i['groupName'] != group_name]

    last_purchased = {}
    for item in group_items:
        name = item['itemName']
        if name not in last_purchased or item['timestamp'] > last_purchased[name]:
            last_purchased[name] = item['timestamp']

    global_counts = defaultdict(int)
    for item in other_items:
        global_counts[item['itemName']] += 1

    suggestions = []
    for name, count in global_counts.items():
        last_date = last_purchased.get(name)
        days_since = (now - last_date).days if last_date else None
        if days_since is None or days_since > 14:  # לא נקנה זמן רב או מעולם
            suggestions.append({
                "itemName": name,
                "reason": "מוצר פופולרי שלא נרכש לאחרונה",
                "lastPurchased": last_date.isoformat() if last_date else "מעולם לא",
                "globalUsage": count
            })

    return sorted(suggestions, key=lambda x: -x['globalUsage'])[:10]

def main():
    try:
        group_name = sys.argv[1]
        data = json.loads(sys.stdin.read())
        items = parse_items(data)
        recommendations = recommend(group_name, items)
        print(json.dumps(recommendations, ensure_ascii=False))
    except Exception as e:
        import traceback
        print("Python crashed:\n", traceback.format_exc(), file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
