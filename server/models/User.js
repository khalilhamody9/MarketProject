const mongoose = require('mongoose');

// User Schema
const UserSchema = new mongoose.Schema({
    email: { type: String, required: true, unique: true },
    username: { type: String, required: true, unique: true },
    password: { type: String, required: true },
    groups: [{ type: String }]  // Array to store group names
}, { timestamps: true });

// User Model
const User = mongoose.model('User', UserSchema);

module.exports = User;
