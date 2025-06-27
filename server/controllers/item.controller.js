const Item = require('../models/Item');
const History = require('../models/History');
const FinalizedItem = require('../models/FinalizedItem');
const { spawn } = require("child_process");
const Group = require('../models/Group');
const fs = require('fs');
const path = require('path');
async function increaseRecommendationScore(req, res) {
    const { itemName } = req.body;
    try {
        const item = await Item.findOne({ name: itemName });
        if (!item) return res.status(404).json({ message: "Item not found" });

        item.score = (item.score || 0) + 1;
        await item.save();

        res.status(200).json({ message: "Score updated", score: item.score });
    } catch (err) {
        console.error("Failed to update score:", err);
        res.status(500).json({ message: "Server error", error: err.message });
    }
}
async function getSmartRecommendationsInternal(username) {
    const userGroups = await Group.find({ members: username });
    const groupNames = userGroups.map(g => g.groupName);
    const allHistory = await History.find({});
    const now = new Date();
    const scoreMap = new Map();

    for (const entry of allHistory) {
        const key = entry.itemName;
        if (!scoreMap.has(key)) {
            scoreMap.set(key, {
                itemName: key,
                lastPurchased: entry.date,
                userCount: 0,
                groupCount: 0,
                globalCount: 0
            });
        }

        const data = scoreMap.get(key);
        if (entry.date > data.lastPurchased) {
            data.lastPurchased = entry.date;
        }

        if (entry.username === username) data.userCount++;
        else if (groupNames.includes(entry.groupName)) data.groupCount++;
        else data.globalCount++;
    }

    const itemNames = Array.from(scoreMap.keys());
    const dbItems = await Item.find({ name: { $in: itemNames } });
    const itemScoreMap = new Map();
    for (const dbItem of dbItems) {
        itemScoreMap.set(dbItem.name, dbItem.score || 0);
    }

    const scored = [];
    for (const item of scoreMap.values()) {
        let score = item.userCount * 0.5 + item.groupCount * 0.3 + item.globalCount * 0.2;
        const dbScore = itemScoreMap.get(item.itemName) || 0;
        score += dbScore * 0.4;

        const daysSince = (now - item.lastPurchased) / (1000 * 60 * 60 * 24);
        if (daysSince > 14) score += 1.5;

        scored.push({
            itemName: item.itemName,
            score: score.toFixed(2),
            reason: `נרכש ${item.userCount} פעמים אישית, ${item.groupCount} קבוצתי, ${item.globalCount} כללית`,
            daysSince: Math.floor(daysSince)
        });
    }

    const selectedMap = userGroups.reduce((map, group) => {
        for (const [item, qty] of group.selectedItems.entries()) {
            map[item] = true;
        }
        return map;
    }, {});

    return scored
        .filter(i => !selectedMap[i.itemName] && i.daysSince > 3)
        .sort((a, b) => b.score - a.score)
        .slice(0, 10);
}

// ----------------------------
// פונקציה פנימית 2: המלצות לפי משתמשים דומים
// ----------------------------
async function getCollaborativeRecommendationsInternal(username) {
    const allHistory = await History.find({});
    const userPurchaseMap = new Map();

    for (const h of allHistory) {
        if (!userPurchaseMap.has(h.username)) {
            userPurchaseMap.set(h.username, new Set());
        }
        userPurchaseMap.get(h.username).add(h.itemName);
    }

    const myItems = userPurchaseMap.get(username) || new Set();
    const similarUsers = [];

    for (const [otherUser, otherItems] of userPurchaseMap.entries()) {
        if (otherUser === username) continue;

        const intersection = new Set([...myItems].filter(x => otherItems.has(x)));
        const union = new Set([...myItems, ...otherItems]);
        const similarity = union.size === 0 ? 0 : intersection.size / union.size;

        if (similarity >= 0.3) {
            similarUsers.push({ username: otherUser, similarity, otherItems });
        }
    }

    const recommendedMap = new Map();
    for (const user of similarUsers) {
        for (const item of user.otherItems) {
            if (!myItems.has(item)) {
                recommendedMap.set(item, (recommendedMap.get(item) || 0) + 1);
            }
        }
    }

    return [...recommendedMap.entries()]
        .sort((a, b) => b[1] - a[1])
        .slice(0, 20)
        .map(([itemName, count]) => ({ itemName, score: count, reason: `נקנה ע"י ${count} משתמשים דומים` }));
}
async function getSmartRecommendations(req, res) {
    const { username } = req.params;

    try {
        const recommendations = await getSmartRecommendationsInternal(username);
        res.json({ recommendations });
    } catch (err) {
        console.error("Smart Recommendation Error:", err);
        res.status(500).json({ message: "Recommendation failed", error: err.message });
    }
}

// ----------------------------
// פונקציה משולבת: המלצה חכמה + דמיון משתמשים
// ----------------------------
async function getUnifiedRecommendations(req, res) {
    const { username } = req.params;

    try {
        const smart = await getSmartRecommendationsInternal(username);
        const collab = await getCollaborativeRecommendationsInternal(username);

        const merged = new Map();

        for (const rec of smart) {
            if (!merged.has(rec.itemName)) {
                merged.set(rec.itemName, { itemName: rec.itemName, score: 0, reason: [] });
            }
            const item = merged.get(rec.itemName);
            item.score += parseFloat(rec.score);
            item.reason.push(rec.reason);
        }

        for (const rec of collab) {
            if (!merged.has(rec.itemName)) {
                merged.set(rec.itemName, { itemName: rec.itemName, score: 0, reason: [] });
            }
            const item = merged.get(rec.itemName);
            item.score += parseFloat(rec.score);
            item.reason.push(rec.reason);
        }

        const result = Array.from(merged.values())
            .sort((a, b) => b.score - a.score)
            .slice(0, 10);

        res.json({ recommendations: result });
    } catch (err) {
        console.error("Unified Recommendation Error:", err);
        res.status(500).json({ message: "Recommendation failed", error: err.message });
    }
}
async function searchItems(req, res) {
    const query = req.query.query?.toLowerCase() || '';
    const filePath = path.resolve(__dirname, "../data/data.json");

    fs.readFile(filePath, "utf8", (err, data) => {
        if (err) {
            console.error("שגיאה בקריאת קובץ:", err);
            return res.status(500).json({ error: "שגיאה בקריאת הקובץ" });
        }

        try {
            const allItems = JSON.parse(data);
            const filtered = allItems.filter(item =>
                item.name?.toLowerCase().includes(query)
            );
            res.json(filtered);
        } catch (e) {
            console.error("שגיאה בניתוח JSON:", e);
            res.status(500).json({ error: "פורמט JSON לא תקין" });
        }
    });
}

async function loadDataIfEmpty() {
    try {
        const count = await Item.countDocuments();
        if (count === 0) {
            const filePath = path.join(__dirname, "../data/data.json");
            const rawData = fs.readFileSync(filePath, "utf8");
            const items = JSON.parse(rawData);
            await Item.insertMany(items);
            console.log("✅ data.json loaded into MongoDB");
        } else {
            console.log("ℹ️ Items already exist in database, skipping load");
        }
    } catch (err) {
        console.error("שגיאה בקריאת קובץ:", err);
    }
}

function getItemsFromFile(req, res) {
    const filePath = path.join(__dirname, '../data/Data.json');

    fs.readFile(filePath, 'utf8', (err, data) => {
        if (err) {
            console.error("❌ Error reading Data.json:", err);
            return res.status(500).json({ message: 'Failed to read items file' });
        }

        try {
            const items = JSON.parse(data);
            res.status(200).json(items);
        } catch (parseErr) {
            console.error("❌ JSON parse error:", parseErr);
            res.status(500).json({ message: 'Invalid JSON format' });
        }
    });
}

function getPaginatedItemsFromFile(req, res) {
    const offset = parseInt(req.query.offset) || 0;
    const limit = parseInt(req.query.limit) || 50;
    const filePath = path.resolve(__dirname, "../data/data.json");

    fs.readFile(filePath, "utf8", (err, data) => {
        if (err) {
            console.error("שגיאה בקריאת קובץ:", err);
            return res.status(500).json({ error: "שגיאה בקריאת הקובץ" });
        }

        try {
            const allItems = JSON.parse(data);
            const paginatedItems = allItems.slice(offset, offset + limit);
            res.json(paginatedItems);
        } catch (e) {
            console.error("שגיאה בניתוח JSON:", e);
            res.status(500).json({ error: "פורמט JSON לא תקין" });
        }
    });
}



async function storeFinalizedItems(itemsMap, groupName) {
    for (const [itemName, data] of Object.entries(itemsMap)) {
        const { quantity, category, imageUrl } = data;

        if (quantity > 0) {
            const finalized = new FinalizedItem({
                itemName,
                quantity,
                category,
                imageUrl,
                groupName,
                timestamp: new Date()
            });
            await finalized.save();
        }
    }
}

async function getUnboughtItems(req, res) {
    try {
        const items = await Item.find({ isBought: false });
        res.status(200).json(items);
    } catch (error) {
        res.status(500).json({ message: 'Server Error', error: error.message });
    }
}

async function getHistory(req, res) {
    try {
        const history = await History.find().sort({ timestamp: -1 });
        res.status(200).json(history);
    } catch (error) {
        res.status(500).json({ message: 'Server error', error: error.message });
    }
}

async function addHistory(req, res) {
    try {
        const {
            itemName, action, price, category, imageUrl,
            groupName, username, quantity
        } = req.body;

        const parsedQuantity = parseInt(quantity);

        const newHistory = new History({
            itemName,
            action,
            price,
            category,
            imageUrl,
            groupName,
            username,
            quantity: isNaN(parsedQuantity) ? 1 : parsedQuantity,
            date: new Date()
        });

        await newHistory.save();

        // 🚀 הפעלת אימון מודל דרך conda
const { spawn } = require("child_process");
const condaPath = "C:\\Users\\khali\\miniconda3\\Scripts\\conda.exe";
const py = spawn(condaPath, [
  "run", "--no-capture-output", "-n", "recsys", "python", "ml/train_model.py"
], { env: { ...process.env, PYTHONIOENCODING: "utf-8" } });
        py.stdout.on("data", data => console.log("ML Train:", data.toString()));
        py.stderr.on("data", data => console.error("ML Train Error:", data.toString()));
        py.on("close", (code) => {
            if (code === 0) {
                console.log("✅ Model training finished");
            } else {
                console.error("❌ Model training failed with code", code);
            }
        });

        res.status(201).json({ message: 'History entry added successfully' });

    } catch (error) {
        res.status(500).json({ message: 'Server error', error: error.message });
    }
}

async function getHistoryByGroup(req, res) {
    try {
        const { groupName } = req.params;
        const history = await History.find({ groupName }).sort({ timestamp: -1 });
        res.status(200).json(history);
    } catch (error) {
        res.status(500).json({ message: 'Server error', error: error.message });
    }
}

async function updateItemStatus(req, res) {
    try {
        const { name, isBought, groupName } = req.body;
        const item = await Item.findOne({ name, groupName });

        if (!item) {
            return res.status(404).json({ message: 'Item not found' });
        }

        item.isBought = isBought;
        await item.save();

        res.status(200).json({ message: 'Item status updated successfully', item });
    } catch (error) {
        res.status(500).json({ message: 'Server error', error: error.message });
    }
}

async function getFullHistory(req, res) {
    try {
        const history = await History.find().sort({ timestamp: -1 });
        res.status(200).json(history);
    } catch (error) {
        res.status(500).json({ message: 'Server error', error: error.message });
    }
}

async function getPopularItems(req, res) {
    const { groupName } = req.params;

    try {
        const topItems = await History.aggregate([
            { $match: { groupName, action: "bought" } },
            {
                $group: {
                    _id: "$itemName",
                    quantity: { $sum: "$quantity" },
                    imageUrl: { $first: "$imageUrl" },
                    category: { $first: "$category" }
                }
            },
            { $sort: { quantity: -1 } },
            { $limit: 5 }
        ]);

        const formatted = topItems.map(doc => ({
            itemName: doc._id,
            quantity: doc.quantity,
            imageUrl: doc.imageUrl,
            category: doc.category
        }));

        res.status(200).json(formatted);
    } catch (error) {
        res.status(500).json({ message: "Server error", error: error.message });
    }
}

async function getPopularFinalizedItems(req, res) {
    const { groupName } = req.params;

    try {
        const topItems = await FinalizedItem.aggregate([
            { $match: { groupName } },
            {
                $group: {
                    _id: "$itemName",
                    quantity: { $sum: "$quantity" },
                    category: { $first: "$category" }
                }
            },
            { $sort: { quantity: -1 } },
            { $limit: 5 }
        ]);

        const formatted = topItems.map(doc => ({
            itemName: doc._id,
            quantity: doc.quantity,
            category: doc.category
        }));

        res.status(200).json(formatted);
    } catch (error) {
        res.status(500).json({ message: "Server error", error: error.message });
    }
}
async function getRecommendations(req, res) {
    try {
        const items = await Item.find().limit(10);
        res.status(200).json({ recommendations: items });
    } catch (error) {
        res.status(500).json({ message: "Server error", error: error.message });
    }
}
module.exports = {
    loadDataIfEmpty,
    getItemsFromFile,
    getRecommendations,
    getPaginatedItemsFromFile,
    getSmartRecommendations,
    getUnboughtItems,
    getHistory,
    addHistory,
    getHistoryByGroup,
    updateItemStatus,
    getFullHistory,
    getPopularItems,
    getPopularFinalizedItems,
    searchItems,
    increaseRecommendationScore ,
    getUnifiedRecommendations
};
