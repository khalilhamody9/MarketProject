const express = require('express');
const fs = require('fs');
const csv = require('csv-parser');
const router = express.Router();

// קריאה מה-CSV והחזרת כל המוצרים
router.get('/', (req, res) => {
    const results = [];
    fs.createReadStream('products.csv', { encoding: 'utf8' }) // ודא שהקובץ בתיקייה הראשית של השרת
        .pipe(csv())
        .on('data', (data) => {
            // בחר עמודות רלוונטיות בלבד
            results.push({
                name: data['שם מוצר'] || '',
                category: data['category'] || '',
                imageUrl: data['imageUrl'] || '', // ניתן לשים "" או URL ברירת מחדל
                barcode: data['ברקוד'] || ''
            });
        })
        .on('end', () => {
            res.json(results);
        })
        .on('error', (err) => {
            console.error('❌ שגיאה בקריאת products.csv:', err);
            res.status(500).json({ message: 'שגיאה בקריאת הקובץ' });
        });
});

module.exports = router;
