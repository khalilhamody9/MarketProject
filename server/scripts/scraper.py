# -*- coding: utf-8 -*-
import sys
import json
import time
import re
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from collections import defaultdict
from pymongo import MongoClient

def normalize(text):
    text = re.sub(r'["“”׳״]', '', text)
    text = re.sub(r'[^\w\s,]', '', text)
    text = re.sub(r'\s+', ' ', text)
    return text.strip().lower()

def run_scraper(city, products, user_location, group_name):
    chrome_options = Options()
    chrome_options.add_argument("--ignore-certificate-errors")
    chrome_options.add_argument("--ignore-ssl-errors")
    chrome_options.add_argument("--no-sandbox")

    driver = webdriver.Chrome(options=chrome_options)
    all_results = []

    try:
        driver.get("https://chp.co.il")
        time.sleep(3)
        driver.find_element(By.ID, "shopping_address").send_keys(city)
        time.sleep(1)
        driver.find_element(By.ID, "shopping_address").send_keys(Keys.RETURN)
        time.sleep(2)

        for product in products:
            name = product.get("name", "")
            barcode = str(product.get("barcode", "")).split(".")[0]

            product_input = driver.find_element(By.ID, "product_name_or_barcode")
            product_input.clear()
            product_input.send_keys(barcode)
            time.sleep(1)
            product_input.send_keys(Keys.RETURN)

            driver.find_element(By.ID, "get_compare_results_button").click()
            time.sleep(6)

            try:
                rows = driver.find_elements(By.CSS_SELECTOR, "#compare_results table tbody tr")
            except Exception:
                continue

            for row in rows:
                cols = row.find_elements(By.TAG_NAME, "td")
                if len(cols) >= 5:
                    chain = cols[0].text.strip()
                    store_name = cols[1].text.strip()
                    address = cols[2].text.strip()
                    sale = cols[3].text.strip()
                    price = cols[4].text.strip()

                    if city not in address:
                        address = f"{address}, {city}"
                    if address.strip().split()[-1].replace('.', '', 1).isdigit():
                        address = " ".join(address.strip().split()[:-1])

                    all_results.append({
                        "שם": name,
                        "ברקוד": barcode,
                        "רשת": chain,
                        "שם החנות": store_name,
                        "כתובת": address,
                        "מבצע": sale,
                        "מחיר": price
                    })

    finally:
        driver.quit()

    if not all_results:
        # עדיין שומרים עם רשימה ריקה
        client = MongoClient("mongodb://localhost:27017/")
        db = client["market_db"]
        collection = db["scraping_results"]

        collection.insert_one({
            "groupName": group_name,
            "city": city,
            "location": {
                "latitude": user_location[0],
                "longitude": user_location[1]
            },
            "shops": {},
            "timestamp": time.time()
        })

        print(json.dumps({
            "message": "לא נמצאו תוצאות",
            "total_shops": 0,
            "shops": []
        }, ensure_ascii=False), flush=True)
        return


    shop_to_products = defaultdict(list)
    for entry in all_results:
        shop_key = f"{entry['רשת']} - {entry['שם החנות']} - {entry['כתובת']}"
        shop_to_products[shop_key].append({
            "שם המוצר": entry["שם"],
            "מחיר": entry["מחיר"],
            "מבצע": entry["מבצע"]
        })

    # Save to MongoDB
    client = MongoClient("mongodb://localhost:27017/")
    db = client["market_db"]
    collection = db["scraping_results"]

    collection.insert_one({
        "groupName": group_name,
        "city": city,
        "location": {
            "latitude": user_location[0],
            "longitude": user_location[1]
        },
        "shops": dict(shop_to_products),
        "timestamp": time.time()
    })

    print(json.dumps({
        "message": "Success",
        "total_shops": len(shop_to_products),
        "shops": list(shop_to_products.keys())
    }, ensure_ascii=False), flush=True)

if __name__ == "__main__":
    try:
        city = sys.argv[1]
        products = json.loads(sys.argv[2])
        latitude = float(sys.argv[3])
        longitude = float(sys.argv[4])
        group_name = sys.argv[5]
        user_location = (latitude, longitude)
        run_scraper(city, products, user_location, group_name)
    except Exception as e:
        print(json.dumps({"message": "Scraping failed", "error": str(e)}), flush=True)
        sys.exit(0)