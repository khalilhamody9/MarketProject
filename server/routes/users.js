const express = require('express');
const router = express.Router();
const User = require('../models/User');
// Use bcryptjs instead of bcrypt
const bcrypt = require('bcryptjs');

// Register User
router.post('/register', async (req, res) => {
    const { username, email, password, groupName } = req.body;

    try {
        const existingUser = await User.findOne({ username });
        if (existingUser) {
            return res.status(400).json({ message: "Username already exists" });
        }

        const hashedPassword = await bcrypt.hash(password, 10);

        const newUser = new User({
            username,
            email,
            password: hashedPassword,
            groups: groupName ? [groupName] : [] // Associate user with group if provided
        });

        await newUser.save();
        res.status(201).json({ message: "User registered successfully" });
    } catch (error) {
        res.status(500).json({ message: "Server error" });
    }
});

// Login User
router.post('/login', async (req, res) => {
    const { username, password } = req.body;

    try {
        const user = await User.findOne({ username });
        if (!user) {
            return res.status(400).json({ message: "Invalid username or password" });
        }

        const isMatch = await bcrypt.compare(password, user.password);
        if (!isMatch) {
            return res.status(400).json({ message: "Invalid username or password" });
        }

        res.status(200).json({ 
            message: "Login successful",
            username: user.username,
            groups: user.groups // Return user's groups for session management
        });
    } catch (error) {
        res.status(500).json({ message: "Server error" });
    }
});

// Get User's Groups
router.get('/:username/groups', async (req, res) => {
    const { username } = req.params;

    try {
        const user = await User.findOne({ username });

        if (!user) {
            return res.status(404).json({ message: "User not found" });
        }

        res.status(200).json({ groups: user.groups });
    } catch (error) {
        res.status(500).json({ message: "Server error" });
    }
});

// Add User to Group
router.post('/addToGroup', async (req, res) => {
    const { username, groupName } = req.body;

    try {
        // Check if the user exists
        const user = await User.findOne({ username });
        if (!user) {
            return res.status(404).json({ message: "User not found" });
        }

        // Add the group to the user's groups array
        await User.findOneAndUpdate(
            { username: username },
            { $addToSet: { groups: groupName } }
        );

        res.status(200).json({ message: "User added to group" });
    } catch (error) {
        res.status(500).json({ message: "Server error" });
    }
});

// Remove User from Group
router.post('/removeFromGroup', async (req, res) => {
    const { username, groupName } = req.body;

    try {
        // Check if the user exists
        const user = await User.findOne({ username });
        if (!user) {
            return res.status(404).json({ message: "User not found" });
        }

        // Remove the group from the user's groups array
        await User.findOneAndUpdate(
            { username: username },
            { $pull: { groups: groupName } }
        );

        res.status(200).json({ message: "User removed from group" });
    } catch (error) {
        res.status(500).json({ message: "Server error" });
    }
});

module.exports = router;
