const mongoose = require('mongoose');

const ItemSchema = new mongoose.Schema({
    name: {
        type: String,
        required: true
    },

    category: {
        type: String,
        required: true
    },
    imageUrl: {
        type: String,
        required: true
    },
    isBought: {
        type: Boolean,
        default: false // New field for bought status
    },
    groupName: {
        type: String,
        required: true // Added groupName to associate items with a group
    }
});

module.exports = mongoose.model('Item', ItemSchema);
