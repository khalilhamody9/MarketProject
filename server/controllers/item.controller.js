const Item = require('../models/Item');
const History = require('../models/History');
const FinalizedItem = require('../models/FinalizedItem');
const { spawn } = require("child_process");
const Group = require('../models/Group');

// controller/recommendation.controller.js
exports.getSmartRecommendations = async (req, res) => {
    const { username } = req.params;

    try {
        // שלב 1: שליפת קבוצות של המשתמש
        const userGroups = await Group.find({ members: username });
        const groupNames = userGroups.map(g => g.groupName);

        // שלב 2: שליפת רכישות
        const allHistory = await History.find({});
        const now = new Date();

        // שלב 3: בניית שכבות מידע
        const scoreMap = new Map(); // itemName -> score info

        for (const entry of allHistory) {
            const name = entry.itemName;
            const key = name;

            if (!scoreMap.has(key)) {
                scoreMap.set(key, {
                    itemName: name,
                    lastPurchased: entry.date,
                    userCount: 0,
                    groupCount: 0,
                    globalCount: 0,
                    totalDaysBetween: 0,
                    intervalCount: 0
                });
            }

            const data = scoreMap.get(key);

            // עדכון אחרון קנייה
            if (entry.date > data.lastPurchased) {
                data.lastPurchased = entry.date;
            }

            // ניקוד לפי רמה
            if (entry.username === username) {
                data.userCount++;
            } else if (groupNames.includes(entry.groupName)) {
                data.groupCount++;
            } else {
                data.globalCount++;
            }
        }

        // שלב 4: ניקוד כולל
        const scored = [];
        for (const [_, item] of scoreMap) {
            const score =
                item.userCount * 0.5 +
                item.groupCount * 0.3 +
                item.globalCount * 0.2;

            // בונוס אם עבר זמן רב
            const daysSince = (now - item.lastPurchased) / (1000 * 60 * 60 * 24);
            if (daysSince > 14) {
                score += 1.5;
            }

            scored.push({
                itemName: item.itemName,
                score: score.toFixed(2),
                lastPurchased: item.lastPurchased,
                reason: `נרכש ${item.userCount} פעמים אישית, ${item.groupCount} קבוצתי, ${item.globalCount} כללית`,
                daysSince: Math.floor(daysSince)
            });
        }

        // שלב 5: סינון מוצרים שנרכשו ממש עכשיו או קיימים ברשימת הקבוצה
        const selectedMap = userGroups.reduce((map, group) => {
            for (const [item, qty] of group.selectedItems.entries()) {
                map[item] = true;
            }
            return map;
        }, {});

        const filtered = scored.filter(i => {
            return !selectedMap[i.itemName] && i.daysSince > 3;
        });

        // שלב 6: החזרת ההמלצות
        filtered.sort((a, b) => b.score - a.score);

        res.json({ recommendations: filtered.slice(0, 10) });
    } catch (err) {
        console.error("❌ Recommendation error:", err);
        res.status(500).json({ message: "Recommendation failed", error: err.message });
    }
};

exports.getRecommendations = async (req, res) => {
    const { groupName } = req.params;

    try {
        const allItems = await FinalizedItem.find();
        console.log("🔄 Sending finalized items to Python:", allItems);

        const py = spawn("python", ["ml/recommend.py", groupName]);
        let output = "";

        py.stdout.on("data", (data) => {
            output += data.toString();
        });

        py.stderr.on("data", (err) => {
            console.error("Python error:", err.toString());
        });

        py.on("close", (code) => {
            if (code !== 0) {
                return res.status(500).json({ message: "Python script failed" });
            }

            try {
                const result = JSON.parse(output);
                res.json({ recommendations: result });
            } catch (e) {
                res.status(500).json({ message: "Invalid Python output" });
            }
        });

        py.stdin.write(JSON.stringify(allItems));
        py.stdin.end();
    } catch (error) {
        res.status(500).json({ message: "Server error", error });
    }
};
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

// Get All Unbought Items
exports.getUnboughtItems = async (req, res) => {
    try {
        const items = await Item.find({ isBought: false });
        res.status(200).json(items);
    } catch (error) {
        res.status(500).json({ message: 'Server Error', error: error.message });
    }
};

// Get History Sorted by Timestamp (Descending)
exports.getHistory = async (req, res) => {
    try {
        const history = await History.find().sort({ timestamp: -1 });
        res.status(200).json(history);
    } catch (error) {
        res.status(500).json({ message: 'Server error', error: error.message });
    }
};

// Add History Entry
exports.addHistory = async (req, res) => {
    try {
        const { itemName, action, price, category, imageUrl, groupName, username } = req.body;
        const newHistory = new History({
            itemName,
            action,
            price,
            category,
            imageUrl,
            groupName,
            username,
            timestamp: new Date()
        });

        await newHistory.save();
        res.status(201).json({ message: 'History entry added successfully' });
    } catch (error) {
        res.status(500).json({ message: 'Server error', error: error.message });
    }
};

// Get History by Group
exports.getHistoryByGroup = async (req, res) => {
    try {
        const { groupName } = req.params;
        const history = await History.find({ groupName }).sort({ timestamp: -1 });
        res.status(200).json(history);
    } catch (error) {
        res.status(500).json({ message: 'Server error', error: error.message });
    }
};

// Update Item Status
exports.updateItemStatus = async (req, res) => {
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
};

// Full History
exports.getFullHistory = async (req, res) => {
    try {
        const history = await History.find().sort({ timestamp: -1 });
        res.status(200).json(history);
    } catch (error) {
        res.status(500).json({ message: 'Server error', error: error.message });
    }
};

// ✅ Popular Items
exports.getPopularItems = async (req, res) => {
    const { groupName } = req.params;

    try {
        const topItems = await History.aggregate([
            { $match: { groupName: groupName, action: "bought" } },
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
        console.error("Error fetching popular items:", error);
        res.status(500).json({ message: "Server error", error: error.message });
    }
};

exports.getPopularFinalizedItems = async (req, res) => {
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
};