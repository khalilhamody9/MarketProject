const express = require('express');
const router = express.Router();
const itemController = require('../controllers/item.controller');

// Get Unbought Items
router.get('/unboughtItems', itemController.getUnboughtItems);

// Update Item Status by Name
router.put('/updateItemStatus', itemController.updateItemStatus);

// Get History of Actions (Sorted by Timestamp Descending)
router.get('/history', itemController.getFullHistory); 

// Add History Entry
router.post('/history', itemController.addHistory);

// Get History by Group (Sorted by Timestamp Descending)
router.get('/history/:groupName', itemController.getHistoryByGroup);

// Popular Finalized Items
router.get('/finalized-popular/:groupName', itemController.getPopularFinalizedItems);

// Recommendations
router.get('/recommendations/:groupName', itemController.getRecommendations);
router.get('/recommendations-smart/:username', itemController.getSmartRecommendations);
router.post('/increase-score', itemController.increaseRecommendationScore);

// Get items from file (full or paginated)
router.get('/from-file', itemController.getItemsFromFile);
router.get('/from-file-paginated', itemController.getPaginatedItemsFromFile);
router.get('/products', itemController.getPaginatedItemsFromFile);
router.get('/search', itemController.searchItems);
router.get('/recommendations-unified/:username', itemController.getUnifiedRecommendations);

module.exports = router;
