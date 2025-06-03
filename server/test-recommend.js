const { spawn } = require("child_process");

const py = spawn("python", ["ml/recommend.py", "yy"]);

let output = "";

py.stdout.on("data", (data) => {
    output += data.toString();
});

py.stderr.on("data", (err) => {
    console.error("PYTHON ERROR:", err.toString());
});

py.on("close", (code) => {
    console.log("Script exited with code", code);
    console.log("OUTPUT:", output);
});

py.stdin.write(JSON.stringify([
    {
      itemName: "Cola",
      quantity: 1,
      category: "Groceries",
      groupName: "other_group",
      timestamp: "2025-03-20T15:00:00Z"
    },
    {
      itemName: "Milk",
      quantity: 2,
      category: "Groceries",
      groupName: "other_group",
      timestamp: "2025-03-21T12:00:00Z"
    },
    {
      itemName: "Bread",
      quantity: 1,
      category: "Groceries",
      groupName: "yy",
      timestamp: "2025-03-22T10:00:00Z"
    }
  ]));
  
  py.stdin.end();
