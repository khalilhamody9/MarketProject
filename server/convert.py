import csv
import time
from opencage.geocoder import OpenCageGeocode

API_KEY = '6121bcf423c44b948c9c7c8b26fb7f84'
geocoder = OpenCageGeocode(API_KEY)

input_file = 'multi_products_prices.csv'
output_file = 'clean_coordinates.csv'

coords_cache = {}

def normalize_address(addr):
    addr = addr.replace("ת\"א", "תל אביב").replace("ת״א", "תל אביב")
    addr = addr.replace("תל-אביב", "תל אביב")
    return addr.strip()

with open(input_file, newline='', encoding='utf-8') as infile, \
     open(output_file, 'w', newline='', encoding='utf-8-sig') as outfile:

    reader = csv.DictReader(infile)
    fieldnames = ['רשת', 'שם החנות', 'כתובת', 'עיר', 'lat', 'lon']
    writer = csv.DictWriter(outfile, fieldnames=fieldnames)
    writer.writeheader()

    for row in reader:
        network = row['רשת'].strip()
        store = row['שם החנות'].strip()
        address = normalize_address(row['כתובת'].strip())

        full_address = f"{address}, ישראל"
        if full_address in coords_cache:
            lat, lon = coords_cache[full_address]
        else:
            print(f"📍 גיאוקודינג לכתובת: {full_address}")
            try:
                results = geocoder.geocode(full_address)
                if results and len(results):
                    lat = results[0]['geometry']['lat']
                    lon = results[0]['geometry']['lng']
                    coords_cache[full_address] = (lat, lon)
                    time.sleep(1)
                else:
                    lat, lon = '', ''
            except Exception as e:
                print(f"❌ שגיאה בגיאוקודינג ל-{full_address}: {e}")
                lat, lon = '', ''

        city = row.get('כתובת', '').split(',')[-1].strip()
        writer.writerow({
            'רשת': network,
            'שם החנות': store,
            'כתובת': address,
            'עיר': city,
            'lat': lat,
            'lon': lon
        })
