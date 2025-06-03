const express = require('express');
const router = express.Router();
const ShopResult = require('../models/ShopResult');

router.post('/save', async (req, res) => {
  const results = req.body;

  try {
    for (const result of results) {
      await ShopResult.create(result);
    }
    res.status(200).json({ message: "Results saved to MongoDB" });
  } catch (err) {
    console.error("❌ Mongo error:", err.message);
    res.status(500).json({ message: "Failed to save results", error: err.message });
  }
});

module.exports = router;
