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
        default: false
    },
    groupName: {
        type: String,
        required: true
    },
    score: {
        type: Number,
        default: 0 // שדה חדש לניקוד המלצות
    }
});

module.exports = mongoose.model('Item', ItemSchema);
