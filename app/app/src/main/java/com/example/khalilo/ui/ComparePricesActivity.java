package com.example.khalilo.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
    private ProgressBar loadingSpinner;
    private boolean showOnlyFavorites = true;

    Button buttonDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare_prices);

        emptyMessage = findViewById(R.id.emptyMessage);
        loadingSpinner = findViewById(R.id.loadingSpinner);
        selectedItems = (HashMap<Item, Integer>) getIntent().getSerializableExtra("selectedItems");
        username = getIntent().getStringExtra("username");
        groupName = getIntent().getStringExtra("groupName");

        shopRecyclerView = findViewById(R.id.recyclerViewShops);
        shopRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        Button toggleViewMode = findViewById(R.id.toggleViewMode);
        toggleViewMode.setOnClickListener(v -> {
            showOnlyFavorites = !showOnlyFavorites;
            fetchLastScrape();
        });
        buttonDone = findViewById(R.id.buttonDone);

        buttonDone.setOnClickListener(v -> {
            Intent intent = new Intent(ComparePricesActivity.this, GroupDetailsActivity.class);
            intent.putExtra("username", username);
            intent.putExtra("groupName", groupName);
           // intent.putExtra("selectedItems", selectedItems); // אם זה Serializable
            startActivity(intent);
            finish();
        });
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.categoryBathroom));

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

                        List<Map<String, Object>> productList = new ArrayList<>();
                        for (Item item : selectedItems.keySet()) {
                            if (selectedItems.get(item) > 0 && item.getBarcode() > 0) {
                                Map<String, Object> productData = new HashMap<>();
                                productData.put("name", item.getName().trim());
                                productData.put("barcode", item.getBarcode()); // אין צורך ב־trim, כי זה מספר
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

                            String message = "בחר עיר מהרשימה לצורך השוואת מחירים.";

                            new AlertDialog.Builder(this)
                                    .setTitle("השוואת מחירים לפי עיר")
                                    .setMessage(message)
                                    .setPositiveButton("בחר עיר", (dialog, which) -> {
                                        showCitySelectionDialog(productList);
                                    })
                                    .setCancelable(false)
                                    .show();

                        } catch (IOException e) {
                            Log.e("SCRAPE_CITY", "❌ שגיאה ב-Geocoder: " + e.getMessage());
                            showCitySelectionDialog(productList);
                        }
                    } else {
                        Toast.makeText(this, "Location is null", Toast.LENGTH_LONG).show();
                    }
                });
    }


    private void showCitySelectionDialog(List<Map<String, Object>> productList) {
        String[] cities = {"תל אביב", "ירושלים", "חיפה", "באר שבע", "אשדוד", "אשקלון", "ראשון לציון",
                "פתח תקווה", "רמת גן", "נתניה", "הרצליה", "כפר סבא", "הוד השרון", "רעננה",
                "אילת", "טבריה", "נצרת", "עפולה", "נהריה", "קריית שמונה", "מודיעין", "קרית גת"};
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

    private void sendScrapeRequestWithRetrofit(String city, List<Map<String, Object>> products){

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
        loadingSpinner.setVisibility(View.VISIBLE);
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        apiService.getLastScrape(groupName).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                loadingSpinner.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    Object shopsObj = response.body().get("shops");
                    if (shopsObj instanceof List) {
                        List<Map<String, Object>> shopsList = (List<Map<String, Object>>) shopsObj;
                        List<ShopWithProducts> shopList = new ArrayList<>();

                        // ✅ רשימת הברקודים שבאמת נבחרו
                        List<Integer> requestedBarcodes = new ArrayList<>();
                        for (Item item : selectedItems.keySet()) {
                            if (selectedItems.get(item) > 0 && item.getBarcode() > 0) {
                                requestedBarcodes.add((int) item.getBarcode());
                            }
                        }

                        for (Map<String, Object> shopMap : shopsList) {
                            String name = (String) shopMap.get("name");
                            String address = (String) shopMap.get("address");
                            double total = ((Number) shopMap.get("total")).doubleValue();

                            List<Map<String, Object>> productsData = (List<Map<String, Object>>) shopMap.get("products");
                            List<ShopWithProducts.Product> products = new ArrayList<>();

                            for (Map<String, Object> productData : productsData) {
                                Object barcodeObj = productData.get("barcode");
                                if (barcodeObj instanceof Number) {
                                    int barcode = ((Number) barcodeObj).intValue();
                                    if (!requestedBarcodes.contains(barcode)) continue; // ❌ דלג אם לא ברשימת המוצרים הנבחרים
                                }

                                String pname = (String) productData.get("name");
                                String priceStr = (String) productData.get("price");
                                String discount = (String) productData.get("discount");
                                products.add(new ShopWithProducts.Product(pname, priceStr, discount));
                            }

                            if (!products.isEmpty()) {
                                ShopWithProducts shop = new ShopWithProducts();
                                shop.setShopKey(name + " - " + address);
                                shop.setTotalPrice(total); // שים לב שזה עדיין סוכם לפי כל המוצרים המקוריים
                                shop.setProducts(products);
                                shopList.add(shop);
                            }
                        }

                        if (shopList.isEmpty()) {
                            Toast.makeText(ComparePricesActivity.this, "⚠️ לא נמצאו תוצאות.", Toast.LENGTH_LONG).show();
                        }

                        if (showOnlyFavorites) {
                            fetchFavoriteStores(groupName, shopList);
                        } else {
                            showShopResults(shopList);
                        }
                    } else {
                        Toast.makeText(ComparePricesActivity.this, "שגיאה: פורמט תוצאות לא צפוי", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ComparePricesActivity.this, "שגיאה בנתונים מהשרת", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                loadingSpinner.setVisibility(View.GONE);
                Toast.makeText(ComparePricesActivity.this, "שגיאה בטעינת חנויות", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void fetchFavoriteStores(String groupName, List<ShopWithProducts> fullList) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        apiService.getFavoriteStores(groupName).enqueue(new Callback<Map<String, List<String>>>() {
            @Override
            public void onResponse(Call<Map<String, List<String>>> call, Response<Map<String, List<String>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<String> favoriteList = response.body().get("favoriteStores");
                    List<ShopWithProducts> filtered = new ArrayList<>();

                    for (ShopWithProducts shop : fullList) {
                        String shopKey = shop.getShopKey().toLowerCase().trim();

                        for (String favorite : favoriteList) {
                            String fav = favorite.toLowerCase().trim();
                            if (shopKey.contains(fav)) {
                                filtered.add(shop);
                                break;
                            }
                        }
                    }

                    showShopResults(filtered);  // ✅ הצג רק חנויות מועדפות
                } else {
                    showShopResults(fullList); // fallback במקרה שאין מועדפים או תגובה שגויה
                }
            }

            @Override
            public void onFailure(Call<Map<String, List<String>>> call, Throwable t) {
                showShopResults(fullList); // fallback במקרה של כשל רשת
            }
        });
    }


    private void showShopResults(List<ShopWithProducts> shopList) {
        ShopWithProductsAdapter adapter = new ShopWithProductsAdapter(this, shopList);
        shopRecyclerView.setAdapter(adapter);
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


}