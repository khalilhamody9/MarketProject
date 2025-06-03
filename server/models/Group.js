
const mongoose = require('mongoose');

const GroupSchema = new mongoose.Schema({
    groupName: { type: String, required: true, unique: true },
    maxUsers: { type: Number, required: true },
    adminName: { type: String, required: true },
    members: { type: [String], default: [] },
    selectedItems: { type: Map, of: Number, default: {} }  // Store item names as keys
});


module.exports = mongoose.model('Group', GroupSchema);
