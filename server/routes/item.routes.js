const express = require('express');
const router = express.Router();
const itemController = require('../controllers/item.controller');
const recommendationController = require('../controllers/item.controller');

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
// router.get('/popular/:groupName', itemController.getPopularItems);
router.get('/finalized-popular/:groupName', itemController.getPopularFinalizedItems);
router.get('/recommendations/:groupName', itemController.getRecommendations);
router.get("/recommendations-smart/:username", recommendationController.getSmartRecommendations);
router.get('/from-file', itemController.getItemsFromFile);

module.exports = router;