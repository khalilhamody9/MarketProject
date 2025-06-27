const express = require('express');
const router = express.Router();
const { spawn } = require('child_process');

router.get('/ml/:username', (req, res) => {
    const username = req.params.username;
    console.log("📩 Received request to predict for user:", username);

    const py = spawn('C:\\Users\\khali\\miniconda3\\envs\\recsys\\python.exe', ['ml/predict_model.py', username]);

    let output = '';
    py.stdout.on('data', data => output += data.toString());
    py.stderr.on('data', data => console.error('ML Error:', data.toString()));

    py.on('close', (code) => {
        if (code !== 0) {
            return res.status(500).json({ message: 'Python script failed' });
        }

        try {
            const recommendations = JSON.parse(output);
            res.json({ recommendations });
        } catch (err) {
            console.error("❌ Failed to parse JSON from Python:", output);
            res.status(500).json({ message: 'Failed to parse Python output', error: err.message });
        }
    });
});

module.exports = router;
