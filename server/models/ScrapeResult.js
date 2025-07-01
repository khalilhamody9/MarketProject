// models/ScrapeResult.js
const mongoose = require('mongoose');

const ShopSchema = new mongoose.Schema({
  name: String,
  total: Number,
  address: String,
  products: [{
    name: String,
    price: String,
    discount: String,
    barcode: Number
  }]
}, { _id: false });

const ScrapeResultSchema = new mongoose.Schema({
  groupName: { type: String, required: true },
  city: String,
  location: {
    latitude: Number,
    longitude: Number
  },
  timestamp: { type: Date, default: Date.now },
  shops: [ShopSchema]
});

module.exports = mongoose.model('ScrapeResult', ScrapeResultSchema);
