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
    barcode: { type: Number },  // <-- הוספת שדה ברקוד

    score: {
        type: Number,
        default: 0 // שדה חדש לניקוד המלצות
    }
});

module.exports = mongoose.model('Item', ItemSchema);
