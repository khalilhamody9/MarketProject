import csv
from geopy.distance import geodesic

# מיקום המשתמש (לדוגמה)
user_location = (32.085300, 34.781768)  # ת"א סנטר

def parse_coords(coord_str):
    try:
        lat, lon = map(float, coord_str.strip().split(","))
        return lat, lon
    except:
        return None

def load_shops(filename):
    shops = []
    with open(filename, newline='', encoding='utf-8') as csvfile:
        reader = csv.reader(csvfile)
        for row in reader:
            if len(row) < 2 or not row[1]:
                continue
            coords = parse_coords(row[1])
            if coords:
                distance = geodesic(user_location, coords).km
                shops.append({
                    "name": row[0],
                    "coords": row[1],
                    "city": row[2] if len(row) > 2 else "",
                    "distance_km": round(distance, 3)
                })
    return shops

def get_nearest_shops(filename, top_n=5):
    shops = load_shops(filename)
    sorted_shops = sorted(shops, key=lambda x: x["distance_km"])
    return sorted_shops[:top_n]

# דוגמה לשימוש
if __name__ == "__main__":
    nearest = get_nearest_shops("cordinates_final.csv")
    for shop in nearest:
        print(f"{shop['name']} | {shop['coords']} | {shop['distance_km']} ק\"מ")
