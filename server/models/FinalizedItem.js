const mongoose = require('mongoose');


const finalizedItemSchema = new mongoose.Schema({
    itemName: String,
    quantity: Number,
    category: String,
    groupName: String,
    timestamp: Date
});

module.exports = mongoose.model('FinalizedItem', finalizedItemSchema);

module.exports = mongoose.model('FinalizedItem', finalizedItemSchema);
