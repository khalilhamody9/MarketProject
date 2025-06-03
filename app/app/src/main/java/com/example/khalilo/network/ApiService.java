package com.example.khalilo.network;

import com.example.khalilo.entities.Item;
import com.example.khalilo.models.GroupResponse;
import com.example.khalilo.models.History;
import com.example.khalilo.models.LoginResponse;
import com.example.khalilo.models.Product;
import com.example.khalilo.models.RecommendationResponse;
import com.example.khalilo.models.SignUpResponse;
import com.example.khalilo.models.StorePriceResult;

import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // Login Request
    @POST("users/login")
    Call<LoginResponse> loginUser(@Body Map<String, String> body);

    // Sign Up Request
    @POST("users/register")
    Call<SignUpResponse> registerUser(@Body Map<String, String> body);

    // Check Group Name
    @GET("groups/check/{groupName}")
    Call<GroupResponse> checkGroupName(@Path("groupName") String groupName);

    // Create Group
    @POST("groups/create")
    Call<GroupResponse> createGroup(@Body Map<String, String> body);

    // Check if User is in Group
    @GET("groups/checkUserInGroup/{username}/{groupName}")
    Call<GroupResponse> checkUserInGroup(@Path("username") String username, @Path("groupName") String groupName);

    // Check if User is Admin
    @GET("groups/checkAdmin")
    Call<Map<String, Boolean>> checkIfAdmin(@Query("username") String username, @Query("groupName") String groupName);

    // Add User to Group (After Approve)
    @POST("groups/addUserToGroup")
    Call<GroupResponse> addUserToGroup(@Body Map<String, String> body);

    // New Methods for Insert and Delete User
    @POST("groups/insertUser")
    Call<GroupResponse> insertUserToGroup(@Body Map<String, String> body);

    @GET("groups/getUserGroups/{username}")
    Call<GroupResponse> getUserGroups(@Path("username") String username);

    // Remove User from Group
    @POST("groups/deleteUserFromGroup")
    Call<GroupResponse> deleteUserFromGroup(@Body Map<String, String> body);

    // Add Group to User's Details
    @GET("items/unboughtItems")
    Call<List<Item>> getUnboughtItems();

    @PUT("items/markAsBought")
    Call<Void> markAsBought(@Body Item item);

    @GET("items/history")
    Call<List<History>> getHistory();

    @PUT("items/updateItemStatus")
    Call<Void> updateItemStatus(@Body Map<String, Object> body);

    @POST("items/history")
    Call<Void> addHistory(@Body Map<String, Object> body);

    @GET("items/history/{groupName}")
    Call<List<History>> getHistoryByGroup(@Path("groupName") String groupName);

    @GET("groups/{groupName}/members")
    Call<List<String>> getGroupMembers(@Path("groupName") String groupName);

    // In backend controller
    @GET("items/history/{groupName}")
    Call<List<History>> getHistoryByGroup(@Path("groupName") String groupName, @Query("sort") String sort);
    // Corrected Endpoint
// Delete User from Group
    @POST("groups/saveChanges")
    Call<Void> saveGroupChanges(@Body Map<String, Object> body);
    // Check if User is Admin
// Save Selected Items
    @POST("groups/saveSelectedItems")
    Call<Void> saveSelectedItems(@Body Map<String, Object> body);

    // Get Selected Items for Group
    @GET("groups/getSelectedItems/{groupName}")
    Call<Map<String, Integer>> getSelectedItems(@Path("groupName") String groupName);
    // Save Selected Items for Group
    @GET("items/finalized-popular/{groupName}")
    Call<List<History>> getPopularItemsByGroup(@Path("groupName") String groupName);
//    @GET("items/recommendations/{groupName}")
//    Call<List<History>> getRecommendations(@Path("groupName") String groupName);
    @GET("items/recommendations/{groupName}")
    Call<RecommendationResponse> getRecommendations(@Path("groupName") String groupName);
    @GET("prices/compare")
    Call<List<StorePriceResult>> comparePrices(
            @Query("groupName") String groupName,
            @Query("lat") double latitude,
            @Query("lng") double longitude
    );
    @GET("/api/products")
    Call<List<Item>> getAllItems();

    @POST("scrape")
    Call<ResponseBody> runScraping(@Body Map<String, Object> body);
    @GET("/api/shopProducts")
    Call<Map<String, List<Product>>> getShopWithProducts();
    @GET("shop_with_products.json")
    Call<Map<String, Object>> getShopProducts();
    @GET("/api/last_scrape")
    Call<Map<String, Object>> getLastScrape(@Query("groupName") String groupName);

}
