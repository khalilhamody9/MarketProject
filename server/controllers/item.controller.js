const Item = require('../models/Item');
const History = require('../models/History');
const FinalizedItem = require('../models/FinalizedItem');
const { spawn } = require("child_process");
const Group = require('../models/Group');
const fs = require('fs');
const path = require('path');
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

async function getSmartRecommendations(req, res) {
    const { username } = req.params;

    try {
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

        const scored = [];
        for (const item of scoreMap.values()) {
            let score = item.userCount * 0.5 + item.groupCount * 0.3 + item.globalCount * 0.2;
            const daysSince = (now - item.lastPurchased) / (1000 * 60 * 60 * 24);
            if (daysSince > 14) score += 1.5;

            scored.push({
                itemName: item.itemName,
                score: score.toFixed(2),
                lastPurchased: item.lastPurchased,
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

        const filtered = scored.filter(i => !selectedMap[i.itemName] && i.daysSince > 3);
        filtered.sort((a, b) => b.score - a.score);

        res.json({ recommendations: filtered.slice(0, 10) });
    } catch (err) {
        console.error("❌ Recommendation error:", err);
        res.status(500).json({ message: "Recommendation failed", error: err.message });
    }
}

async function getRecommendations(req, res) {
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

module.exports = {
    loadDataIfEmpty,
    getItemsFromFile,
    getPaginatedItemsFromFile,
    getSmartRecommendations,
    getRecommendations,
    getUnboughtItems,
    getHistory,
    addHistory,
    getHistoryByGroup,
    updateItemStatus,
    getFullHistory,
    getPopularItems,
    getPopularFinalizedItems,
    searchItems
};