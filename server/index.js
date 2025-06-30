const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const app = express();

// Middleware
app.use(cors());
app.use(express.urlencoded({ extended: true }));
app.use(express.json());
const { loadDataIfEmpty, loadDataWithBarcode } = require('./controllers/item.controller'); // שנה נתיב אם צריך

// MongoDB Connection
mongoose.connect('mongodb://localhost:27017/market_db')
.then(async () => {
    console.log('MongoDB connected');
    // כאן קוראים לפונקציה שמטענת את הנתונים
  //  await loadDataWithBarcode();
})
.catch((err) => console.error('MongoDB connection error:', err));

// Routes
const userRoutes = require('./routes/users');
const groupRoutes = require('./routes/groups');
const groupRequestRoutes = require('./routes/groupRequests');
const itemRoutes = require('./routes/item.routes');
const scrapeRoute = require('./routes/scrape');
app.use('/api/scrape', scrapeRoute);
// Register Routes
app.use('/api/users', userRoutes);
app.use('/api/groups', groupRoutes);
app.use('/api/groupRequests', groupRequestRoutes);
app.use('/api/items', itemRoutes);
const shopProductsRoute = require('./routes/shopProducts');
app.use('/api/shop_products', shopProductsRoute);
const scrapeRoutes = require("./routes/scrape");
app.use("/api", scrapeRoutes);
const mlRoutes = require('./routes/recommendations');
app.use('/api', mlRoutes); // זה יאפשר ל-route שלך להגיב על /api/ml/:username

// Default Route
app.get('/', (req, res) => {
    res.send('Welcome to the Market API');
});

// 404 Handler
app.use((req, res) => {
    res.status(404).json({ message: 'Endpoint not found' });
});

const PORT = process.env.PORT || 5000;
app.listen(PORT, '0.0.0.0', () => {
    console.log(`✅ Server running and accessible at http://0.0.0.0:${PORT}`);
});
