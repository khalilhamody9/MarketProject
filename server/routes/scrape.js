const express = require('express');
const router = express.Router();
const { spawn } = require('child_process');
const { MongoClient } = require("mongodb");

const uri = "mongodb://localhost:27017";
const client = new MongoClient(uri);

// נרמול ברקוד
function normalizeBarcode(barcode) {
  return typeof barcode === 'string' ? barcode.trim() : barcode;
}

async function getCachedProducts(city, groupName, productBarcodes) {
  const db = client.db("market_db");
  const result = await db.collection("scraping_results")
    .find({ groupName, city })
    .sort({ timestamp: -1 })
    .limit(1)
    .toArray();

  if (!result.length) return { cached: {}, toScrape: productBarcodes };

  const cached = {}; // key: shopKey, value: array of products
  const seenBarcodesPerShop = {}; // key: shopKey, value: Set of barcodes
  const globalSeenBarcodes = new Set(); // להימנע מסריקה כפולה

  for (const shop of result[0].shops) {
    const key = `${shop.name} - ${shop.address}`;

    if (!cached[key]) {
      cached[key] = [];
      seenBarcodesPerShop[key] = new Set();
    }

    for (const p of shop.products) {
      const barcode = normalizeBarcode(p.barcode);

      // הוספה רק אם זה מוצר שרלוונטי וגם לא קיים בחנות הזו
      if (productBarcodes.includes(barcode) && !seenBarcodesPerShop[key].has(barcode)) {
        cached[key].push(p);
        seenBarcodesPerShop[key].add(barcode);
        globalSeenBarcodes.add(barcode); // שייך לכל הקאש
      }
    }
  }

  const toScrape = productBarcodes.filter(b => !globalSeenBarcodes.has(b));
  return { cached, toScrape };
}

router.post('/', async (req, res) => {
  const { city, products, latitude, longitude, groupName } = req.body;
  const db = client.db("market_db");

  if (!city || !Array.isArray(products) || !latitude || !longitude) {
    return res.status(400).json({ message: 'Missing city, products or location' });
  }

  const barcodeToProduct = {};
  const barcodes = [];

  for (const p of products) {
    const barcode = normalizeBarcode(p.barcode);
    if (barcode) {
      barcodeToProduct[barcode] = p;
      barcodes.push(barcode);
    }
  }

  const { cached, toScrape } = await getCachedProducts(city, groupName, barcodes);

  // אם אין צורך בסריקה חדשה – השתמש רק במטמון
  if (toScrape.length === 0) {
    console.log("✅ כל המוצרים קיימים במטמון");

    const shopsArray = Object.entries(cached).map(([shopKey, products]) => {
      const [name, address] = shopKey.split(" - ");
      const total = products.reduce(
        (sum, p) => sum + (parseFloat(p.price?.replace(/[^\d.]/g, '')) || 0), 0
      );

      return {
        name,
        address,
        total,
        products
      };
    }).filter(shop => shop.products.length > 0);

    return res.status(200).json({ shops: shopsArray, fromCache: true });
  }

  // סריקה חדשה דרך פייתון
  const scrapeProducts = toScrape.map(b => barcodeToProduct[b]);
  const py = spawn('python', [
    'scripts/scraper.py',
    city,
    JSON.stringify(scrapeProducts),
    latitude.toString(),
    longitude.toString(),
    groupName
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
      const result = JSON.parse(data);
      const newShops = result.shops || {};

      const merged = {};
      const barcodePerShop = {};

      // מיזוג חכם – כולל שמירה על מוצרים קודמים
const mergeShopProducts = (source) => {
  for (const [rawShopKey, products] of Object.entries(source)) {
    // 🔄 פיצול shopKey
    const [name, address] = rawShopKey.split(" - ");
    const shopKey = `${name.trim()} - ${address.trim()}`;

    merged[shopKey] = merged[shopKey] || [];
    barcodePerShop[shopKey] = barcodePerShop[shopKey] || new Set();

    for (const p of products) {
      const barcode = normalizeBarcode(p.barcode);
      if (barcode && !barcodePerShop[shopKey].has(barcode)) {
        merged[shopKey].push(p);
        barcodePerShop[shopKey].add(barcode);
      }
    }
  }
};

      mergeShopProducts(cached);
      mergeShopProducts(newShops);

      // המרה למערך עם ניקוי ו־total
      const shopsArray = Object.entries(merged).map(([shopKey, products]) => {
        const [name, address] = shopKey.split(" - ");
        const total = products.reduce((sum, p) =>
          sum + (parseFloat(p.price?.replace(/[^\d.]/g, '')) || 0), 0
        );

        const cleanProducts = products.map(p => ({
          name: p["שם המוצר"] || p.name,
          price: p["מחיר"] || p.price || "",
          discount: p["מבצע"] || p.discount || "",
          barcode: parseInt(p.barcode)
        }));

        return { name, address, total, products: cleanProducts };
      }).filter(shop => shop.products.length > 0);

      await db.collection("scraping_results").insertOne({
        groupName,
        city,
        location: { latitude, longitude },
        shops: shopsArray,
        timestamp: new Date()
      });

      return res.status(200).json({ shops: shopsArray, fromCache: false });

    } catch (e) {
      return res.status(500).json({ message: 'Invalid Python output', error: e.message });
    }
  });
});



module.exports = router;
router.get('/last_scrape', async (req, res) => {
  const groupName = req.query.groupName;
  if (!groupName) return res.status(400).json({ message: "Missing groupName" });

  try {
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

    res.json({
      message: "Success",
      groupName: scraping.groupName,
      city: scraping.city,
      location: scraping.location,
      timestamp: scraping.timestamp,
      shops: scraping.shops
    });

  } catch (err) {
    res.status(500).json({ message: "Server error", error: err.message });
  }
});

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

// async function getCachedProducts(city, groupName, productBarcodes) {
//     const db = client.db("market_db");
//     const result = await db.collection("scraping_results")
//         .find({ groupName, city })
//         .sort({ timestamp: -1 })
//         .limit(1)
//         .toArray();

//     if (!result.length) return { cached: {}, toScrape: productBarcodes };

//     const shopData = result[0].shops;
//     const cached = {};
//     const alreadySeen = new Set();

//     for (const [shopName, products] of Object.entries(shopData)) {
//         for (const p of products) {
//             const barcode = p.barcode?.trim();
//             if (barcode && productBarcodes.includes(barcode) && !alreadySeen.has(barcode)) {
//                 cached[barcode] = p;
//                 alreadySeen.add(barcode);
//             }
//         }
//     }

//     const toScrape = productBarcodes.filter(b => !alreadySeen.has(b));
//     return { cached, toScrape };
// }

// router.post('/', async (req, res) => {
// const { city, products, latitude, longitude, groupName } = req.body;

//     console.log("📍 קיבלתי עיר:", city);
//     console.log("🛒 מוצרים:", products);
//     console.log("📡 מיקום:", { latitude, longitude });

//     if (!city || !Array.isArray(products) || !latitude || !longitude) {
//         return res.status(400).json({ message: 'Missing city, products or location' });
//     }
// const barcodes = products.map(p => p.barcode?.toString().trim()).filter(Boolean);
// const { cached, toScrape } = await getCachedProducts(city, groupName, barcodes);

// if (toScrape.length === 0) {
//   return res.status(200).json({ shops: cached, fromCache: true });
// }

// const py = spawn('python', [
//     'scripts/scraper.py',
//     city,
//     JSON.stringify(products.filter(p => toScrape.includes(p.barcode?.trim()))),
//     latitude.toString(),
//     longitude.toString(),
//     groupName // ✅ נוסף
// ], {
//     env: { ...process.env, PYTHONIOENCODING: 'utf-8' }
// });


//     let data = '', error = '';

//     py.stdout.on('data', chunk => data += chunk.toString('utf-8'));
//     py.stderr.on('data', chunk => error += chunk.toString('utf-8'));

//     py.on('close', async (code) => {
//         if (code !== 0 || error) {
//             return res.status(500).json({ message: 'Scraping failed', error });
//         }

//         try {
//             const results = JSON.parse(data);
//             res.status(200).json({ results });
//         } catch (e) {
//             res.status(500).json({ message: 'Invalid Python output', error: e.message });
//         }
//     });
// });

module.exports = router;