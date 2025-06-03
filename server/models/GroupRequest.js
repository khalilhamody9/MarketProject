// models/GroupRequest.js
const mongoose = require('mongoose');

const GroupRequestSchema = new mongoose.Schema({
    groupName: { type: String, required: true },
    username: { type: String, required: true },
    status: { type: String, default: 'pending' }  // 'pending', 'approved', 'denied'
});

module.exports = mongoose.model('GroupRequest', GroupRequestSchema);
