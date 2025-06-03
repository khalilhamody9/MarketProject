const express = require('express');
const router = express.Router();
const { spawn } = require('child_process');
// routes/scrape.js
const { MongoClient } = require("mongodb");

const uri = "mongodb://localhost:27017";
const client = new MongoClient(uri);
const mongoose = require('mongoose');
const ScrapeResult = require('../models/ScrapeResult'); // ודא שזה הנתיב הנכון למודל שלך

// מסיר חנויות אונליין לפי מילות מפתח
function isOnlineStore(name) {
  const lower = name.toLowerCase();
  return lower.includes("אונליין") || lower.includes("online");
}

// GET /api/last_scrape?groupName=...
router.get('/last_scrape', async (req, res) => {
  const groupName = req.query.groupName;

  if (!groupName) {
    return res.status(400).json({ message: "Missing groupName" });
  }

  try {
    await client.connect();
    const db = client.db("market_db");
    const result = await db.collection("scraping_results")
      .find({ groupName })
      .sort({ timestamp: -1 })
      .limit(1)
      .toArray();

    if (result.length === 0) {
      return res.status(404).json({ message: "No data found for group" });
    }

    const scraping = result[0];

    const filteredShops = {};
    for (const [shopName, products] of Object.entries(scraping.shops)) {
      if (!/https?:\/\//.test(shopName)) {
        filteredShops[shopName] = products;
      }
    }

    res.json({
      message: "Success",
      groupName: scraping.groupName,
      city: scraping.city,
      location: scraping.location,
      timestamp: scraping.timestamp,
      total_shops: Object.keys(filteredShops).length,
      shops: filteredShops
    });
  } catch (err) {
    console.error("❌ Error fetching last scrape:", err);
    res.status(500).json({ message: "Server error", error: err.message });
  }
});


module.exports = router;

// פונקציה לבדוק אם חנות היא אונליין
function isOnlineShop(shopName) {
  return /https?:\/\//.test(shopName);
}

router.get("/scrape/last/:groupName", async (req, res) => {
  try {
    await client.connect();
    const db = client.db("market_db");
    const result = await db.collection("scraping_results")
      .find({ groupName: req.params.groupName })
      .sort({ timestamp: -1 })
      .limit(1)
      .toArray();

    if (result.length === 0) {
      return res.status(404).json({ message: "No results for this group" });
    }

    const scraping = result[0];
    const filteredShops = {};

    // סינון החנויות שלא אונליין
    for (const [shopName, products] of Object.entries(scraping.shops)) {
      if (!isOnlineShop(shopName)) {
        filteredShops[shopName] = products;
      }
    }

    res.json({
      message: "Success",
      groupName: scraping.groupName,
      city: scraping.city,
      location: scraping.location,
      timestamp: scraping.timestamp,
      total_shops: Object.keys(filteredShops).length,
      shops: filteredShops
    });
  } catch (err) {
    res.status(500).json({ message: "Error", error: err.message });
  }
});

module.exports = router;

router.post('/', async (req, res) => {
const { city, products, latitude, longitude, groupName } = req.body;

    console.log("📍 קיבלתי עיר:", city);
    console.log("🛒 מוצרים:", products);
    console.log("📡 מיקום:", { latitude, longitude });

    if (!city || !Array.isArray(products) || !latitude || !longitude) {
        return res.status(400).json({ message: 'Missing city, products or location' });
    }

const py = spawn('python', [
    'scripts/scraper.py',
    city,
    JSON.stringify(products),
    latitude.toString(),
    longitude.toString(),
    groupName // ✅ נוסף
], {
    env: { ...process.env, PYTHONIOENCODING: 'utf-8' }
});


    let data = '', error = '';

    py.stdout.on('data', chunk => data += chunk.toString('utf-8'));
    py.stderr.on('data', chunk => error += chunk.toString('utf-8'));

    py.on('close', async (code) => {
        if (code !== 0 || error) {
            return res.status(500).json({ message: 'Scraping failed', error });
        }

        try {
            const results = JSON.parse(data);
            res.status(200).json({ results });
        } catch (e) {
            res.status(500).json({ message: 'Invalid Python output', error: e.message });
        }
    });
});

module.exports = router;
