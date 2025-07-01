# 🛒 Smart Market Project

This is a full-stack shopping assistant app that helps users manage shopping lists, compare prices, and get intelligent product recommendations. The system consists of two main parts:

- **`app/`** – The Android application (built with Java/Kotlin)
- **`server/`** – The backend server (built with Node.js and integrated Python scripts using Anaconda and Scikit-learn)

---

## 📁 Project Structure

root/
│
├── app/ # Android mobile application (client)
│
└── server/ # Node.js backend server
├── ml/ # Python machine learning scripts
├── routes/ # Express API routes
├── models/ # MongoDB models (Mongoose)
└── ...


## 📱 Android App (`app/`)

The `app/` folder contains the Android application code. Main features include:

- User/group registration and login
- Shared shopping list per group
- Smart item suggestions based on purchase history
- Price comparison across nearby stores
- Favorite store management
- Clean and modern UI

---

## 🧠 Server (`server/`)

The `server/` folder is a Node.js + Express backend with:

- MongoDB database integration
- RESTful API endpoints
- Python scripts for:
  - Machine learning (recommendation system using SVD)
  - Price scraping
- Integration with Anaconda (Python environment with `scikit-learn`, `surprise`, etc.)

### Requirements

#### Node.js
- Express
- Mongoose
- Axios
- Body-parser
- CORS

#### Python (inside Anaconda)
- `scikit-learn`
- `surprise`
- `pandas`
- `pymongo`

---

## ⚙️ Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git
cd YOUR_REPO_NAME
2. Backend Setup (server/)
Install Node dependencies:
bash
Copy
Edit
cd server
npm install
Setup Conda environment:
bash
Copy
Edit
conda create -n recsys python=3.10
conda activate recsys
pip install -r requirements.txt
If requirements.txt doesn't exist, install manually:

bash
Copy
Edit
pip install pandas scikit-learn pymongo scikit-surprise
Start the server:
bash
Copy
Edit
node index.js
3. Android App Setup (app/)
Open the app/ folder in Android Studio.

Ensure you have an emulator or physical device connected.

Click Run ▶️

🔁 ML Training & Prediction
Training script: server/ml/train_model.py

Prediction script: server/ml/predict_model.py

These are called automatically when new purchase data is added.

🧪 Features in Development
Smart cache to reduce scraping

Enhanced collaborative filtering

Push notifications for group updates

📫 Contact
For questions, suggestions, or contributions:
Khalil Hamody
Ibrahem kewan
📧 khalilhamody69@gmail.com
📧 ibrahem.kewan@gmail.com


📝 License
This project is licensed under the MIT License.


Let me know if you'd like me to auto-generate the `requirements.txt` file for the Python part, or w
