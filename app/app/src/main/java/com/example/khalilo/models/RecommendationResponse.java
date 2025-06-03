// File: models/RecommendationResponse.java
package com.example.khalilo.models;

import java.util.List;

public class RecommendationResponse {
    private List<History> recommendations;

    public List<History> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<History> recommendations) {
        this.recommendations = recommendations;
    }
}
