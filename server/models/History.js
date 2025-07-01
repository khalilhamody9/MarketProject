const mongoose = require('mongoose');

const HistorySchema = new mongoose.Schema({
    itemName: { type: String, required: true },
    imageUrl: { type: String, required: true },
    action: { type: String, required: true },
    category: { type: String, required: true },
    groupName: { type: String, required: true },
    username: { type: String, required: true },
    barcode: { type: Number, required: true },
    quantity: { type: Number, required: false, default: 1 }, 
    date: { type: Date, default: Date.now }
});

module.exports = mongoose.model('History', HistorySchema);
