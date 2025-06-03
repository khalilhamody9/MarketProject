// routes/groupRequests.js
const express = require('express');
const router = express.Router();
const GroupRequest = require('../models/GroupRequest');
const Group = require('../models/Group');

// Request to Join Group
router.post('/request', async (req, res) => {
    const { groupName, username } = req.body;

    try {
        const existingRequest = await GroupRequest.findOne({ groupName, username });
        if (existingRequest) {
            return res.status(400).json({ message: "Request already exists" });
        }

        const newRequest = new GroupRequest({
            groupName,
            username
        });

        await newRequest.save();
        res.status(201).json({ message: "Join request sent" });
    } catch (error) {
        res.status(500).json({ message: "Server error" });
    }
});

// Get Pending Requests for a Group
router.get('/pending/:groupName/:username', async (req, res) => {
    const { groupName, username } = req.params;

    try {
        const requests = await GroupRequest.find({ 
            groupName, 
            status: 'pending', 
            username: username  // Make sure this field is correct in your schema
        });
        res.status(200).json({ requests });
    } catch (error) {
        console.error(error);
        res.status(500).json({ message: "Server error" });
    }
});

// Handle Request (Approve/Deny)
router.post('/handle', async (req, res) => {
    const { requestId, action } = req.body;

    try {
        const request = await GroupRequest.findById(requestId);

        if (!request) {
            return res.status(404).json({ message: "Request not found" });
        }

        if (action === 'approve') {
            // Add user to group if approved
            const group = await Group.findOne({ groupName: request.groupName });

            if (group) {
                if (group.members.length < group.maxUsers) {
                    // Check if user is already a member
                    if (!group.members.includes(request.username)) {
                        group.members.push(request.username);
                        await group.save();
                    }
                    request.status = 'approved';
                    await request.save();
                } else {
                    return res.status(400).json({ message: "Group is full" });
                }
            } else {
                return res.status(404).json({ message: "Group not found" });
            }
        } else if (action === 'deny') {
            request.status = 'denied';
            await request.save();
        }

        // **Remove the request after approval/denial**
        await GroupRequest.findByIdAndDelete(requestId);

        res.status(200).json({ message: `Request ${request.status}` });
    } catch (error) {
        res.status(500).json({ message: "Server error" });
    }
});

module.exports = router;
