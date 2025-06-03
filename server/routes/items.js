const express = require('express');
const router = express.Router();
const Item = require('../models/Item');

// Get All Items
router.get('/', async (req, res) => {
    try {
        const items = await Item.find();
        res.json(items);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// Get Items by Category and Group Name
router.get('/:category/:groupName', async (req, res) => {
    try {
        const items = await Item.find({ 
            category: req.params.category, 
            groupName: req.params.groupName 
        });
        res.json(items);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// Add New Item
router.post('/', async (req, res) => {
    const item = new Item({
        name: req.body.name,
        //price: req.body.price,
        category: req.body.category,
        imageUrl: req.body.imageUrl,
        groupName: req.body.groupName  // Added groupName to associate with a group
    });

    try {
        const newItem = await item.save();
        res.status(201).json(newItem);
    } catch (err) {
        res.status(400).json({ message: err.message });
    }
});

// Delete Item
router.delete('/:id', async (req, res) => {
    try {
        await Item.findByIdAndDelete(req.params.id);
        res.json({ message: 'Item deleted' });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});
// GET /api/items/popular/:groupName
router.get('/popular/:groupName', async (req, res) => {
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

        // Rename _id to itemName for Android compatibility
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
});


module.exports = router;

