const Item = require('../models/Item');
const History = require('../models/History');
const FinalizedItem = require('../models/FinalizedItem');
const { spawn } = require("child_process");

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
