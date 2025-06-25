const express = require('express');
const router = express.Router();
const Group = require('../models/Group'); 
const GroupRequest = require('../models/GroupRequest');
const User = require('../models/User');
const FinalizedItem = require('../models/FinalizedItem');


// Get Pending Requests for a Group
router.get('/pending/:groupName/:username', async (req, res) => {
    const { groupName, username } = req.params;

    try {
        const requests = await GroupRequest.find({ 
            groupName, 
            status: 'pending'
        });
        res.status(200).json({ requests });
    } catch (error) {
        console.error(error);
        res.status(500).json({ message: "Server error" });
    }
});

// Add User to Group
router.post('/addUserToGroup', async (req, res) => {
    const { groupName, username, adminName } = req.body;

    try {
        // Find the group
        const group = await Group.findOne({ groupName });

        if (!group) {
            return res.status(404).json({ message: "Group not found" });
        }

        // Check if the requester is the admin
        if (group.adminName !== adminName) {
            return res.status(403).json({ message: "Only the admin can add users" });
        }

        // Check if user is already in the group
        if (group.members.includes(username)) {
            return res.status(400).json({ message: "User already in the group" });
        }

        // Check if group is full
        if (group.members.length >= group.maxUsers) {
            return res.status(400).json({ message: "Group is full" });
        }

        // Add user to group
        group.members.push(username);
        // Add the group to the user's groups array
        await User.findOneAndUpdate(
            { username: username },
            { $addToSet: { groups: groupName } }
        );       
        await group.save();

        res.status(200).json({ message: "User added to group" });
    } catch (error) {
        console.error(error);
        res.status(500).json({ message: "Server error" });
    }
});

// Get Members by Group Name
router.get('/members/:groupName', async (req, res) => {
    try {
        const groupName = req.params.groupName;
        const group = await Group.findOne({ groupName });

        if (!group) {
            return res.status(404).json({ message: 'Group not found' });
        }

        res.status(200).json({ members: group.members });
    } catch (error) {
        res.status(500).json({ message: 'Server Error', error: error.message });
    }
});

// Get User's Groups
router.get('/getUserGroups/:username', async (req, res) => {
    const { username } = req.params;

    try {
        // Find the user and get their groups
        const user = await User.findOne({ username });

        if (!user) {
            return res.status(404).json({ message: "User not found" });
        }

        res.status(200).json({ groups: user.groups });
    } catch (error) {
        console.error('Error getting user groups:', error);
        res.status(500).json({ message: "Server error" });
    }
});

// Delete User from Group
router.post('/deleteUserFromGroup', async (req, res) => {
    const { username, groupName, adminName } = req.body;

    try {
        const group = await Group.findOne({ groupName });

        if (!group) {
            return res.status(404).json({ message: "Group not found" });
        }

        // Check if the requester is the admin
        if (group.adminName !== adminName) {
            return res.status(403).json({ message: "Only the admin can remove users" });
        }

        // Check if the user is in the group
        if (!group.members.includes(username)) {
            return res.status(400).json({ message: "User not in group" });
        }

        // Remove user from group
        group.members = group.members.filter(member => member !== username);
        await group.save();

        res.status(200).json({ message: "User deleted successfully" });
    } catch (error) {
        res.status(500).json({ message: "Server error" });
    }
});

// Create Group
router.post('/create', async (req, res) => {
    const { groupName, maxUsers, adminName } = req.body;

    try {
        // Check if the group name already exists
        const existingGroup = await Group.findOne({ groupName });
        if (existingGroup) {
            return res.status(409).json({ message: 'Group name already exists' });
        }

        // Create the new group
        const newGroup = new Group({
            groupName,
            maxUsers,
            adminName,
            members: [adminName]  // Add admin as the first member
        });
        await newGroup.save();

        // Add the group to the admin's groups array
        await User.findOneAndUpdate(
            { username: adminName },
            { $addToSet: { groups: groupName } }
        );

        res.status(201).json({ message: 'Group created successfully' });
    } catch (err) {
        console.error('Error creating group:', err);
        res.status(500).json({ message: 'Server error', error: err.message });
    }
});

// Check if Group Name exists
router.get('/check/:groupName', async (req, res) => {
    const groupName = req.params.groupName;
    try {
        const group = await Group.findOne({ groupName });
        if (group) {
            res.json({ message: "Group name valid" });
        } else {
            res.json({ message: "Invalid group name" });
        }
    } catch (error) {
        res.status(500).json({ message: "Server error" });
    }
});

// Check if User is in Group
router.get('/checkUserInGroup/:username/:groupName', async (req, res) => {
    const { username, groupName } = req.params;
    try {
        // Check if the group exists and the user is in the members list
        const group = await Group.findOne({ groupName });
        if (group && (group.members.includes(username) || group.adminName === username)) {
            res.json({ message: "User in group" });
        } else {
            res.json({ message: "User not in group" });
        }
    } catch (error) {
        res.status(500).json({ message: "Server error" });
    }
});

// Check if User is Admin
router.get('/checkAdmin', async (req, res) => {
    const { username, groupName } = req.query;

    try {
        const group = await Group.findOne({ groupName });

        if (!group) {
            return res.status(404).json({ message: "Group not found" });
        }

        // Check if the username is the admin
        if (group.adminName === username) {
            res.status(200).json({ isAdmin: true });
        } else {
            res.status(200).json({ isAdmin: false });
        }
    } catch (error) {
        res.status(500).json({ message: 'Server error', error });
    }
});



// Save Group Changes
router.post('/saveChanges', async (req, res) => {
    const { groupName, selectedItems, username } = req.body;

    try {
        const group = await Group.findOne({ groupName });

        if (!group) {
            return res.status(404).json({ message: "Group not found" });
        }

        // Save the changes
        group.selectedItems = selectedItems;
        await group.save();

        res.status(200).json({ message: "Changes saved successfully" });
    } catch (error) {
        res.status(500).json({ message: "Server error", error });
    }
});


// Get Pending Requests
router.get('/pendingRequests', async (req, res) => {
    const { groupName } = req.query;

    try {
        const requests = await GroupRequest.find({ groupName, status: 'pending' });
        res.json({ requests });
    } catch (error) {
        res.status(500).json({ message: 'Server error', error });
    }
});

// Handle Join Request (Approve/Deny)
router.post('/handleRequest', async (req, res) => {
    const { requestId, action } = req.body;

    try {
        const request = await GroupRequest.findById(requestId);
        if (!request) {
            return res.status(404).json({ message: 'Request not found' });
        }

        if (action === 'approve') {
            request.status = 'approved';
            await request.save();

            // Add user to the group
            const group = await Group.findOne({ groupName: request.groupName });
            group.members.push(request.username);
            await group.save();

            res.json({ message: 'User approved and added to group' });
        } else if (action === 'deny') {
            request.status = 'denied';
            await request.save();
            res.json({ message: 'User request denied' });
        } else {
            res.status(400).json({ message: 'Invalid action' });
        }
    } catch (error) {
        res.status(500).json({ message: 'Server error', error });
    }
});
// routes/groups.js
router.get('/:groupName/members', async (req, res) => {
    try {
        const groupName = req.params.groupName;
        const group = await Group.findOne({ groupName });

        if (!group) {
            return res.status(404).json({ message: 'Group not found' });
        }

        res.status(200).json(group.members);
    } catch (error) {
        res.status(500).json({ message: 'Server Error', error: error.message });
    }
});

router.post('/:groupName/favorites', async (req, res) => {
    const { shopName } = req.body;
    const { groupName } = req.params;

    try {
        const group = await Group.findOneAndUpdate(
            { groupName },
            { $addToSet: { favoriteStores: shopName } },
            { new: true }
        );

        if (!group) return res.status(404).json({ message: "Group not found" });

        res.json({ message: "Shop added to favorites", favoriteStores: group.favoriteStores });
    } catch (error) {
        res.status(500).json({ message: "Server error", error });
    }
});
router.get('/:groupName/favorites', async (req, res) => {
    try {
        const group = await Group.findOne({ groupName: req.params.groupName });

        if (!group) return res.status(404).json({ message: "Group not found" });

        res.json({ favoriteStores: group.favoriteStores || [] });
    } catch (error) {
        res.status(500).json({ message: "Server error", error });
    }
});

router.delete('/:groupName/favorites', async (req, res) => {
    const { shopName } = req.body;
    const { groupName } = req.params;

    try {
        const group = await Group.findOneAndUpdate(
            { groupName },
            { $pull: { favoriteStores: shopName } },
            { new: true }
        );

        if (!group) return res.status(404).json({ message: "Group not found" });

        res.json({ message: "Shop removed from favorites", favoriteStores: group.favoriteStores });
    } catch (error) {
        res.status(500).json({ message: "Server error", error });
    }
});

// Save or Clear Selected Items
router.post('/saveSelectedItems', async (req, res) => {
    const { groupName, selectedItems, username, clearList, imageUrls = {}, categories = {} } = req.body;

    try {
        const group = await Group.findOne({ groupName });

        if (!group) {
            return res.status(404).json({ message: "Group not found" });
        }

        // ✅ If admin clears the list (i.e., finalizes it)
        if (clearList && group.adminName === username) {
            group.selectedItems = {};

            // Save finalized items to the new collection
            for (const [itemName, quantity] of Object.entries(selectedItems)) {
                if (quantity > 0) {
                    const finalized = new FinalizedItem({
                        itemName,
                        quantity,
                        category: categories[itemName] || "",
                        groupName,
                        timestamp: new Date()
                    });

                    await finalized.save();
                }
            }

            // Clear the list
            group.selectedItems = [];
        } else {
            // Save regular selected items
            group.selectedItems = selectedItems;
        }

        await group.save();

        res.status(200).json({ message: "Changes saved successfully" });
    } catch (error) {
        console.error("Error saving selected items:", error);
        res.status(500).json({ message: "Server error", error });
    }
});

// Get Selected Items for Group
router.get('/getSelectedItems/:groupName', async (req, res) => {
    const { groupName } = req.params;

    try {
        const group = await Group.findOne({ groupName });

        if (!group) {
            return res.status(404).json({ message: "Group not found" });
        }

        res.status(200).json(group.selectedItems || {});
    } catch (error) {
        res.status(500).json({ message: "Server error", error });
    }
});


module.exports = router;
