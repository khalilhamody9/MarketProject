package com.example.khalilo.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.khalilo.R;
import com.example.khalilo.adapter.ShopWithProductsAdapter;
import com.example.khalilo.entities.Item;
import com.example.khalilo.models.ShopWithProducts;
import com.example.khalilo.network.ApiService;
import com.example.khalilo.network.RetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ComparePricesActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private double latitude, longitude;
    private HashMap<Item, Integer> selectedItems;
    private String username, groupName;
    private RecyclerView shopRecyclerView;
    private TextView emptyMessage;
    private ProgressBar loadingSpinner; // or ImageView loadingLogo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare_prices); // 🔁 must come before findViewById

        emptyMessage = findViewById(R.id.emptyMessage);
        loadingSpinner = findViewById(R.id.loadingSpinner); // 🔁 fixed: after setContentView

        selectedItems = (HashMap<Item, Integer>) getIntent().getSerializableExtra("selectedItems");
        username = getIntent().getStringExtra("username");
        groupName = getIntent().getStringExtra("groupName");

        shopRecyclerView = findViewById(R.id.recyclerViewShops);
        shopRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        requestLocationPermissionAndFetch();
    }

    private void requestLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchUserLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchUserLocation();
        } else {
            Toast.makeText(this, "Permission denied. Cannot fetch location.", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchUserLocation() {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        Log.d("LocationCheck", "Lat: " + latitude + ", Lng: " + longitude);

                        List<Map<String, String>> productList = new ArrayList<>();
                        for (Item item : selectedItems.keySet()) {
                            if (selectedItems.get(item) > 0 && item.getBarcode() != null && !item.getBarcode().isEmpty()) {
                                Map<String, String> productData = new HashMap<>();
                                productData.put("name", item.getName().trim());
                                productData.put("barcode", item.getBarcode().trim());
                                productList.add(productData);
                            }
                        }

                        Geocoder geocoder = new Geocoder(this, new Locale("he"));
                        try {
                            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                            String detectedCity = null;

                            if (addresses != null && !addresses.isEmpty()) {
                                detectedCity = addresses.get(0).getLocality();
                                if (detectedCity != null) {
                                    detectedCity = cleanCityName(detectedCity);
                                }
                            }

                            final String cityFromGeo = detectedCity;

                            new AlertDialog.Builder(this)
                                    .setTitle("בחר עיר")
                                    .setMessage(cityFromGeo != null ?
                                            "זוהה עיר: " + cityFromGeo + "\nהאם להשתמש בה?" :
                                            "לא הצלחנו לזהות עיר אוטומטית. בחר עיר מהרשימה:")
                                    .setPositiveButton(cityFromGeo != null ? "כן, השתמש" : null, (dialog, which) -> {
                                        if (cityFromGeo != null) {
                                            sendScrapeRequestWithRetrofit(cityFromGeo, productList);
                                        }
                                    })
                                    .setNegativeButton("בחר עיר אחרת", (dialog, which) -> {
                                        showCitySelectionDialog(productList);
                                    })
                                    .setCancelable(false)
                                    .show();

                        } catch (IOException e) {
                            Log.e("SCRAPE_CITY", "\u274C שגיאה ב‏-Geocoder: " + e.getMessage());
                            showCitySelectionDialog(productList);
                        }
                    } else {
                        Toast.makeText(this, "Location is null", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showCitySelectionDialog(List<Map<String, String>> productList) {
        String[] cities = { "תל אביב", "ירושלים", "חיפה", "באר שבע", "אשדוד", "אשקלון", "ראשון לציון",
                "פתח תקווה", "רמת גן", "נתניה", "הרצליה", "כפר סבא", "הוד השרון", "רעננה",
                "אילת", "טבריה", "נצרת", "עפולה", "נהריה", "קריית שמונה", "מודיעין", "קרית גת" };
        new AlertDialog.Builder(this)
                .setTitle("בחר עיר להשוואת מחירים")
                .setItems(cities, (dialog, which) -> {
                    String selectedCity = cities[which];
                    Log.d("SCRAPE_CITY", "🏩 עיר שנבחרה ידנית: " + selectedCity);
                    if (latitude != 0 && longitude != 0) {
                        sendScrapeRequestWithRetrofit(selectedCity, productList);
                    } else {
                        Toast.makeText(this, "⚠️ מיקום לא תקין, נסה שוב מאוחר ", Toast.LENGTH_LONG).show();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void sendScrapeRequestWithRetrofit(String city, List<Map<String, String>> products) {
        if (products == null || products.isEmpty()) {
            Toast.makeText(this, "⚠️ לא נבחרו מוצרים תקפים, נטען את הסריקה האחרונה.", Toast.LENGTH_LONG).show();
            fetchLastScrape();
            return;
        }

        loadingSpinner.setVisibility(View.VISIBLE); // ✅ Show spinner before request

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        Map<String, Object> body = new HashMap<>();
        body.put("city", city);
        body.put("products", products);
        body.put("latitude", latitude);
        body.put("longitude", longitude);
        body.put("groupName", groupName);

        Log.d("SCRAPE_CITY", "📬 שולח לעיר: " + city); // ✅ Restored log line

        apiService.runScraping(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                loadingSpinner.setVisibility(View.GONE); // ✅ Hide on response

                if (response.isSuccessful() && response.body() != null) {
                    new Thread(() -> {
                        try {
                            String jsonResult = response.body().string();
                            Log.d("SCRAPE_RESULT", "📦 JSON: " + jsonResult);

                            runOnUiThread(() -> {
                                Toast.makeText(ComparePricesActivity.this, "📊 תוצאות התקבלו!", Toast.LENGTH_SHORT).show();
                                fetchLastScrape();
                            });

                        } catch (IOException e) {
                            Log.e("SCRAPE_RESULT", "❌ שגיאה בקריאת JSON: " + e.getMessage());
                            runOnUiThread(() -> Toast.makeText(ComparePricesActivity.this, "שגיאה בעיבוד תוצאות", Toast.LENGTH_SHORT).show());
                        }
                    }).start();
                } else {
                    Log.e("SCRAPE", "❌ תגובה נכשלה: " + response.code());
                    Toast.makeText(ComparePricesActivity.this, "❌ שגיאה בקוד תגובה", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                loadingSpinner.setVisibility(View.GONE); // ✅ Hide on failure
                Log.e("SCRAPE", "❌ שגיאה ברשת: " + t.getMessage());
                Toast.makeText(ComparePricesActivity.this, "⚠️ תקלה: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchLastScrape() {
        loadingSpinner.setVisibility(View.VISIBLE); // ✅ Show spinner

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        apiService.getLastScrape(groupName).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                loadingSpinner.setVisibility(View.GONE); // ✅ Hide spinner

                if (response.isSuccessful() && response.body() != null) {
                    Object shopsObj = response.body().get("shops");

                    if (shopsObj instanceof Map) {
                        Map<String, List<Map<String, Object>>> shopsMap = (Map<String, List<Map<String, Object>>>) shopsObj;
                        List<ShopWithProducts> shopList = new ArrayList<>();

                        for (Map.Entry<String, List<Map<String, Object>>> entry : shopsMap.entrySet()) {
                            String shopName = entry.getKey();
                            if (!shopName.contains("אונליין") && !shopName.toLowerCase().contains("online")) {
                                ShopWithProducts shop = new ShopWithProducts();
                                shop.setShopKey(shopName);

                                List<ShopWithProducts.Product> products = new ArrayList<>();
                                double total = 0;

                                for (Map<String, Object> productData : entry.getValue()) {
                                    String name = (String) productData.get("שם המוצר");
                                    String priceStr = (String) productData.get("מחיר");
                                    String sale = (String) productData.get("מבצע");

                                    double price = 0;
                                    try {
                                        price = Double.parseDouble(priceStr.replace("₪", "").trim());
                                    } catch (Exception ignored) {}

                                    total += price;
                                    products.add(new ShopWithProducts.Product(name, priceStr, sale));
                                }

                                shop.setProducts(products);
                                shop.setTotalPrice(total);
                                shopList.add(shop);
                            }
                        }

                        // ✅ בדיקה והודעה למשתמש
                        if (shopList.isEmpty()) {
                            Toast.makeText(ComparePricesActivity.this, "⚠️ לא נמצאו תוצאות. מוצגת סריקה אחרונה ללא חנויות פעילות.", Toast.LENGTH_LONG).show();
                        }

                        showShopResults(shopList); // <- הצגת התוצאות
                    }
                } else {
                    Toast.makeText(ComparePricesActivity.this, "שגיאה בנתונים מהשרת", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                loadingSpinner.setVisibility(View.GONE); // ✅ Hide on error

                Toast.makeText(ComparePricesActivity.this, "שגיאה בטעינת חנויות", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private ShopWithProducts parseShopWithProducts(Map<String, Object> map) {
        ShopWithProducts shop = new ShopWithProducts();
        shop.setShopKey((String) map.get("shopKey"));

        Object totalPriceObj = map.get("total_price");
        if (totalPriceObj instanceof Number) {
            shop.setTotalPrice(((Number) totalPriceObj).doubleValue());
        }

        List<ShopWithProducts.Product> productList = new ArrayList<>();
        Object productsObj = map.get("products");
        if (productsObj instanceof List) {
            for (Object productObj : (List<?>) productsObj) {
                if (productObj instanceof Map) {
                    Map<String, Object> p = (Map<String, Object>) productObj;
                    ShopWithProducts.Product product = new ShopWithProducts.Product(
                            (String) p.get("שם המוצר"),
                            (String) p.get("מחיר"),
                            (String) p.get("מבצע")
                    );
                    productList.add(product);
                }
            }
        }

        shop.setProducts(productList);
        return shop;
    }

    private String cleanCityName(String cityName) {
        if (cityName == null) return "";
        return cityName.trim().replace("־", "-");
    }

    private void showShopResults(List<ShopWithProducts> shopList) {
        ShopWithProductsAdapter adapter = new ShopWithProductsAdapter(this, shopList);
        shopRecyclerView.setAdapter(adapter);
    }
}