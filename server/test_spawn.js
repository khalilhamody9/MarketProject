const { spawn } = require("child_process");
const path = require("path");

const username = "khalil"; // החלף בשם המשתמש שלך
const scriptPath = path.join(__dirname, "ml", "predict_model.py");
const condaPath = "C:\\Users\\khali\\miniconda3\\Scripts\\conda.exe";

console.log("🚀 Running predict_model.py directly...");
const py = spawn(condaPath, [
  "run", "--no-capture-output", "-n", "recsys", "python", scriptPath, username
], { env: { ...process.env, PYTHONIOENCODING: "utf-8" } });

let output = "";

py.stdout.on("data", (data) => {
  const text = data.toString();
  output += text;
  console.log("📤 STDOUT:", text);
});

py.stderr.on("data", (data) => {
  console.error("❌ STDERR:", data.toString());
});

py.on("close", (code) => {
  console.log("🔚 Process exited with code", code);
  try {
    const parsed = JSON.parse(output);
    console.log("✅ Parsed JSON:", parsed);
  } catch (err) {
    console.error("❌ Failed to parse JSON:", output);
  }
});
