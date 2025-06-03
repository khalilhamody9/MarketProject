const mongoose = require('mongoose');

const productSchema = new mongoose.Schema({
  שם: String,
  מחיר: String,
  מבצע: String
});

const shopResultSchema = new mongoose.Schema({
  groupName: String,
  timestamp: { type: Date, default: Date.now },
  shopKey: String,
  products: [productSchema],
  total_price: Number
});

module.exports = mongoose.model('ShopResult', shopResultSchema);
